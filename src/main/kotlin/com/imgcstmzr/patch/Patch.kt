package com.imgcstmzr.patch

import com.imgcstmzr.cli.PATCH_DECORATION_FORMATTER
import com.imgcstmzr.libguestfs.GuestfishCommandLine
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommandsBuilder
import com.imgcstmzr.libguestfs.GuestfishDsl
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.CustomizationsBuilder
import com.imgcstmzr.libguestfs.VirtCustomizeDsl
import com.imgcstmzr.os.DiskPath
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystemProcess
import com.imgcstmzr.os.Program
import com.imgcstmzr.os.boot
import koodies.asString
import koodies.builder.BooleanBuilder
import koodies.builder.BuilderTemplate
import koodies.builder.Init
import koodies.builder.build
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.io.path.deleteDirectoryEntriesRecursively
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.Banner.banner
import koodies.text.LineSeparators
import koodies.text.Semantics.formattedAs
import koodies.text.withRandomSuffix
import koodies.tracing.CurrentSpan
import koodies.tracing.rendering.BlockStyles.Dotted
import koodies.tracing.rendering.BlockStyles.Solid
import koodies.tracing.rendering.Renderable
import koodies.tracing.rendering.ReturnValues
import koodies.tracing.rendering.spanningLine
import koodies.tracing.spanning
import koodies.tracing.tracing
import koodies.unit.Size
import java.nio.file.Path
import kotlin.io.path.isRegularFile

/**
 * A patch to customize an [OperatingSystemImage]
 * by applying a range of operations on it.
 */
interface Patch {

    /**
     * Name of the patch.
     */
    val name: CharSequence

    /**
     * Number of operations this patch encompassed.
     */
    val operationCount: Int

    /**
     * Whether this patch has no operations.
     */
    val isEmpty: Boolean get() = operationCount == 0

    /**
     * Whether this patch has at least one operation.
     */
    val isNotEmpty: Boolean get() = !isEmpty

    /**
     * Applies this patch to the given [osImage].
     *
     * Detailed logging can be activated using [trace].
     */
    fun patch(osImage: OperatingSystemImage, trace: Boolean = false): ReturnValues<Throwable>
}

/**
 * A patch with different phases.
 */
interface PhasedPatch : Patch {

    /**
     * Operations to be applied on the actual raw `.img` file
     * before operations "inside" the `.img` file take place.
     */
    val diskPreparations: List<(OperatingSystemImage) -> Unit>

    /**
     * Options to be applied on the img file
     * using the [VirtCustomizeCommandLine].
     */
    val diskCustomizations: List<(OperatingSystemImage) -> Customization>

    /**
     * Operations to be applied on the mounted file system
     * using the [GuestfishCommandLine] tool.
     */
    val diskOperations: List<(OperatingSystemImage) -> GuestfishCommand>

    /**
     * Operations on files of the externally, not immediately
     * accessible file system.
     */
    val fileOperations: List<FileOperation>

    /**
     * Operations to be applied on the actual raw `.img` file
     * after operations "inside" the `.img` file take place.
     */
    val osPreparations: List<(OperatingSystemImage) -> Unit>

    /**
     * Whether to boot the operating system.
     */
    val osBoot: Boolean

    /**
     * Operations to be applied
     * on the booted operating system.
     */
    val osOperations: List<(OperatingSystemImage) -> Program>

    companion object {

        /**
         * Builds a new [Patch].
         */
        fun build(name: String, init: Init<PhasedPatchBuilder.Context>): PhasedPatch = PhasedPatchBuilder(name).build(init)
    }
}

/**
 * Simple [PhasedPatch] implementation.
 */
data class SimplePhasedPatch(
    override val name: CharSequence,
    override val diskPreparations: List<(OperatingSystemImage) -> Unit>,
    override val diskCustomizations: List<(OperatingSystemImage) -> Customization>,
    override val diskOperations: List<(OperatingSystemImage) -> GuestfishCommand>,
    override val fileOperations: List<FileOperation>,
    override val osPreparations: List<(OperatingSystemImage) -> Unit>,
    override val osBoot: Boolean,
    override val osOperations: List<(OperatingSystemImage) -> Program>,
) : PhasedPatch {
    override val operationCount: Int = diskPreparations.size + diskCustomizations.size + diskOperations.size + fileOperations.size + osOperations.size

    override fun patch(osImage: OperatingSystemImage, trace: Boolean): ReturnValues<Throwable> =
        spanning(
            name,
            nameFormatter = NAME_FORMATTER,
            contentFormatter = PATCH_DECORATION_FORMATTER,
            blockStyle = Solid
        ) {
            applyDiskPreparations(osImage) +
                applyDiskCustomizations(osImage, trace) +
                applyDiskAndFileOperations(osImage, trace) +
                applyOsPreparations(osImage) +
                applyOsBoot(osImage) +
                applyOsOperations(osImage)
        }

    private fun collectingExceptions(
        name: CharSequence,
        operationCount: Int,
        transform: CurrentSpan.((Throwable) -> Unit) -> Unit,
    ): ReturnValues<Throwable> =
        if (operationCount == 0) {
            tracing { log("◼ $name".ansi.gray) }
            ReturnValues()
        } else {
            spanning("$name ($operationCount)", blockStyle = Dotted) {
                val exceptions = mutableListOf<Throwable>()
                runCatching { transform { exceptions.add(it) } }.onFailure { exceptions.add(it) }
                ReturnValues(*exceptions.toTypedArray())
            }
        }

    private fun <T> Collection<T>.collectingExceptions(
        name: CharSequence,
        transform: CurrentSpan.(index: Int, operation: T) -> Unit,
    ): ReturnValues<Throwable> =
        collectingExceptions(name, size) { exceptionCallback ->
            mapIndexedNotNull { index, operation: T ->
                runCatching { transform(index, operation) }.exceptionOrNull()?.also(exceptionCallback)
            }
        }

    private fun applyDiskPreparations(osImage: OperatingSystemImage): ReturnValues<Throwable> =
        diskPreparations.collectingExceptions("Disk Preparation") { _, preparation ->
            preparation(osImage)
        }

    private fun applyDiskCustomizations(osImage: OperatingSystemImage, trace: Boolean): ReturnValues<Throwable> =
        collectingExceptions("Disk Customization", diskCustomizations.size) {
            osImage.virtCustomize(trace) {
                diskCustomizations.forEach { customizationOption(it) }
            }
        }

    private fun applyDiskAndFileOperations(osImage: OperatingSystemImage, trace: Boolean): ReturnValues<Throwable> {
        val exceptions = ReturnValues<Throwable>()

        osImage.hostPath(LinuxRoot).deleteDirectoryEntriesRecursively()
        collectingExceptions("Disk Operations", diskOperations.size + fileOperations.size) {
            osImage.guestfish(trace) {
                fileOperations.forEach { (file) -> copyOut { file } }
                diskOperations.forEach { command(it) }
            }
        }.also { exceptions.addAll(it) }

        fun countFiles() = osImage.hostPath(LinuxRoot).listDirectoryEntriesRecursively().filter { it.isRegularFile() }.size
        val fileOperationsCount = if (countFiles() > 0) fileOperations.size + 1 else fileOperations.size
        collectingExceptions("File Operations", fileOperationsCount) { exceptionCallback ->
            @Suppress("Destructure")
            fileOperations.forEach { fileOperation ->
                kotlin.runCatching { fileOperation(osImage.hostPath(fileOperation.file)) }.onFailure(exceptionCallback)
            }

            val changedFiles = countFiles()
            if (changedFiles > 0) {
                spanningLine("Copying in $changedFiles file(s)") {
                    osImage.guestfish(trace) { tarIn() }
                }
            }
        }.also { exceptions.addAll(it) }

        return exceptions
    }

    private fun applyOsPreparations(osImage: OperatingSystemImage): ReturnValues<Throwable> =
        osPreparations.collectingExceptions("OS Preparations") { _, osPreparation ->
            osPreparation(osImage)
        }

    private fun applyOsBoot(osImage: OperatingSystemImage): ReturnValues<Throwable> =
        collectingExceptions("OS Boot", if (osBoot) 1 else 0) {
            osImage.boot(
                name.withRandomSuffix(),
                nameFormatter = NAME_FORMATTER,
                decorationFormatter = PATCH_DECORATION_FORMATTER,
                blockStyle = Dotted,
                autoLogin = false,
                autoShutdown = false,
            )
        }


    private fun applyOsOperations(osImage: OperatingSystemImage): ReturnValues<Throwable> =
        collectingExceptions("OS Operations", osOperations.size) {
            osImage.boot(
                name.withRandomSuffix(),
                *osOperations.map { it(osImage) }.toTypedArray(),
                nameFormatter = NAME_FORMATTER,
                decorationFormatter = PATCH_DECORATION_FORMATTER,
                blockStyle = Dotted,
                autoLogin = true,
                autoShutdown = true,
            )
        }

    companion object {
        private val NAME_FORMATTER = Formatter<CharSequence> { it.ansi.cyan }
    }
}

/**
 * Builder for instances of [PhasedPatch].
 */
class PhasedPatchBuilder(private val name: CharSequence) : BuilderTemplate<PhasedPatchBuilder.Context, PhasedPatch>() {

    /** Context for building instances of [PhasedPatch]. */
    @VirtCustomizeDsl
    @GuestfishDsl
    @Suppress("PublicApiImplicitType")
    class Context(override val captures: CapturesMap) : CapturingContext() {

        /**
         * Operations to be applied on the actual raw `.img` file
         * before operations "inside" the `.img` file take place.
         */
        val prepareDisk by ImageOperationsBuilder default emptyList()

        /**
         * Options to be applied on the img file
         * using the [VirtCustomizeCommandLine].
         */
        @VirtCustomizeDsl
        val customizeDisk by CustomizationsBuilder default emptyList()

        /**
         * Operations to be applied on the mounted file system
         * using the [GuestfishCommandLine] tool.
         */
        @GuestfishDsl
        val modifyDisk by GuestfishCommandsBuilder default emptyList()

        /**
         * Operations on files of the externally, not immediately
         * accessible file system.
         */
        val modifyFiles by FileOperationsBuilder default emptyList()

        /**
         * Operations to be applied on the actual raw `.img` file
         * after operations "inside" the `.img` file take place.
         */
        val prepareOs by ImageOperationsBuilder default emptyList()

        /**
         * Whether to boot the operating system.
         */
        val bootOs by BooleanBuilder.YesNo default { no }

        /**
         * Operations to be applied
         * on the booted operating system.
         */
        val runPrograms by OsOperationsBuilder default emptyList()
    }

    override fun BuildContext.build(): PhasedPatch = ::Context {
        SimplePhasedPatch(
            name,
            ::prepareDisk.eval(),
            ::customizeDisk.eval(),
            ::modifyDisk.eval(),
            ::modifyFiles.eval(),
            ::prepareOs.eval(),
            ::bootOs.eval(),
            ::runPrograms.eval(),
        )
    }

    /** Builds image operations. */
    object ImageOperationsBuilder : BuilderTemplate<ImageOperationsBuilder.Context, List<(OperatingSystemImage) -> Unit>>() {

        @Suppress("PublicApiImplicitType")
        /** Context for building image operations. */
        class Context(override val captures: CapturesMap) : CapturingContext() {

            /**
             * Operations applied to the given [OperatingSystemImage].
             */
            val diskOperation by function<(OperatingSystemImage) -> Unit>()

            /**
             * Resizes the [OperatingSystemImage] to the specified [size].
             */
            fun resize(size: Size) {
                diskOperation { osImage: OperatingSystemImage ->
                    osImage.increaseDiskSpace(size)
                }
            }

            /**
             * Changes the username to be used for login from [oldUsername] to [newUsername].
             */
            fun updateUsername(oldUsername: String, newUsername: String) {
                diskOperation { osImage: OperatingSystemImage ->
                    spanning("Updating username of user ${oldUsername.formattedAs.input} to ${newUsername.formattedAs.input}") {
                        osImage.credentials = osImage.credentials.copy(username = newUsername)
                    }
                }
            }

            /**
             * Changes the password used to login using the given [username] to [password].
             */
            fun updatePassword(username: String, password: String) {
                diskOperation { osImage: OperatingSystemImage ->
                    if (osImage.credentials.username != username) return@diskOperation
                    spanning("Updating password of user ${osImage.credentials.username.formattedAs.input}") {
                        osImage.credentials = osImage.credentials.copy(password = password)
                    }
                }
            }
        }

        override fun BuildContext.build(): List<(OperatingSystemImage) -> Unit> = ::Context {
            ::diskOperation.evalAll()
        }
    }

    /** Builder for instances of [FileOperation]. */
    object FileOperationsBuilder : BuilderTemplate<FileOperationsBuilder.Context, List<(OperatingSystemImage) -> FileOperation>>() {

        /** Context for building instances of [FileOperation]. */
        @Suppress("PublicApiImplicitType")
        class Context(override val captures: CapturesMap) : CapturingContext() {

            /**
             * File-based operations to be applied to the [OperatingSystemImage].
             */
            val fileOperation by function<(OperatingSystemImage) -> FileOperation>()

            /**
             * Adds a [FileOperation] that edits the given [path] using the given [operations]
             * in order to satisfy the provided [validator].
             *
             * That is, if the [validator] does not throw, [path] is considered already respectively successfully changed.
             */
            fun edit(path: DiskPath, validator: (Path) -> Unit, operations: (Path) -> Unit) =
                fileOperation { FileOperation(path, validator, operations) }
        }

        override fun BuildContext.build(): List<(OperatingSystemImage) -> FileOperation> = ::Context {
            ::fileOperation.evalAll()
        }
    }

    /** Builder for OS operations. */
    object OsOperationsBuilder : BuilderTemplate<OsOperationsBuilder.Context, List<(OperatingSystemImage) -> Program>>() {

        /** Context for building OS operations. */
        @Suppress("PublicApiImplicitType")
        class Context(override val captures: CapturesMap) : CapturingContext() {

            /**
             * [Program] list to be executed inside of the running [OperatingSystemImage].
             */
            val programs by function<(OperatingSystemImage) -> Program>()

            /**
             * Adds a [Program] to be executed inside of the running [OperatingSystemImage]
             * for the given [purpose] and instructions defined by [initialState] and [states].
             */
            fun program(
                purpose: String,
                initialState: OperatingSystemProcess.() -> String?,
                vararg states: Pair<String, OperatingSystemProcess.(String) -> String?>,
            ) = programs { Program(purpose, initialState, *states) }

            /**
             * Adds a [Program] to be executed inside of the running [OperatingSystemImage]
             * that runs the given [commandLines].
             */
            fun script(name: String, vararg commandLines: String) =
                programs { osImage: OperatingSystemImage -> osImage.compileScript(name, *commandLines) }
        }

        override fun BuildContext.build(): List<(OperatingSystemImage) -> Program> = ::Context {
            ::programs.evalAll()
        }
    }
}

/**
 * Applies this patch to the given [OperatingSystemImage].
 */
@Deprecated("delete")
inline val patch: Patch.(osImage: OperatingSystemImage) -> ReturnValues<Throwable>
    get() = { osImage: OperatingSystemImage -> patch(osImage) }

/**
 * Applies all patches of this collection to the given [OperatingSystemImage].
 */
fun List<Patch>.patch(osImage: OperatingSystemImage): ReturnValues<Throwable> =
    spanning(banner("Applying $size patches to ${osImage.shortName}")) {
        ReturnValues(*flatMap { it.patch(osImage) }.toTypedArray())
    }

/**
 * A patch that combines the specified [patches].
 *
 * Composing patches allows for faster image customizations
 * as less reboots are necessary.
 */
class CompositePatch(
    /**
     * Patches encompassed by this composite patch.
     */
    val patches: Collection<PhasedPatch>,
) : PhasedPatch by SimplePhasedPatch(
    name = Renderable.of(patches.joinToString(LineSeparators.LF) { it.name }) { _, _ -> this },
    diskPreparations = patches.flatMap { it.diskPreparations }.toList(),
    diskCustomizations = patches.flatMap { it.diskCustomizations }.toList(),
    diskOperations = patches.flatMap { it.diskOperations }.toList(),
    fileOperations = patches.flatMap { it.fileOperations }.toList(),
    osPreparations = patches.flatMap { it.osPreparations }.toList(),
    osBoot = patches.any { patch -> patch.osBoot },
    osOperations = patches.flatMap { it.osOperations }.toList(),
) {
    constructor(vararg patches: PhasedPatch) : this(patches.toList())

    override fun toString(): String = asString {
        "patches" to patches
    }
}

/**
 * An [operation] that is applied to the specified [file]
 * if considered necessary by the given [verify].
 */
data class FileOperation(
    /**
     * The file that needs to be operated on.
     */
    val file: DiskPath,
    /**
     * Used to check if [file] is in the desired state. If it is not, an
     * exception describing the problem is expected to be thrown.
     */
    val verify: (Path) -> Unit,
    /**
     * Used to transition [file] to the desired state.
     * Only invoked if [verify] throws an exception.
     */
    val operation: (Path) -> Unit,
) {

    /**
     * Checks if [localFile] is in the desired state using [verify]
     * and if not, applies [operation] it and verifies again.
     *
     * Throws if the final verification fails.
     */
    operator fun invoke(localFile: Path) {
        spanningLine(localFile.fileName.toString()) {
            log("Action needed?")
            runCatching { verify(localFile) }.recover {
                log("Yes".formattedAs.warning)

                operation(localFile)

                log("Verifying")
                verify(localFile)
            }
        }
    }
}

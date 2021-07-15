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
import com.imgcstmzr.patch.Patch.Companion.FileSystemOperationsCollector.FileSystemOperationsContext
import com.imgcstmzr.patch.Patch.Companion.ImgOperationsCollector.ImgOperationsContext
import com.imgcstmzr.patch.Patch.Companion.PatchContext
import com.imgcstmzr.patch.Patch.Companion.ProgramsBuilder.ProgramsContext
import koodies.asString
import koodies.builder.BooleanBuilder
import koodies.builder.BuilderTemplate
import koodies.builder.Init
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
     * Operations to be applied on the actual raw `.img` file
     * before operations "inside" the `.img` file take place.
     */
    val diskPreparations: List<DiskOperation>

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
    val osPreparations: List<DiskOperation>

    /**
     * Whether to boot the operating system.
     */
    val osBoot: Boolean

    /**
     * Operations (in the form of [osOperations]) to be applied
     * on the booted operating system.
     */
    val osOperations: List<(OperatingSystemImage) -> Program>

    /**
     * Applies this patch to the given [osImage].
     *
     * Detailed logging can be activated using [trace].
     */
    fun patch(osImage: OperatingSystemImage, trace: Boolean = false): ReturnValues<Throwable> {
        return spanning(
            name,
            nameFormatter = NAME_FORMATTER,
            contentFormatter = PATCH_DECORATION_FORMATTER,
            blockStyle = Solid
        ) {
            applyDiskPreparations(osImage, trace) +
                applyDiskCustomizations(osImage, trace) +
                applyDiskAndFileOperations(osImage, trace) +
                applyOsPreparations(osImage, trace) +
                applyOsBoot(osImage, trace) +
                applyOsOperations(osImage, trace)
        }
    }

    fun <T, R : Any> collectingExceptions(
        operations: List<T>,
        transform: (index: Int, operation: T) -> R?,
    ): ReturnValues<Throwable> =
        operations.mapIndexedNotNull { index, operation: T ->
            runCatching {
                transform(index, operation)
            }.exceptionOrNull()
        }.let { ReturnValues(*it.toTypedArray()) }

    fun collectingExceptions(name: String, transform: CurrentSpan.() -> Unit): ReturnValues<Throwable> =
        spanning(name, blockStyle = Dotted) { runCatching(transform).fold({ ReturnValues() }, { ReturnValues(it) }) }

    private fun none(operationName: String): ReturnValues<Throwable> {
        tracing { log("◼ $operationName".ansi.gray) }
        return ReturnValues()
    }

    fun applyDiskPreparations(osImage: OperatingSystemImage, trace: Boolean): ReturnValues<Throwable> {
        if (diskPreparations.isEmpty()) return none("Disk Preparation")
        return collectingExceptions("Disk Preparation (${diskPreparations.size})") {
            collectingExceptions(diskPreparations) { _, preparation -> preparation(osImage) }
        }
    }

    fun applyDiskCustomizations(osImage: OperatingSystemImage, trace: Boolean): ReturnValues<Throwable> {
        if (diskCustomizations.isEmpty()) return none("Disk Customization")
        return collectingExceptions("Disk Customization (${diskCustomizations.size})") {
            if (diskCustomizations.isNotEmpty()) {
                osImage.virtCustomize(trace) {
                    diskCustomizations.forEach { customizationOption(it) }
                }
            }
        }
    }

    fun applyDiskAndFileOperations(osImage: OperatingSystemImage, trace: Boolean): ReturnValues<Throwable> {
        val exceptions = ReturnValues<Throwable>()

        val diskOperationsAndFilePreparationOperations = diskOperations.size + fileOperations.size
        if (diskOperationsAndFilePreparationOperations == 0) return none("Disk Operations")

        osImage.hostPath(LinuxRoot).deleteDirectoryEntriesRecursively()
        collectingExceptions("Disk Operations (${diskOperationsAndFilePreparationOperations})") {
            if (diskOperationsAndFilePreparationOperations > 0) {
                osImage.guestfish(trace) {
                    fileOperations.map { it.file }.forEach { sourcePath ->
                        copyOut { sourcePath }
                    }
                    diskOperations.forEach { command(it) }
                }
            }
        }.also { exceptions.addAll(it) }

        fun countFiles() = osImage.hostPath(LinuxRoot).listDirectoryEntriesRecursively().filter { it.isRegularFile() }.size
        osImage.hostPath(LinuxRoot).listDirectoryEntriesRecursively()
        val fileOperationsCount = fileOperations.size + if (countFiles() > 0) 1 else 0
        collectingExceptions("File Operations ($fileOperationsCount)") {
            val filesToPatch = fileOperations.toMutableList()
            if (filesToPatch.isNotEmpty()) {
                collectingExceptions(filesToPatch) { _, fileOperation ->
                    val path = fileOperation.file
                    fileOperation(osImage.hostPath(path))
                }.forEach { exceptions.add(it) }
            }

            val changedFiles = countFiles()
            if (changedFiles > 0) {
                collectingExceptions("Copying in $changedFiles file(s)") {
                    osImage.guestfish(trace) {
                        tarIn()
                    }
                }.also { exceptions.addAll(it) }
            }
        }

        return exceptions
    }

    fun applyOsPreparations(osImage: OperatingSystemImage, trace: Boolean): ReturnValues<Throwable> {
        if (osPreparations.isEmpty()) return none("OS Preparation")
        return collectingExceptions("OS Preparation (${osPreparations.size})") {
            collectingExceptions(osPreparations) { _, preparation -> preparation(osImage) }
        }
    }

    fun applyOsBoot(osImage: OperatingSystemImage, trace: Boolean): ReturnValues<Throwable> {
        if (!osBoot) return none("OS Boot")
        else return runCatching {
            osImage.boot(
                name.withRandomSuffix(),
                nameFormatter = NAME_FORMATTER,
                decorationFormatter = PATCH_DECORATION_FORMATTER,
                blockStyle = Dotted,
                autoLogin = false,
                autoShutdown = false,
            )
        }.fold({ ReturnValues() }, { ReturnValues(it) })
    }


    fun applyOsOperations(osImage: OperatingSystemImage, trace: Boolean): ReturnValues<Throwable> {
        if (osOperations.isEmpty()) return none("OS Operations")
        else return runCatching {
            osImage.boot(
                name.withRandomSuffix(),
                *osOperations.map { it(osImage) }.toTypedArray(),
                nameFormatter = NAME_FORMATTER,
                decorationFormatter = PATCH_DECORATION_FORMATTER,
                blockStyle = Dotted,
                autoLogin = true,
                autoShutdown = true,
            )
        }.fold({ ReturnValues() }, { ReturnValues(it) })
    }

    val operationCount: Int

    val isEmpty: Boolean get() = operationCount == 0
    val isNotEmpty: Boolean get() = !isEmpty

    companion object : BuilderTemplate<PatchContext, (String) -> Patch>() {

        private val NAME_FORMATTER = Formatter<CharSequence> { it.ansi.cyan }

        @DslMarker
        annotation class PatchDsl

        fun buildPatch(name: String, init: Init<PatchContext>): Patch = invoke(init)(name)

        @PatchDsl
        @VirtCustomizeDsl
        @GuestfishDsl
        class PatchContext(override val captures: CapturesMap) : CapturingContext() {

            /**
             * Operations to be applied on the actual raw `.img` file
             * before operations "inside" the `.img` file take place.
             */
            val prepareDisk by ImgOperationsCollector default emptyList()

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
            val guestfish by GuestfishCommandsBuilder default emptyList()

            /**
             * Operations on files of the externally, not immediately
             * accessible file system.
             */
            val files by FileSystemOperationsCollector default emptyList()

            /**
             * Operations to be applied on the actual raw `.img` file
             * after operations "inside" the `.img` file take place.
             */
            val osPrepare by ImgOperationsCollector default emptyList()

            /**
             * Whether to boot the operating system.
             */
            val boot by BooleanBuilder.YesNo default { no }

            /**
             * Operations (in the form of [osOperations]) to be applied
             * on the booted operating system.
             */
            val os by ProgramsBuilder default emptyList()
        }

        override fun BuildContext.build() = ::PatchContext {
            { name: String ->
                DeprecatedSimplePatch(
                    name,
                    ::prepareDisk.eval(),
                    ::customizeDisk.eval(),
                    ::guestfish.eval(),
                    ::files.eval(),
                    ::osPrepare.eval(),
                    ::boot.eval(),
                    ::os.eval(),
                )
            }
        }

        object ImgOperationsCollector : BuilderTemplate<ImgOperationsContext, List<DiskOperation>>() {
            @PatchDsl
            class ImgOperationsContext(override val captures: CapturesMap) : CapturingContext() {
                val diskOperation by function<DiskOperation>()

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

            override fun BuildContext.build(): List<DiskOperation> = ::ImgOperationsContext {
                ::diskOperation.evalAll()
            }
        }

        object FileSystemOperationsCollector : BuilderTemplate<FileSystemOperationsContext, List<(OperatingSystemImage) -> FileOperation>>() {

            @PatchDsl
            class FileSystemOperationsContext(override val captures: CapturesMap) : CapturingContext() {

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

            override fun BuildContext.build(): List<(OperatingSystemImage) -> FileOperation> = ::FileSystemOperationsContext {
                ::fileOperation.evalAll()
            }
        }

        object ProgramsBuilder : BuilderTemplate<ProgramsContext, List<(OperatingSystemImage) -> Program>>() {

            @PatchDsl
            class ProgramsContext(override val captures: CapturesMap) : CapturingContext() {

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

            override fun BuildContext.build(): List<(OperatingSystemImage) -> Program> = ::ProgramsContext {
                ::programs.evalAll()
            }
        }
    }
}

/**
 * Applies this patch to the given [OperatingSystemImage].
 */
@Deprecated("delete")
inline val patch: Patch.(osImage: OperatingSystemImage) -> ReturnValues<Throwable>
    get() = { osImage: OperatingSystemImage -> patch(osImage) }

typealias DiskOperation = (osImage: OperatingSystemImage) -> Unit

/**
 * Applies all patches of this collection to the given [OperatingSystemImage].
 */
fun List<Patch>.patch(osImage: OperatingSystemImage): ReturnValues<Throwable> =
    spanning(banner("Applying $size patches to ${osImage.shortName}")) {
        ReturnValues(*flatMap { it.patch(osImage) }.toTypedArray())
    }


data class DeprecatedSimplePatch(
    override val name: CharSequence,
    override val diskPreparations: List<DiskOperation>,
    override val diskCustomizations: List<(OperatingSystemImage) -> Customization>,
    override val diskOperations: List<(OperatingSystemImage) -> GuestfishCommand>,
    override val fileOperations: List<FileOperation>,
    override val osPreparations: List<DiskOperation>,
    override val osBoot: Boolean,
    override val osOperations: List<(OperatingSystemImage) -> Program>,
) : Patch {
    override val operationCount by lazy {
        diskPreparations.size + diskCustomizations.size + diskOperations.size + fileOperations.size + osOperations.size
    }
}

/**
 * A patch that combines the specified [patches].
 *
 * Composing patches allows for faster image customizations
 * as less reboots are necessary.
 */
class CompositePatch(
    val patches: Collection<Patch>,
) : Patch by DeprecatedSimplePatch(
    name = Renderable.of(patches.joinToString(LineSeparators.LF) { it.name }) { _, _ -> this },
    diskPreparations = patches.flatMap { it.diskPreparations }.toList(),
    diskCustomizations = patches.flatMap { it.diskCustomizations }.toList(),
    diskOperations = patches.flatMap { it.diskOperations }.toList(),
    fileOperations = patches.flatMap { it.fileOperations }.toList(),
    osPreparations = patches.flatMap { it.osPreparations }.toList(),
    osBoot = patches.any { patch -> patch.osBoot },
    osOperations = patches.flatMap { it.osOperations }.toList(),
) {
    constructor(vararg patches: Patch) : this(patches.toList())

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


data class SimplePatch(
    override val name: CharSequence,
    override val diskPreparations: List<(OperatingSystemImage) -> Unit>,
    override val diskCustomizations: List<(OperatingSystemImage) -> Customization>,
    override val diskOperations: List<(OperatingSystemImage) -> GuestfishCommand>,
    override val fileOperations: List<FileOperation>,
    override val osPreparations: List<(OperatingSystemImage) -> Unit>,
    override val osBoot: Boolean,
    override val osOperations: List<(OperatingSystemImage) -> Program>,
) : Patch {
    override val operationCount by lazy {
        diskPreparations.size + diskCustomizations.size + diskOperations.size + fileOperations.size + osOperations.size
    }
}

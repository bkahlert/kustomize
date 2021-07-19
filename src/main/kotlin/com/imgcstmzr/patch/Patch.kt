package com.imgcstmzr.patch

import com.imgcstmzr.cli.Layouts
import com.imgcstmzr.cli.PATCH_DECORATION_FORMATTER
import com.imgcstmzr.cli.PATCH_NAME_FORMATTER
import com.imgcstmzr.libguestfs.GuestfishCommandLine
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommandsBuilder
import com.imgcstmzr.libguestfs.GuestfishDsl
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.CustomizationsBuilder
import com.imgcstmzr.libguestfs.VirtCustomizeDsl
import com.imgcstmzr.os.DiskPath
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystemProcess
import com.imgcstmzr.os.Program
import com.imgcstmzr.os.boot
import koodies.asString
import koodies.builder.Init
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.LineSeparators
import koodies.text.Semantics.formattedAs
import koodies.text.withRandomSuffix
import koodies.tracing.CurrentSpan
import koodies.tracing.rendering.Renderable
import koodies.tracing.rendering.ReturnValues
import koodies.tracing.rendering.Styles.Dotted
import koodies.tracing.rendering.Styles.Solid
import koodies.tracing.rendering.spanningLine
import koodies.tracing.spanning
import koodies.tracing.tracing
import koodies.unit.Size
import java.nio.file.Path

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
    val diskPreparations: List<() -> Unit>

    /**
     * Options to be applied on the img file
     * using the [VirtCustomizeCommandLine].
     */
    val diskCustomizations: List<Customization>

    /**
     * Operations to be applied on the mounted file system
     * using the [GuestfishCommandLine] tool.
     */
    val diskOperations: List<GuestfishCommand>

    /**
     * Operations on files of the externally, not immediately
     * accessible file system.
     */
    val fileOperations: List<FileOperation>

    /**
     * Operations to be applied on the actual raw `.img` file
     * after operations "inside" the `.img` file take place.
     */
    val osPreparations: List<() -> Unit>

    /**
     * Whether to boot the operating system.
     */
    val osBoot: Boolean

    /**
     * Operations to be applied
     * on the booted operating system.
     */
    val osOperations: List<Program>

    companion object {

        /**
         * Builds a new [Patch].
         */
        fun build(name: String, osImage: OperatingSystemImage, init: Init<PhasedPatchBuilder.PhasedPatchContext>): PhasedPatch =
            PhasedPatchBuilder(name, osImage).build(init)
    }
}

/**
 * Simple [PhasedPatch] implementation.
 */
data class SimplePhasedPatch(
    override val name: CharSequence,
    override val diskPreparations: List<() -> Unit>,
    override val diskCustomizations: List<Customization>,
    override val diskOperations: List<GuestfishCommand>,
    override val fileOperations: List<FileOperation>,
    override val osPreparations: List<() -> Unit>,
    override val osBoot: Boolean,
    override val osOperations: List<Program>,
) : PhasedPatch {
    override val operationCount: Int = diskPreparations.size + diskCustomizations.size + diskOperations.size + fileOperations.size + osOperations.size

    override fun patch(osImage: OperatingSystemImage, trace: Boolean): ReturnValues<Throwable> =
        spanning(name,
            nameFormatter = PATCH_NAME_FORMATTER,
            decorationFormatter = PATCH_DECORATION_FORMATTER,
            style = Solid
        ) {
            applyDiskPreparations() +
                applyDiskCustomizations(osImage, trace) +
                applyDiskOperations(osImage, trace) +
                applyFileOperations(osImage, trace) +
                applyOsPreparations() +
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
            spanning("$name",
                nameFormatter = {
                    "${it.ansi.brightCyan} ${PATCH_DECORATION_FORMATTER("(")}${operationCount.toString().ansi.brightCyan}${PATCH_DECORATION_FORMATTER(")")}"
                },
                decorationFormatter = PATCH_DECORATION_FORMATTER,
                style = Dotted) {
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

    private fun applyDiskPreparations(): ReturnValues<Throwable> =
        diskPreparations.collectingExceptions("Disk Preparations") { _, preparation ->
            preparation()
        }

    private fun applyDiskCustomizations(osImage: OperatingSystemImage, trace: Boolean): ReturnValues<Throwable> =
        collectingExceptions("Disk Customizations", diskCustomizations.size) {
            osImage.virtCustomize(trace, *diskCustomizations.toTypedArray())
        }

    private fun applyDiskOperations(osImage: OperatingSystemImage, trace: Boolean): ReturnValues<Throwable> =
        collectingExceptions("Disk Operations", diskOperations.size + fileOperations.size) { // copy-in `CopyIn` files and copy out file operation files
            osImage.guestfish(trace, *diskOperations.toTypedArray()) {
                @Suppress("Destructure")
                fileOperations.map { fileOperation -> copyOut { fileOperation.file } }
            }
        }

    private fun applyFileOperations(osImage: OperatingSystemImage, trace: Boolean): ReturnValues<Throwable> =
        collectingExceptions("File Operations", fileOperations.size) { exceptionCallback ->
            @Suppress("Destructure")
            fileOperations.forEach { fileOperation ->
                kotlin.runCatching { fileOperation(osImage.hostPath(fileOperation.file)) }.onFailure(exceptionCallback)
            }

            val fileOperationFiles: List<Path> = fileOperations.map { osImage.hostPath(it.file) }
            osImage.guestfish(trace) {
                // tar-in only relevant files to avoid corrupting incorrect attributes (i.e. executable flag)
                tarIn { it in fileOperationFiles }
            }
        }

    private fun applyOsPreparations(): ReturnValues<Throwable> =
        osPreparations.collectingExceptions("OS Preparations") { _, osPreparation ->
            osPreparation()
        }

    private fun applyOsBoot(osImage: OperatingSystemImage): ReturnValues<Throwable> =
        collectingExceptions("OS Boot", if (osBoot) 1 else 0) {
            osImage.boot(
                name.withRandomSuffix(),
                style = Dotted,
                autoLogin = false,
                autoShutdown = false,
            )
        }


    private fun applyOsOperations(osImage: OperatingSystemImage): ReturnValues<Throwable> =
        collectingExceptions("OS Operations", osOperations.size) {
            osImage.boot(
                name.withRandomSuffix(),
                *osOperations.toTypedArray(),
                style = Dotted,
                autoLogin = true,
                autoShutdown = true,
            )
        }
}

/**
 * Builder for instances of [PhasedPatch].
 */
class PhasedPatchBuilder(private val name: CharSequence, private val osImage: OperatingSystemImage) {

    /**
     * Builds a new [PhasedPatch].
     */
    fun build(init: PhasedPatchContext.() -> Unit): PhasedPatch {
        val diskPreparations: MutableList<() -> Unit> = mutableListOf()
        val diskCustomizations: MutableList<Customization> = mutableListOf()
        val diskOperations: MutableList<GuestfishCommand> = mutableListOf()
        val fileOperations: MutableList<FileOperation> = mutableListOf()
        val osPreparations: MutableList<() -> Unit> = mutableListOf()
        val osBoot: MutableList<Boolean> = mutableListOf()
        val osOperations: MutableList<Program> = mutableListOf()

        PhasedPatchContext(
            diskPreparations,
            diskCustomizations,
            diskOperations,
            fileOperations,
            osPreparations,
            osBoot,
            osOperations,
        ).init()

        return SimplePhasedPatch(
            name,
            diskPreparations,
            diskCustomizations,
            diskOperations,
            fileOperations,
            osPreparations,
            osBoot.lastOrNull() ?: false,
            osOperations,
        )
    }

    /** Context for building instances of [PhasedPatch]. */
    @VirtCustomizeDsl
    @GuestfishDsl
    @Suppress("PublicApiImplicitType")
    inner class PhasedPatchContext(
        private val diskPreparations: MutableList<() -> Unit>,
        private val diskCustomizations: MutableList<Customization>,
        private val diskOperations: MutableList<GuestfishCommand>,
        private val fileOperations: MutableList<FileOperation>,
        private val osPreparations: MutableList<() -> Unit>,
        private val osBoot: MutableList<Boolean>,
        private val osOperations: MutableList<Program>,
    ) {

        /**
         * Operations to be applied on the actual raw `.img` file
         * before operations "inside" the `.img` file take place.
         */
        fun prepareDisk(init: ImageOperationsBuilder.ImageOperationsContext.() -> Unit) {
            diskPreparations.addAll(ImageOperationsBuilder(osImage).build(init))
        }

        /**
         * Options to be applied on the img file
         * using the [VirtCustomizeCommandLine].
         */
        @VirtCustomizeDsl
        fun customizeDisk(init: CustomizationsBuilder.CustomizationsContext.() -> Unit) {
            diskCustomizations.addAll(CustomizationsBuilder(osImage).build(init))
        }

        /**
         * Operations to be applied on the mounted file system
         * using the [GuestfishCommandLine] tool.
         */
        @GuestfishDsl
        fun modifyDisk(init: GuestfishCommandsBuilder.GuestfishCommandsContext.() -> Unit) {
            diskOperations.addAll(GuestfishCommandsBuilder(osImage).build(init))
        }

        /**
         * Operations on files of the externally, not immediately
         * accessible file system.
         */
        fun modifyFiles(init: FileOperationsBuilder.FileOperationsContext.() -> Unit) {
            fileOperations.addAll(FileOperationsBuilder.build(init))
        }

        /**
         * Operations to be applied on the actual raw `.img` file
         * after operations "inside" the `.img` file take place.
         */
        fun prepareOs(init: ImageOperationsBuilder.ImageOperationsContext.() -> Unit) {
            osPreparations.addAll(ImageOperationsBuilder(osImage).build(init))
        }

        /**
         * Whether to boot the operating system.
         */
        var bootOs: Boolean
            get() = osBoot.lastOrNull() ?: false
            set(value) {
                osBoot.add(value)
            }

        /**
         * Operations to be applied
         * on the booted operating system.
         */
        fun runPrograms(init: OsOperationsBuilder.OsOperationsContext.() -> Unit) {
            osOperations.addAll(OsOperationsBuilder(osImage).build(init))
        }
    }

    /** Builds image operations. */
    class ImageOperationsBuilder(private val osImage: OperatingSystemImage) {

        /**
         * Builds a new image operation.
         */
        fun build(init: ImageOperationsContext.() -> Unit): List<() -> Unit> =
            mutableListOf<() -> Unit>().also { ImageOperationsContext(it).init() }

        /** Context for building image operations. */
        inner class ImageOperationsContext(private val diskOperations: MutableList<() -> Unit>) {

            /**
             * Resizes the [OperatingSystemImage] to the specified [size].
             */
            fun resize(size: Size) {
                diskOperations.add { osImage.increaseDiskSpace(size) }
            }

            /**
             * Changes the username to be used for login from [oldUsername] to [newUsername].
             */
            fun updateUsername(oldUsername: String, newUsername: String) {
                diskOperations.add {
                    spanning("Updating username of user ${oldUsername.formattedAs.input} to ${newUsername.formattedAs.input}") {
                        osImage.credentials = osImage.credentials.copy(username = newUsername)
                    }
                }
            }

            /**
             * Changes the password used to login using the given [username] to [password].
             */
            fun updatePassword(username: String, password: String) {
                diskOperations.add {
                    if (osImage.credentials.username != username) return@add
                    spanning("Updating password of user ${osImage.credentials.username.formattedAs.input}") {
                        osImage.credentials = osImage.credentials.copy(password = password)
                    }
                }
            }
        }
    }

    /** Builder for instances of [FileOperation]. */
    object FileOperationsBuilder {

        /**
         * Builds a new [FileOperation].
         */
        fun build(init: FileOperationsContext.() -> Unit): List<FileOperation> =
            mutableListOf<FileOperation>().also { FileOperationsContext(it).init() }

        /** Context for building instances of [FileOperation]. */
        class FileOperationsContext(private val fileOperations: MutableList<FileOperation>) {

            /**
             * Adds a [FileOperation] that edits the given [path] using the given [operations]
             * in order to satisfy the provided [validator].
             *
             * That is, if the [validator] does not throw, [path] is considered already respectively successfully changed.
             */
            fun edit(path: DiskPath, validator: (Path) -> Unit, operations: (Path) -> Unit) {
                fileOperations.add(FileOperation(path, validator, operations))
            }
        }
    }

    /** Builder for OS operations. */
    class OsOperationsBuilder(private val osImage: OperatingSystemImage) {

        /**
         * Builds a new [Program] list.
         */
        fun build(init: OsOperationsContext.() -> Unit): List<Program> =
            mutableListOf<Program>().also { OsOperationsContext(it).init() }

        /** Context for building OS operations. */
        inner class OsOperationsContext(private val programs: MutableList<Program>) {

            /**
             * Adds a [Program] to be executed inside of the running [OperatingSystemImage]
             * for the given [purpose] and instructions defined by [initialState] and [states].
             */
            fun program(
                purpose: String,
                initialState: OperatingSystemProcess.() -> String?,
                vararg states: Pair<String, OperatingSystemProcess.(String) -> String?>,
            ) {
                programs.add(Program(purpose, initialState, *states))
            }

            /**
             * Adds a [Program] to be executed inside of the running [OperatingSystemImage]
             * that runs the given [commandLines].
             */
            fun script(name: String, vararg commandLines: String) {
                programs.add(osImage.compileScript(name, *commandLines))
            }
        }
    }
}

/**
 * Applies all given patches to this [OperatingSystemImage].
 */
fun OperatingSystemImage.patch(vararg patches: (OperatingSystemImage) -> PhasedPatch): ReturnValues<Throwable> =
    patches
        .map { it(this) }
        .filter { it.isNotEmpty }
        .takeIf { it.isNotEmpty() }
        ?.let {
            spanning(
                "Applying ${it.size} patches to $shortName",
                nameFormatter = PATCH_NAME_FORMATTER,
                decorationFormatter = PATCH_DECORATION_FORMATTER,
                layout = Layouts.DESCRIPTION,
                style = Dotted,
            ) {
                ReturnValues(*it.flatMap { phasedPatch -> phasedPatch.patch(this@patch) }.toTypedArray())
            }
        } ?: ReturnValues()

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
    val patches: Collection<(OperatingSystemImage) -> PhasedPatch>,
) : (OperatingSystemImage) -> PhasedPatch {

    constructor(vararg patches: (OperatingSystemImage) -> PhasedPatch) : this(patches.toList())

    /**
     * Applies this patch to the given [osImage].
     */
    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = patches.map { it(osImage) }.run {
        SimplePhasedPatch(
            name = Renderable.of(joinToString(LineSeparators.LF) { it.name }) { _, _ -> this },
            diskPreparations = flatMap { it.diskPreparations }.toList(),
            diskCustomizations = flatMap { it.diskCustomizations }.toList(),
            diskOperations = flatMap { it.diskOperations }.toList(),
            fileOperations = flatMap { it.fileOperations }.toList(),
            osPreparations = flatMap { it.osPreparations }.toList(),
            osBoot = any { patch -> patch.osBoot },
            osOperations = flatMap { it.osOperations }.toList(),
        )
    }

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

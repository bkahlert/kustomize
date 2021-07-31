package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.cli.Layouts
import com.bkahlert.kustomize.cli.PATCH_DECORATION_FORMATTER
import com.bkahlert.kustomize.cli.PATCH_NAME_FORMATTER
import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine
import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine.GuestfishCommand
import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine.GuestfishCommandsBuilder
import com.bkahlert.kustomize.libguestfs.GuestfishDsl
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomizationsBuilder
import com.bkahlert.kustomize.libguestfs.VirtCustomizeDsl
import com.bkahlert.kustomize.os.DiskPath
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.boot
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
    val virtCustomizations: List<VirtCustomization>

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
     * Whether to boot the operating system.
     */
    val osBoot: Boolean

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
    override val virtCustomizations: List<VirtCustomization>,
    override val diskOperations: List<GuestfishCommand>,
    override val fileOperations: List<FileOperation>,
    override val osBoot: Boolean,
) : PhasedPatch {
    override val operationCount: Int = diskPreparations.size + virtCustomizations.size + diskOperations.size + fileOperations.size + (if (osBoot) 1 else 0)

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
                applyOsBoot(osImage)
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
        collectingExceptions("Disk Customizations", virtCustomizations.size) {
            osImage.virtCustomize(trace, *virtCustomizations.toTypedArray())
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
                // Tar-in only relevant files to avoid corrupting incorrect attributes (i.e. executable flag)
                tarIn { it in fileOperationFiles }
            }
        }

    private fun applyOsBoot(osImage: OperatingSystemImage): ReturnValues<Throwable> =
        collectingExceptions("OS Boot", if (osBoot) 1 else 0) {
            osImage.boot(
                name.withRandomSuffix(),
                style = Dotted,
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
        val virtCustomizations: MutableList<VirtCustomization> = mutableListOf()
        val diskOperations: MutableList<GuestfishCommand> = mutableListOf()
        val fileOperations: MutableList<FileOperation> = mutableListOf()
        val osBoot: MutableList<Boolean> = mutableListOf()

        PhasedPatchContext(
            diskPreparations,
            virtCustomizations,
            diskOperations,
            fileOperations,
            osBoot,
        ).init()

        return SimplePhasedPatch(
            name,
            diskPreparations,
            virtCustomizations,
            diskOperations,
            fileOperations,
            osBoot.lastOrNull() ?: false,
        )
    }

    /** Context for building instances of [PhasedPatch]. */
    @VirtCustomizeDsl
    @GuestfishDsl
    @Suppress("PublicApiImplicitType")
    inner class PhasedPatchContext(
        private val diskPreparations: MutableList<() -> Unit>,
        private val virtCustomizations: MutableList<VirtCustomization>,
        private val diskOperations: MutableList<GuestfishCommand>,
        private val fileOperations: MutableList<FileOperation>,
        private val osBoot: MutableList<Boolean>,
    ) {

        /**
         * Operations to be applied on the actual raw `.img` file
         * before operations "inside" the `.img` file take place.
         */
        fun disk(init: ImageOperationsBuilder.ImageOperationsContext.() -> Unit) {
            diskPreparations.addAll(ImageOperationsBuilder(osImage).build(init))
        }

        /**
         * Options to be applied on the img file
         * using the [VirtCustomizeCommandLine].
         */
        @VirtCustomizeDsl
        fun virtCustomize(init: VirtCustomizationsBuilder.VirtCustomizationsContext.() -> Unit) {
            virtCustomizations.addAll(VirtCustomizationsBuilder(osImage).build(init))
        }

        /**
         * Operations to be applied on the mounted file system
         * using the [GuestfishCommandLine] tool.
         */
        @GuestfishDsl
        fun guestfish(init: GuestfishCommandsBuilder.GuestfishCommandsContext.() -> Unit) {
            diskOperations.addAll(GuestfishCommandsBuilder(osImage).build(init))
        }

        /**
         * Operations on files of the externally, not immediately
         * accessible file system.
         */
        @Deprecated("delete")
        fun modifyFiles(init: FileOperationsBuilder.FileOperationsContext.() -> Unit) {
            fileOperations.addAll(FileOperationsBuilder.build(init))
        }

        /**
         * Whether to boot the operating system.
         */
        var bootOs: Boolean
            get() = osBoot.lastOrNull() ?: false
            set(value) {
                osBoot.add(value)
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
                diskOperations.add { osImage.resize(size) }
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
 * as fewer reboots are necessary.
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
            virtCustomizations = flatMap { it.virtCustomizations }.toList(),
            diskOperations = flatMap { it.diskOperations }.toList(),
            fileOperations = flatMap { it.fileOperations }.toList(),
            osBoot = any { patch -> patch.osBoot },
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


class PatchContext(
    private val trace: Boolean = false,
    private val osImage: OperatingSystemImage,
    private val exceptions: MutableList<Throwable>,
) {

    /**
     * Resizes the [OperatingSystemImage] to the specified [size].
     */
    fun resize(size: Size) {
        kotlin.runCatching { osImage.resize(size) }.onFailure { exceptions.add(it) }
    }

    /**
     * Options to be applied on the img file
     * using the [VirtCustomizeCommandLine].
     */
    fun virtCustomize(virtCustomizationsBuilder: VirtCustomizationsBuilder.VirtCustomizationsContext.() -> Unit) {
        osImage.virtCustomize(trace, *VirtCustomizationsBuilder(osImage).build(virtCustomizationsBuilder).toTypedArray())
    }

    /**
     * Operations to be applied on the mounted file system
     * using the [GuestfishCommandLine] tool.
     */
    fun guestfish(guestfishCommandsBuilder: GuestfishCommandsBuilder.GuestfishCommandsContext.() -> Unit) {
        osImage.guestfish(trace, *GuestfishCommandsBuilder(osImage).build(guestfishCommandsBuilder).toTypedArray())
    }

    fun edit(vararg fileOperations: FileOperation) {
        spanning("Edit files") {
            osImage.guestfish(trace) {
                @Suppress("Destructure")
                fileOperations.map { fileOperation -> copyOut { fileOperation.file } }
            }

            @Suppress("Destructure")
            fileOperations.forEach { fileOperation ->
                kotlin.runCatching { fileOperation(osImage.hostPath(fileOperation.file)) }.onFailure { exceptions.add(it) }
            }

            val fileOperationFiles: List<Path> = fileOperations.map { osImage.hostPath(it.file) }
            osImage.guestfish(trace) {
                // Tar-in only relevant files to avoid corrupting incorrect attributes (i.e. executable flag)
                tarIn { it in fileOperationFiles }
            }
        }
    }

    fun boot() {
        osImage.boot(
            osImage.name.withRandomSuffix(),
            style = Dotted,
        )
    }
}

fun OperatingSystemImage.patch(trace: Boolean = false, patch: PatchContext.() -> Unit): ReturnValues<Throwable> {
    val exceptions = mutableListOf<Throwable>()
    PatchContext(trace, this, exceptions).apply(patch)
    return ReturnValues(*exceptions.toTypedArray())
}

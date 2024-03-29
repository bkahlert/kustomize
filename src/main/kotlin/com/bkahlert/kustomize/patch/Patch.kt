package com.bkahlert.kustomize.patch

import com.bkahlert.kommons.asString
import com.bkahlert.kommons.builder.Init
import com.bkahlert.kommons.builder.buildList
import com.bkahlert.kommons.shell.ShellScript
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.LineSeparators
import com.bkahlert.kommons.text.withRandomSuffix
import com.bkahlert.kommons.tracing.SpanScope
import com.bkahlert.kommons.tracing.rendering.Renderable
import com.bkahlert.kommons.tracing.rendering.ReturnValues
import com.bkahlert.kommons.tracing.rendering.Styles.Dotted
import com.bkahlert.kommons.tracing.rendering.Styles.Solid
import com.bkahlert.kommons.tracing.runSpanning
import com.bkahlert.kommons.tracing.spanScope
import com.bkahlert.kommons.unit.Size
import com.bkahlert.kustomize.cli.Layouts
import com.bkahlert.kustomize.cli.PATCH_DECORATION_FORMATTER
import com.bkahlert.kustomize.cli.PATCH_NAME_FORMATTER
import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine
import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine.GuestfishCommand
import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine.GuestfishCommandsBuilder
import com.bkahlert.kustomize.libguestfs.GuestfishDsl
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.FirstBootCommandOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomizationsBuilder
import com.bkahlert.kustomize.libguestfs.VirtCustomizeDsl
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.boot

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
    val diskOperations: List<() -> Unit>

    /**
     * Options to be applied on the img file
     * using the [VirtCustomizeCommandLine].
     */
    val virtCustomizations: List<VirtCustomization>

    /**
     * Operations to be applied on the mounted file system
     * using the [GuestfishCommandLine] tool.
     */
    val guestfishCommands: List<GuestfishCommand>

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
    override val diskOperations: List<() -> Unit>,
    override val virtCustomizations: List<VirtCustomization>,
    override val guestfishCommands: List<GuestfishCommand>,
    override val osBoot: Boolean,
) : PhasedPatch {
    override val operationCount: Int = diskOperations.size + virtCustomizations.size + guestfishCommands.size + (if (osBoot) 1 else 0)

    override fun patch(osImage: OperatingSystemImage, trace: Boolean): ReturnValues<Throwable> =
        runSpanning(Renderable.of(name) { _, _ -> toString() },
            nameFormatter = PATCH_NAME_FORMATTER,
            decorationFormatter = PATCH_DECORATION_FORMATTER,
            style = Solid
        ) {
            applyDiskOperations() +
                applyVirtCustomizations(osImage, trace) +
                applyGuestfishCommands(osImage, trace) +
                applyOsBoot(osImage)
        }

    private fun collectingExceptions(
        name: CharSequence,
        operationCount: Int,
        transform: SpanScope.((Throwable) -> Unit) -> Unit,
    ): ReturnValues<Throwable> =
        if (operationCount == 0) {
            spanScope { log("◼ $name".ansi.gray) }
            ReturnValues()
        } else {
            runSpanning("$name",
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
        transform: SpanScope.(index: Int, operation: T) -> Unit,
    ): ReturnValues<Throwable> =
        collectingExceptions(name, size) { exceptionCallback ->
            mapIndexedNotNull { index, operation: T ->
                runCatching { transform(index, operation) }.exceptionOrNull()?.also(exceptionCallback)
            }
        }

    private fun applyDiskOperations(): ReturnValues<Throwable> =
        diskOperations.collectingExceptions("disk") { _, preparation ->
            preparation()
        }

    private fun applyVirtCustomizations(osImage: OperatingSystemImage, trace: Boolean): ReturnValues<Throwable> =
        collectingExceptions("virt-customize", virtCustomizations.size) {
            osImage.virtCustomize(trace, *virtCustomizations.toTypedArray())
        }

    private fun applyGuestfishCommands(osImage: OperatingSystemImage, trace: Boolean): ReturnValues<Throwable> =
        collectingExceptions("guestfish", guestfishCommands.size) {
            osImage.guestfish(trace, *guestfishCommands.toTypedArray())
        }

    private fun applyOsBoot(osImage: OperatingSystemImage): ReturnValues<Throwable> =
        collectingExceptions("boot", if (osBoot) 1 else 0) {
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
        val diskOperations: MutableList<() -> Unit> = mutableListOf()
        val virtCustomizations: MutableList<VirtCustomization> = mutableListOf()
        val guestfishCommands: MutableList<GuestfishCommand> = mutableListOf()
        val osBoot: MutableList<Boolean> = mutableListOf()

        PhasedPatchContext(
            diskOperations,
            virtCustomizations,
            guestfishCommands,
            osBoot,
        ).init()

        return SimplePhasedPatch(
            name,
            diskOperations,
            virtCustomizations,
            guestfishCommands,
            osBoot.lastOrNull() ?: false,
        )
    }

    /** Context for building instances of [PhasedPatch]. */
    @VirtCustomizeDsl
    @GuestfishDsl
    @Suppress("PublicApiImplicitType")
    inner class PhasedPatchContext(
        private val diskOperations: MutableList<() -> Unit>,
        private val virtCustomizations: MutableList<VirtCustomization>,
        private val guestfishCommands: MutableList<GuestfishCommand>,
        private val osBoot: MutableList<Boolean>,
    ) {

        /**
         * Operations to be applied on the actual raw `.img` file
         * before operations "inside" the `.img` file take place.
         */
        fun disk(init: ImageOperationsBuilder.ImageOperationsContext.() -> Unit) {
            diskOperations.addAll(ImageOperationsBuilder(osImage).build(init))
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
            guestfishCommands.addAll(GuestfishCommandsBuilder(osImage).build(init))
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
            runSpanning(
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
        val needsBoot = any { patch -> patch.osBoot }
        SimplePhasedPatch(
            name = Renderable.of(joinToString(LineSeparators.LF) { it.name }) { _, _ -> this },
            diskOperations = flatMap { it.diskOperations }.toList(),
            virtCustomizations = buildList {
                this@run.forEach { addAll(it.virtCustomizations) }
                if (needsBoot) add(FirstBootCommandOption(ShellScript("Shutdown") { shutdown }.toString()))
            },
            guestfishCommands = flatMap { it.guestfishCommands }.toList(),
            osBoot = needsBoot,
        )
    }

    override fun toString(): String = asString {
        "patches" to patches
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

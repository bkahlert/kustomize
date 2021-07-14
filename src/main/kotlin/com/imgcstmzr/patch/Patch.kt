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

    var trace: Boolean

    /**
     * Name of the patch.
     */
    val name: String

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
    val fileOperations: List<(OperatingSystemImage) -> FileOperation>

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
        this@Patch.trace = trace
        return spanning(name, nameFormatter = NAME_FORMATTER, blockStyle = Solid) {
            applyDiskPreparations(osImage) +
                applyDiskCustomizations(osImage) +
                applyDiskAndFileOperations(osImage) +
                applyOsPreparations(osImage) +
                applyOsBoot(osImage) +
                applyOsOperations(osImage)
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

    fun applyDiskPreparations(osImage: OperatingSystemImage): ReturnValues<Throwable> {
        if (diskPreparations.isEmpty()) return none("Disk Preparation")
        return collectingExceptions("Disk Preparation (${diskPreparations.size})") {
            collectingExceptions(diskPreparations) { _, preparation -> preparation(osImage) }
        }
    }

    fun applyDiskCustomizations(osImage: OperatingSystemImage): ReturnValues<Throwable> {
        if (diskCustomizations.isEmpty()) return none("Disk Customization")
        return collectingExceptions("Disk Customization (${diskCustomizations.size})") {
            if (diskCustomizations.isNotEmpty()) {
                osImage.virtCustomize(this@Patch.trace) {
                    diskCustomizations.forEach { customizationOption(it) }
                }
            }
        }
    }

    fun applyDiskAndFileOperations(osImage: OperatingSystemImage): ReturnValues<Throwable> {
        val exceptions = ReturnValues<Throwable>()

        val diskOperationsAndFilePreparationOperations = diskOperations.size + fileOperations.size
        if (diskOperationsAndFilePreparationOperations == 0) return none("Disk Operations")

        osImage.hostPath(LinuxRoot).deleteDirectoryEntriesRecursively()
        collectingExceptions("Disk Operations (${diskOperationsAndFilePreparationOperations})") {
            if (diskOperationsAndFilePreparationOperations > 0) {
                osImage.guestfish(this@Patch.trace) {
                    fileOperations.map { it(osImage).target }.forEach { sourcePath ->
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
                    val path = fileOperation(osImage).target
                    fileOperation(osImage).invoke(osImage.hostPath(path))
                }.forEach { exceptions.add(it) }
            }

            val changedFiles = countFiles()
            if (changedFiles > 0) {
                collectingExceptions("Copying in $changedFiles file(s)") {
                    osImage.guestfish(this@Patch.trace) {
                        tarIn()
                    }
                }.also { exceptions.addAll(it) }
            }
        }

        return exceptions
    }

    fun applyOsPreparations(osImage: OperatingSystemImage): ReturnValues<Throwable> {
        if (osPreparations.isEmpty()) return none("OS Preparation")
        return collectingExceptions("OS Preparation (${osPreparations.size})") {
            collectingExceptions(osPreparations) { _, preparation -> preparation(osImage) }
        }
    }

    fun applyOsBoot(osImage: OperatingSystemImage): ReturnValues<Throwable> {
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


    fun applyOsOperations(osImage: OperatingSystemImage): ReturnValues<Throwable> {
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
                SimplePatch(
                    false,
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


data class SimplePatch(
    override var trace: Boolean,
    override val name: String,
    override val diskPreparations: List<DiskOperation>,
    override val diskCustomizations: List<(OperatingSystemImage) -> Customization>,
    override val diskOperations: List<(OperatingSystemImage) -> GuestfishCommand>,
    override val fileOperations: List<(OperatingSystemImage) -> FileOperation>,
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
) : Patch by SimplePatch(
    patches.any { it.trace },
    patches.joinToString(LineSeparators.LF) { it.name },
    patches.flatMap { it.diskPreparations }.toList(),
    patches.flatMap { it.diskCustomizations }.toList(),
    patches.flatMap { it.diskOperations }.toList(),
    patches.flatMap { it.fileOperations }.toList(),
    patches.flatMap { it.osPreparations }.toList(),
    patches.any { patch -> patch.osBoot },
    patches.flatMap { it.osOperations }.toList(),
)

class FileOperation(val target: DiskPath, val verifier: (Path) -> Unit, val handler: (Path) -> Unit) {

    operator fun invoke(target: Path) {
        spanningLine(target.fileName.toString()) {
            log("Action needed?")
            val result = runCatching { verifier.invoke(target) }
            if (result.isFailure) {
                log("Yes".formattedAs.warning)

                handler.invoke(target)

                log("Verifying")
                verifier.invoke(target)
            }
        }
    }
}

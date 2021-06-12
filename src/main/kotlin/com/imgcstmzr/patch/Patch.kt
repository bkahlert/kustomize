package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.GuestfishCommandLine
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommandsBuilder
import com.imgcstmzr.libguestfs.GuestfishDsl
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.CustomizationsBuilder
import com.imgcstmzr.libguestfs.VirtCustomizeDsl
import com.imgcstmzr.logMeta
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
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.logging.FixedWidthRenderingLogger
import koodies.logging.FixedWidthRenderingLogger.Border.DOTTED
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
import koodies.logging.RenderingLogger
import koodies.logging.ReturnValues
import koodies.logging.logging
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.Banner.banner
import koodies.text.LineSeparators
import koodies.text.Semantics.formattedAs
import koodies.text.withRandomSuffix
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
     * Applies this patch to the given [osImage] while logging with `this` [RenderingLogger].
     *
     * Detailed logging can be activated using [trace].
     */
    fun FixedWidthRenderingLogger.patch(osImage: OperatingSystemImage, trace: Boolean = false): ReturnValues<Throwable> {
        this@Patch.trace = trace
        return blockLogging(HEADLINE_FORMATTER(name), border = SOLID) {
            applyDiskPreparations(osImage) +
                applyDiskCustomizations(osImage) +
                applyDiskAndFileOperations(osImage) +
                applyOsPreparations(osImage) +
                applyOsBoot(osImage) +
                applyOsOperations(osImage)
        }
    }

    fun <T, R : Any, L : FixedWidthRenderingLogger> L.runCollecting(
        operations: List<T>,
        transform: (index: Int, operation: T, logger: L) -> R?,
    ): ReturnValues<Throwable> =
        operations.mapIndexedNotNull { index, operation: T ->
            runCatching {
                transform(index, operation, this)
            }.exceptionOrNull()
        }.let { ReturnValues(*it.toTypedArray()) }

    fun <R : Any, L : FixedWidthRenderingLogger> L.runCollecting(
        transform: (logger: L) -> R?,
    ): ReturnValues<Throwable> = runCatching {
        transform(this)
    }.exceptionOrNull()?.let { ReturnValues(it) } ?: ReturnValues()

    fun FixedWidthRenderingLogger.applyDiskPreparations(osImage: OperatingSystemImage): ReturnValues<Throwable> =
        if (diskPreparations.isEmpty()) ReturnValues<Throwable>().also { logLine { HEADLINE_FORMATTER("Disk Preparation: —") } }
        else logging(
            HEADLINE_FORMATTER("Disk Preparation (${diskPreparations.size})"),
            decorationFormatter = HEADLINE_FORMATTER,
            border = DOTTED,
        ) {
            runCollecting(diskPreparations) { _, preparation, logger -> preparation(osImage, logger) }
        }

    fun FixedWidthRenderingLogger.applyDiskCustomizations(osImage: OperatingSystemImage): ReturnValues<Throwable> =
        if (diskCustomizations.isEmpty()) {
            logLine { HEADLINE_FORMATTER("Disk Customization: —") }
            ReturnValues()
        } else {
            runCollecting {
                logging(
                    HEADLINE_FORMATTER("Disk Customization (${diskCustomizations.size})"),
                    decorationFormatter = HEADLINE_FORMATTER,
                    border = DOTTED,
                ) {
                    osImage.virtCustomize(this, trace = trace) {
                        diskCustomizations.forEach { customizationOption(it) }
                    }
                }
            }
        }

    fun FixedWidthRenderingLogger.applyDiskAndFileOperations(osImage: OperatingSystemImage): ReturnValues<Throwable> =
        if (diskOperations.isEmpty() && fileOperations.isEmpty()) {
            logLine { HEADLINE_FORMATTER("Disk Operations: —") }
            logLine { HEADLINE_FORMATTER("File Operations: —") }
            ReturnValues()
        } else {
            val exceptions = ReturnValues<Throwable>()

            runCollecting {
                logging(
                    HEADLINE_FORMATTER("Disk Operations (${diskOperations.size})"),
                    null,
                    decorationFormatter = HEADLINE_FORMATTER,
                    border = DOTTED,
                ) {
                    osImage.guestfish(this, trace, {
                        "Applying ${diskOperations.size} disk and ${fileOperations.size} file operation(s)"
                    }, false) {
                        fileOperations.map { it(osImage).target }.forEach { sourcePath ->
                            copyOut { sourcePath }
                        }
                        diskOperations.forEach { command(it) }
                    }
                }
            }.also { exceptions.addAll(it) }

            logging(
                HEADLINE_FORMATTER("File Operations (${fileOperations.size.coerceAtLeast(1)})"),
                decorationFormatter = HEADLINE_FORMATTER,
                border = DOTTED,
            ) {
                val filesToPatch = fileOperations.toMutableList()
                if (filesToPatch.isNotEmpty()) {
                    runCollecting(filesToPatch) { _, fileOperation, logger ->
                        val path = fileOperation(osImage).target
                        fileOperation(osImage).invoke(osImage.hostPath(path), logger)
                    }.forEach { exceptions.add(it) }
                }

                val changedFiles = osImage.hostPath(LinuxRoot).listDirectoryEntriesRecursively().filter { it.isRegularFile() }.size
                if (changedFiles > 0) {
                    runCollecting {
                        osImage.guestfish(this, trace, { "Copying in $changedFiles file(s)" }, false) {
                            tarIn()
                        }
                    }.also { exceptions.addAll(it) }
                } else {
                    logMeta("No changed files to copy back.")
                }
            }

            exceptions
        }

    fun FixedWidthRenderingLogger.applyOsPreparations(osImage: OperatingSystemImage): ReturnValues<Throwable> =
        if (osPreparations.isEmpty()) ReturnValues<Throwable>().also { logLine { HEADLINE_FORMATTER("OS Preparation: —") } }
        else compactLogging(
            HEADLINE_FORMATTER("OS Preparation (${osPreparations.size})"),
            decorationFormatter = HEADLINE_FORMATTER,
        ) {
            runCollecting(osPreparations) { _, preparation, logger -> preparation(osImage, logger) }
        }

    fun FixedWidthRenderingLogger.applyOsBoot(osImage: OperatingSystemImage): ReturnValues<Throwable> =
        if (!osBoot) ReturnValues<Throwable>().also { logLine { HEADLINE_FORMATTER("OS Boot: —") } }
        else runCollecting {
            osImage.boot(
                name.withRandomSuffix(),
                this@applyOsBoot,
                headlineFormatter = HEADLINE_FORMATTER,
                decorationFormatter = HEADLINE_FORMATTER,
                autoLogin = false,
                autoShutdown = false,
                border = DOTTED,
            )
        }

    fun FixedWidthRenderingLogger.applyOsOperations(osImage: OperatingSystemImage): ReturnValues<Throwable> =
        if (osOperations.isEmpty()) ReturnValues<Throwable>().also { logLine { HEADLINE_FORMATTER("OS Operations: —") } }
        else runCollecting {
            osImage.boot(
                name.withRandomSuffix(),
                this@applyOsOperations,
                *osOperations.map { it(osImage) }.toTypedArray(),
                headlineFormatter = HEADLINE_FORMATTER,
                decorationFormatter = HEADLINE_FORMATTER,
                border = DOTTED,
            )
        }

    val operationCount: Int

    val isEmpty: Boolean get() = operationCount == 0
    val isNotEmpty: Boolean get() = !isEmpty

    companion object : BuilderTemplate<PatchContext, (String) -> Patch>() {

        private val HEADLINE_FORMATTER = Formatter { it.ansi.cyan }

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
                    diskOperation { osImage: OperatingSystemImage, logger: FixedWidthRenderingLogger ->
                        osImage.increaseDiskSpace(size, logger)
                    }
                }

                /**
                 * Changes the username to be used for login from [oldUsername] to [newUsername].
                 */
                fun updateUsername(oldUsername: String, newUsername: String) {
                    diskOperation { osImage: OperatingSystemImage, logger: RenderingLogger ->
                        osImage.credentials = osImage.credentials.copy(username = newUsername)
                        logger.logMeta("Username of user ${oldUsername.formattedAs.input} updated to ${newUsername.formattedAs.input}.")
                    }
                }

                /**
                 * Changes the password used to login using the given [username] to [password].
                 */
                fun updatePassword(username: String, password: String) {
                    diskOperation { osImage: OperatingSystemImage, logger: RenderingLogger ->
                        if (osImage.credentials.username == username) {
                            osImage.credentials = osImage.credentials.copy(password = password)
                            logger.logMeta("Password of user ${username.formattedAs.input} updated.")
                        } else {
                            logger.logMeta("Password of user ${password.formattedAs.input} updated${"*".ansi.magenta}.")
                            logger.logMeta("ImgCstmzr will to continue to use user ${osImage.credentials.username.formattedAs.input}.")
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
 * Applies this patch to the given [OperatingSystemImage] while logging with `this` [RenderingLogger].
 */
inline val FixedWidthRenderingLogger.patch: Patch.(osImage: OperatingSystemImage) -> ReturnValues<Throwable>
    get() = { osImage: OperatingSystemImage -> patch(osImage) }

typealias DiskOperation = (osImage: OperatingSystemImage, logger: FixedWidthRenderingLogger) -> Unit

/**
 * Applies all patches of this collection to the given [OperatingSystemImage] while logging with `this` [RenderingLogger].
 */
fun List<Patch>.patch(osImage: OperatingSystemImage): ReturnValues<Throwable> =
    logging(banner("Applying $size patches to ${osImage.shortName}")) {
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

    operator fun invoke(target: Path, logger: FixedWidthRenderingLogger) {
        logger.compactLogging(target.fileName.toString()) {
            logLine { "Action needed? …".ansi.yellow }
            val result = runCatching { verifier.invoke(target) }
            if (result.isFailure) {
                logLine { "Yes …".ansi.yellow.bold }

                handler.invoke(target)

                logLine { "Verifying …".ansi.yellow }
                verifier.invoke(target)
            }
        }
    }
}

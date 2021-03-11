package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.libguestfs.HostPath
import com.imgcstmzr.libguestfs.Libguestfs.Companion.hostPath
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommand
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.Companion.copyOut
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.Companion.guestfish
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.GuestfishCommandsBuilder
import com.imgcstmzr.libguestfs.guestfish.GuestfishDsl
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine.Companion.virtCustomize
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine.VirtCustomizeCustomizationOptionsBuilder
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeDsl
import com.imgcstmzr.patch.Patch.Companion.FileSystemOperationsCollector.FileSystemOperationsContext
import com.imgcstmzr.patch.Patch.Companion.ImgOperationsCollector.ImgOperationsContext
import com.imgcstmzr.patch.Patch.Companion.PatchContext
import com.imgcstmzr.patch.Patch.Companion.ProgramsBuilder.ProgramsContext
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystemProcess
import com.imgcstmzr.runtime.Program
import com.imgcstmzr.runtime.execute
import koodies.builder.BooleanBuilder
import koodies.builder.BuilderTemplate
import koodies.builder.Init
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.concurrent.process.IO.Type.META
import koodies.concurrent.process.IO.Type.OUT
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.io.path.renameTo
import koodies.logging.*
import koodies.terminal.ANSI
import koodies.terminal.AnsiColors.brightMagenta
import koodies.terminal.AnsiColors.magenta
import koodies.terminal.AnsiColors.yellow
import koodies.terminal.AnsiFormats.bold
import koodies.text.LineSeparators
import koodies.text.quoted
import koodies.text.withRandomSuffix
import koodies.unit.Size
import kotlin.io.path.exists
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
    val diskCustomizations: List<(OperatingSystemImage) -> VirtCustomizeCustomizationOption>

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

    fun patch(osImage: OperatingSystemImage, logger: RenderingLogger?, trace: Boolean = false): ReturnValues<Throwable> {
        this.trace = trace
        return logger.blockLogging(name.brightMagenta(), null, true) {
            applyDiskPreparations(osImage) +
                applyDiskCustomizations(osImage) +
                applyDiskAndFileOperations(osImage) +
                applyOsPreparations(osImage) +
                applyOsBoot(osImage) +
                applyOsOperations(osImage)
        }
    }

    fun <T, R : Any, L : RenderingLogger> L.runCollecting(
        operations: List<T>,
        transform: (index: Int, operation: T, logger: L) -> R?,
    ): ReturnValues<Throwable> =
        operations.mapIndexedNotNull { index, operation: T ->
            runCatching {
                transform(index, operation, this)
            }.exceptionOrNull()
        }.let { ReturnValues(*it.toTypedArray()) }

    fun <R : Any, L : RenderingLogger> L.runCollecting(
        transform: (logger: L) -> R?,
    ): ReturnValues<Throwable> = runCatching {
        transform(this)
    }.exceptionOrNull()?.let { ReturnValues(it) } ?: ReturnValues()

    fun RenderingLogger.applyDiskPreparations(osImage: OperatingSystemImage): ReturnValues<Throwable> =
        if (diskPreparations.isEmpty()) ReturnValues<Throwable>().also { logLine { META typed "Disk Preparation: —" } }
        else logging("Disk Preparation (${diskPreparations.size})", null, bordered = false) {
            runCollecting(diskPreparations) { _, preparation, logger -> preparation(osImage, logger) }
        }

    fun RenderingLogger.applyDiskCustomizations(osImage: OperatingSystemImage): ReturnValues<Throwable> =
        if (diskCustomizations.isEmpty()) {
            logLine { META typed "Disk Customization: —" }
            ReturnValues()
        } else {
            runCollecting {
                logging("Disk Customization (${diskCustomizations.size})", null, bordered = false) {
                    virtCustomize(osImage, trace = trace) {
                        diskCustomizations.forEach { customizationOption(it) }
                    }
                }
            }
        }

    fun RenderingLogger.applyDiskAndFileOperations(osImage: OperatingSystemImage): ReturnValues<Throwable> =
        if (diskOperations.isEmpty() && fileOperations.isEmpty()) {
            logLine { META typed "Disk Operations: —" }
            logLine { META typed "File Operations: —" }
            ReturnValues()
        } else {
            val exceptions = ReturnValues<Throwable>()

            runCollecting {
                logging("Disk Operations (${diskOperations.size})", null, bordered = false) {
                    guestfish(
                        osImage,
                        trace = trace,
                        { "Applying ${diskOperations.size} disk and ${fileOperations.size} file operation(s)" },
                        bordered = false,
                    ) {
                        fileOperations.map { it(osImage).target }.forEach { sourcePath ->
                            copyOut { sourcePath }
                        }
                        diskOperations.forEach { command(it) }
                    }
                }
            }.also { exceptions.addAll(it) }

            logging("File Operations (${fileOperations.size.coerceAtLeast(1)})", null, bordered = false) {
                val filesToPatch = fileOperations.toMutableList()
                if (filesToPatch.isNotEmpty()) {
                    runCollecting(filesToPatch) { _, fileOperation, logger ->
                        val path = fileOperation(osImage).target
                        fileOperation(osImage).invoke(osImage.hostPath(path), logger)
                    }.forEach { exceptions.add(it) }
                }

                val changedFiles = osImage.hostPath(DiskPath("/")).listDirectoryEntriesRecursively().filter { it.isRegularFile() }.size
                if (changedFiles > 0) {
                    runCollecting {
                        guestfish(
                            osImage,
                            trace = trace,
                            { "Copying in $changedFiles file(s)" }) {
                            tarIn()
                        }
                    }.also { exceptions.addAll(it) }
                } else {
                    logLine { META typed "No changed files to copy back." }
                }
            }

            exceptions
        }

    fun RenderingLogger.applyOsPreparations(osImage: OperatingSystemImage): ReturnValues<Throwable> =
        if (osPreparations.isEmpty()) ReturnValues<Throwable>().also { logLine { META typed "OS Preparation: —" } }
        else compactLogging("OS Preparation (${osPreparations.size})") {
            runCollecting(osPreparations) { _, preparation, logger -> preparation(osImage, logger) }
        }

    fun RenderingLogger.applyOsBoot(osImage: OperatingSystemImage): ReturnValues<Throwable> =
        if (!osBoot) ReturnValues<Throwable>().also { logLine { META typed "OS Boot: —" } }
        else runCollecting {
            osImage.execute(
                name = name.withRandomSuffix(),
                logger = this@applyOsBoot,
                autoLogin = false,
                autoShutdown = false,
                bordered = false,
            )
        }

    fun RenderingLogger.applyOsOperations(osImage: OperatingSystemImage): ReturnValues<Throwable> =
        if (osOperations.isEmpty()) ReturnValues<Throwable>().also { logLine { META typed "OS Operations: —" } }
        else runCollecting {
            osImage.execute(
                name = name.withRandomSuffix(),
                logger = this@applyOsOperations,
                autoLogin = true,
                autoShutdown = true,
                bordered = false,
                *osOperations.map { it(osImage) }.toTypedArray()
            )
        }

    val operationCount: Int

    val isEmpty: Boolean get() = operationCount == 0
    val isNotEmpty: Boolean get() = !isEmpty


    companion object : BuilderTemplate<PatchContext, (String) -> Patch>() {

        @DslMarker
        annotation class PatchDsl

        fun buildPatch(name: String, init: Init<PatchContext>): Patch = invoke(init)(name)

        @PatchDsl
        @VirtCustomizeDsl
        @GuestfishDsl
        class PatchContext(override val captures: CapturesMap) : CapturingContext() {

            val prepareDisk by ImgOperationsCollector default emptyList()

            @VirtCustomizeDsl
            val customizeDisk by VirtCustomizeCustomizationOptionsBuilder default emptyList()

            @GuestfishDsl
            val guestfish by GuestfishCommandsBuilder default emptyList()
            val files by FileSystemOperationsCollector default emptyList()
            val osPrepare by ImgOperationsCollector default emptyList()
            val boot by BooleanBuilder.YesNo default { no }
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

                fun resize(size: Size) {
                    diskOperation { osImage: OperatingSystemImage, logger: RenderingLogger ->
                        osImage.increaseDiskSpace(logger, size)
                    }
                }

                fun updateUsername(oldUsername: String, newUsername: String) {
                    diskOperation { osImage: OperatingSystemImage, logger: RenderingLogger ->
                        osImage.credentials = osImage.credentials.copy(username = newUsername)
                        logger.logLine { META.format("Username of user ${oldUsername.quoted} updated to ${newUsername.quoted}.") }
                    }
                }

                fun updatePassword(username: String, password: String) {
                    diskOperation { osImage: OperatingSystemImage, logger: RenderingLogger ->
                        if (osImage.credentials.username == username) {
                            osImage.credentials = osImage.credentials.copy(password = password)
                            logger.logLine { META.format("Password of user ${username.quoted} updated.") }
                        } else {
                            logger.logLine { META.format("Password of user ${password.quoted} updated${"*".magenta()}.") }
                            logger.logLine { META.format("ImgCstmzr will to continue to use user ${osImage.credentials.username.quoted}.") }
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

                val fileOperation by function<(OperatingSystemImage) -> FileOperation>()

                fun edit(path: DiskPath, validator: (HostPath) -> Unit, operations: (HostPath) -> Unit) =
                    fileOperation { FileOperation(path, validator, operations) }

            }

            override fun BuildContext.build(): List<(OperatingSystemImage) -> FileOperation> = ::FileSystemOperationsContext {
                ::fileOperation.evalAll()
            }
        }

        object ProgramsBuilder : BuilderTemplate<ProgramsContext, List<(OperatingSystemImage) -> Program>>() {

            @PatchDsl
            class ProgramsContext(override val captures: CapturesMap) : CapturingContext() {

                val programs by function<(OperatingSystemImage) -> Program>()

                fun program(
                    purpose: String,
                    initialState: OperatingSystemProcess.() -> String?,
                    vararg states: Pair<String, OperatingSystemProcess.(String) -> String?>,
                ) = programs { Program(purpose, initialState, *states) }

                fun script(name: String, vararg commandLines: String) =
                    programs { osImage: OperatingSystemImage -> osImage.compileScript(name, *commandLines) }

            }

            override fun BuildContext.build(): List<(OperatingSystemImage) -> Program> = ::ProgramsContext {
                ::programs.evalAll()
            }
        }
    }
}

typealias DiskOperation = (osImage: OperatingSystemImage, logger: RenderingLogger) -> Unit

fun List<Patch>.patch(osImage: OperatingSystemImage): List<Throwable> =
    blockLogging("Applying $size patches to ${osImage.shortName}") {
        flatMap {
            patch(osImage, it)
                .also {
                    osImage.copyOut("/usr/lib/virt-sysprep")
                        .takeIf { it.exists() }
                        ?.also { it.renameTo("${System.currentTimeMillis()}-${it.fileName}") }
                }
        }
    }

fun RenderingLogger?.patch(osImage: OperatingSystemImage, patch: Patch): List<Throwable> =
    patch.patch(osImage, this)


data class SimplePatch(
    override var trace: Boolean,
    override val name: String,
    override val diskPreparations: List<DiskOperation>,
    override val diskCustomizations: List<(OperatingSystemImage) -> VirtCustomizeCustomizationOption>,
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

@Deprecated("no more used")
class FileOperation(val target: DiskPath, val verifier: (HostPath) -> Any, val handler: (HostPath) -> Any) {

    operator fun invoke(target: HostPath, logger: RenderingLogger) {
        logger.compactLogging(target.fileName.toString()) {
            logLine { OUT typed ANSI.termColors.yellow("Action needed? …") }
            val result = runCatching { verifier.invoke(target) }
            if (result.isFailure) {
                logLine { OUT typed " Yes …".yellow().bold() }

                handler.invoke(target)

                logLine { OUT typed ANSI.termColors.yellow("Verifying …") }
                runCatching { verifier.invoke(target) }.onFailure {
                    logCaughtException { it }
                }
            }
        }
    }
}

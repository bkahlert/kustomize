package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.SharedPath
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommand
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.Companion.runGuestfishOn
import com.imgcstmzr.libguestfs.resolveOnDisk
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine.Companion.virtCustomize
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption
import com.imgcstmzr.patch.Operation.Status.*
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.Program
import com.imgcstmzr.runtime.execute
import com.imgcstmzr.util.asRootFor
import koodies.concurrent.process.IO.Type.META
import koodies.concurrent.process.IO.Type.OUT
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.logging.*
import koodies.terminal.ANSI
import koodies.terminal.AnsiColors.brightMagenta
import koodies.terminal.AnsiColors.yellow
import koodies.terminal.AnsiFormats.bold
import koodies.text.LineSeparators
import koodies.text.withRandomSuffix
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

    fun patch(osImage: OperatingSystemImage, logger: RenderingLogger?, trace: Boolean = false): List<Throwable> {
        this.trace = trace
        return logger.logging(name.brightMagenta(), null, true) {
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
    ): List<Throwable> =
        operations.mapIndexedNotNull { index, operation: T ->
            runCatching {
                transform(index, operation, this)
            }.exceptionOrNull()
        }

    fun <R : Any, L : RenderingLogger> L.runCollecting(
        transform: (logger: L) -> R?,
    ): List<Throwable> = runCatching {
        transform(this)
    }.exceptionOrNull()?.let { listOf(it) } ?: emptyList()

    fun RenderingLogger.applyDiskPreparations(osImage: OperatingSystemImage): List<Throwable> =
        if (diskPreparations.isEmpty()) emptyList<Throwable>().also { logLine { META typed "Disk Preparation: —" } }
        else logging("Disk Preparation (${diskPreparations.size})", null, bordered = false) {
            runCollecting(diskPreparations) { _, preparation, logger -> preparation(osImage, logger) }
        }

    fun RenderingLogger.applyDiskCustomizations(osImage: OperatingSystemImage): List<Throwable> =
        if (diskCustomizations.isEmpty()) {
            logLine { META typed "Disk Customization: —" }
            emptyList()
        } else {
            runCollecting {
                logging("Disk Customization (${diskCustomizations.size})", null, bordered = false) {
                    virtCustomize(osImage, trace = trace) {
                        +diskCustomizations
                    }
                }
            }
        }

    fun RenderingLogger.applyDiskAndFileOperations(osImage: OperatingSystemImage): List<Throwable> =
        if (diskOperations.isEmpty() && fileOperations.isEmpty()) {
            logLine { META typed "Disk Operations: —" }
            logLine { META typed "File Operations: —" }
            emptyList()
        } else {
            val exceptions = mutableListOf<Throwable>()

            runCollecting {
                logging("Disk Operations (${diskOperations.size})", null) {
                    runGuestfishOn(
                        osImage,
                        trace = trace,
                        { "Applying ${diskOperations.size} and fishing for ${fileOperations.size} files" },
                        bordered = false,
                    ) {
                        fileOperations.map { it.target }.forEach { sourcePath ->
                            val sanitizedSourcePath = Path.of("/").asRootFor(sourcePath)
                            copyOut { it.resolveOnDisk(sanitizedSourcePath) }
                        }
                        +diskOperations
                    }
                }
            }.also { exceptions.addAll(it) }

            logging("File Operations (${fileOperations.size})", null) {
                val root = SharedPath.Host.resolveRoot(osImage)
                val filesToPatch = fileOperations.toMutableList()
                if (filesToPatch.isNotEmpty()) {
                    runCollecting(filesToPatch) { _, fileOperation, logger ->
                        val path = fileOperation.target
                        fileOperation.invoke(root.asRootFor(path), logger)
                    }.forEach { exceptions.add(it) }
                } else {
                    logLine { META typed "No files to patch." }
                }

                val changedFiles = root.listDirectoryEntriesRecursively().filter { it.isRegularFile() }.size
                if (changedFiles > 0) {
                    runCollecting {
                        runGuestfishOn(
                            osImage,
                            trace = trace,
                            { "Throwing $changedFiles files back into water" }) { tarIn() }
                    }.also { exceptions.addAll(it) }
                } else {
                    logLine { META typed "No changed files to copy back." }
                }
            }

            exceptions
        }

    fun RenderingLogger.applyOsPreparations(osImage: OperatingSystemImage): List<Throwable> =
        if (osPreparations.isEmpty()) emptyList<Throwable>().also { logLine { META typed "OS Preparation: —" } }
        else logging("OS Preparation (${osPreparations.size})", null, bordered = false) {
            runCollecting(osPreparations) { _, preparation, logger -> preparation(osImage, logger) }
        }

    fun RenderingLogger.applyOsBoot(osImage: OperatingSystemImage): List<Throwable> =
        if (!osBoot) emptyList<Throwable>().also { logLine { META typed "OS Boot: —" } }
        else runCollecting {
            osImage.execute(
                name = name.withRandomSuffix(),
                logger = this@applyOsBoot,
                autoLogin = false,
                autoShutdown = false,
            )
        }

    fun RenderingLogger.applyOsOperations(osImage: OperatingSystemImage): List<Throwable> =
        if (osOperations.isEmpty()) emptyList<Throwable>().also { logLine { META typed "OS Operations: —" } }
        else runCollecting {
            osImage.execute(
                name = name.withRandomSuffix(),
                logger = this@applyOsOperations,
                autoLogin = true,
                autoShutdown = true,
                *osOperations.map { it(osImage) }.toTypedArray()
            )
        }
}

fun List<Patch>.patch(osImage: OperatingSystemImage): List<Throwable> =
    logging("Applying $size patches to ${osImage.shortName}") {
        flatMap { patch(osImage, it) }
    }

fun RenderingLogger?.patch(osImage: OperatingSystemImage, patch: Patch): List<Throwable> =
    patch.patch(osImage, this)

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

interface Operation<TARGET> : HasStatus {
    var currentStatus: Status

    enum class Status(private val formatter: (String) -> String) {
        Ready({ label -> ANSI.termColors.bold(label) }),
        Running({ label -> ANSI.termColors.bold(label) }),
        Finished({ label -> ANSI.termColors.strikethrough(label) }),
        Failure({ label -> ANSI.termColors.red(label) });

        operator fun invoke(label: String): String = formatter(label)
    }

    operator fun invoke(target: TARGET, logger: BlockRenderingLogger)

    val target: TARGET

    override fun renderStatus(): String
}

class FileOperation(override val target: Path, val verifier: (Path) -> Any, val handler: (Path) -> Any) :
    Operation<Path> {

    override var currentStatus: Operation.Status = Ready

    override operator fun invoke(target: Path, logger: BlockRenderingLogger) {
        logger.compactLogging(target.fileName.toString()) {
            logLine { OUT typed ANSI.termColors.yellow("Action needed? ...") }
            val result = runCatching { verifier.invoke(target) }
            if (result.isFailure) {
                currentStatus = Running
                logLine { OUT typed " Yes...".yellow().bold() }

                handler.invoke(target)

                logLine { OUT typed ANSI.termColors.yellow("Verifying ...") }
                runCatching { verifier.invoke(target) }.onFailure {
                    currentStatus = Failure
                }
            }
            currentStatus = Finished
        }
    }

    override fun renderStatus(): String = currentStatus(target.fileName.toString())
}

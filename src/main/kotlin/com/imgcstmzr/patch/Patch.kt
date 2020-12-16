package com.imgcstmzr.patch

import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.string.withRandomSuffix
import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiColors.yellow
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.bold
import com.imgcstmzr.libguestfs.SharedPath
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommand
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.Companion.runGuestfishOn
import com.imgcstmzr.libguestfs.resolveOnDisk
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine.Companion.virtCustomize
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption
import com.imgcstmzr.patch.Operation.Status.Failure
import com.imgcstmzr.patch.Operation.Status.Finished
import com.imgcstmzr.patch.Operation.Status.Ready
import com.imgcstmzr.patch.Operation.Status.Running
import com.imgcstmzr.runtime.HasStatus
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.Program
import com.imgcstmzr.runtime.execute
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.logging
import com.imgcstmzr.runtime.log.singleLineLogging
import com.imgcstmzr.util.asRootFor
import com.imgcstmzr.util.isFile
import com.imgcstmzr.util.listFilesRecursively
import java.nio.file.Path

/**
 * A patch to customize an [OperatingSystemImage]
 * by applying a range of operations on it.
 */
interface Patch {
    /**
     * Name of the patch.
     */
    val name: String

    /**
     * Operations to be applied on the actual raw `.img` file
     * before operations "inside" the `.img` file take place.
     */
    @Deprecated("replace with virtcustomize")
    val preFileImgOperations: List<ImgOperation>

    /**
     * Options to be applied on the img file
     * using the [VirtCustomizeCommandLine].
     */
    val customizationOptions: List<(OperatingSystemImage) -> VirtCustomizeCustomizationOption>

    /**
     * Operations to be applied on the mounted file system
     * using the [Guestfish] tool.
     */
    val guestfishCommands: List<(OperatingSystemImage) -> GuestfishCommand>

    /**
     * Operations on files of the externally, not immediately
     * accessible file system.
     */
    val fileSystemOperations: List<PathOperation>

    /**
     * Operations to be applied on the actual raw `.img` file
     * after operations "inside" the `.img` file take place.
     */
    val postFileImgOperations: List<ImgOperation>

    /**
     * Operations (in the form of [programs]) to be applied
     * on the booted operating system.
     */
    val programs: List<Program>

    fun RenderingLogger?.patch(osImage: OperatingSystemImage) =
        logging(name.toUpperCase(), null, borderedOutput = false) {
            applyPreFileImgOperations(osImage)
            applyCustomizationOptions(osImage)
            applyGuestfishCommandsAndFileSystemOperations(osImage)
            applyPostFileImgOperations(osImage)
            applyPrograms(osImage)
        }

    fun RenderingLogger.applyPreFileImgOperations(osImage: OperatingSystemImage): Any =
        if (preFileImgOperations.isEmpty()) {
            logLine { META typed "IMG Operations: —" }
        } else {
            logging("IMG Operations (${preFileImgOperations.size})", null, borderedOutput = false) {
                preFileImgOperations.onEachIndexed { _, op ->
                    op(osImage, this)
                }
            }
        }

    fun RenderingLogger.applyCustomizationOptions(osImage: OperatingSystemImage): Any =
        if (customizationOptions.isEmpty()) {
            logLine { META typed "Customization Options: —" }
        } else {
            logging("Customization Options (${customizationOptions.size})", null, borderedOutput = false) {
                virtCustomize(osImage) {
                    +customizationOptions
                }
            }
        }

    fun RenderingLogger.applyGuestfishCommands(osImage: OperatingSystemImage): Any =
        if (guestfishCommands.isEmpty()) {
            logLine { META typed "Guestfish Commands: —" }
        } else {
            logging("Guestfish Commands (${guestfishCommands.size})", null, borderedOutput = false) {
                runGuestfishOn(osImage) {
                    +guestfishCommands
                }
            }
        }

    fun RenderingLogger.applyGuestfishCommandsAndFileSystemOperations(osImage: OperatingSystemImage): Any =
        if (guestfishCommands.isEmpty() && fileSystemOperations.isEmpty()) {
            logLine { META typed "File System Operations: —" }
        } else {
            logging("File System Operations (${guestfishCommands.size + fileSystemOperations.size})", null) {
                logLine { META typed "Starting Guestfish VM..." }

                applyGuestfishCommands(osImage)

                val guestPaths: List<Path> = fileSystemOperations.map { it.target }
                if (guestPaths.isNotEmpty()) {
                    runGuestfishOn(osImage) {
                        guestPaths.forEach { sourcePath ->
                            val sanitizedSourcePath = Path.of("/").asRootFor(sourcePath)
                            copyOut { it.resolveOnDisk(sanitizedSourcePath) }
                        }
                        +guestfishCommands
                    }
                } else {
                    logLine { META typed "No files to extract." }
                }

                val root = SharedPath.Host.resolveRoot(osImage)
                val filesToPatch = fileSystemOperations.toMutableList()
                if (filesToPatch.isNotEmpty()) {
                    val unprocessedActions: MutableList<PathOperation> = filesToPatch
                    while (unprocessedActions.isNotEmpty()) {
                        val action = unprocessedActions.removeFirst()
                        val path = action.target
                        action.invoke(root.asRootFor(path), this)
                    }
                } else {
                    logLine { META typed "No files to patch." }
                }

                val changedFiles = root.listFilesRecursively({ it.isFile }).map { root.relativize(it) }.toList()
                if (changedFiles.isNotEmpty()) {
                    logging("Syncing changes back") {
                        runGuestfishOn(osImage) { tarIn() }
                    }
                } else {
                    logLine { META typed "No changed files to copy back." }
                }
            }
        }

    fun RenderingLogger.applyPostFileImgOperations(osImage: OperatingSystemImage): Any =
        if (postFileImgOperations.isEmpty()) {
            logLine { META typed "IMG Operations II: —" }
            false
        } else {
            logging("IMG Operations II (${postFileImgOperations.size})", null, borderedOutput = false) {
                postFileImgOperations.onEachIndexed { index, op ->
                    op(osImage, this)
                }
            }
            true
        }

    fun RenderingLogger.applyPrograms(osImage: OperatingSystemImage): Any =
        if (programs.isEmpty()) {
            logLine { META typed "Scripts: —" }
        } else {
            logging("Scripts (${programs.size})", null) {
                programs.onEach { program ->
                    osImage.execute(
                        name = program.name.withRandomSuffix(),
                        logger = this,
                        autoLogin = true,
                        program
                    )
                }
            }
        }
}

/**
 * A patch that combines the specified [patches].
 *
 * Composing patches allows for faster image customizations
 * as less reboots are necessary.
 */
class CompositePatch(
    private val patches: Collection<Patch>,
) : Patch by SimplePatch(
    patches.joinToString(" + ") { it.name },
    patches.flatMap { it.preFileImgOperations }.toList(),
    patches.flatMap { it.customizationOptions }.toList(),
    patches.flatMap { it.guestfishCommands }.toList(),
    patches.flatMap { it.fileSystemOperations }.toList(),
    patches.flatMap { it.postFileImgOperations }.toList(),
    patches.flatMap { it.programs }.toList(),
)

interface Operation<TARGET> : HasStatus {
    var currentStatus: Status

    enum class Status(private val formatter: (String) -> String) {
        Ready({ label -> ANSI.termColors.bold(label) }),
        Running({ label -> ANSI.termColors.bold(label) }),
        Finished({ label -> ANSI.termColors.strikethrough(label) }),
        Failure({ label -> ANSI.termColors.red(label + "XXTODO") });

        operator fun invoke(label: String): String = formatter(label)
    }

    operator fun invoke(target: TARGET, log: BlockRenderingLogger)

    val target: TARGET

    override fun status(): String
}

class PathOperation(override val target: Path, val verifier: (Path) -> Any, val handler: (Path) -> Any) : Operation<Path> {

    override var currentStatus: Operation.Status = Ready

    override operator fun invoke(target: Path, log: BlockRenderingLogger) {
        log.singleLineLogging(target.fileName.toString()) {
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
            currentStatus
        }
    }

    override fun status(): String = currentStatus(target.fileName.toString())
}

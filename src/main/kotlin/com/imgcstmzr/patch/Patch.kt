package com.imgcstmzr.patch

import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.docker.DockerContainerName
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.nio.file.toPath
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiColors.blue
import com.bkahlert.koodies.terminal.ansi.AnsiColors.brightBlue
import com.bkahlert.koodies.terminal.ansi.AnsiColors.yellow
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.bold
import com.bkahlert.koodies.terminal.ascii.wrapWithBorder
import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.guestfish.Guestfish
import com.imgcstmzr.libguestfs.Libguestfs
import com.imgcstmzr.libguestfs.guestfish.CopyOutCommand
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommand
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.Companion.fish
import com.imgcstmzr.libguestfs.resolveOnHost
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine.Companion.customize
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
import kotlin.io.path.relativeTo

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
    val customizationOptions: List<VirtCustomizeCustomizationOption>

    /**
     * Operations to be applied on the mounted file system
     * using the [Guestfish] tool.
     */
    val guestfishCommands: List<GuestfishCommand>

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

fun Patch.banner() { // TODO make use of it or delete
    echo("""
            Patch: $name
              img.raw: ${preFileImgOperations.size}    img.fs: ${guestfishCommands.size}
              host.fs: ${fileSystemOperations.size}    img.run: ${programs.size}
        """.trimIndent().wrapWithBorder(padding = 6, margin = 1, ansiCode = (ANSI.termColors.bold + ANSI.termColors.cyan)))
    echo("")
}

fun Patch.patch(osImage: OperatingSystemImage, logger: RenderingLogger? = null) {
    logger.logging(name.toUpperCase(), null, borderedOutput = false) {
        applyPreFileImgOperations(osImage, this@patch)
        applyCustomizationOptions(osImage, this@patch)
        applyGuestfishCommandsAndFileSystemOperations(osImage, this@patch)
        applyPostFileImgOperations(osImage, this@patch)
        applyPrograms(osImage, this@patch)
    }
}

fun RenderingLogger.applyPreFileImgOperations(osImage: OperatingSystemImage, patch: Patch): Any =
    if (patch.preFileImgOperations.isEmpty()) {
        logLine { META typed "IMG Operations: —" }
    } else {
        logging("IMG Operations (${patch.preFileImgOperations.size})", null, borderedOutput = false) {
            patch.preFileImgOperations.onEachIndexed { _, op ->
                op(osImage, this)
            }
        }
    }

fun RenderingLogger.applyCustomizationOptions(osImage: OperatingSystemImage, patch: Patch): Any =
    if (patch.customizationOptions.isEmpty()) {
        logLine { META typed "Customization Options: —" }
    } else {
        logging("Customization Options (${patch.customizationOptions.size})", null, borderedOutput = false) {
            customize(osImage) {
                +patch.customizationOptions
            }
        }
    }

fun RenderingLogger.applyGuestfishCommands(osImage: OperatingSystemImage, patch: Patch): Any =
    if (patch.guestfishCommands.isEmpty()) {
        logLine { META typed "Guestfish Commands: —" }
    } else {
        logging("Guestfish Commands (${patch.guestfishCommands.size})", null, borderedOutput = false) {
            fish(osImage) {
                +patch.guestfishCommands
            }
        }
    }

fun RenderingLogger.applyGuestfishCommandsAndFileSystemOperations(osImage: OperatingSystemImage, patch: Patch): Any =
    if (patch.guestfishCommands.isEmpty() && patch.fileSystemOperations.isEmpty()) {
        logLine { META typed "File System Operations: —" }
    } else {
        logging("File System Operations (${patch.guestfishCommands.size + patch.fileSystemOperations.size})", null) {
            logLine { META typed "Starting Guestfish VM..." }
            val guestfish = Guestfish(osImage, this, DockerContainerName(patch.name + "." + String.random(16)))

            applyGuestfishCommands(osImage, patch)

            val guestPaths: List<Path> = patch.fileSystemOperations.map { it.target }
            if (guestPaths.isNotEmpty()) {
                val guestfishOperation = Guestfish.copyOutCommands(guestPaths)
                guestfish.run(guestfishOperation)

                val guestfishCommands: List<GuestfishCommand> = Libguestfs.Guestfish.commands {
                    guestPaths.forEach { sourcePath ->
                        val sanitizedSourcePath = Path.of("/").asRootFor(sourcePath)
                        val destDir = osImage.file.resolveSibling("shared").asRootFor(sourcePath).parent
                        listOf("!mkdir -p $destDir", "- copy-out $sanitizedSourcePath $destDir")

                        runLocally {
                            command("mkdir", "-p", destDir.relativeTo(osImage.resolveOnHost("..".toPath()).normalize()).serialized)
                        }

                        ignoreErrors {
                            copyOut { CopyOutCommand(sanitizedSourcePath) }
                        }
                    }
                }
                println(guestfishOperation.toString().blue())
                println(guestfishCommands.toString().brightBlue())
            } else {
                logLine { META typed "No files to extract." }
            }

            val root = guestfish.guestRootOnHost
            val filesToPatch = patch.fileSystemOperations.toMutableList()
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
                guestfish.tarIn()
            } else {
                logLine { META typed "No changed files to copy back." }
            }
        }
    }

fun RenderingLogger.applyPostFileImgOperations(osImage: OperatingSystemImage, patch: Patch): Any =
    if (patch.postFileImgOperations.isEmpty()) {
        logLine { META typed "IMG Operations II: —" }
        false
    } else {
        logging("IMG Operations II (${patch.postFileImgOperations.size})", null, borderedOutput = false) {
            patch.postFileImgOperations.onEachIndexed { index, op ->
                op(osImage, this)
            }
        }
        true
    }

fun RenderingLogger.applyPrograms(osImage: OperatingSystemImage, patch: Patch): Any =
    if (patch.programs.isEmpty()) {
        logLine { META typed "Scripts: —" }
    } else {
        logging("Scripts (${patch.programs.size})", null) {
            patch.programs.onEach { program ->
                osImage.execute(
                    name = program.name,
                    logger = this,
                    autoLogin = true,
                    program
                )
            }
        }
    }

fun <E : Patch> Collection<E>.merge(): Patch = CompositePatch(this)

fun <E : Patch> Collection<E>.patch(img: OperatingSystemImage) = merge().patch(img)

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

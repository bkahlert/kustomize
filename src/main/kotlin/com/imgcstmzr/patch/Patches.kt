package com.imgcstmzr.patch

import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiColors.yellow
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.bold
import com.bkahlert.koodies.terminal.ascii.wrapWithBorder
import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.patch.Operation.Status.Failure
import com.imgcstmzr.patch.Operation.Status.Finished
import com.imgcstmzr.patch.Operation.Status.Ready
import com.imgcstmzr.patch.Operation.Status.Running
import com.imgcstmzr.patch.new.Patch
import com.imgcstmzr.process.Guestfish
import com.imgcstmzr.runtime.ArmRunner.runOn
import com.imgcstmzr.runtime.HasStatus
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.RenderingLogger.Companion.singleLineLogger
import com.imgcstmzr.runtime.log.RenderingLogger.Companion.subLogger
import com.imgcstmzr.util.asRootFor
import com.imgcstmzr.util.isFile
import com.imgcstmzr.util.listFilesRecursively
import java.nio.file.Path

fun Patch.banner() { // TODO make use of it or delete
    echo("""
            Patch: $name
              img.raw: ${preFileImgOperations.size}    img.fs: ${guestfishOperations.size}
              host.fs: ${fileSystemOperations.size}    img.run: ${programs.size}
        """.trimIndent().wrapWithBorder(padding = 6, margin = 1, ansiCode = (ANSI.termColors.bold + ANSI.termColors.cyan)))
    echo("")
}

fun Patch.patch(osImage: OperatingSystemImage, logger: RenderingLogger<Any>? = null) {
    logger.subLogger(name.toUpperCase(), null, borderedOutput = false) {
        applyPreFileImgOperations(osImage, this@patch)
        applyGuestfishAndFileSystemOperations(osImage, this@patch)
        applyPostFileImgOperations(osImage, this@patch)
        applyPrograms(osImage, this@patch)
    }
}

fun RenderingLogger<Any>.applyPreFileImgOperations(osImage: OperatingSystemImage, patch: Patch) {
    val count = patch.preFileImgOperations.size
    if (count == 0) {
        logLine { META typed "IMG Operations: —" }
        return
    }

    subLogger("IMG Operations ($count)", null, borderedOutput = false) {
        patch.preFileImgOperations.onEachIndexed { index, op ->
            op(osImage, this)
        }
        0
    }
}

fun RenderingLogger<Any>.applyGuestfishAndFileSystemOperations(osImage: OperatingSystemImage, patch: Patch): Any {
    val count = patch.guestfishOperations.size + patch.fileSystemOperations.size
    if (count == 0) {
        logLine { META typed "File System Operations: —" }
        return 0
    }

    return subLogger("File System Operations", null) {
        logLine { META typed "Starting Guestfish VM..." }
        val guestfish = Guestfish(osImage, this, this::class.qualifiedName + "." + String.random(16))

        val remainingGuestfishOperations = patch.guestfishOperations.toMutableList()
        if (remainingGuestfishOperations.isNotEmpty()) {
            while (remainingGuestfishOperations.isNotEmpty()) {
                val op = remainingGuestfishOperations.removeFirst()
                guestfish.run(op) // TODO
            }
        } else {
            logLine { META typed "No Guestfish operations to run." }
        }

        val guestPaths = patch.fileSystemOperations.map { it.target }
        if (guestPaths.isNotEmpty()) {
            guestfish.run(Guestfish.copyOutCommands(guestPaths))
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
            guestfish.run(Guestfish.copyInCommands(changedFiles))
        } else {
            logLine { META typed "No changed files to copy back." }
        }
    }
}

fun RenderingLogger<Any>.applyPostFileImgOperations(osImage: OperatingSystemImage, patch: Patch) {
    val count = patch.postFileImgOperations.size
    if (count == 0) {
        logLine { META typed "IMG Operations II: —" }
        return
    }

    subLogger("IMG Operations II ($count)", null, borderedOutput = false) {
        patch.postFileImgOperations.onEachIndexed { index, op ->
            op(osImage, this)
        }
        0
    }
}

fun RenderingLogger<Any>.applyPrograms(osImage: OperatingSystemImage, patch: Patch): Any {
    val count = patch.programs.size
    if (count == 0) {
        logLine { META typed "Scripts: —" }
        return 0
    }

    return subLogger("Scripts", null) {
        patch.programs.onEach { program ->
            program.runOn(osImage, this)
        }
        patch.programs.size
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

    operator fun invoke(target: TARGET, log: RenderingLogger<Any>)

    val target: TARGET

    override fun status(): String
}

class PathOperation(override val target: Path, val verifier: (Path) -> Any, val handler: (Path) -> Any) : Operation<Path> {

    override var currentStatus: Operation.Status = Ready

    override operator fun invoke(target: Path, log: RenderingLogger<Any>) {
        log.singleLineLogger(target.fileName.toString()) {
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

package com.imgcstmzr.patch

import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.terminal.ANSI.EscapeSequences.term16Colors
import com.bkahlert.koodies.terminal.ANSI.EscapeSequences.termColors
import com.bkahlert.koodies.terminal.ascii.wrapWithBorder
import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.patch.Operation.Status.Failure
import com.imgcstmzr.patch.Operation.Status.Finished
import com.imgcstmzr.patch.Operation.Status.Ready
import com.imgcstmzr.patch.Operation.Status.Running
import com.imgcstmzr.patch.new.Patch
import com.imgcstmzr.process.Guestfish
import com.imgcstmzr.process.Output.Type.META
import com.imgcstmzr.process.Output.Type.OUT
import com.imgcstmzr.runtime.HasStatus
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.runtime.bootRunStop
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.miniSegment
import com.imgcstmzr.runtime.log.segment
import com.imgcstmzr.util.asRootFor
import com.imgcstmzr.util.isFile
import com.imgcstmzr.util.listFilesRecursively
import java.nio.file.Path

fun Patch.banner() { // TODO make use of it or delete
    echo("""
            Patch: $name
              img.raw: ${imgOperations.size}    img.fs: ${guestfishOperations.size}
              host.fs: ${fileSystemOperations.size}    img.run: ${programs.size}
        """.trimIndent().wrapWithBorder(padding = 6, margin = 1, ansiCode = (term16Colors.bold + term16Colors.cyan)))
    echo("")
}

fun Patch.patch(img: Path, parentLogger: BlockRenderingLogger<Any>? = null) {
    parentLogger.segment<Any, Any>(name.toUpperCase(), null, borderedOutput = false) {
        applyImgOperations(img, this@patch)
        applyGuestfishAndFileSystemOperations(img, this@patch)
        applyPrograms(img, this@patch)
    }
}

fun BlockRenderingLogger<Any>.applyImgOperations(img: Path, patch: Patch) {
    val count = patch.imgOperations.size
    if (count == 0) {
        logStatus { META typed "IMG Operations: —" }
        return
    }

    segment<Any, Any>("IMG Operations ($count)", null, borderedOutput = false) {
        patch.imgOperations.onEachIndexed { index, op ->
            op(OperatingSystems.RaspberryPiLite, img, this)
        }
    }
}

fun BlockRenderingLogger<Any>.applyGuestfishAndFileSystemOperations(img: Path, patch: Patch): Any {
    val count = patch.guestfishOperations.size + patch.fileSystemOperations.size
    if (count == 0) {
        logStatus { META typed "File System Operations: —" }
        return 0
    }

    return segment("File System Operations", null) {
        logStatus { META typed "Starting Guestfish VM..." }
        val guestfish = Guestfish(img, this, this::class.qualifiedName + "." + String.random(16))

        val remainingGuestfishOperations = patch.guestfishOperations.toMutableList()
        if (remainingGuestfishOperations.isNotEmpty()) {
            while (remainingGuestfishOperations.isNotEmpty()) {
                val op = remainingGuestfishOperations.removeFirst()
                guestfish.run(op) // TODO
            }
        } else {
            logStatus { META typed "No Guestfish operations to run." }
        }


        val guestPaths = patch.fileSystemOperations.map { it.target }
        if (guestPaths.isNotEmpty()) {
            guestfish.run(Guestfish.copyOutCommands(guestPaths))
        } else {
            logStatus { META typed "No files to extract." }
        }

        val root = guestfish.guestRootOnHost
        val filesToPatch = patch.fileSystemOperations.toMutableList() // TODO might need extra guestfish instance for correct indentation
        if (filesToPatch.isNotEmpty()) {
            segment<Any, Any>("Patching files in ${root.fileName}", null) {
                val unprocessedActions: MutableList<PathOperation> = filesToPatch
                while (unprocessedActions.isNotEmpty()) {
                    val action = unprocessedActions.removeFirst()
                    val path = action.target
                    action.invoke(root.asRootFor(path), this)
                }
            }
        } else {
            logStatus { META typed "No files to patch." }
        }

        val changedFiles = root.listFilesRecursively({ it.isFile }).map { root.relativize(it) }.toList()
        if (changedFiles.isNotEmpty()) {
            guestfish.run(Guestfish.copyInCommands(changedFiles))
        } else {
            logStatus { META typed "No changed files to copy back." }
        }
    }
}

fun BlockRenderingLogger<Any>.applyPrograms(img: Path, patch: Patch): Any {
    val count = patch.programs.size
    if (count == 0) {
        logStatus { META typed "Scripts: —" }
        return 0
    }

    return segment<Any, Any>("Scripts", null) {
        val os = OperatingSystems.RaspberryPiLite
        patch.programs.onEachIndexed { index, op ->
            op.bootRunStop(scenario = op.name, os, img, this)
        }
        patch.programs.size
    }
}

fun <E : Patch> Collection<E>.merge(): Patch = CompositePatch(this)

fun <E : Patch> Collection<E>.patch(img: Path) = merge().patch(img)

interface Operation<TARGET> : HasStatus {
    var currentStatus: Status

    enum class Status(private val formatter: (String) -> String) {
        Ready({ label -> termColors.bold(label) }),
        Running({ label -> termColors.bold(label) }),
        Finished({ label -> termColors.strikethrough(label) }),
        Failure({ label -> termColors.red(label + "XXTODO") });

        operator fun invoke(label: String): String = formatter(label)
    }

    operator fun invoke(target: TARGET, log: BlockRenderingLogger<Any>)

    val target: TARGET

    override fun status(): String
}

//class ImgAction2 private constructor(
//    val commands: MutableList<ImgOperation> = mutableListOf(),
//) {
//    data class Builder(val commands: MutableList<ImgOperation> = mutableListOf()) {
//        fun resize(size: Size) {
//            commands += { os: OperatingSystem, img: Path -> os.increaseDiskSpace(size, img) }
//        }
//
//        fun build() = ImgAction2(commands)
//    }
//}

typealias ImgOperation = (OperatingSystem, Path, BlockRenderingLogger<Any>?) -> Any

//class ImgInvocation(override val target: ImgInvocation) : Operation<ImgInvocation> {
//
//    override var currentStatus: Operation.Status = Ready
//
//    override operator fun invoke(target: ImgInvocation, log: RenderingLogger<HasStatus>) {
//        log.rawLogStart("Preparing invocations ...")
//        currentStatus = Running
//        val result = runCatching { target.invoke(log) }
//        if (result.isFailure) {
//            currentStatus = Failure
//            log.rawLogEnd((tc.red + tc.bold)(" Failure."))
//        } else {
//            currentStatus = Finished
//            log.rawLogEnd((tc.green + tc.bold)(" Success."))
//        }
//    }
//
//    override fun status(): String = currentStatus("Img preparation")
//}

//class GuestfishOperation(override val target: GuestfishCommandBuilder, val handler: GuestfishCommandBuilder.() -> Any) : Operation<GuestfishCommandBuilder> {
//
//    class GuestfishCommandBuilder {
//        val commands: MutableList<String> = mutableListOf()
//
//
//        override fun toString(): String = "Building commands"
//    }
//
//    override var currentStatus: Operation.Status = Ready
//
//    override operator fun invoke(target: GuestfishCommandBuilder, log: RenderingLogger<HasStatus>) {
//        log.rawLogStart("Preparing invocations ...")
//        currentStatus = Running
//        val result = runCatching { handler.invoke(target) }
//        if (result.isFailure) {
//            currentStatus = Failure
//            log.rawLogEnd((tc.red + tc.bold)(" Failure."))
//        } else {
//            currentStatus = Finished
//            log.rawLogEnd((tc.green + tc.bold)(" Success."))
//        }
//    }
//
//    override fun status(): String = currentStatus("Guestfish preparation")
//}

class PathOperation(override val target: Path, val verifier: (Path) -> Any, val handler: (Path) -> Any) : Operation<Path> {

    override var currentStatus: Operation.Status = Ready

    override operator fun invoke(target: Path, log: BlockRenderingLogger<Any>) {
        log.miniSegment<Any, Any>(target.fileName.toString()) {
            logStatus { OUT typed termColors.yellow("Action needed? ...") }
            val result = runCatching { verifier.invoke(target) }
            if (result.isFailure) {
                currentStatus = Running
                logStatus { OUT typed ((termColors.yellow + termColors.bold)(" Yes...")) }

                handler.invoke(target)

                logStatus { OUT typed termColors.yellow("Verifying ...") }
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

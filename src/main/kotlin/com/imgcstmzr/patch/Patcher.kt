package com.imgcstmzr.patch

import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.cli.ColorHelpFormatter.Companion.tc
import com.imgcstmzr.patch.Action.Status.Failure
import com.imgcstmzr.patch.Action.Status.Finished
import com.imgcstmzr.patch.Action.Status.Ready
import com.imgcstmzr.patch.Action.Status.Running
import com.imgcstmzr.patch.GuestfishAction.GuestfishCommandBuilder
import com.imgcstmzr.process.Guestfish
import com.imgcstmzr.process.Guestfish.Companion.copyInCommands
import com.imgcstmzr.process.Guestfish.Companion.copyOutCommands
import com.imgcstmzr.process.Output.Type.ERR
import com.imgcstmzr.process.Output.Type.OUT
import com.imgcstmzr.runtime.HasStatus
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.util.asRootFor
import com.imgcstmzr.util.isFile
import com.imgcstmzr.util.listFilesRecursively
import java.nio.file.Path

class Patcher {
    operator fun invoke(img: Path, vararg patches: Patch) {
        val guestfish = Guestfish(img)
        val commands = patches.flatMap { patch -> patch.commands }
        val copyCommands = patches.flatMap { patch -> patch.paths }.toList().let { copyOutCommands(it) }

        patches.onEach { patch ->
            runCatching {
                guestfish.run(commands + copyCommands)
            }.onFailure { echo((tc.magenta + tc.bold)("Fatal error: $it")) }
        }

        val root = guestfish.guestRootOnHost

        patches.onEach { patch ->
            runCatching {
                patch(root)
            }.onFailure { echo((tc.magenta + tc.bold)("Fatal error: $it")) }
        }

        val changedFiles = root.listFilesRecursively({ it.isFile }).map { root.relativize(it) }.toList()
        guestfish.run(copyInCommands(changedFiles))
    }
}

interface Patch {
    val name: String
    val actions: List<Action<*>>

    val commands: List<String>
        get() {
            val logger = BlockRenderingLogger<HasStatus>(name)
            val collectedCommands = mutableListOf<String>()
            runCatching {
                val unprocessedActions = actions.mapNotNull { it as? GuestfishAction }.toMutableList()
                while (unprocessedActions.isNotEmpty()) {
                    val action = unprocessedActions.removeFirst()
                    logger.logLine(OUT typed action.target.toString(), listOf(action).plus(unprocessedActions))
                    action(action.target, logger)
                    collectedCommands += action.target.commands
                }
            }
                .onSuccess { logger.endLogging(name, 0) }
                .onFailure { logger.endLogging("Patch preparation failure: $it", 1) }
            return collectedCommands
        }
    val paths: List<Path>
        get() = actions.targets()

    operator fun invoke(root: Path) {
        val logger = BlockRenderingLogger<HasStatus>(name)
        val unprocessedActions: MutableList<PathAction> = actions.mapNotNull { it as? PathAction }.toMutableList()
        runCatching {
            while (unprocessedActions.isNotEmpty()) {
                val action = unprocessedActions.removeFirst()
                logger.logLine(OUT typed action.target.toString(), listOf(action).plus(unprocessedActions))
                action(root.asRootFor(action.target), logger)
            }
        }
            .onSuccess { logger.endLogging(name, 0) }
            .onFailure { logger.endLogging("Patch application failure: $it", 1) }
        logger.endLogging(name, 0)
    }

    companion object {
        inline fun <reified T> List<Action<*>>.targets(): List<T> = map { it.target }.filterIsInstance<T>().toList()
    }
}


interface Action<TARGET> : HasStatus {
    var currentStatus: Status

    enum class Status(private val formatter: (String) -> String) {
        Ready({ label -> tc.bold(label) }),
        Running({ label -> tc.bold(label) }),
        Finished({ label -> tc.strikethrough(label) }),
        Failure({ label -> tc.red(label) });

        operator fun invoke(label: String): String = formatter(label)
    }

    operator fun invoke(target: TARGET, log: RenderingLogger<HasStatus>)

    val target: TARGET

    override fun status(): String
}

class GuestfishAction(override val target: GuestfishCommandBuilder, val handler: GuestfishCommandBuilder.() -> Unit) : Action<GuestfishCommandBuilder> {

    class GuestfishCommandBuilder {
        val commands: MutableList<String> = mutableListOf()

        fun changePassword(username: String, password: String, salt: String) {
            commands += Guestfish.changePasswordCommand(username, password, salt)
        }

        override fun toString(): String = "Building commands"
    }

    override var currentStatus: Action.Status = Ready

    override operator fun invoke(target: GuestfishCommandBuilder, log: RenderingLogger<HasStatus>) {
        log.rawLogStart("Preparing invocations ...")
        currentStatus = Running
        val result = runCatching { handler.invoke(target) }
        if (result.isFailure) {
            currentStatus = Failure
            log.rawLogEnd((tc.red + tc.bold)(" Failure."))
        } else {
            currentStatus = Finished
            log.rawLogEnd((tc.green + tc.bold)(" Success."))
        }
    }

    override fun status(): String = currentStatus("Guestfish preparation")
}

class PathAction(override val target: Path, val verifier: (Path) -> Unit, val handler: (Path) -> Unit) : Action<Path> {

    override var currentStatus: Action.Status = Ready

    override operator fun invoke(target: Path, log: RenderingLogger<HasStatus>) {
        log.rawLogStart(tc.yellow("Action needed? ..."))
        val result = runCatching { verifier.invoke(target) }
        if (result.isFailure) {
            currentStatus = Running
            log.rawLog((tc.yellow + tc.bold)(" Yes."))

            log.rawLogEnd((tc.yellow)(" Performing ..."))
            handler.invoke(target)

            log.rawLogStart(tc.yellow("Verifying ..."))
            val postResult = runCatching { verifier.invoke(target) }
            if (postResult.isFailure) {
                currentStatus = Failure
                log.rawLogEnd((tc.red + tc.bold)(" Failure."))
                log.logLine(ERR typed postResult.exceptionOrNull()?.message)
            } else {
                currentStatus = Finished
                log.rawLogEnd((tc.green + tc.bold)(" Success."))
            }
        } else {
            currentStatus = Finished
            log.rawLogEnd((tc.green + tc.bold)(" No."))
        }
    }

    override fun status(): String = currentStatus(target.fileName.toString())
}

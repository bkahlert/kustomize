package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.boolean.asEmoji
import com.bkahlert.koodies.concurrent.process.Processes.isTempScriptFile
import com.bkahlert.koodies.exception.dump
import com.bkahlert.koodies.exception.toSingleLineString
import com.bkahlert.koodies.isLazyInitialized
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.nio.file.toPath
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import org.codehaus.plexus.util.cli.Commandline
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.lang.Process as JavaProcess


interface LightweightProcess : Process {
    companion object {
        fun from(commandLine: CommandLine): LightweightProcess = LightweightJavaProcess(commandLine)
    }

    val output: String
}

fun CommandLine.toLightweightProcess(): LightweightProcess = LightweightProcess.from(this)

private fun CommandLine.toJavaProcess(): java.lang.Process {
    val scriptFile: String = if (command.toPath().isTempScriptFile()) command else toShellScript().serialized

    return Commandline(scriptFile).let {
        it.workingDirectory = workingDirectory.toFile()
        environment.forEach { env -> it.addEnvironment(env.key, env.value) }
        it.execute()
    }
}

open class LightweightJavaProcess(
    protected val commandLine: CommandLine,
) : ProcessDelegate(), LightweightProcess {
    companion object;

    /**
     * The [JavaProcess] that is delegated to.
     */
    override val javaProcess: java.lang.Process by lazy {
        commandLine.toJavaProcess()
    }

    override val onExit: CompletableFuture<Process>
        get() {
            return javaProcess.onExit().exceptionally { throwable ->
                val cause = if (throwable is CompletionException) throwable.cause else throwable
                val dump = dump("""
                Process $commandLine terminated with ${cause.toSingleLineString()}.
            """.trimIndent()) { output }.also { dump -> metaLog(dump) }
                throw RuntimeException(dump.removeEscapeSequences(), cause)
            }.thenApply { this }
        }

    override val output: String = javaProcess.inputStream.bufferedReader().readText().trim()

    private val preparedToString = StringBuilder().apply {
        append(" commandLine=${commandLine.commandLine}")
    }

    override fun toString(): String {
        val delegateString =
            if (::javaProcess.isLazyInitialized) "$javaProcess; result=${onExit.isCompletedExceptionally.not().asEmoji}"
            else "not yet initialized"
        return "${this::class.simpleName}[delegate=$delegateString;$preparedToString]"
    }
}

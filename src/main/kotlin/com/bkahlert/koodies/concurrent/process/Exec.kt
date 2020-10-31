package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.Exec.Async.startShellScript
import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.concurrent.process.RunningProcess.Companion.nullRunningProcess
import com.imgcstmzr.process.RunningProcessProvidingCommandLineUtil
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.Paths.WORKING_DIRECTORY
import com.imgcstmzr.util.appendText
import com.imgcstmzr.util.makeExecutable
import com.imgcstmzr.util.quoted
import org.codehaus.plexus.util.cli.Commandline
import java.io.InputStream
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Helper functions to facilitate the integration of command line tools in ImgCstmzr.
 */
object Exec {

    @DslMarker
    annotation class ShellScriptMarker

    @ShellScriptMarker
    class ShellScript {
        val lines: MutableList<String> = mutableListOf()

        operator fun String.not() {
            lines.add(this)
        }

        /**
         * Builds a script line based on [words]. All words are combined using a single space.
         */
        fun line(vararg words: String) {
            lines.add(words.joinToString(" "))
        }

        /**
         * Builds a script [line] based on a single string already making up that script.
         */
        fun line(line: String) {
            lines.add(line)
        }

        /**
         * Builds a [command] call using the [arguments].
         */
        fun command(command: String, vararg arguments: String) {
            lines.add(listOf(command, *arguments).joinToString(" "))
        }
    }

    /**
     * Family of functions to interact with the command line synchronously.
     */
    object Sync {

        fun checkIfOutputContains(command: String, needle: String, caseSensitive: Boolean = false): Boolean = runCatching {
            val flags = if (caseSensitive) "" else "i"
            check(startShellScript { line("$command | grep -q$flags '$needle'") }.waitForCompletion().exitCode == 0)
        }.isSuccess


        /**
         * Builds and starts a shell script synchronously and returns its output.
         */
        fun evalShellScript(
            workingDirectory: Path = WORKING_DIRECTORY,
            env: Map<String, String> = emptyMap(),
            inputStream: InputStream? = null,
            init: ShellScript.() -> Unit,
        ): CompletedProcess {
            return startShellScript(
                workingDirectory = workingDirectory,
                env = env,
                inputStream = inputStream,
                init = init,
            ).waitForCompletion()
        }
    }

    /**
     * Family of functions to interact with the command line asynchronously.
     */
    object Async {

        /**
         * Builds and starts a shell script asynchronously.
         *
         * The build process is enabled by a builder that hopefully helps you making less mistakes.
         */
        fun startShellScript(
            workingDirectory: Path = WORKING_DIRECTORY,
            env: Map<String, String> = emptyMap(),
            runAfterProcessTermination: (() -> Unit)? = null,
            inputStream: InputStream? = null,
            outputProcessor: (RunningProcess.(IO) -> Unit)? = null,
            init: ShellScript.() -> Unit,
        ): RunningProcess {
            val shellScript = ShellScript()
            shellScript.init()
            return startShellScript(
                inputStream = inputStream,
                outputProcessor = outputProcessor,
                *shellScript.lines.toTypedArray(),
                workingDirectory = workingDirectory,
                env = env,
                runAfterProcessTermination = runAfterProcessTermination,
            )
        }

        /**
         * Starts a shell script asynchronously.
         *
         * **Important**
         * Each passed line is considered to actually be one as all lines are tucked together on execution using a line feed.
         * You are to introduce further line breaks on purpose if you know the consequences.
         */
        private fun startShellScript(
            inputStream: InputStream? = null,
            outputProcessor: (RunningProcess.(IO) -> Unit)? = null,
            vararg lines: String,
            workingDirectory: Path = WORKING_DIRECTORY,
            env: Map<String, String> = emptyMap(),
            runAfterProcessTermination: (() -> Unit)? = null,
        ): RunningProcess = startCommand(
            command = lines.toCommand(workingDirectory).toString(),
            workingDirectory = null,
            env = env,
            runAfterProcessTermination = runAfterProcessTermination,
            inputStream = inputStream,
            outputProcessor = outputProcessor,
        )

        /**
         * Starts a command asynchronously.
         *
         * **Important**
         * The command and arguments are forwarded as is. That means the [command] has to be a binary—no built-in shell command—
         * and its arguments are also passed unchanged. Possibly existing whitespaces will **not** be tokenized which circumvents a lot of pitfalls.
         */
        @OptIn(ExperimentalTime::class)
        private fun startCommand(
            command: String,
            vararg arguments: String,
            workingDirectory: Path? = WORKING_DIRECTORY,
            env: Map<String, String> = emptyMap(),
            runAfterProcessTermination: (() -> Unit)? = null,
            inputStream: InputStream? = InputStream.nullInputStream(),
            outputProcessor: (RunningProcess.(IO) -> Unit)? = { line -> println(line) },
        ): RunningProcess {
            val commandline = commandLine(command, arguments, workingDirectory, env)
            outputProcessor?.let { it(nullRunningProcess, META typed "Executing $commandline") }
            lateinit var runningProcess: RunningProcess
            return RunningProcessProvidingCommandLineUtil.executeCommandLineAsCallable(
                commandLine = commandline,
                inputProvider = inputStream,
                systemOutProcessor = { line -> outputProcessor?.let { it(runningProcess, OUT typed line) } },
                systemErrProcessor = { line -> outputProcessor?.let { it(runningProcess, ERR typed line) } },
                timeout = Duration.ZERO,
                runAfterProcessTermination = runAfterProcessTermination,
            ).also { runningProcess = it }
        }
    }

    fun commandLine(
        command: String,
        arguments: Array<out String>,
        workingDirectory: Path?,
        env: Map<String, String>,
    ): Commandline {
        val commandline = Commandline(command)
        commandline.addArguments(arguments)
        commandline.workingDirectory = workingDirectory?.toFile()
        env.forEach { commandline.addEnvironment(it.key, it.value) }
        return commandline
    }

    private fun Array<out String>.toCommand(workingDirectory: Path): Path =
        Paths.tempFile(extension = ".sh").apply {
            appendText("#!/bin/sh\n")
            appendText("cd ${workingDirectory.quoted}\n")
            this@toCommand.forEach { appendText("$it\n") }
            makeExecutable()
        }
}

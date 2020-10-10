package com.imgcstmzr.process

import com.bkahlert.koodies.shell.toHereDoc
import com.bkahlert.koodies.string.random
import com.imgcstmzr.process.Output.Type.META
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.Paths.WORKING_DIRECTORY
import com.imgcstmzr.util.appendText
import com.imgcstmzr.util.makeExecutable
import com.imgcstmzr.util.quoted
import org.apache.maven.shared.utils.cli.CommandLineCallable
import org.apache.maven.shared.utils.cli.CommandLineUtils
import org.apache.maven.shared.utils.cli.Commandline
import java.io.InputStream
import java.nio.file.Path

/**
 * Helper functions to facilitate the integration of command line tools in ImgCstmzr.
 */
object Exec {
    const val REDIRECT = ">"
    const val REFERENCE = "&"
    const val DEV_NULL = "/dev/null"
    const val ONE_TO_DEV_NULL = "$REDIRECT$DEV_NULL"
    const val TWO_TO_ONE = "2$REDIRECT${REFERENCE}1"
    const val REDIRECT_ALL_TO_NULL = "$ONE_TO_DEV_NULL $TWO_TO_ONE"

    @DslMarker
    annotation class ShellScriptMarker

    @ShellScriptMarker
    class ShellScript {
        val lines: MutableList<String> = mutableListOf()

        /**
         * Creates a [here document](https://en.wikipedia.org/wiki/Here_document) consisting of the given [lines].
         */
        fun heredoc(vararg heredocLines: String) {
            val hereDoc = heredocLines.toList().toHereDoc("HERE-" + String.random(8).toUpperCase())
            lines.add(hereDoc)
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
            check(execShellScript { line("$command | grep -q$flags '$needle'") } == 0)
        }.isSuccess

        /**
         * Builds and starts a shell script synchronously.
         *
         * The build process is enabled by a builder that hopefully helps you making less mistakes.
         */
        fun execShellScript(
            workingDirectory: Path = WORKING_DIRECTORY,
            env: Map<String, String> = emptyMap(),
            inputStream: InputStream? = null,
            outputProcessor: ((Output) -> Unit)? = null,
            init: ShellScript.() -> Unit,
        ): Int {
            val shellScript = ShellScript()
            shellScript.init()
            return execShellScript(
                inputStream = inputStream,
                outputProcessor = outputProcessor,
                *shellScript.lines.toTypedArray(),
                workingDirectory = workingDirectory,
                env = env,
            )
        }

        /**
         * Starts a shell script synchronously (blocking).
         *
         * **Important**
         * Each passed line is considered to actually be one as all lines are tucked together on execution using a line feed.
         * You are to introduce further line breaks on purpose if you know the consequences.
         */
        fun execShellScript(
            inputStream: InputStream? = null,
            outputProcessor: ((Output) -> Unit)? = null,
            vararg lines: String,
            workingDirectory: Path = WORKING_DIRECTORY,
            env: Map<String, String> = emptyMap(),
        ): Int {
            return execCommand(
                command = lines.toCommand(workingDirectory).toString(),
                arguments = emptyArray(),
                workingDirectory = null,
                env = env,
                inputStream = inputStream,
                outputProcessor = outputProcessor
            )
        }

        /**
         * Starts a command synchronously (blocking).
         *
         * **Important**
         * The command and arguments are forwarded as is. That means the [command] has to be a binary—no built-in shell command—
         * and its arguments are also passed unchanged. Possibly existing whitespaces will **not** be tokenized which circumvents a lot of pitfalls.
         */
        fun execCommand(
            command: String,
            vararg arguments: String,
            workingDirectory: Path? = WORKING_DIRECTORY,
            env: Map<String, String> = emptyMap(),
            inputStream: InputStream? = InputStream.nullInputStream(),
            outputProcessor: ((Output) -> Unit)? = { line -> println(line) },
        ): Int {
            val commandline = commandLine(command, arguments, workingDirectory, env)
            outputProcessor?.let { it(META typed "Executing $commandline") }
            return CommandLineUtils.executeCommandLine(
                commandline,
                inputStream,
                { line -> outputProcessor?.let { it(Output.Type.OUT typed line) } },
                { line -> outputProcessor?.let { it(Output.Type.ERR typed line) } },
            )
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
            inputStream: InputStream? = null,
            outputProcessor: ((Output) -> Unit)? = null,
            init: ShellScript.() -> Unit,
        ): CommandLineCallable {
            val shellScript = ShellScript()
            shellScript.init()
            return startShellScript(
                inputStream = inputStream,
                outputProcessor = outputProcessor,
                *shellScript.lines.toTypedArray(),
                workingDirectory = workingDirectory,
                env = env,
            )
        }

        /**
         * Starts a shell script asynchronously.
         *
         * **Important**
         * Each passed line is considered to actually be one as all lines are tucked together on execution using a line feed.
         * You are to introduce further line breaks on purpose if you know the consequences.
         */
        fun startShellScript(
            inputStream: InputStream? = null,
            outputProcessor: ((Output) -> Unit)? = null,
            vararg lines: String,
            workingDirectory: Path = WORKING_DIRECTORY,
            env: Map<String, String> = emptyMap(),
        ): CommandLineCallable = startCommand(
            command = lines.toCommand(workingDirectory).toString(),
            workingDirectory = null,
            env = env,
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
        fun startCommand(
            command: String,
            vararg arguments: String,
            workingDirectory: Path? = WORKING_DIRECTORY,
            env: Map<String, String> = emptyMap(),
            inputStream: InputStream? = InputStream.nullInputStream(),
            outputProcessor: ((Output) -> Unit)? = { line -> println(line) },
        ): CommandLineCallable {
            val commandline = commandLine(command, arguments, workingDirectory, env)
            outputProcessor?.let { it(META typed "Executing $commandline") }
            return CommandLineUtils.executeCommandLineAsCallable(
                commandline,
                inputStream,
                { line -> outputProcessor?.let { it(Output.Type.OUT typed line) } },
                { line -> outputProcessor?.let { it(Output.Type.ERR typed line) } },
                0,
                {}
            )
        }
    }

    private fun commandLine(
        command: String,
        arguments: Array<out String>,
        workingDirectory: Path?,
        env: Map<String, String>,
    ): Commandline {
        val commandline = Commandline(command)
        commandline.addArguments(*arguments)
        commandline.workingDirectory = workingDirectory?.toFile()
        env.forEach { commandline.addEnvironment(it.key, it.value) }
        return commandline
    }

    private fun Array<out String>.toCommand(workingDirectory: Path): Path {
        val script = Paths.tempFile(extension = ".sh")
            .also { scriptFile -> scriptFile.appendText("#!/bin/sh\n") }
            .also { scriptFile -> scriptFile.appendText("cd ${workingDirectory.quoted}\n") }
            .also { scriptFile -> forEach { scriptFile.appendText("$it\n") } }
            .also { scriptFile -> scriptFile.makeExecutable() }
        //                .also {
        //                    val log = "Script Generated: $it\n${it.readAll()}"
        //                    if (outputProcessor != null) outputProcessor(META typed log)
        //                    else println(META.format(log))
        //                }
//        val inlineScript = joinToString(("; ")).singleQuoted
        return script
    }
}

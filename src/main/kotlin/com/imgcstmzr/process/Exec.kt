package com.imgcstmzr.process

import com.github.ajalt.clikt.output.TermUi
import com.imgcstmzr.util.Paths.WORKING_DIRECTORY
import com.imgcstmzr.util.debug
import com.imgcstmzr.util.hereDoc
import com.imgcstmzr.util.slf4jFormat
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.ProcessResult
import org.zeroturnaround.exec.StartedProcess
import org.zeroturnaround.exec.listener.ProcessListener
import org.zeroturnaround.exec.stream.LogOutputStream
import java.io.OutputStream
import java.nio.file.Path
import java.util.Locale
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Helper functions to facilitate the integration of command line tools in ImgCstmzr.
 */
object Exec {
    private val IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).startsWith("windows")
    private val SHELL_CMD = if (IS_WINDOWS) "cmd.exe" else "sh"
    private val SHELL_CMD_ARG = if (IS_WINDOWS) "/c" else "-c"

    /**
     * Redirects log messages just as [ProcessExecutor.redirectOutput] and [ProcessExecutor.redirectError]
     * do with their input.
     */
    fun ProcessExecutor.redirectMsg(outputStream: OutputStream) {
        setMessageLogger { log, format, arguments ->
            outputStream.write(slf4jFormat(format + "\n", *arguments).toByteArray())
        }
    }

    @DslMarker
    annotation class ShellScriptMarker

    @ShellScriptMarker
    class ShellScript {
        val lines: MutableList<String> = mutableListOf()

        /**
         * Creates a [here document](https://en.wikipedia.org/wiki/Here_document) consisting of the given [lines].
         */
        fun heredoc(vararg heredocLines: String) {
            val hereDoc = hereDoc(heredocLines.toList())
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

        /**
         * Builds and starts a shell script synchronously.
         *
         * The build process is enabled by a builder that hopefully helps you making less mistakes.
         */
        fun execShellScript(
            workingDirectory: Path = WORKING_DIRECTORY,
            env: Map<String, String> = emptyMap(),
            expectedExitValue: Int? = 0,
            outputProcessor: (Process?.(Output) -> Unit)? = null,
            customizer: ProcessExecutor.() -> Unit = {},
            init: ShellScript.() -> Unit,
        ): ProcessResult {
            val shellScript = ShellScript()
            shellScript.init()
            return execShellScript(
                outputProcessor = outputProcessor,
                customizer = customizer,
                *shellScript.lines.toTypedArray(),
                workingDirectory = workingDirectory,
                env = env,
                expectedExitValue = expectedExitValue,
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
            outputProcessor: (Process?.(Output) -> Unit)? = null,
            customizer: ProcessExecutor.() -> Unit = {},
            vararg lines: String,
            workingDirectory: Path = WORKING_DIRECTORY,
            env: Map<String, String> = emptyMap(),
            expectedExitValue: Int? = 0,
        ): ProcessResult = execCommand(
            SHELL_CMD, SHELL_CMD_ARG, lines.joinToString("\n"),
            workingDirectory = workingDirectory,
            env = env,
            expectedExitValue = expectedExitValue,
            outputProcessor = outputProcessor,
            customizer = customizer,
        )

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
            workingDirectory: Path = WORKING_DIRECTORY,
            env: Map<String, String> = emptyMap(),
            expectedExitValue: Int? = 0,
            outputProcessor: (Process?.(Output) -> Unit)? = null,
            customizer: ProcessExecutor.() -> Unit = {},
        ): ProcessResult = prepareCommand(
            command,
            arguments,
            workingDirectory,
            env,
            expectedExitValue,
            outputProcessor,
            customizer,
        ).execute()
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
            expectedExitValue: Int? = 0,
            outputProcessor: (Process?.(Output) -> Unit)? = null,
            customizer: ProcessExecutor.() -> Unit = {},
            init: ShellScript.() -> Unit,
        ): StartedProcess {
            val shellScript = ShellScript()
            shellScript.init()
            return startShellScript(
                outputProcessor = outputProcessor,
                customizer = customizer,
                *shellScript.lines.toTypedArray(),
                workingDirectory = workingDirectory,
                env = env,
                expectedExitValue = expectedExitValue,
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
            outputProcessor: (Process?.(Output) -> Unit)? = null,
            customizer: ProcessExecutor.() -> Unit = {},
            vararg lines: String,
            workingDirectory: Path = WORKING_DIRECTORY,
            env: Map<String, String> = emptyMap(),
            expectedExitValue: Int? = 0,
        ): StartedProcess = Async.startCommand(
            SHELL_CMD, SHELL_CMD_ARG, lines.joinToString("\n"),
            workingDirectory = workingDirectory,
            env = env,
            expectedExitValue = expectedExitValue,
            outputProcessor = outputProcessor,
            customizer = customizer,
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
            workingDirectory: Path = WORKING_DIRECTORY,
            env: Map<String, String> = emptyMap(),
            expectedExitValue: Int? = 0,
            outputProcessor: (Process?.(Output) -> Unit)? = null,
            customizer: ProcessExecutor.() -> Unit = {},
        ): StartedProcess = prepareCommand(
            command,
            arguments,
            workingDirectory,
            env,
            expectedExitValue,
            outputProcessor,
            customizer,
        ).start()
    }

    /**
     * Sets up a command to run to the extend that only the firing call has to be done.
     */
    private fun prepareCommand(
        command: String,
        arguments: Array<out String>,
        workingDirectory: Path,
        env: Map<String, String>,
        expectedExitValue: Int?,
        outputProcessor: (Process?.(Output) -> Unit)? = null,
        customizer: ProcessExecutor.() -> Unit,
    ): ProcessExecutor {
        return ProcessExecutor()
            .command(command, *arguments)
            .directory(workingDirectory.toFile())
            .environment(env)
            .exitValue(expectedExitValue)
            .apply(customizer)
            .also { if (outputProcessor != null) it.apply(outputMultiplexerFor(outputProcessor)) }
    }

    private fun outputMultiplexerFor(outputProcessor: (Process?.(Output) -> Unit)): ProcessExecutor.() -> Unit = {
        val processExecutor: ProcessExecutor = this
        val process by ProcessProperty(processExecutor)
        var runningLogOutputStreams = 0
        fun convertingOutputStream(conversionType: Output.Type): LogOutputStream {
            return object : LogOutputStream() {
                init {
                    runningLogOutputStreams++
                    processExecutor.addListener(object : ProcessListener() {
                        override fun afterFinish(process: Process?, result: ProcessResult?) {
                            close()
                            runningLogOutputStreams--
                        }
                    })
                }

                override fun processLine(line: String) {
                    kotlin.runCatching {
                        val output = conversionType typed line
                        process.outputProcessor(output)
                    }
                }
            }
        }
        redirectMsg(convertingOutputStream(Output.Type.META))
        redirectErrorStream(false)
        redirectOutput(convertingOutputStream(Output.Type.OUT))
        redirectError(convertingOutputStream(Output.Type.ERR))

        processExecutor.addListener(object : ProcessListener() {
            override fun afterFinish(process: Process?, result: ProcessResult?) {
                while (runningLogOutputStreams > 0) {
                    TermUi.debug("Busy wait")
                    Thread.sleep(100)
                }
            }
        })
    }
}

class ProcessProperty(processExecutor: ProcessExecutor) : ReadOnlyProperty<Nothing?, Process?>, ProcessListener() {
    init {
        processExecutor.addListener(this)
    }

    private var startedProcess: Process? = null

    override fun afterStart(process: Process?, executor: ProcessExecutor?) {
        startedProcess = process
    }

    override fun getValue(thisRef: Nothing?, property: KProperty<*>): Process? = startedProcess
}

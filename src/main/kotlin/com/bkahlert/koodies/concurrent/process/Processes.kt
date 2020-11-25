package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.cleanUpOldTempFiles
import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.concurrent.process.RunningProcess.Companion.nullRunningProcess
import com.bkahlert.koodies.concurrent.process.ShellScript.Companion.build
import com.bkahlert.koodies.concurrent.process.UserInput.enter
import com.bkahlert.koodies.concurrent.startAsCompletableFuture
import com.bkahlert.koodies.exception.dump
import com.bkahlert.koodies.exception.toSingleLineString
import com.bkahlert.koodies.nio.NonBlockingReader
import com.bkahlert.koodies.nio.file.exists
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.terminal.ansi.AnsiStyles.tag
import com.bkahlert.koodies.time.Now
import com.bkahlert.koodies.time.poll
import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.util.Paths.WORKING_DIRECTORY
import com.imgcstmzr.util.unquoted
import org.codehaus.plexus.util.cli.Commandline
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.time.Duration
import kotlin.time.milliseconds

/**
 * Provides methods to start a new [Process] and to access running ones.
 */
object Processes {

    private const val shellScriptPrefix: String = "koodies.process."
    private const val shellScriptExtension: String = ".sh"

    /**
     * Builds a proper script that runs at [workingDirectory] and saved it as a
     * temporary file (to be deleted once in a while).
     */
    internal fun buildShellScriptToTempFile(
        workingDirectory: Path,
        shellScriptBuilder: ShellScript.() -> Unit,
    ): Path {
        val shellScriptLines = shellScriptBuilder.build().lines
        val shellScriptWithWorkingDirectory = ShellScript().apply {
            shebang()
            changeDirectoryOrExit(directory = workingDirectory)
            shellScriptLines.forEach { line(it) }
        }
        return shellScriptWithWorkingDirectory.buildTo(tempFile(base = shellScriptPrefix, extension = shellScriptExtension))
    }

    /**
     * Contains all accessible files contained in this command line.
     */
    val Commandline.includesFiles
        get() = listOf(executable, arguments).map { Path.of(it.toString().unquoted) }.filter { it.exists }

    /**
     * Contains a formatted list of files contained in this command line.
     */
    val Commandline.formattedIncludesFiles
        get() = includesFiles.joinToString("\n") { "📄 ${it.toUri()}" }


    init {
        cleanUpOldTempFiles(shellScriptPrefix, shellScriptExtension)
    }

    /**
     * Runs the [shellScript] asynchronously and with no helping wrapper.
     *
     * Returns the raw [Process].
     */
    fun startShellScriptDetached(
        workingDirectory: Path = WORKING_DIRECTORY,
        env: Map<String, String> = emptyMap(),
        shellScript: ShellScript.() -> Unit,
    ): Process {
        val command = ShellScript().apply {
            shebang()
            changeDirectoryOrExit(directory = workingDirectory)
            shellScript()
        }.buildTo(tempFile(base = shellScriptPrefix, extension = shellScriptExtension)).serialized
        return Commandline(command).apply {
            addArguments(arguments)
            @Suppress("ExplicitThis")
            this.workingDirectory = workingDirectory.toFile()
            env.forEach { addEnvironment(it.key, it.value) }
        }.execute()
    }

    /**
     * Same as [evalShellScript] but reads `std output` synchronously
     * with neither additional comfort nor additional threads overhead.
     */
    fun cheapEvalShellScript(
        workingDirectory: Path = WORKING_DIRECTORY,
        env: Map<String, String> = emptyMap(),
        shellScript: ShellScript.() -> Unit,
    ): String =
        startShellScriptDetached(workingDirectory, env, shellScript)
            .inputStream.bufferedReader().readText()
            .removeEscapeSequences()
            .trim()

    /**
     * Runs the [command] synchronously in a lightweight fashion and returns if the [substring] is contained in the output.
     */
    fun checkIfOutputContains(command: String, substring: String, caseSensitive: Boolean = false): Boolean = runCatching {
        val flags = if (caseSensitive) "" else "i"
        check(startShellScriptDetached { line("$command | grep -q$flags '$substring'") }.waitFor() == 0)
    }.isSuccess

    /**
     * Runs the [shellScriptBuilder] synchronously and returns the [CompletedProcess].
     */
    fun evalShellScript(
        workingDirectory: Path = WORKING_DIRECTORY,
        env: Map<String, String> = emptyMap(),
        inputStream: InputStream? = null,
        shellScriptBuilder: ShellScript.() -> Unit,
    ): CompletedProcess {
        return startShellScript(
            workingDirectory = workingDirectory,
            env = env,
            inputStream = inputStream,
            shellScriptBuilder = shellScriptBuilder,
        ).waitForCompletion()
    }

    /**
     * Runs the [shellScriptBuilder] asynchronously and returns the [RunningProcess].
     */
    fun startScript(
        workingDirectory: Path = WORKING_DIRECTORY,
        env: Map<String, String> = emptyMap(),
        runAfterProcessTermination: (() -> Unit)? = null,
        inputStream: InputStream? = null,
        ioProcessor: (LoggingProcess.(IO) -> Unit)? = null,
        shellScriptBuilder: ShellScript.() -> Unit,
    ): LoggingProcess = LoggingProcess(
        command = buildShellScriptToTempFile(workingDirectory, shellScriptBuilder).serialized,
        arguments = emptyList(),
        workingDirectory = null, // part of the shell script
        env = env,
        runAfterProcessTermination = runAfterProcessTermination,
        userProvidedInputStream = inputStream,
        ioProcessor = ioProcessor,
    )

    /**
     * Runs the [shellScriptBuilder] asynchronously and returns the [RunningProcess].
     */
    fun startShellScript(
        workingDirectory: Path = WORKING_DIRECTORY,
        env: Map<String, String> = emptyMap(),
        runAfterProcessTermination: (() -> Unit)? = null,
        inputStream: InputStream? = null,
        ioProcessor: (RunningProcess.(IO) -> Unit)? = null,
        shellScriptBuilder: ShellScript.() -> Unit,
    ): RunningProcess = startCommand(
        command = buildShellScriptToTempFile(workingDirectory, shellScriptBuilder).serialized,
        workingDirectory = null, // part of the shell script
        env = env,
        runAfterProcessTermination = runAfterProcessTermination,
        inputStream = inputStream,
        ioProcessor = ioProcessor,
    )

    /**
     * Starts the [command] asynchronously and returns the [RunningProcess].
     *
     * **Important**
     * The command and arguments are forwarded as is. That means the [command] has to be a binary (that is, no built-in shell command)
     * and its arguments are also passed unchanged. Possibly existing whitespaces will **not** be tokenized which circumvents a lot of pitfalls.
     */
    private fun startCommand(
        command: String,
        vararg arguments: String,
        workingDirectory: Path? = WORKING_DIRECTORY,
        env: Map<String, String> = emptyMap(),
        runAfterProcessTermination: (() -> Unit)? = null,
        inputStream: InputStream? = InputStream.nullInputStream(),
        ioProcessor: (RunningProcess.(IO) -> Unit)? = { line -> echo(line) },
    ): RunningProcess {
        val commandline = Commandline(command).apply {
            addArguments(arguments)
            @Suppress("ExplicitThis")
            this.workingDirectory = workingDirectory?.toFile()
            env.forEach { addEnvironment(it.key, it.value) }
        }

        lateinit var runningProcess: RunningProcess
        return executeCommandLine(
            commandLine = commandline,
            metaProcessor = { line -> ioProcessor?.let { it(runCatching { runningProcess }.getOrElse { nullRunningProcess }, META typed line) } },
            inputProvider = inputStream,
            systemOutProcessor = { line -> ioProcessor?.let { it(runningProcess, OUT typed line) } },
            systemErrProcessor = { line -> ioProcessor?.let { it(runningProcess, ERR typed line) } },
            timeout = Duration.ZERO,
            runAfterProcessTermination = runAfterProcessTermination,
        ).also { runningProcess = it }
    }

    private var executor = Executors.newCachedThreadPool()

    /**
     * Starts the specified [commandLine] as an immediately forked a process
     * wrapping it in a [RunningProcess].
     *
     * @param commandLine The command line to be executed.
     * @param metaProcessor A thread-safe processor that receives information about the process.
     * @param inputProvider Optional, yet thread-safe input for the [RunningProcess] to read from.
     *                    Alternatively the [RunningProcess]'s [RunningProcess.getOutputStream] can be used to provide data.
     * @param systemOutProcessor A thread-safe processor that receives the processes system output.
     * @param systemErrProcessor A thread-safe processor that receives the processes errors output.
     * @param timeout The time after which the [RunningProcess] will be [destroyed][RunningProcess.destroy].
     *                Any non-positive duration will be interpreted as no timeout, which is also the default.
     * @param runAfterProcessTermination An optional callback which gets called after process terminated or the timeout occurred.
     * @param nonBlockingReader Whether a non-blocking [Reader] should be used (default `true`). In contrast to a blocking reader,
     *                          a non-blocking one will also call the processors if nothing was read for a certain time.
     *                          This becomes handy if the [Process] generates output with no trailing line separator.
     *                          The exception to this is rule the initial output: The reader becomes un-blocking on the very first output.
     *
     * @return A [RunningProcess] with a couple of convenience functionality.
     */
    private fun executeCommandLine(
        commandLine: Commandline,
        metaProcessor: (String) -> Unit,
        inputProvider: InputStream?,
        systemOutProcessor: (String) -> Unit,
        systemErrProcessor: (String) -> Unit,
        timeout: Duration = Duration.ZERO,
        runAfterProcessTermination: Runnable?,
        nonBlockingReader: Boolean = true,
    ): RunningProcess {
        val process = commandLine.execute()
        val processHook: Thread = ShutdownHookUtils.processHookFor(process)
        ShutdownHookUtils.addShutDownHook(processHook)
        val pid = process.pid()

        return object : RunningProcess() {
            override val process: Process = process
            override val ioLog: IOLog = IOLog()

            init {
                "Executing $commandLine".log()
                commandLine.formattedIncludesFiles.log()
            }

            var disabled: Boolean = false
            override val result: CompletableFuture<CompletedProcess> = executor.startAsCompletableFuture {
                arrayOf(
                    executor.setupInputFeeder("stdin").exceptionally {
                        process.destroy()
                        throw RuntimeException("An error occurred while processing ${"stdin".tag()}.", it)
                    },
                    executor.setupOutputPumper("stdout").exceptionally {
                        process.destroy()
                        throw RuntimeException("An error occurred while processing ${"stdout".tag()}.", it)
                    },
                    executor.setupErrorPumper("stderr").exceptionally {
                        process.destroy()
                        throw RuntimeException("An error occurred while processing ${"stderr".tag()}.", it)
                    },
                ) to awaitExitCode()
            }.thenApply { (ioHelper, exitCode) ->

                disabled = true
                runAfterProcessTermination?.runCatching { run() }
                ShutdownHookUtils.removeShutdownHook(processHook)
                processHook.runCatching { run() }
                outputStream.runCatching { close() }

                val helperException: Throwable? = CompletableFuture.allOf(*ioHelper).handle { _, ex -> ex }.get()?.let {
                    if (it is CompletionException) it.cause else it
                }

                if (helperException != null) throw helperException

                exitCode
            }.exceptionally {
                dump("""
                    Process $process terminated with ${it.toSingleLineString()}.
                    ${commandLine.formattedIncludesFiles}
                """.trimIndent()) { ioLog.dump() }.log()
                throw it
            }.thenApply { exitCode ->
                if (exitCode != 0) {
                    dump("""
                        Process $pid terminated with exit code $exitCode.
                        ${commandLine.formattedIncludesFiles}
                    """.trimIndent()) { ioLog.dump() }.log()
                } else {
                    "Process $pid terminated successfully.".log()
                }
                CompletedProcess(pid, exitCode, ioLog.logged)
            }

            private fun awaitExitCode() = if (timeout <= Duration.ZERO) {
                Thread.currentThread().name = "$commandLine::wait"
                process.waitFor()
            } else {
                Thread.currentThread().name = "$commandLine::wait until ${Now + timeout}"
                500.milliseconds.poll { !process.isAlive }.forAtMost(timeout) { throw InterruptedException("Process $this timed out after $it") }
                exitValue()
            }

            private fun String.log() {
                metaStream.enter(this, delay = Duration.ZERO)
                metaProcessor(this)
            }

            private fun ExecutorService.setupInputFeeder(name: String): CompletableFuture<Any?> = inputProvider?.run {
                startAsCompletableFuture {
                    use {
                        Thread.currentThread().name = "$commandLine::$name"
                        var bytesCopied: Long = 0
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var bytes = read(buffer)
                        while (bytes >= 0) {
                            if (!disabled) {
                                outputStream.write(buffer, 0, bytes)
                                bytesCopied += bytes
                                bytes = read(buffer)
                            } else {
                                break
                            }
                        }
                    }
                }
            } ?: CompletableFuture.completedFuture(null)

            private fun ExecutorService.setupOutputPumper(name: String) = startAsCompletableFuture {
                inputStream.readerForStream(nonBlockingReader).forEachLine { line ->
                    Thread.currentThread().name = "$commandLine::$name"
                    if (!disabled) systemOutProcessor(line)
                }
            }

            private fun ExecutorService.setupErrorPumper(name: String) = startAsCompletableFuture {
                errorStream.readerForStream(nonBlockingReader).forEachLine { line ->
                    Thread.currentThread().name = "$commandLine::$name"
                    if (!disabled) systemErrProcessor(line)
                }
            }

            private fun InputStream.readerForStream(nonBlockingReader: Boolean): Reader =
                if (nonBlockingReader) NonBlockingReader(this, blockOnEmptyLine = true) else InputStreamReader(this)
        }
    }

}

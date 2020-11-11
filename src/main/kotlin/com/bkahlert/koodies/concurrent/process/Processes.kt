package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.concurrent.process.RunningProcess.Companion.nullRunningProcess
import com.bkahlert.koodies.concurrent.process.UserInput.enter
import com.bkahlert.koodies.concurrent.startAsCompletableFuture
import com.bkahlert.koodies.nio.NonBlockingReader
import com.bkahlert.koodies.nio.file.age
import com.bkahlert.koodies.nio.file.conditioned
import com.bkahlert.koodies.nio.file.exists
import com.bkahlert.koodies.nio.file.list
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.terminal.ansi.AnsiStyles.tag
import com.bkahlert.koodies.time.Now
import com.bkahlert.koodies.time.poll
import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.Paths.WORKING_DIRECTORY
import com.imgcstmzr.util.delete
import com.imgcstmzr.util.isFile
import com.imgcstmzr.util.unquoted
import org.codehaus.plexus.util.cli.Commandline
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.Executors
import kotlin.streams.asSequence
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.minutes

/**
 * Provides methods to start a new [Process] and to access running ones.
 */
@OptIn(ExperimentalTime::class)
object Processes {
    val current: ProcessHandle get() = ProcessHandle.current()

    val descendants: Sequence<ProcessHandle>
        get() = current.descendants().filter { it.isAlive && it.info().startInstant().isPresent }.asSequence()

    val children: Sequence<ProcessHandle>
        get() = descendants.filter { descendant -> descendant.parent().map { parent -> parent.pid() == current.pid() }.orElse(false) }

    val recentChildren: List<ProcessHandle>
        get() = children.toList().sortedByDescending { it.info().startInstant().get() }

    val mostRecentChild: ProcessHandle
        get() = recentChildren.firstOrNull()
            ?: throw IllegalStateException("This process $current has no recent child processes!\nChildren: ${children.toList()}")

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
        }.buildTo(Paths.tempFile(base = shellScriptPrefix, extension = shellScriptExtension)).conditioned
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
     * Runs the [shellScript] synchronously and returns the [CompletedProcess].
     */
    fun evalShellScript(
        workingDirectory: Path = WORKING_DIRECTORY,
        env: Map<String, String> = emptyMap(),
        inputStream: InputStream? = null,
        shellScript: ShellScript.() -> Unit,
    ): CompletedProcess {
        return startShellScript(
            workingDirectory = workingDirectory,
            env = env,
            inputStream = inputStream,
            shellScript = shellScript,
        ).waitForCompletion()
    }


    /**
     * Runs the [shellScript] asynchronously and returns the [RunningProcess].
     */
    fun startShellScript(
        workingDirectory: Path = WORKING_DIRECTORY,
        env: Map<String, String> = emptyMap(),
        runAfterProcessTermination: (() -> Unit)? = null,
        inputStream: InputStream? = null,
        outputProcessor: (RunningProcess.(IO) -> Unit)? = null,
        shellScript: ShellScript.() -> Unit,
    ): RunningProcess = startShellScriptLines(
        inputStream = inputStream,
        outputProcessor = outputProcessor,
        *(ShellScript().apply(shellScript)).lines.toTypedArray(),
        workingDirectory = workingDirectory,
        env = env,
        runAfterProcessTermination = runAfterProcessTermination,
    )

    /**
     * Starts the [shellScriptLines] asynchronously and returns the [RunningProcess].
     */
    private fun startShellScriptLines(
        inputStream: InputStream? = null,
        outputProcessor: (RunningProcess.(IO) -> Unit)? = null,
        vararg shellScriptLines: String,
        workingDirectory: Path = WORKING_DIRECTORY,
        env: Map<String, String> = emptyMap(),
        runAfterProcessTermination: (() -> Unit)? = null,
    ): RunningProcess = startCommand(
        command = ShellScript().apply {
            shebang()
            changeDirectoryOrExit(workingDirectory)
            shellScriptLines.forEach { line(it) }
        }.buildTo(Paths.tempFile(base = shellScriptPrefix, extension = shellScriptExtension)).conditioned,
        workingDirectory = null, // part of the shell script
        env = env,
        runAfterProcessTermination = runAfterProcessTermination,
        inputStream = inputStream,
        outputProcessor = outputProcessor,
    )

    /**
     * Starts the [command] asynchronously and returns the [RunningProcess].
     *
     * **Important**
     * The command and arguments are forwarded as is. That means the [command] has to be a binary (that is, no built-in shell command)
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
        outputProcessor: (RunningProcess.(IO) -> Unit)? = { line -> echo(line) },
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
            metaProcessor = { line -> outputProcessor?.let { it(runCatching { runningProcess }.getOrElse { nullRunningProcess }, META typed line) } },
            inputProvider = inputStream,
            systemOutProcessor = { line -> outputProcessor?.let { it(runningProcess, OUT typed line) } },
            systemErrProcessor = { line -> outputProcessor?.let { it(runningProcess, ERR typed line) } },
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
    @ExperimentalTime
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
                listOf(commandLine.executable, commandLine.arguments)
                    .map { Path.of(it.toString().unquoted) }
                    .filter { it.exists }
                    .joinToString("\n") { "📄 ${it.toUri()}" }.log()
            }

            var disabled: Boolean = false
            override val result: CompletableFuture<CompletedProcess> = executor.startAsCompletableFuture {
                val exceptionHandler: (Throwable) -> Unit = {
                    process.destroy()
                }
                runCatching {
                    listOf(
                        "stdin" to setupInputFeeder(exceptionHandler),
                        "stdout" to setupOutputPumper(exceptionHandler),
                        "stderr" to setupErrorPumper(exceptionHandler),
                    ).let { ioHelper ->
                        awaitExitCode().also {
                            ioHelper.forEach { (_, completable) -> completable?.join() }
                            ioHelper.forEach { (label, completable) ->
                                completable?.takeIf { it.isCompletedExceptionally }?.exceptionally {
                                    throw RuntimeException("An error occurred while processing ${label.tag()}.", it)
                                }
                            }
                        }
                    }
                }.run {
                    disabled = true
                    runAfterProcessTermination?.runCatching { run() }
                    ShutdownHookUtils.removeShutdownHook(processHook)
                    processHook.runCatching { run() }
                    outputStream.runCatching { close() }

                    fold(
                        onSuccess = { CompletedProcess(pid, it, ioLog.logged) },
                        onFailure = {
                            if (it is CompletionException) {
                                throw it.cause ?: it
                            }
                            throw RuntimeException("An unexpected error occurred while killing ${"$commandLine".tag()}.", it)
                        }
                    )
                }
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

            private fun setupInputFeeder(exceptionHandler: (Throwable) -> Unit): CompletableFuture<Any>? = inputProvider?.run {
                executor.startAsCompletableFuture {
                    runCatching {
                        use {
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
                    }.onFailure { exceptionHandler(it) }.getOrThrow()
                }
            }

            private fun setupOutputPumper(exceptionHandler: (Throwable) -> Unit) = executor.startAsCompletableFuture {
                kotlin.runCatching {
                    inputStream.readerForStream(nonBlockingReader).forEachLine { line ->
                        Thread.currentThread().name = "$commandLine::out:pump"
                        if (!disabled) systemOutProcessor(line)
                    }
                }.onFailure { exceptionHandler(it) }.getOrThrow()
            }

            private fun setupErrorPumper(exceptionHandler: (Throwable) -> Unit) = executor.startAsCompletableFuture {
                kotlin.runCatching {
                    errorStream.readerForStream(nonBlockingReader).forEachLine { line ->
                        Thread.currentThread().name = "$commandLine::err:pump"
                        if (!disabled) systemErrProcessor(line)
                    }
                }.onFailure { exceptionHandler(it) }.getOrThrow()
            }

            @OptIn(ExperimentalTime::class)
            private fun InputStream.readerForStream(nonBlockingReader: Boolean): Reader =
                if (nonBlockingReader) NonBlockingReader(this, blockOnEmptyLine = true) else InputStreamReader(this)
        }
    }

    const val shellScriptPrefix: String = "koodies.process."
    const val shellScriptExtension: String = ".sh"

    init {
        cleanUp()
        ShutdownHookUtils.addShutDownHook { cleanUp() }
    }

    /**
     * Deletes temporary shell scripts created during [Process] generation
     * that are older than 10 minutes.
     *
     * This method is automatically called during startup and shutdown.
     */
    fun cleanUp() {
        Paths.TEMP.list()
            .filter { it.isFile }
            .filter { file ->
                file.fileName.toString().let {
                    it.startsWith(shellScriptPrefix)
                        && it.endsWith(shellScriptExtension)
                }
            }
            .filter { it.age > 10.minutes }
            .forEach { it.delete() }
    }
}

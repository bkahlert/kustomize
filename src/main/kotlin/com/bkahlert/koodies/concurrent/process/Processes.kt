package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.concurrent.process.RunningProcess.Companion.nullRunningProcess
import com.bkahlert.koodies.concurrent.startAsCompletableFuture
import com.bkahlert.koodies.nio.NonBlockingReader
import com.bkahlert.koodies.terminal.ansi.AnsiStyles.tag
import com.bkahlert.koodies.time.Now
import com.bkahlert.koodies.time.poll
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.Paths.WORKING_DIRECTORY
import com.imgcstmzr.util.appendText
import com.imgcstmzr.util.makeExecutable
import com.imgcstmzr.util.quoted
import org.codehaus.plexus.util.cli.Commandline
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.streams.asSequence
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

/**
 * Provides methods to start a new [Process] and to access running ones.
 */
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
     * Runs the [command] synchronously in a lightweight fashion and returns if the [substring] is contained in the output.
     */
    fun checkIfOutputContains(command: String, substring: String, caseSensitive: Boolean = false): Boolean = runCatching {
        val flags = if (caseSensitive) "" else "i"
        check(startShellScript { line("$command | grep -q$flags '$substring'") }.waitForCompletion().exitCode == 0)
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
        command = Paths.tempFile(extension = ".sh").apply {
            appendText("#!/bin/sh\n")
            appendText("cd ${workingDirectory.quoted}\n")
            shellScriptLines.forEach { appendText("$it\n") }
            makeExecutable()
        }.toString(),
        workingDirectory = null,
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
        outputProcessor: (RunningProcess.(IO) -> Unit)? = { line -> println(line) },
    ): RunningProcess {
        val commandline = Commandline(command)
        commandline.addArguments(arguments)
        commandline.workingDirectory = workingDirectory?.toFile()
        env.forEach { commandline.addEnvironment(it.key, it.value) }

        outputProcessor?.let { it(nullRunningProcess, META typed "Executing $commandline") }
        lateinit var runningProcess: RunningProcess
        return executeCommandLine(
            commandLine = commandline,
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
    fun executeCommandLine(
        commandLine: Commandline,
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

        return object : RunningProcess() {
            override val process: Process = process
            var disabled: Boolean = false
            override val result: CompletableFuture<CompletedProcess> = executor.startAsCompletableFuture {
                runCatching {
                    listOf(
                        "stdin" to setupInputFeeder(),
                        "stdout" to setupOutputPumper(),
                        "stderr" to setupErrorPumper(),
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
                        onSuccess = { CompletedProcess(it, ioLog.logged) },
                        onFailure = { throw RuntimeException("An error occurred while killing ${"$commandLine".tag()}.", it) }
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

            private fun setupInputFeeder(): CompletableFuture<Any>? = inputProvider?.run {
                executor.startAsCompletableFuture {
                    use {
                        var bytesCopied: Long = 0
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var bytes = read(buffer)
                        while (bytes >= 0) {
                            if (!disabled) {
                                outputStream.write(buffer, 0, bytes)
                                bytesCopied += bytes
                                bytes = inputProvider.read(buffer)
                            } else {
                                break
                            }
                        }
                    }
                }
            }

            private fun setupOutputPumper() = executor.startAsCompletableFuture {
                inputStream.readerForStream(nonBlockingReader).forEachLine { line ->
                    Thread.currentThread().name = "$commandLine::out:pump"
                    if (!disabled) systemOutProcessor(line)
                }
            }

            private fun setupErrorPumper() = executor.startAsCompletableFuture {
                errorStream.readerForStream(nonBlockingReader).forEachLine { line ->
                    Thread.currentThread().name = "$commandLine::err:pump"
                    if (!disabled) systemErrProcessor(line)
                }
            }

            @OptIn(ExperimentalTime::class)
            private fun InputStream.readerForStream(nonBlockingReader: Boolean): Reader =
                if (nonBlockingReader) NonBlockingReader(this, blockOnEmptyLine = true) else InputStreamReader(this)
        }
    }
}

package com.imgcstmzr.process

import com.bkahlert.koodies.concurrent.process.CompletedProcess
import com.bkahlert.koodies.concurrent.process.RunningProcess
import com.bkahlert.koodies.concurrent.startAsCompletableFuture
import com.bkahlert.koodies.nio.NonBlockingReader
import com.bkahlert.koodies.terminal.ansi.AnsiStyles.tag
import com.bkahlert.koodies.time.Now
import com.bkahlert.koodies.time.poll
import com.imgcstmzr.process.ShutdownHookUtils.processHookFor
import org.codehaus.plexus.util.cli.Commandline
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.security.AccessControlException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds


object RunningProcessProvidingCommandLineUtil {
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
    fun executeCommandLineAsCallable(
        commandLine: Commandline,
        inputProvider: InputStream?,
        systemOutProcessor: (String) -> Unit,
        systemErrProcessor: (String) -> Unit,
        timeout: Duration = Duration.ZERO,
        runAfterProcessTermination: Runnable?,
        nonBlockingReader: Boolean = true,
    ): RunningProcess {
        val process = commandLine.execute()
        val processHook: Thread = processHookFor(process)
        ShutdownHookUtils.addShutDownHook(processHook)

        return object : RunningProcess() {
            override val process: Process = process
            var disabled: Boolean = false
            override val result: CompletableFuture<CompletedProcess> = executor.startAsCompletableFuture {
                try {
                    val inputFeeder = setupInputFeeder()
                    val outputPumper = setupOutputPumper()
                    val errorPumper = setupErrorPumper()

                    val exitCode: Int = waitSomeTime()

                    listOf("stdin" to inputFeeder, "stdout" to outputPumper, "stderr" to errorPumper)
                        .onEach { (_, completable) -> completable?.join() }
                        .onEach { (label, completable) ->
                            if (completable?.isCompletedExceptionally == true) {
                                completable?.exceptionally { ex -> throw RuntimeException("An error occurred while processing ${label.tag()}.", ex) }
                            }
                        }
                    CompletedProcess(exitCode, ioLog.logged)
                } catch (ex: InterruptedException) {
                    throw RuntimeException("An error occurred while killing ${"$commandLine".tag()}.", ex)
                } finally {
                    disabled = true
                    try {
                        runAfterProcessTermination?.run()
                    } finally {
                        ShutdownHookUtils.removeShutdownHook(processHook)
                        try {
                            processHook.run()
                        } finally {
                            kotlin.runCatching { outputStream.close() }
                        }
                    }
                }
            }

            private fun waitSomeTime() = if (timeout <= Duration.ZERO) {
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
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun InputStream.readerForStream(nonBlockingReader: Boolean): Reader =
        if (nonBlockingReader) NonBlockingReader(this, blockOnEmptyLine = true) else InputStreamReader(this)

}

object ShutdownHookUtils {
    fun processHookFor(process: Process) = thread(
        start = false,
        name = "${RunningProcessProvidingCommandLineUtil::class.simpleName} process shutdown hook",
        contextClassLoader = null
    ) { process.destroy() }

    fun addShutDownHook(hook: Thread): Any = kotlin.runCatching { Runtime.getRuntime().addShutdownHook(hook) }.onFailure { it.rethrowIfUnexpected() }
    fun removeShutdownHook(hook: Thread): Any = kotlin.runCatching { Runtime.getRuntime().removeShutdownHook(hook) }.onFailure { it.rethrowIfUnexpected() }
    private fun Throwable.rethrowIfUnexpected(): Any = if (this !is IllegalStateException && this !is AccessControlException) throw this else Unit
}

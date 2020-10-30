package com.imgcstmzr.process

import com.bkahlert.koodies.concurrent.startAsCompletableFuture
import com.bkahlert.koodies.nio.NonBlockingReader
import com.bkahlert.koodies.terminal.ansi.AnsiStyles.tag
import com.imgcstmzr.process.ShutdownHookUtils.processHookFor
import org.codehaus.plexus.util.cli.Commandline
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.security.AccessControlException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.time.ExperimentalTime


object RunningProcessProvidingCommandLineUtil {
    private var executor = Executors.newCachedThreadPool()

    /**
     * Immediately forks a process and returns a [RunningProcess].
     *
     * @param commandLine The command line to execute
     * @param systemIn The input to read from, must be thread safe
     * @param systemOut A consumer that receives output, must be thread safe
     * @param systemErr A consumer that receives system error stream output, must be thread safe
     * @param timeoutInSeconds Positive integer to specify timeout, zero and negative integers for no timeout.
     * @param runAfterProcessTermination Optional callback to run after the process terminated or the the timeout was
     *
     * @return A [RunningProcess] that provides the process [return value][RunningProcess.exitValue].
     */
    @ExperimentalTime
    fun executeCommandLineAsCallable(
        // TODO remove exec tools by maven
        commandLine: Commandline,
        systemIn: InputStream?,
        systemOut: (String) -> Unit,
        systemErr: (String) -> Unit,
        timeoutInSeconds: Int,
        runAfterProcessTermination: Runnable?,
        nonBlockingReader: Boolean = true,
    ): RunningProcess {
        val process = commandLine.execute()
        val processHook: Thread = processHookFor(process)
        ShutdownHookUtils.addShutDownHook(processHook)
        return object : RunningProcess() {
            override val process: Process = process
            override val result: CompletableFuture<CompletedProcess> = executor.completableFutureFor {
                var inputFeeder: CompletableFuture<Any>? = null
                var disabled: Boolean = false
                try {
                    if (systemIn != null) {
                        inputFeeder = executor.completableFutureFor {
                            /**
                             * Copies this stream to the given output stream, returning the number of bytes copied
                             *
                             * **Note** It is the caller's responsibility to close both of these resources.
                             */
                            var bytesCopied: Long = 0
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var bytes = systemIn.read(buffer)
                            while (bytes >= 0) {
                                if (!disabled) {
                                    outputStream.write(buffer, 0, bytes)
                                    bytesCopied += bytes
                                    bytes = systemIn.read(buffer)
                                }
                            }
                        }
                    }

                    val outputPumper = executor.completableFutureFor {
                        inputStream.readerForStream(nonBlockingReader).forEachLine { line ->
                            Thread.currentThread().name = "$commandLine::out:pump"
                            if (!disabled) systemOut(line)
                        }
                    }
                    val errorPumper = executor.completableFutureFor {
                        errorStream.readerForStream(nonBlockingReader).forEachLine { line ->
                            Thread.currentThread().name = "$commandLine::err:pump"
                            if (!disabled) systemErr(line)
                        }
                    }
                    val returnValue: Int = if (timeoutInSeconds <= 0) {
                        Thread.currentThread().name = "$commandLine::wait"
                        @Suppress("DEPRECATION") // only place were this call is wanted: waiting for the actual process
                        waitFor()
                    } else {
                        val now = System.nanoTime()
                        val timeout = now + NANOS_PER_SECOND * timeoutInSeconds
                        while (isAlive(process) && System.nanoTime() < timeout) {
                            // The timeout is specified in seconds. Therefore we must not sleep longer than one second
                            // but we should sleep as long as possible to reduce the number of iterations performed.
                            Thread.sleep(MILLIS_PER_SECOND - 1L)
                        }
                        if (isAlive(process)) {
                            throw InterruptedException(String.format("Process timed out after %d seconds.", timeoutInSeconds))
                        }
                        exitValue()
                    }
                    inputFeeder?.join()
                    outputPumper.join()
                    errorPumper.join()
                    if (inputFeeder != null) {
                        kotlin.runCatching { systemIn?.close() }
                            .mapCatching { outputStream.close() }
                            .exceptionOrNull()
                        if (inputFeeder.isCompletedExceptionally) {
                            inputFeeder.exceptionally {
                                throw RuntimeException("An error occurred while processing ${"stdin".tag()}.", it)
                            }
                        }
                    }
                    if (outputPumper.isCompletedExceptionally) {
                        outputPumper.exceptionally {
                            throw RuntimeException("An error occurred while processing ${"stdout".tag()}.", it)
                        }
                    }
                    if (errorPumper.isCompletedExceptionally) {
                        errorPumper.exceptionally {
                            throw RuntimeException("An error occurred while processing ${"stderr".tag()}.", it)
                        }
                    }
                    CompletedProcess(returnValue, ioLog.logged)
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
                            kotlin.runCatching { systemIn?.close() }
                                .mapCatching {
                                    outputStream.close()
                                }
                        }
                    }
                }
            }
        }
    }

    private inline fun <reified T> ExecutorService.completableFutureFor(noinline block: () -> T): CompletableFuture<T> =
        startAsCompletableFuture(executor = this, block = block)

    @OptIn(ExperimentalTime::class)
    private fun InputStream.readerForStream(nonBlockingReader: Boolean): Reader =
        if (nonBlockingReader) NonBlockingReader(this, blockOnEmptyLine = true) else InputStreamReader(this)

    /**
     * Number of milliseconds per second.
     */
    private const val MILLIS_PER_SECOND = 1000L

    /**
     * Number of nanoseconds per second.
     */
    private const val NANOS_PER_SECOND = 1000000000L

    private fun isAlive(p: Process?): Boolean {
        return if (p == null) {
            false
        } else try {
            p.exitValue()
            false
        } catch (e: IllegalThreadStateException) {
            true
        }
    }
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

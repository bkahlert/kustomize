package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.io.RedirectingOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import org.apache.commons.io.input.TeeInputStream as CommonsTeeInputStream
import org.apache.commons.io.output.TeeOutputStream as CommonsTeeOutputStream

/**
 * A [Process] which is currently executed and considered running as long as the [result]
 * is not computed. In contrast to the default [Process], a [RunningProcess] finishes after
 * eventually wrapping instances of stream pumpers have finished.
 */
@Suppress("KDocMissingDocumentation")
abstract class RunningProcess : Process() {
    protected abstract val process: Process
    protected abstract val result: CompletableFuture<CompletedProcess>

    protected val ioLog: IOLog = IOLog()

    class TeeOutputStream(outputStream: OutputStream, branch: OutputStream) : CommonsTeeOutputStream(outputStream, branch) {
        override fun close() {
            kotlin.runCatching { super.close() }
                .onFailure {
                    (0 until 10).forEach { println("caught close error $it") }
                }
        }
    }

    class TeeInputStream(inputStream: InputStream, branch: OutputStream) : CommonsTeeInputStream(inputStream, branch, false) {
        override fun close() {
            kotlin.runCatching { super.close() }
                .onFailure {
                    (0 until 10).forEach { println("caught close error: $it") }
                }
        }
    }

    override fun getOutputStream(): OutputStream = capturingOutputStream
    private val capturingOutputStream: OutputStream by lazy {
        TeeOutputStream(process.outputStream, RedirectingOutputStream {
            ioLog.add(IO.Type.IN, it)
        })
    }

    override fun getInputStream(): InputStream = capturingInputStream
    private val capturingInputStream: InputStream by lazy { TeeInputStream(process.inputStream, RedirectingOutputStream { ioLog.add(IO.Type.OUT, it) }) }
    override fun getErrorStream(): InputStream = capturingErrorStream
    private val capturingErrorStream: InputStream by lazy { TeeInputStream(process.errorStream, RedirectingOutputStream { ioLog.add(IO.Type.ERR, it) }) }

    override fun exitValue(): Int = process.exitValue()
    override fun destroy(): Unit = process.destroy()
    override fun destroyForcibly(): Process = process.destroyForcibly()
    override fun supportsNormalTermination(): Boolean = process.supportsNormalTermination()
    override fun isAlive(): Boolean = process.isAlive
    override fun pid(): Long = process.pid()
    override fun onExit(): CompletableFuture<Process> = process.onExit()
    override fun toHandle(): ProcessHandle = process.toHandle()
    override fun info(): ProcessHandle.Info = process.info()
    override fun children(): Stream<ProcessHandle> = process.children()
    override fun descendants(): Stream<ProcessHandle> = process.descendants()
    override fun toString(): String = "RunningProcess(process=$process, result=$result)"

    /**
     * Causes the current thread to wait, if necessary, until the
     * process represented by this [RunningProcess] has
     * terminated.
     *
     * This method returns immediately if the process
     * has already terminated.
     *
     * If the process has not yet terminated, the calling thread will be
     * blocked until the process exits.
     *
     * @return The [exitValue] of the [CompletedProcess].
     *         By convention, the value `0` indicates normal termination.
     *         **Use [waitForCompletion] if you also want to access the logged [IO].**
     * @throws InterruptedException if the current thread is
     *         {@linkplain Thread#interrupt() interrupted} by another
     *         thread while it is waiting, then the wait is ended and
     *         an {@link InterruptedException} is thrown.
     */
    override fun waitFor(): Int = waitForCompletion().exitCode
    override fun waitFor(timeout: Long, unit: TimeUnit?): Boolean = process.waitFor(timeout, unit)

    /**
     * Causes the current thread to wait, if necessary, until the
     * process represented by this [RunningProcess] has terminated.
     *
     * This method returns immediately if the process has already terminated.
     *
     * If the process has not yet terminated, the calling thread will be
     * blocked until the process exits.
     *
     * @return The [CompletedProcess] comprising its [exitValue] and logged [IO].
     *         By convention, the value `0` indicates normal termination.
     * @throws InterruptedException if the current thread is
     *         [interrupted][Thread.interrupt] by another
     *         thread while it is waiting, then the wait is ended and
     *         an [InterruptedException] is thrown.
     */
    fun waitForCompletion(): CompletedProcess = result.get()

    /**
     * Whether this [Process] failed, that is, returned with an exit value other than `0`.
     */
    val failed: Boolean
        get() = result.isCancelled && waitForCompletion().exitCode != 0

    companion object {
        @Suppress("SpellCheckingInspection")
        private const val errorMessage = "This instance must only be used as an alternative to `lateinit`. All methods of this instance throw an exception."

        /**
         * A non existent [Process] that serves as a not-null instance but whose methods are never meant to be called.
         */
        val nullProcess: Process = object : Process() {
            override fun getOutputStream(): OutputStream = error(errorMessage)
            override fun getInputStream(): InputStream = error(errorMessage)
            override fun getErrorStream(): InputStream = error(errorMessage)
            override fun waitFor(): Int = error(errorMessage)
            override fun exitValue(): Int = error(errorMessage)
            override fun destroy() = error(errorMessage)
            override fun toString(): String = "NullProcess"
        }

        /**
         * A non existent [RunningProcess] that serves as a not-null instance but whose methods are never meant to be called.
         */
        val nullRunningProcess: RunningProcess = object : RunningProcess() {
            override val process: Process = nullProcess
            override val result: CompletableFuture<CompletedProcess> = CompletableFuture.completedFuture(CompletedProcess(-1, emptyList()))
        }
    }
}

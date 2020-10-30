package com.imgcstmzr.process

import com.bkahlert.koodies.io.RedirectingOutputStream
import com.bkahlert.koodies.string.LineSeparators.CRLF
import com.bkahlert.koodies.string.withSuffix
import com.imgcstmzr.process.Output.Type.ERR
import com.imgcstmzr.process.Output.Type.IN
import com.imgcstmzr.process.Output.Type.META
import com.imgcstmzr.process.Output.Type.OUT
import org.apache.commons.io.input.TeeInputStream
import org.apache.commons.io.output.TeeOutputStream
import java.io.BufferedWriter
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

/**
 * A [Process] which is currently executed and considered running as long as the [result]
 * is not computed. In contrast to the default [Process], a [RunningProcess] finishes after
 * eventually wrapping instances of stream pumpers have finished.
 */
@Suppress("KDocMissingDocumentation")
abstract class RunningProcess : Process() {
    protected abstract val process: Process
    protected abstract val result: CompletableFuture<CompletedProcess>

    protected val ioLog = IOLog()

    class XX(outputStream: OutputStream, branch: OutputStream) : TeeOutputStream(outputStream, branch) {
        override fun close() {
            kotlin.runCatching { super.close() }
                .onFailure {
                    println("caught close error")
                }
        }
    }

    class YY(inputStream: InputStream, branch: OutputStream) : TeeInputStream(inputStream, branch, false) {
        override fun close() {
            kotlin.runCatching { super.close() }
                .onFailure {
                    println("caught close error")
                }
        }
    }

    override fun getOutputStream(): OutputStream = capturingOutputStream
    private val capturingOutputStream: OutputStream by lazy {
        XX(process.outputStream, RedirectingOutputStream {
            ioLog.add(IN, it)
        })
    }

    override fun getInputStream(): InputStream = capturingInputStream
    private val capturingInputStream: InputStream by lazy { YY(process.inputStream, RedirectingOutputStream { ioLog.add(OUT, it) }) }
    override fun getErrorStream(): InputStream = capturingErrorStream
    private val capturingErrorStream: InputStream by lazy { YY(process.errorStream, RedirectingOutputStream { ioLog.add(ERR, it) }) }

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
    @Deprecated("waitForCompletion also provides the logged I/O", ReplaceWith("waitForCompletion()")) override fun waitFor(): Int = process.waitFor()
    override fun waitFor(timeout: Long, unit: TimeUnit?): Boolean = process.waitFor(timeout, unit)

    /**
     * Whether this [Process] failed, that is, returned with an exit value other than `0`.
     */
    val failed: Boolean
        get() = result.isCancelled && waitForCompletion().exitCode != 0

    /**
     * Causes the current thread to wait, if necessary, until the
     * process represented by this [RunningProcess] has terminated.
     *
     * This method returns immediately if the process has already terminated.
     *
     * If the process has not yet terminated, the calling thread will be
     * blocked until the process exits.
     *
     * @return the [CompletedProcess] comprising its exit value and logged I/O.
     *         By convention, the value `0` indicates normal termination.
     * @throws InterruptedException if the current thread is
     *         [interrupted][Thread.interrupt] by another
     *         thread while it is waiting, then the wait is ended and
     *         an [InterruptedException] is thrown.
     */
    fun waitForCompletion(): CompletedProcess = result.get()

    companion object {
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


/**
 * What is left of a completed [Process].
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class CompletedProcess(
    /**
     * Exit code of the completed [Process].
     */
    val exitCode: Int,
    /**
     * All [Output] of the completed [Process].
     */
    val all: List<Output>,
    /**
     * All [Output] of type [META] of the completed [Process].
     */
    val meta: Output,
    /**
     * All [Output] of type [IN] of the completed [Process].
     */
    val input: Output,
    /**
     * All [Output] of type [OUT] of the completed [Process].
     */
    val output: Output,
    /**
     * All [Output] of type [ERR] of the completed [Process].
     */
    val error: Output,
) : CharSequence by all.joinToString("\n") {
    constructor(exitCode: Int, io: List<Output>) : this(
        exitCode,
        io,
        META typed io.filter { it.type == META }.joinToString("\n") { it.unformatted },
        IN typed io.filter { it.type == IN }.joinToString("\n") { it.unformatted },
        OUT typed io.filter { it.type == OUT }.joinToString("\n") { it.unformatted },
        ERR typed io.filter { it.type == ERR }.joinToString("\n") { it.unformatted },
    )

    /**
     * Exit code of the completed [Process].
     */
    operator fun component1(): Int = exitCode

    /**
     * All [Output] of the completed [Process].
     */
    operator fun component2(): List<Output> = all
}


/**
 * Returns this [Process] if it is not `null` and alive.
 *
 * @throws IllegalStateException
 */
fun <T : Process> T?.checkAlive(): T = this
    ?.also { check(it.isAlive) { "Process is not alive." } }
    ?: throw IllegalStateException("Process does not exist.")

/**
 * Enters the given [input] by writing it on the [Process]'s [InputStream] as
 * if it was a user's input sent by a hit of the enter key.
 */
@OptIn(ExperimentalTime::class)
fun Process.enter(vararg inputs: String, delay: Duration = 10.milliseconds): Unit = input(*inputs, delay = delay)

/**
 * Write the given [input] strings with a slight delay between
 * each input on the [Process]'s [InputStream].
 */
@OptIn(ExperimentalTime::class)
fun Process.input(vararg input: String, delay: Duration = 10.milliseconds): Unit = outputStream.input(*input, delay = delay)

/**
 * Enters the given [input] by writing it on the [Process]'s [InputStream] as
 * if it was a user's input sent by a hit of the enter key.
 */
@OptIn(ExperimentalTime::class)
fun OutputStream.enter(vararg inputs: String, delay: Duration = 10.milliseconds): Unit = input(*inputs, delay = delay)

/**
 * Write the given [input] strings with a slight delay between
 * each input on the [Process]'s [InputStream].
 */
@OptIn(ExperimentalTime::class)
fun OutputStream.input(vararg input: String, delay: Duration = 10.milliseconds) {
    val stdin = BufferedWriter(OutputStreamWriter(this))
    input.forEach {
        TimeUnit.MILLISECONDS.sleep(delay.toLongMilliseconds())
        stdin.write(it.withSuffix(CRLF))
        stdin.flush()
    }
}

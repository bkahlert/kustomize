package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.boolean.asEmoji
import com.bkahlert.koodies.concurrent.process.UserInput.enter
import org.apache.commons.io.output.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * Process that wraps an existing [Process] and forwards all
 * calls with the following features:
 *
 * 1) It can be specified what should be called on process termination and
 *    if the started process should be destroyed if the JVM shuts down.
 *
 * 2) The command line used to create the lazy process can be provided so
 *    it can always be reliably displayed.
 *
 * 3) The expected exit value can be specified. [waitFor] and [onExit]
 *    will throw an exception if the process exits with a different one.
 *
 * 4) The actual process can be [Lazy]. Nothing  in this implementation
 *    triggers it. Also this class can delegate itself without changing its
 *    behaviour.
 */
open class DelegatingProcess(
    protected val lazyProcess: Lazy<Process>,
    protected val commandLine: CommandLine,
    val expectedExitValue: Int = 0,
    protected val processTerminationCallback: (() -> Unit)? = null,
    protected val destroyOnShutdown: Boolean = true,
) : Process() {
    constructor(delegatingProcess: DelegatingProcess) : this(
        lazyProcess = delegatingProcess.lazyProcess,
        commandLine = delegatingProcess.commandLine,
        expectedExitValue = delegatingProcess.expectedExitValue,
        processTerminationCallback = delegatingProcess.processTerminationCallback,
        destroyOnShutdown = delegatingProcess.destroyOnShutdown)

    /**
     * The [Process] that is delegated to.
     */
    protected open val delegate: Process by lazy {
        kotlin.runCatching {
            lazyProcess.value.also {
                if (destroyOnShutdown) {
                    val shutdownHook = thread(start = false, name = "shutdown hook for $it", contextClassLoader = null) { destroy() }
                    ShutdownHookUtils.addShutDownHook(shutdownHook)

                    it.onExit().handle { _, _ -> ShutdownHookUtils.removeShutdownHook(shutdownHook) }
                }

                processTerminationCallback?.let { callback ->
                    it.onExit().handle { _, _ -> runCatching { callback.invoke() } }
                }
            }
        }.onFailure {
            kotlin.runCatching { processTerminationCallback?.invoke() }
        }.getOrThrow()
    }

    /**
     * The [Process] that is delegated to if it is initialized.
     * Otherwise `null`.
     */
    protected open val nonTriggeringDelegate: Process?
        get() {
            return if (lazyProcess.isInitialized()) {
                delegate
            } else null
        }

    fun String.log() = metaStream.enter(this, delay = Duration.ZERO)
    open val metaStream: OutputStream by lazy { alternativeMetaStream ?: ByteArrayOutputStream() }
    var alternativeMetaStream: OutputStream? = null
    override fun getOutputStream(): OutputStream = alternativeOutputStream ?: delegate.outputStream
    var alternativeOutputStream: OutputStream? = null
    override fun getInputStream(): InputStream = delegate.inputStream
    override fun getErrorStream(): InputStream = delegate.errorStream
    override fun exitValue(): Int = delegate.exitValue()
    override fun destroy(): Unit = delegate.destroy()
    override fun destroyForcibly(): Process = delegate.destroyForcibly()
    override fun supportsNormalTermination(): Boolean = delegate.supportsNormalTermination()
    override fun isAlive(): Boolean = delegate.isAlive
    override fun pid(): Long = delegate.pid()
    override fun onExit(): CompletableFuture<Process> = onExit
    override fun toHandle(): ProcessHandle = delegate.toHandle()
    override fun info(): ProcessHandle.Info = delegate.info()
    override fun children(): Stream<ProcessHandle> = delegate.children()
    override fun descendants(): Stream<ProcessHandle> = delegate.descendants()
    override fun waitFor(): Int = onExit().join().exitValue()
    override fun waitFor(timeout: Long, unit: DurationUnit): Boolean = delegate.waitFor(timeout, unit)
    fun waitFor(timeout: Duration): Boolean = waitFor(timeout.toLongMilliseconds(), TimeUnit.MILLISECONDS)

    private val onExit: CompletableFuture<Process> by lazy {
        delegate.onExit().thenApply { process ->
            checkValueMismatch(process.exitValue())
            process
        }
    }

    protected open fun checkValueMismatch(exitValue: Int) {
        if (exitValue != expectedExitValue) {
            mismatchException(exitValue)
        }
    }

    protected open fun mismatchException(exitValue: Int) {
        throw ProcessExecutionException(pid(), commandLine, exitValue, expectedExitValue)
    }

    private val preparedToString = StringBuilder().apply {
        append(" commandLine=$commandLine;")
        append(" expectedExitValue=$expectedExitValue;")
        append(" processTerminationCallback=${processTerminationCallback.asEmoji};")
        append(" destroyOnShutdown=${destroyOnShutdown.asEmoji}")
    }

    override fun toString(): String {
        val delegateString = nonTriggeringDelegate?.let { "$it; result=${onExit().isCompletedExceptionally.not().asEmoji}" }
            ?: "not yet initialized"
        return "DelegatingProcess[delegate=$delegateString;$preparedToString]"
    }
}


package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.boolean.asEmoji
import com.bkahlert.koodies.exception.dump
import com.bkahlert.koodies.exception.toSingleLineString
import com.bkahlert.koodies.io.RedirectingOutputStream
import com.bkahlert.koodies.isLazyInitialized
import com.bkahlert.koodies.nio.file.Paths
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import org.apache.commons.io.output.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import kotlin.concurrent.thread
import java.lang.Process as JavaProcess

/**
 * Process that wraps an existing [JavaProcess] and forwards all
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
open class ManagedProcess(
    protected val commandLine: CommandLine,

    protected val workingDirectory: Path = Paths.WorkingDirectory,
    protected val environment: Map<String, String> = emptyMap(),

    protected val expectedExitValue: Int = 0,
    protected val processTerminationCallback: (() -> Unit)? = {},
    protected val destroyOnShutdown: Boolean = true,
) : ProcessDelegate() {

    /**
     * Explicitly starts this process in case no other stimulus
     * took place.
     */
    fun start() {
        javaProcess.pid()
    }

    /**
     * The [JavaProcess] that is delegated to.
     */
    override val javaProcess: JavaProcess by lazy {
        kotlin.runCatching {
            commandLine.lazyProcess(workingDirectory, environment).value.apply {
                metaLog("Executing ${commandLine.commandLine}")
                commandLine.formattedIncludesFiles.takeIf { it.isNotBlank() }?.let { metaLog(it) }

                if (destroyOnShutdown) {
                    val shutdownHook = thread(start = false, name = "shutdown hook for $this", contextClassLoader = null) { destroy() }
                    ShutdownHookUtils.addShutDownHook(shutdownHook)

                    onExit().handle { _, _ -> ShutdownHookUtils.removeShutdownHook(shutdownHook) }
                }

                processTerminationCallback?.let { callback ->
                    onExit().handle { _, _ -> runCatching { callback.invoke() } }
                }
            }
        }.onFailure {
            kotlin.runCatching { processTerminationCallback?.invoke() }
        }.getOrThrow()
    }

    private val capturingMetaStream: OutputStream by lazy { TeeOutputStream(ByteArrayOutputStream(), RedirectingOutputStream { ioLog.add(IO.Type.META, it) }) }
    private val capturingOutputStream: OutputStream by lazy { TeeOutputStream(javaProcess.outputStream, RedirectingOutputStream { ioLog.add(IO.Type.IN, it) }) }
    private val capturingInputStream: InputStream by lazy { TeeInputStream(javaProcess.inputStream, RedirectingOutputStream { ioLog.add(IO.Type.OUT, it) }) }
    private val capturingErrorStream: InputStream by lazy { TeeInputStream(javaProcess.errorStream, RedirectingOutputStream { ioLog.add(IO.Type.ERR, it) }) }

    final override val metaStream: OutputStream get() = capturingMetaStream
    final override val outputStream: OutputStream get() = capturingOutputStream
    final override val inputStream: InputStream get() = capturingInputStream
    final override val errorStream: InputStream get() = capturingErrorStream

    val ioLog: IOLog by lazy { IOLog() }

    var externalSync: CompletableFuture<*> = CompletableFuture.completedFuture(Unit)
    override var onExit: CompletableFuture<Process>
        get() {
            return externalSync.thenCombine(javaProcess.onExit()) { _, process ->
                process
            }.exceptionally { throwable ->
                val cause = if (throwable is CompletionException) throwable.cause else throwable
                val dump = dump("""
                Process $commandLine terminated with ${cause.toSingleLineString()}.
            """.trimIndent()) { ioLog.dump() }.also { dump -> metaLog(dump) }
                throw RuntimeException(dump.removeEscapeSequences(), cause)
            }.thenApply { process ->
                if (exitValue != expectedExitValue) {
                    val message = ProcessExecutionException(pid, commandLine, exitValue, expectedExitValue).message
                    message?.also { metaLog(it) }
                    val dump = dump(null) { ioLog.dump() }.also { dump -> metaLog(dump) }
                    throw ProcessExecutionException(pid, commandLine, exitValue, expectedExitValue, dump.removeEscapeSequences())
                }
                metaLog("Process $pid terminated successfully.")
                this@ManagedProcess
            }
        }
        set(value) {
            externalSync = value
        }

    private val preparedToString = StringBuilder().apply {
        append(" commandLine=${commandLine.commandLine};")
        append(" expectedExitValue=$expectedExitValue;")
        append(" processTerminationCallback=${processTerminationCallback.asEmoji};")
        append(" destroyOnShutdown=${destroyOnShutdown.asEmoji}")
    }

    override fun toString(): String {
        val delegateString =
            if (::javaProcess.isLazyInitialized) "$javaProcess; result=${onExit.isCompletedExceptionally.not().asEmoji}"
            else "not yet initialized"
        return "${this::class.simpleName}[delegate=$delegateString;$preparedToString]"
    }
}

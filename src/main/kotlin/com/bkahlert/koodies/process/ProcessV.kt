package com.bkahlert.koodies.process

import com.bkahlert.koodies.boolean.asEmoji
import com.bkahlert.koodies.concurrent.process.CommandLine
import com.bkahlert.koodies.concurrent.process.DelegatingProcess
import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.process.IOLog
import com.bkahlert.koodies.concurrent.process.LoggingProcess
import com.bkahlert.koodies.concurrent.process.ProcessExecutionException
import com.bkahlert.koodies.concurrent.process.Processes
import com.bkahlert.koodies.concurrent.process.Processor
import com.bkahlert.koodies.concurrent.process.ShutdownHookUtils
import com.bkahlert.koodies.concurrent.process.UserInput.enter
import com.bkahlert.koodies.concurrent.startAsCompletableFuture
import com.bkahlert.koodies.exception.dump
import com.bkahlert.koodies.exception.toSingleLineString
import com.bkahlert.koodies.io.RedirectingOutputStream
import com.bkahlert.koodies.isLazyInitialized
import com.bkahlert.koodies.nio.NonBlockingReader
import com.bkahlert.koodies.nio.file.Paths
import com.bkahlert.koodies.process.Processors.ioProcessingThreadPool
import com.bkahlert.koodies.process.Processors.noopProcessor
import com.bkahlert.koodies.shell.ShellScript
import com.bkahlert.koodies.shell.ShellScript.Companion.build
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.terminal.ansi.AnsiStyles.tag
import com.github.ajalt.clikt.output.TermUi
import org.apache.commons.io.output.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.Reader
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.time.Duration
import java.lang.Process as JavaProcess

private class TeeOutputStream(outputStream: OutputStream, branch: OutputStream) : org.apache.commons.io.output.TeeOutputStream(outputStream, branch)
private class TeeInputStream(inputStream: InputStream, branch: OutputStream) : org.apache.commons.io.input.TeeInputStream(inputStream, branch, false)


interface ProcessV {
    fun metaLog(metaMessage: String) = metaStream.enter(metaMessage, delay = Duration.ZERO)
    val metaStream: OutputStream
    val outputStream: OutputStream
    val inputStream: InputStream
    val errorStream: InputStream
    val pid: Long
    val alive: Boolean
    val exitValue: Int
    val onExit: CompletableFuture<out ProcessV>
    fun waitFor(): Int = onExit.join().exitValue
    fun stop(): ProcessV
    fun kill(): ProcessV
}

abstract class ProcessDelegate : ProcessV {
    protected abstract val javaProcess: JavaProcess

    override val metaStream: OutputStream by lazy { ByteArrayOutputStream() }
    override val outputStream: OutputStream by lazy { javaProcess.outputStream }
    override val inputStream: InputStream by lazy { javaProcess.inputStream }
    override val errorStream: InputStream by lazy { javaProcess.errorStream }
    override val pid: Long by lazy { javaProcess.pid() }
    override val alive: Boolean get() = javaProcess.isAlive
    override val exitValue: Int get() = javaProcess.exitValue()
    abstract override val onExit: CompletableFuture<ProcessV>
    override fun waitFor(): Int = onExit.join().exitValue
    override fun stop(): ProcessV = also { javaProcess.destroy() }
    override fun kill(): ProcessV = also { javaProcess.destroyForcibly() }

    override fun toString(): String = "${this::class.simpleName}"
}


open class LightweightProcess(
    protected val commandLine: CommandLine,
) : ProcessDelegate() {

    override val javaProcess: JavaProcess by lazy {
        commandLine.lazyProcess().value
    }

    val output: String by lazy { inputStream.bufferedReader().use { it.readText() }.trim() }

    override val onExit: CompletableFuture<ProcessV>
        get() = javaProcess.onExit().thenApply { this }
}


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
    override var onExit: CompletableFuture<ProcessV>
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


/**
 * Executes the [shellScript] using the specified [processor] and returns the [LoggingProcess].
 */
fun executeShellScript(
    workingDirectory: Path = Paths.Temp,
    env: Map<String, String> = emptyMap(),
    expectedExitValue: Int = 0,
    processTerminationCallback: (() -> Unit)? = null,
    shellScript: ShellScript,
    processor: Processor<ManagedProcess> = noopProcessor(),
): ManagedProcess {
    return ManagedProcess(
        commandLine = CommandLine(command = shellScript.buildTo(Processes.tempScriptFile())),
        workingDirectory = workingDirectory,
        environment = env,
        expectedExitValue = expectedExitValue,
        processTerminationCallback = processTerminationCallback).process(processor)
}

/**
 * Executes the [shellScript] without printing to the console and returns the [LoggingProcess].
 */
fun executeShellScript(
    workingDirectory: Path = Paths.Temp,
    env: Map<String, String> = emptyMap(),
    expectedExitValue: Int = 0,
    processTerminationCallback: (() -> Unit)? = null,
    shellScript: ShellScript.() -> Unit,
): ManagedProcess = executeShellScript(workingDirectory, env, expectedExitValue, processTerminationCallback, shellScript.build())

/**
 * Starts the [shellScript] and returns the corresponding [DelegatingProcess].
 */
fun startShellScript(
    workingDirectory: Path = Paths.Temp,
    env: Map<String, String> = emptyMap(),
    expectedExitValue: Int = 0,
    processTerminationCallback: (() -> Unit)? = null,
    shellScript: ShellScript,
): ManagedProcess {
    return ManagedProcess(
        commandLine = CommandLine(command = shellScript.buildTo(Processes.tempScriptFile())),
        workingDirectory = workingDirectory,
        environment = env,
        expectedExitValue = expectedExitValue,
        processTerminationCallback = processTerminationCallback)
}

/**
 * Starts the [shellScript] and returns the corresponding [DelegatingProcess].
 */
fun startShellScript(
    workingDirectory: Path = Paths.Temp,
    env: Map<String, String> = emptyMap(),
    expectedExitValue: Int = 0,
    processTerminationCallback: (() -> Unit)? = null,
    shellScript: ShellScript.() -> Unit,
): ManagedProcess = startShellScript(workingDirectory, env, expectedExitValue, processTerminationCallback, shellScript.build())


/**
 * All about processing processes.
 */
object Processors {
    /**
     * Thread pool used for processing the [IO] of [DelegatingProcess].
     */
    var ioProcessingThreadPool: ExecutorService = Executors.newCachedThreadPool()

    /**
     * A [Processor] that prints the encountered [IO] to the console.
     */
    fun <P : ProcessV> printingProcessor(): Processor<P> =
        { line -> TermUi.echo(line) }

    /**
     * A [Processor] that does nothing with the [IO].
     *
     * This processor is suited if the process's input and output streams
     * should just be completely consumed—with the side effect of getting logged.
     */
    fun <P : ProcessV> noopProcessor(): Processor<P> =
        { _ -> }
}

/**
 * Just consumes the [IO] / depletes the input and output streams
 * so they get logged.
 */
inline fun <reified P : ManagedProcess> P.silentlyProcess(): ManagedProcess =
    process(false, InputStream.nullInputStream(), noopProcessor())


/**
 * Attaches to the [ProcessV.outputStream] and [ProcessV.errorStream]
 * of the specified [ProcessV] and passed all [IO] to the specified [processor].
 *
 * If no [processor] is specified, the output and the error stream will be
 * printed to the console.
 *
 * TOOD try out NIO processing; or just readLines with keepDelimiters respectively EOF as additional line separator
 */
fun <P : ManagedProcess> P.process(
    processor: Processor<P> = Processors.printingProcessor(),
): ManagedProcess = process(true, InputStream.nullInputStream(), processor)

/**
 * Attaches to the [ProcessV.outputStream] and [ProcessV.errorStream]
 * of the specified [ProcessV] and passed all [IO] to the specified [processor].
 *
 * If no [processor] is specified, the output and the error stream will be
 * printed to the console.
 *
 * TOOD try out NIO processing; or just readLines with keepDelimiters respectively EOF as additional line separator
 */
fun <P : ManagedProcess> P.process(
    nonBlockingReader: Boolean,
    processInputStream: InputStream,
    processor: Processor<P> = Processors.printingProcessor(),
): ManagedProcess {

    fun CompletableFuture<*>.exceptionallyThrow(type: String) = exceptionally {
        throw RuntimeException("An error occurred while processing ${type.tag()}.", it)
    }

    fun InputStream.readerForStream(nonBlockingReader: Boolean): Reader =
        if (nonBlockingReader) NonBlockingReader(this, blockOnEmptyLine = true) else InputStreamReader(this)

    return apply {

        val inputProvider = ioProcessingThreadPool.startAsCompletableFuture(name = "stdin") {
            processInputStream.use {
                var bytesCopied: Long = 0
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytes = it.read(buffer)
                while (bytes >= 0) {
                    outputStream.write(buffer, 0, bytes)
                    bytesCopied += bytes
                    bytes = it.read(buffer)
                }
            }
        }.exceptionallyThrow("stdin")

        val outputConsumer = ioProcessingThreadPool.startAsCompletableFuture(name = "stdout") {
            inputStream.readerForStream(nonBlockingReader).forEachLine { line ->
                processor(this@process, IO.Type.OUT typed line)
            }
        }.exceptionallyThrow("stdout")

        val errorConsumer = ioProcessingThreadPool.startAsCompletableFuture(name = "stderr") {
            errorStream.readerForStream(nonBlockingReader).forEachLine { line ->
                processor(this@process, IO.Type.ERR typed line)
            }
        }.exceptionallyThrow("stderr")

        this@process.externalSync = CompletableFuture.allOf(inputProvider, outputConsumer, errorConsumer)
    }
}

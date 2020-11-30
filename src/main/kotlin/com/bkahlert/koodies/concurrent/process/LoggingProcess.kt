package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.IN
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.concurrent.process.UserInput.enter
import com.bkahlert.koodies.concurrent.startAsCompletableFuture
import com.bkahlert.koodies.exception.dump
import com.bkahlert.koodies.exception.toSingleLineString
import com.bkahlert.koodies.io.RedirectingOutputStream
import com.bkahlert.koodies.nio.NonBlockingReader
import com.bkahlert.koodies.nio.file.exists
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.string.unquoted
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.terminal.ansi.AnsiStyles.tag
import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.util.Paths.WORKING_DIRECTORY
import com.imgcstmzr.util.group
import org.codehaus.plexus.util.cli.Commandline
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStream.nullOutputStream
import java.io.Reader
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlin.time.Duration
import kotlin.time.DurationUnit
import org.apache.commons.io.input.TeeInputStream as CommonsTeeInputStream
import org.apache.commons.io.output.TeeOutputStream as CommonsTeeOutputStream

/**
 * Starts the specified [command] with the [arguments] passed to it
 * using the specified [workingDirectory] and [env].
 *
 * Furthermore it can be specified if a [nonBlockingReader] should be used
 * to process the I/O of the process.
 *
 * If a [ioProcessor] is provided, all I/O will be passed to it.
 * Nonetheless the I/O is also always logged to [ioLog].
 *
 * On termination—no matter if successful or not—[runAfterProcessTermination]
 * will be called.
 */
open class LoggingProcess(
    private val command: String,
    private val arguments: List<String>,
    private val workingDirectory: Path? = WORKING_DIRECTORY,
    private val env: Map<String, String> = emptyMap(),
    private val nonBlockingReader: Boolean = true,
    private val runAfterProcessTermination: (() -> Unit)? = null,
    private val userProvidedInputStream: InputStream? = InputStream.nullInputStream(),
    private val ioProcessor: (LoggingProcess.(IO) -> Unit)? = { line -> echo(line) },
) : Process() {

    constructor(
        command: Command,
        workingDirectory: Path? = WORKING_DIRECTORY,
        env: Map<String, String> = emptyMap(),
        runAfterProcessTermination: (() -> Unit)? = null,
        ioProcessor: (LoggingProcess.(IO) -> Unit)? = { line -> echo(line) },
    ) : this(
        command = Processes.buildShellScriptToTempFile(workingDirectory, command).serialized,
        arguments = emptyList(),
        workingDirectory = null,
        env = env,
        runAfterProcessTermination = runAfterProcessTermination,
        ioProcessor = ioProcessor,
    )

    companion object {
        /**
         * Contains all accessible files contained in this command line.
         */
        val Commandline.includesFiles
            get() = listOf(executable, arguments).map { Path.of(it.toString().unquoted) }.filter { it.exists }

        /**
         * Contains a formatted list of files contained in this command line.
         */
        val Commandline.formattedIncludesFiles: String
            get() = includesFiles.joinToString("\n") { "📄 ${it.toUri()}" }

        /**
         * Simplified version of [CompletableFuture.toString]. Here only the state is kept and
         * the surrounding `CompletableFuture133sds982[` stuff trashed.
         */
        private fun <T> CompletableFuture<T>.toSimpleString(): String =
            "status".let { field -> Regex(".*\\[(?<$field>.*)]").matchEntire(toString())?.group(field)?.value ?: toString() }

        private var executor = Executors.newCachedThreadPool()
    }

    private class TeeOutputStream(outputStream: OutputStream, branch: OutputStream) : CommonsTeeOutputStream(outputStream, branch)
    private class TeeInputStream(inputStream: InputStream, branch: OutputStream) : CommonsTeeInputStream(inputStream, branch, false)

    val ioLog: IOLog = IOLog()
    private val capturingMetaStream: OutputStream by lazy { TeeOutputStream(nullOutputStream(), RedirectingOutputStream { ioLog.add(META, it) }) }
    private val capturingOutputStream: OutputStream by lazy { TeeOutputStream(process.outputStream, RedirectingOutputStream { ioLog.add(IN, it) }) }
    private val capturingInputStream: InputStream by lazy { TeeInputStream(process.inputStream, RedirectingOutputStream { ioLog.add(OUT, it) }) }
    private val capturingErrorStream: InputStream by lazy { TeeInputStream(process.errorStream, RedirectingOutputStream { ioLog.add(ERR, it) }) }

    val metaStream: OutputStream get() = capturingMetaStream
    override fun getOutputStream(): OutputStream = capturingOutputStream
    override fun getInputStream(): InputStream = capturingInputStream
    override fun getErrorStream(): InputStream = capturingErrorStream
    override fun exitValue(): Int = process.exitValue()
    override fun destroy(): Unit = process.destroy()
    override fun destroyForcibly(): Process = process.destroyForcibly()
    override fun supportsNormalTermination(): Boolean = process.supportsNormalTermination()
    override fun isAlive(): Boolean = process.isAlive
    override fun pid(): Long = process.pid()

    val commandLine = Commandline(command).apply {
        addArguments(arguments)
        @Suppress("ExplicitThis")
        workingDirectory = this@LoggingProcess.workingDirectory?.toFile()
        env.forEach { addEnvironment(it.key, it.value) }
    }
    private val process = kotlin.runCatching { commandLine.execute() }.getOrElse { throw ExecutionException(it) }
    private val processHook: Thread = ShutdownHookUtils.processHookFor(process).also { ShutdownHookUtils.addShutDownHook(it) }

    var loggedProcess: CompletableFuture<LoggedProcess> = CompletableFuture.allOf(
        if (userProvidedInputStream == null) CompletableFuture.completedFuture(null)
        else executor.startAsCompletableFuture(name = "$commandLine::stdin") {
            userProvidedInputStream.use {
                var bytesCopied: Long = 0
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytes = it.read(buffer)
                while (bytes >= 0) {
                    outputStream.write(buffer, 0, bytes)
                    bytesCopied += bytes
                    bytes = it.read(buffer)
                }
            }
        }.exceptionallyThrow("stdin"),
        executor.startAsCompletableFuture(name = "$commandLine::stdout") {
            inputStream.readerForStream(nonBlockingReader).forEachLine { line ->
                ioProcessor?.let { // TODO try out NIO processing; or just readLines with keepDelimiters respectively EOF as additional line separator
                    it(this, OUT typed line)
                }
            }
        }.exceptionallyThrow("stdout"),
        executor.startAsCompletableFuture(name = "$commandLine::stderr") {
            errorStream.readerForStream(nonBlockingReader).forEachLine { line ->
                ioProcessor?.let { it(this, ERR typed line) }
            }
        }.exceptionallyThrow("stderr"),
    ).thenCombine(process.onExit()) { _, process ->
        ShutdownHookUtils.removeShutdownHook(processHook)
        process
    }.exceptionally { throwable ->
        val cause = if (throwable is CompletionException) throwable.cause else throwable
        throw dump("""
                $process terminated with ${cause.toSingleLineString()}.
                ${commandLine.formattedIncludesFiles}
            """.trimIndent()) { ioLog.dump() }
            .also { dump -> dump.log() }
            .let { dump -> RuntimeException(dump.removeEscapeSequences(), cause) }
    }.thenApply { process ->
        if (process.exitValue() != 0) {
            dump("""
                    Process ${pid()} terminated with exit code ${exitValue()}.
                    ${commandLine.formattedIncludesFiles}
                """.trimIndent()) { ioLog.dump() }.log()
        } else {
            "Process ${pid()} terminated successfully.".log()
        }
        LoggedProcess(this).also { runAfterProcessTermination?.runCatching { this@runCatching.invoke() } }
    }.run {
        runAfterProcessTermination?.let { callback ->
            handle { returnValue, throwable ->
                runCatching { callback() }
                throwable?.let { throw it } ?: returnValue
            }
        } ?: this
    }

    override fun onExit(): CompletableFuture<Process> = loggedProcess.thenApply { it as Process }
    override fun toHandle(): ProcessHandle = process.toHandle()
    override fun info(): ProcessHandle.Info = process.info()
    override fun children(): Stream<ProcessHandle> = process.children()
    override fun descendants(): Stream<ProcessHandle> = process.descendants()
    override fun waitFor(): Int = loggedProcess.thenApply { it.exitValue() }.get()
    override fun waitFor(timeout: Long, unit: DurationUnit): Boolean = loggedProcess.thenApply { true }.completeOnTimeout(false, timeout, unit).get()
    fun waitFor(timeout: Duration): Boolean = waitFor(timeout.toLongMilliseconds(), TimeUnit.MILLISECONDS)
    fun waitForSuccess(): Unit = loggedProcess.thenApply {
        check(it.exitValue() == 0) {
            dump("""
                    Process ${pid()} terminated with exit code ${exitValue()}.
                    ${commandLine.formattedIncludesFiles}
                """.trimIndent()) { ioLog.dump() }
        }
    }.get()

    override fun toString(): String = "RunningProcess($process; ${loggedProcess.toSimpleString()}; $ioLog)"

    init {
        "Executing $commandLine".log()
        commandLine.formattedIncludesFiles.log()
    }

    protected fun String.log() {
        metaStream.enter(this, delay = Duration.ZERO)
        ioProcessor?.let { it(this@LoggingProcess, META typed this) }
    }

    private fun CompletableFuture<*>.exceptionallyThrow(type: String) = exceptionally {
        process.destroy()
        throw RuntimeException("An error occurred while processing ${type.tag()}.", it)
    }

    private fun InputStream.readerForStream(nonBlockingReader: Boolean): Reader =
        if (nonBlockingReader) NonBlockingReader(this, blockOnEmptyLine = true) else InputStreamReader(this)

}


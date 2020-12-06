package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.IN
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.exception.dump
import com.bkahlert.koodies.exception.toSingleLineString
import com.bkahlert.koodies.io.RedirectingOutputStream
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import org.apache.commons.io.input.TeeInputStream as CommonsTeeInputStream
import org.apache.commons.io.output.TeeOutputStream as CommonsTeeOutputStream

/**
 * A delegating process that logs [DelegatingProcess.metaStream],
 * [Process.getInputStream], [Process.getOutputStream] and [Process.getErrorStream]
 * to [ioLog].
 *
 * Furthermore if the [Process] exits with [expectedExitValue] is also logged
 * and triggers an exception if [waitFor] is called.
 */
open class LoggingProcess(delegatingProcess: DelegatingProcess) : DelegatingProcess(delegatingProcess) {

    private class TeeOutputStream(outputStream: OutputStream, branch: OutputStream) : CommonsTeeOutputStream(outputStream, branch)
    private class TeeInputStream(inputStream: InputStream, branch: OutputStream) : CommonsTeeInputStream(inputStream, branch, false)

    val ioLog: IOLog = IOLog()
    private val capturingMetaStream: OutputStream by lazy { TeeOutputStream(super.metaStream, RedirectingOutputStream { ioLog.add(META, it) }) }
    private val capturingOutputStream: OutputStream by lazy { TeeOutputStream(delegate.outputStream, RedirectingOutputStream { ioLog.add(IN, it) }) }
    private val capturingInputStream: InputStream by lazy { TeeInputStream(delegate.inputStream, RedirectingOutputStream { ioLog.add(OUT, it) }) }
    private val capturingErrorStream: InputStream by lazy { TeeInputStream(delegate.errorStream, RedirectingOutputStream { ioLog.add(ERR, it) }) }

    override val metaStream: OutputStream get() = capturingMetaStream
    override fun getOutputStream(): OutputStream = capturingOutputStream
    override fun getInputStream(): InputStream = capturingInputStream
    override fun getErrorStream(): InputStream = capturingErrorStream

    val loggedProcess: CompletableFuture<LoggedProcess> by lazy {
        super.onExit().exceptionally { throwable ->
            val cause = if (throwable is CompletionException) throwable.cause else throwable
            if (cause is ProcessExecutionException) {
                cause.message?.log()
                val dump = dump(null) { ioLog.dump() }.also { dump -> dump.log() }
                throw ProcessExecutionException(cause.pid, cause.commandLine, cause.exitValue, cause.expectedExitValue, dump.removeEscapeSequences())
            }

            val dump = dump("""
                ${super.delegate} terminated with ${cause.toSingleLineString()}.
                ${commandLine.formattedIncludesFiles}
            """.trimIndent()) { ioLog.dump() }
            dump.log()
            throw RuntimeException(dump.removeEscapeSequences(), cause)
        }.thenApply { _ ->
            "Process ${pid()} terminated successfully.".log()
            LoggedProcess(this)
        }
    }

    override val delegate: Process by lazy {
        super.delegate.also {
            with(commandLine) {
                "Executing $this".log()
                formattedIncludesFiles.log()
            }
        }
    }

    override fun onExit(): CompletableFuture<Process> = loggedProcess.thenApply { it as Process }

    override fun toString(): String {
        val superString = super.toString()
        return superString.replaceBefore("[", "LoggingProcess").dropLast(1) + "$ioLog)"
    }
}


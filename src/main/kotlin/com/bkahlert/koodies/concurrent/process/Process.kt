package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.UserInput.enter
import org.apache.commons.io.output.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import java.lang.Process as JavaProcess

internal open class TeeOutputStream(outputStream: OutputStream, branch: OutputStream) : org.apache.commons.io.output.TeeOutputStream(outputStream, branch)
internal class TeeInputStream(inputStream: InputStream, branch: OutputStream) : org.apache.commons.io.input.TeeInputStream(inputStream, branch, false)

/**
 * Platform independent representation of a process.
 */
interface Process {
    fun metaLog(metaMessage: String) = metaStream.enter(metaMessage, delay = Duration.ZERO)
    val metaStream: OutputStream
    val outputStream: OutputStream
    val inputStream: InputStream
    val errorStream: InputStream
    val pid: Long
    val alive: Boolean
    val exitValue: Int
    val onExit: CompletableFuture<out Process>
    fun waitFor(): Int = onExit.join().exitValue
    fun stop(): Process
    fun kill(): Process
}

abstract class ProcessDelegate : Process {
    protected abstract val javaProcess: JavaProcess

    override val metaStream: OutputStream by lazy { ByteArrayOutputStream() }
    override val outputStream: OutputStream by lazy { javaProcess.outputStream }
    override val inputStream: InputStream by lazy { javaProcess.inputStream }
    override val errorStream: InputStream by lazy { javaProcess.errorStream }
    override val pid: Long by lazy { javaProcess.pid() }
    override val alive: Boolean get() = javaProcess.isAlive
    override val exitValue: Int get() = javaProcess.exitValue()
    abstract override val onExit: CompletableFuture<Process>
    override fun waitFor(): Int = onExit.join().exitValue
    override fun stop(): Process = also { javaProcess.destroy() }
    override fun kill(): Process = also { javaProcess.destroyForcibly() }

    override fun toString(): String = "${this::class.simpleName}"
}

fun Process.waitForTermination() = onExit.thenApply { process -> process.exitValue }.join()

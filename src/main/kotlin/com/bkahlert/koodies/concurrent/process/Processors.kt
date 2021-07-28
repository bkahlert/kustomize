package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.Processors.ioProcessingThreadPool
import com.bkahlert.koodies.concurrent.process.Processors.noopProcessor
import com.bkahlert.koodies.concurrent.startAsCompletableFuture
import com.bkahlert.koodies.nio.NonBlockingReader
import com.bkahlert.koodies.terminal.ansi.AnsiStyles.tag
import com.github.ajalt.clikt.output.TermUi
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * A function that processes the [IO] of a [Process].
 */
typealias Processor<P> = P.(IO) -> Unit

/**
 * All about processing processes.
 */
object Processors {
    /**
     * Thread pool used for processing the [IO] of [Process].
     */
    var ioProcessingThreadPool: ExecutorService = Executors.newCachedThreadPool()

    /**
     * A [Processor] that prints the encountered [IO] to the console.
     */
    fun <P : Process> printingProcessor(): Processor<P> =
        { line -> TermUi.echo(line) }

    /**
     * A [Processor] that does nothing with the [IO].
     *
     * This processor is suited if the process's input and output streams
     * should just be completely consumed—with the side effect of getting logged.
     */
    fun <P : Process> noopProcessor(): Processor<P> =
        { _ -> }
}

/**
 * Just consumes the [IO] / depletes the input and output streams
 * so they get logged.
 */
inline fun <reified P : ManagedProcess> P.silentlyProcess(): ManagedProcess =
    process(false, InputStream.nullInputStream(), noopProcessor())


/**
 * Attaches to the [Process.outputStream] and [Process.errorStream]
 * of the specified [Process] and passed all [IO] to the specified [processor].
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
 * Attaches to the [Process.outputStream] and [Process.errorStream]
 * of the specified [Process] and passed all [IO] to the specified [processor].
 *
 * If no [processor] is specified, the output and the error stream will be
 * printed to the console.
 *
 * TOOD try out NIO processing; or just readLines with keepDelimiters respectively EOF as additional line separator
 */
fun <P : ManagedProcess> P.process(
    nonBlockingReader: Boolean,
    processInputStream: InputStream = InputStream.nullInputStream(),
    processor: Processor<P> = noopProcessor(),
): P {

    fun CompletableFuture<*>.exceptionallyThrow(type: String) = exceptionally {
        throw RuntimeException("An error occurred while processing ${type.tag()}.", it)
    }

    fun InputStream.readerForStream(nonBlockingReader: Boolean): Reader =
        if (nonBlockingReader) NonBlockingReader(this, blockOnEmptyLine = true)
        else InputStreamReader(this)

    return apply {

//        val metaConsumer = metaStream. // TODO meta and info reading

        val inputProvider = ioProcessingThreadPool.startAsCompletableFuture {
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

        val outputConsumer = ioProcessingThreadPool.startAsCompletableFuture {
            inputStream.readerForStream(nonBlockingReader).forEachLine { line ->
                processor(this@process, IO.Type.OUT typed line)
            }
        }.exceptionallyThrow("stdout")

        val errorConsumer = ioProcessingThreadPool.startAsCompletableFuture {
            errorStream.readerForStream(nonBlockingReader).forEachLine { line ->
                processor(this@process, IO.Type.ERR typed line)
            }
        }.exceptionallyThrow("stderr")

        this@process.externalSync = CompletableFuture.allOf(inputProvider, outputConsumer, errorConsumer)
    }
}

package com.bkahlert.koodies.nio

import com.bkahlert.koodies.number.times
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs

@Execution(CONCURRENT)
internal class ConsumableByteArrayOutputStreamTest {

    internal fun createStream() = ConsumableByteArrayOutputStream().also {
        it.writer().apply {
            write("line #1\n")
            write("line #2\r\n")
            write("line #3")
            flush()
        }
    }

    @Test
    internal fun `should return all content`() {
        val stream = createStream()
        expectThat(stream.readAll()).isEqualTo("line #1\nline #2\r\nline #3")
    }

    @Test
    internal fun `should allow acknowledgement`() {
        val stream = createStream().also { it.ack(10) }
        expectThat(stream.readAll()).isEqualTo("ne #2\r\nline #3")
    }

    @Test
    internal fun `should return same instance if read multiple times`() {
        val stream = createStream().also { it.ack(10) }
        val reference = stream.readAll()
        expectThat(reference).isEqualTo("ne #2\r\nline #3")
        10 times { expectThat(stream.readAll()).isSameInstanceAs(reference) }
    }

    @Test
    internal fun `should return null if end is reached and the stream is closed`() {
        val stream = createStream().also { it.ack(23) }.also { it.close() }
        expectThat(stream.readAll()).isEqualTo("3")
        stream.ack(1)
        expectThat(stream.readAll()).isNull()
    }

    @Test
    internal fun `should throw on out of bounds acknowledgement`() {
        expectCatching { createStream().also { it.ack(100) } }.isFailure().isA<IllegalStateException>()
    }
}

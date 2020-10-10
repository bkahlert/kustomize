package com.bkahlert.koodies.nio

import com.bkahlert.koodies.number.times
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs

@Execution(CONCURRENT)
internal class PartialLineReaderTest {

    internal fun createWriterAndReader() = ConsumableByteArrayOutputStream().let { it.writer() to PartialLineReader(it) }

    @Test
    internal fun `should read nothing at the beginning`() {
        val (_, reader) = createWriterAndReader()
        10 times { expectThat(reader.readPartialLine()).isEqualTo("") }
    }

    @Test
    internal fun `should read the same unfinished line all over again`() {
        val line = "some line"
        val (writer, reader) = createWriterAndReader()
        writer.also { it.write(line) }.also { it.flush() }
        val firstReadLine = reader.readPartialLine()
        10 times {
            val readLine = reader.readPartialLine()
            expectThat(readLine).isSameInstanceAs(firstReadLine)
        }
    }

    @Test
    internal fun `should read all lines and repeat unfinished one`() {
        val (writer, reader) = createWriterAndReader()
        writer.also {
            it.write("line #1\n")
            it.write("line #2\r\n")
            it.write("line #3\n")
            it.write("\$prompt: ")
            it.flush()
        }
        expectThat(reader.readPartialLine()).isEqualTo("line #1")
        expectThat(reader.readPartialLine()).isEqualTo("line #2")
        expectThat(reader.readPartialLine()).isEqualTo("line #3")
        10 times { expectThat(reader.readPartialLine()).isEqualTo("\$prompt: ") }
        writer.also {
            it.write("\n")
            it.write("password: ")
            it.flush()
        }
        expectThat(reader.readPartialLine()).isEqualTo("\$prompt: ")
        10 times { expectThat(reader.readPartialLine()).isEqualTo("password: ") }
        writer.also {
            it.write("\n")
            it.write("bye\n")
            it.write("shutting down\r\n")
            it.flush()
            it.close()
        }
        expectThat(reader.readPartialLine()).isEqualTo("password: ")
        expectThat(reader.readPartialLine()).isEqualTo("bye")
        expectThat(reader.readPartialLine()).isEqualTo("shutting down")
        expectThat(reader.readPartialLine()).isNull()
    }
}

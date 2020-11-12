package com.bkahlert.koodies.nio

import com.bkahlert.koodies.test.junit.Slow
import com.bkahlert.koodies.time.sleep
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isBlank
import strikt.assertions.none
import java.io.InputStream
import kotlin.time.Duration
import kotlin.time.seconds
import kotlin.time.toJavaDuration

@Execution(CONCURRENT)
class BlockOnEmptyLineOtherwiseNonBlockingReaderTest :
    SharedReaderTest({ inputStream: InputStream, timeout: Duration, logger: RenderingLogger<*> ->
        NonBlockingReader(inputStream = inputStream, timeout = timeout, logger = logger, blockOnEmptyLine = true)
    }) {

    @Slow @Test
    fun `should not read empty lines due to timeout`(logger: InMemoryLogger<*>) {
        val reader = readerFactory(object : InputStream() {
            override fun read(): Int {
                5.seconds.sleep()
                return -1
            }

        }, 2.seconds, logger)

        val read: MutableList<String> = mutableListOf()
        assertTimeoutPreemptively(100.seconds.toJavaDuration()) {
            read.addAll(reader.readLines())
        }

        expectThat(read).none { isBlank() }
    }
}

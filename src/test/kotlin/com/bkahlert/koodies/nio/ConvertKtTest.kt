package com.bkahlert.koodies.nio

import com.bkahlert.koodies.unit.Mega
import com.bkahlert.koodies.unit.Size
import com.bkahlert.koodies.unit.bytes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.isLessThan
import java.io.ByteArrayInputStream

@Isolated // to allow for benchmarking
@Execution(CONCURRENT)
internal class ConvertKtTest {

    val bytesToCopy: Size = 10.Mega.bytes

    @Test
    internal fun `should convert in a separate thread`() {

        val inputStream = ByteArrayInputStream(bytesToCopy.toZeroFilledByteArray()
            .also { array -> array.forEachIndexed { index, byte -> array[index] = (index % 249).toByte() } })

        val outputStream = inputStream.convert()

        val timeStarted: Long = System.currentTimeMillis()

        expectThat(outputStream.toByteArray().size).isLessThan(bytesToCopy.bytes.toInt() / 2)
        while (outputStream.toByteArray().size < bytesToCopy.bytes.toInt()) {
            Thread.sleep(10)
        }
        expectThat(System.currentTimeMillis() - timeStarted).isLessThan(5000)
    }
}

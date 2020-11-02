package com.bkahlert.koodies.io

import com.bkahlert.koodies.nio.ClassPath
import com.bkahlert.koodies.nio.bufferedInputStream
import com.imgcstmzr.patch.isEqualTo
import com.imgcstmzr.util.readAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
class JoinToStringKtTest {
    @Test
    fun `should join byte arrays to string`() {
        val buffer = ByteArray(1024)
        var read = 0
        val byteArrays = mutableListOf<ByteArray>()
        val file = ClassPath("Journey to the West - Introduction.txt")
        val inputStream = file.bufferedInputStream()
        while (inputStream.read(buffer).also { read = it } > 0) byteArrays.add(buffer.copyOfRange(0, read))

        expectThat(byteArrays.joinToString()).isEqualTo(file.readAll())
    }
}

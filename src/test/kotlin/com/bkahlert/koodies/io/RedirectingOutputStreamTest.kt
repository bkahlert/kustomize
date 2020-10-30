package com.bkahlert.koodies.io

import com.bkahlert.koodies.nio.ClassPath
import com.imgcstmzr.patch.isEqualTo
import com.imgcstmzr.util.readAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
internal class RedirectingOutputStreamTest {
    @Test
    internal fun `should redirect`() {
        val text = ClassPath("Macbeth - Chapter I.txt").readAll()
        val captured = mutableListOf<ByteArray>()
        text.byteInputStream().copyTo(RedirectingOutputStream { captured.add(it) })
        expectThat(captured.joinToString()).isEqualTo(text)
    }

    @Test
    internal fun `should redirect non latin`() {
        val text = ClassPath("Journey to the West - Introduction.txt").readAll()
        val captured = mutableListOf<ByteArray>()
        text.byteInputStream().copyTo(RedirectingOutputStream { captured.add(it) })
        expectThat(captured.joinToString()).isEqualTo(text)
    }
}

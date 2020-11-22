package com.bkahlert.koodies.io

import com.imgcstmzr.patch.isEqualTo
import com.imgcstmzr.util.MiscFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
class RedirectingOutputStreamTest {

    @Test
    fun `should redirect`() {
        val text = MiscFixture.MacBeth.text
        val captured = mutableListOf<ByteArray>()
        text.byteInputStream().copyTo(RedirectingOutputStream { captured.add(it) })
        expectThat(captured.joinToString()).isEqualTo(text)
    }

    @Test
    fun `should redirect non latin`() {
        val text = MiscFixture.JourneyToTheWest.text
        val captured = mutableListOf<ByteArray>()
        text.byteInputStream().copyTo(RedirectingOutputStream { captured.add(it) })
        expectThat(captured.joinToString()).isEqualTo(text)
    }
}

package com.bkahlert.koodies.string

import com.imgcstmzr.patch.isEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
internal class LinesKtTest {

    @Test
    internal fun `should join list to multiline string`() {
        val actual = listOf("line1", "line2").lines("prefix-", "-postfix") { "LINE " + it.drop(4) }
        expectThat(actual).isEqualTo("""
                prefix-LINE 1
                LINE 2-postfix
            """.trimIndent())
    }

    @Test
    internal fun `should join sequence to multiline string`() {
        val actual = sequenceOf("line1", "line2").lines("prefix-", "-postfix") { "LINE " + it.drop(4) }
        expectThat(actual).isEqualTo("""
                prefix-LINE 1
                LINE 2-postfix
            """.trimIndent())
    }
}

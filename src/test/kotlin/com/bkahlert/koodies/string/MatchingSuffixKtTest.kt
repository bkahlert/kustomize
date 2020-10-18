package com.bkahlert.koodies.string

import com.imgcstmzr.patch.isEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isNullOrBlank

@Execution(CONCURRENT)
internal class MatchingSuffixKtTest {
    @Test
    internal fun `should find matching suffix`() {
        expectThat("Prom!§\$%&/())pt".matchingSuffix("pt", "/())pt", "om", "&/())p")).isEqualTo("/())pt")
    }

    @Test
    internal fun `should not find non-matching suffix`() {
        expectThat("Prompt!".matchingSuffix("abc", "def")).isNullOrBlank()
    }
}

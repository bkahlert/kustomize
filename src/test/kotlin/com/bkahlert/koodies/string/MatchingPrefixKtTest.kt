package com.bkahlert.koodies.string

import com.imgcstmzr.patch.isEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isNullOrBlank

@Execution(CONCURRENT)
class MatchingPrefixKtTest {
    @Test
    fun `should find matching prefix`() {
        expectThat("Prom!§\$%&/())pt".matchingPrefix("pt", "Prom!§\$", "om", "&/())p")).isEqualTo("Prom!§\$")
    }

    @Test
    fun `should not find non-matching prefix`() {
        expectThat("Prompt!".matchingPrefix("pt!".trimMargin(), "def")).isNullOrBlank()
    }
}


package com.bkahlert.koodies.string

import com.imgcstmzr.patch.isEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
internal class WithSuffixKtTest {
    @Test
    internal fun `should append suffix if missing`() {
        expectThat("Prompt".withSuffix("!")).isEqualTo("Prompt!")
    }

    @Test
    internal fun `should not append suffix if present`() {
        expectThat("Prompt!".withSuffix("!")).isEqualTo("Prompt!")
    }
}

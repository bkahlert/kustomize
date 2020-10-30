package com.bkahlert.koodies.collections

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
internal class AndKtTest {
    @Test
    internal fun `should create list`() {
        expectThat("first" and "second").isEqualTo(listOf("first", "second"))
    }
}

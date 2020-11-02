package com.bkahlert.koodies.collections

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class AndKtTest {
    @Test
    fun `should create list`() {
        expectThat("first" and "second").isEqualTo(listOf("first", "second"))
    }
}

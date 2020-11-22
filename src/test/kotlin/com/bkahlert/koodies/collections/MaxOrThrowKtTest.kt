package com.bkahlert.koodies.collections

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

@Execution(CONCURRENT)
class MaxOrThrowKtTest {
    @Test
    fun `should find max`() {
        expectThat(listOf(1, 3, 2).maxOrThrow()).isEqualTo(3)
    }

    @Test
    fun `should throw on empty`() {
        expectCatching { emptyList<Int>().maxOrThrow() }.isFailure().isA<NoSuchElementException>()
    }
}

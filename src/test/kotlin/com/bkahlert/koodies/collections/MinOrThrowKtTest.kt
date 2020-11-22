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
class MinOrThrowKtTest {
    @Test
    fun `should find min`() {
        expectThat(listOf(3, 1, 2).minOrThrow()).isEqualTo(1)
    }

    @Test
    fun `should throw on empty`() {
        expectCatching { emptyList<Int>().minOrThrow() }.isFailure().isA<NoSuchElementException>()
    }
}


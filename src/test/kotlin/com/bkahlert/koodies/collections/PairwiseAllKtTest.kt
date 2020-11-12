package com.bkahlert.koodies.collections


import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue


@Execution(CONCURRENT)
class PairwiseAllKtTest {

    @Test
    fun `should success on matching size all filters`() {
        expectThat(listOf("A", "B").pairwiseAll({ it == "A" }, { it.length < 10 })).isTrue()
        expectThat(arrayOf("A", "B").pairwiseAll({ it == "A" }, { it.length < 10 })).isTrue()
    }

    @Test
    fun `should fail on size mismatch`() {
        expectThat(listOf("A", "B").pairwiseAll({ it == "A" })).isFalse()
        expectThat(arrayOf("A", "B").pairwiseAll({ it == "A" })).isFalse()
    }

    @Test
    fun `should fail on negative filter result`() {
        expectThat(listOf("A", "B").pairwiseAll({ it == "A" }, { it == "A" })).isFalse()
        expectThat(arrayOf("A", "B").pairwiseAll({ it == "A" }, { it == "A" })).isFalse()
    }
}

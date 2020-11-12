package com.bkahlert.koodies.collections

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class WithNegativeIndicesKtTest {
    val list = listOf("a", "b", "c").withNegativeIndices()

    @Test
    fun `should support negative indices`() {
        expectThat(list).get { get(-1) }.isEqualTo("c")
    }

    @Test
    fun `should support negative overflow`() {
        expectThat(list).get { get(-5) }.isEqualTo("b")
    }

    @Test
    fun `should support positive overflow`() {
        expectThat(list).get { get(6) }.isEqualTo("a")
    }
}

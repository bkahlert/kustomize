package com.bkahlert.koodies.collections

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(ExecutionMode.CONCURRENT)
internal class DictionaryTest {

    private val dict = dictOf(
        "known" to 42,
        "negative" to -1,
    ) { profile -> Int.MAX_VALUE }

    @Test
    internal fun `should get value on match`() {
        expectThat(dict["known"]).isEqualTo(42)
    }

    @Test
    internal fun `should get default on mismatch`() {
        expectThat(dict["unknown"]).isEqualTo(Int.MAX_VALUE)
    }
}

package com.bkahlert.koodies.string


import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@Execution(ExecutionMode.CONCURRENT)
internal class CharRangesTest {

    @ConcurrentTestFactory
    internal fun `alphanumeric contains`() = listOf(
        CharRanges.`a-z` to
            (listOf('a', 'b', 'c') to
                listOf('A', '1', '$')),
        CharRanges.`A-Z` to
            (listOf('A', 'B', 'C') to
                listOf('a', '1', '$')),
        CharRanges.`0-9` to
            (listOf('1', '2', '3') to
                listOf('A', 'a', '$')),
    ).flatMap { (characterRange, expectations) ->
        val (contained, notContained) = expectations
        listOf(
            dynamicContainer("contained in $characterRange",
                contained.map { char ->
                    dynamicTest("$char") {
                        expectThat(characterRange.contains<Any>(char)).isTrue()
                    }
                }),
            dynamicContainer("not contained in $characterRange",
                notContained.map { char ->
                    dynamicTest("$char") {
                        expectThat(characterRange.contains<Any>(char)).isFalse()
                    }
                })
        )
    }
}
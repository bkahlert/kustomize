package com.bkahlert.koodies.terminal.ascii

import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isGreaterThanOrEqualTo

@Execution(CONCURRENT)
class KaomojisTest {

    @ConcurrentTestFactory
    fun `should create random Kaomoji`() = (0 until 10).map { i ->
        val kaomoji = Kaomojis.random()
        dynamicTest(kaomoji) {
            expectThat(kaomoji).get { codePoints().count() }.isGreaterThanOrEqualTo(3)
        }
    }

    @ConcurrentTestFactory
    fun `should create random Kaomoji from`() = Kaomojis.Generator.values().map { category ->
        dynamicContainer(category.name, (0 until 10).map { i ->
            val kaomoji = category.random()
            dynamicTest(kaomoji) {
                expectThat(kaomoji).get { codePoints().count() }.isGreaterThanOrEqualTo(3)
            }
        })
    }

    @ConcurrentTestFactory
    fun `should create random dogs`() = (0 until 10).map { i ->
        val kaomoji = Kaomojis.Dogs.random()
        dynamicTest(kaomoji) {
            expectThat(kaomoji).get { length }.isGreaterThanOrEqualTo(5)
        }
    }

    @ConcurrentTestFactory
    fun `should create random wizards`() = (0 until 10).map { i ->
        val kaomoji = Kaomojis.`(＃￣_￣)o︠・━・・━・━━・━☆`.random()
        dynamicTest(kaomoji) {
            expectThat(kaomoji).get { length }.isGreaterThanOrEqualTo(5)
        }
    }
}

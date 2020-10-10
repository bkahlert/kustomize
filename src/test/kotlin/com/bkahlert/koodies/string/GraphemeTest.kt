package com.bkahlert.koodies.string

import com.bkahlert.koodies.string.Grapheme.Companion.getGrapheme
import com.bkahlert.koodies.string.Grapheme.Companion.getGraphemeCount
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.imgcstmzr.util.asString
import com.imgcstmzr.util.quoted
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

@Execution(ExecutionMode.CONCURRENT)
internal class GraphemeTest {

    @Test
    internal fun `should be instantiatable from CodePoints`() {
        val subject = Grapheme(listOf(CodePoint("2"), CodePoint("\u20E3")))
        expectThat(subject).asString().isEqualTo("2⃣")
    }

    @Test
    internal fun `should be instantiatable from CharSequence`() {
        expectThat(Grapheme("2⃣")).asString().isEqualTo("2⃣")
    }

    @Test
    internal fun `should throw on empty string`() {
        expectCatching { Grapheme("") }.isFailure().isA<IllegalArgumentException>()
    }

    @Test
    internal fun `should throw on multi grapheme string`() {
        expectCatching { Grapheme("1⃣2⃣") }.isFailure().isA<IllegalArgumentException>()
    }

    @ConcurrentTestFactory
    internal fun using() = listOf(
        "\u0041" to 1, // A
        "\uD83E\uDD13" to 1, // 🤓
        "ᾷ" to 1, // 3 code points
        "\u270B\uD83E\uDD1A" to 2, // ✋🤚
    ).flatMap { (string, graphemeCount) ->
        listOf(
            dynamicTest("${string.quoted} should validate successfully") {
                val actual = Grapheme.isGrapheme(string)
                expectThat(actual).isEqualTo(graphemeCount == 1)
            },

            dynamicTest("${string.quoted} should count $graphemeCount code points") {
                val actual = Grapheme.count(string)
                expectThat(actual).isEqualTo(graphemeCount)
            },

            if (graphemeCount == 1)
                dynamicTest("${string.quoted} should be re-creatable using chars") {
                    val actual = Grapheme(string)
                    expectThat(actual).get { Grapheme(string) }.isEqualTo(actual)
                } else
                dynamicTest("${string.quoted} should throw on Grapheme construction") {
                    expectCatching { Grapheme(string) }
                },
            if (graphemeCount == 1)
                dynamicTest("${string.quoted} should be re-creatable using chars") {
                    val actual = Grapheme(string)
                    expectThat(actual).get { Grapheme(string) }.isEqualTo(actual)
                }
            else
                dynamicTest("${string.quoted} should throw on Grapheme construction") {
                    expectCatching { Grapheme(string) }
                },
        )
    }

    @Test
    internal fun `should return nth grapheme`() {
        val string = "vᾷ⚡⚡⚡⚡"
        expectThat(string).get {
            listOf(
                getGrapheme(0),
                getGrapheme(1),
                getGrapheme(2),
                getGrapheme(3),
                getGrapheme(4),
                getGrapheme(5),
            )
        }.containsExactly("v", "ᾷ", "⚡", "⚡", "⚡", "⚡")
    }

    @Test
    internal fun `should throw n+1th grapheme`() {
        expectCatching { "웃유♋⌚⌛⚡☯✡☪".let { it.getGrapheme(it.getGraphemeCount()) } }.isFailure().isA<StringIndexOutOfBoundsException>()
    }
}

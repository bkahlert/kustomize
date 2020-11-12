package com.imgcstmzr.patch.ini

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@Execution(CONCURRENT)
class RegexParserTest {

    val regexParser = RegexParser(
        "margin" to "\\s*",
        "name" to "[\\w]+",
        "assignmentSymbol" to "[:=]",
        "value" to "[\"\']?\\w*[\"\']?",
    )

    @Nested
    inner class Matches {
        @Test
        fun `should match on match`() {
            val parsed = regexParser.matches("   ab=c")
            expectThat(parsed).isTrue()
        }

        @Test
        fun `should not match on no match`() {
            val parsed = regexParser.matches(" -- ")
            expectThat(parsed).isFalse()
        }
    }

    @Nested
    inner class ParseSingle {
        @Test
        fun `should match all groups`() {
            val parsed = regexParser.parseSingle("   ab=c")
            expectThat(parsed).isEqualTo(result("   ", "ab", "=", "c"))
        }

        @Test
        fun `should fail on no match if single requested`() {
            expectCatching { regexParser.parseSingle("   - ") }
                .isFailure()
                .isA<IllegalArgumentException>()
        }

        @Test
        fun `should fail if not all groups are matched`() {
            expectCatching { regexParser.parseSingle("a b = c") }
                .isFailure()
                .isA<IllegalArgumentException>()
        }

        @Test
        fun `should fail on multiple matches if single requested`() {
            expectCatching { regexParser.parseSingle("   ab=c   ab=c") }
                .isFailure()
                .isA<IllegalArgumentException>()
        }
    }

    @Nested
    inner class ParseAll {
        @Test
        fun `should match all groups multiple times`() {
            val parsed = regexParser.parseAll("   ab=c d:")
            expectThat(parsed).containsExactly(
                result("   ", "ab", "=", "c"),
                result(" ", "d", ":", ""),
            )
        }

        @Test
        fun `should return empty on no match`() {
            val parsed = regexParser.parseAll(" -- ")
            expectThat(parsed).isEmpty()
        }
    }

    @Nested
    inner class Replace {
        @Test
        fun `should replace all groups`() {
            val parsed = regexParser.replace(
                regexParser.parseSingle("   ab=c"),
                "margin" to "\t",
                "name" to "empty",
                "value" to "\"\""
            )
            expectThat(parsed).isEqualTo(result("\t", "empty", "=", "\"\""))
        }

        @Test
        fun `should return unchanged if no replacements`() {
            val parsed = regexParser.replace(regexParser.parseSingle("   ab=c"))
            expectThat("$parsed").isEqualTo("   ab=c")
        }

        @Test
        fun `should fail on illegal replacement`() {
            expectCatching {
                regexParser.replace(
                    regexParser.parseSingle("   ab=c"),
                    "margin" to "- ",
                )
            }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @Nested
    inner class Transform {

        @Test
        fun `should replace all groups`() {
            val parsed = regexParser.transform(
                regexParser.parseSingle("   ab=c"),
                "margin" to { "\n" },
                "name" to { "key" },
                "assignmentSymbol" to { "=" },
                "value" to { "value" },
            )
            expectThat("$parsed").isEqualTo("\nkey=value")
        }

        @Test
        fun `should return unchanged if no transformations`() {
            val parsed = regexParser.transform(regexParser.parseSingle("   ab=c"))
            expectThat("$parsed").isEqualTo("   ab=c")
        }
    }

    @Nested
    inner class ReplaceAll {
        @Test
        fun `should replace all groups`() {
            val parsed = regexParser.replaceAll(
                "   ab=c",
                "margin" to "\t",
                "name" to "empty",
                "value" to "\"\""
            )
            expectThat("$parsed").isEqualTo("\tempty=\"\"")
        }

        @Test
        fun `should return unchanged if no replacements`() {
            val parsed = regexParser.replaceAll("   ab=c")
            expectThat("$parsed").isEqualTo("   ab=c")
        }

        @Test
        fun `should fail on illegal replacement`() {
            expectCatching {
                regexParser.replaceAll(
                    "   ab=c",
                    "margin" to "- ",
                )
            }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @Nested
    inner class TransformAll {

        @Test
        fun `should replace all groups index`() {
            val parsed = regexParser.transformAll(
                "   ab=c d:",
                "margin" to { index, value ->
                    when (index) {
                        0 -> value
                        else -> "\n"
                    }
                },
                "name" to { index, value ->
                    when (index) {
                        0 -> value
                        else -> "key"
                    }
                },
                "assignmentSymbol" to { index, value ->
                    when (index) {
                        0 -> value
                        else -> "="
                    }
                },
                "value" to { index, value ->
                    when (index) {
                        0 -> value
                        else -> "value$index"
                    }
                },
            )
            expectThat("$parsed").isEqualTo("   ab=c\nkey=value1")
        }

        @Test
        fun `should return unchanged if no transformations`() {
            val parsed = regexParser.transformAll("   ab=c d:")
            expectThat("$parsed").isEqualTo("   ab=c d:")
        }
    }

    @Nested
    inner class AParsedElement {

        private val parsedElement = regexParser.parseSingle(" key='value'")

        @Test
        fun `should render as unchanged`() {
            expectThat("$parsedElement").isEqualTo(" key='value'")
        }

        @Test
        fun `should be equal if input was the same`() {
            expectThat(parsedElement).isEqualTo(regexParser.parseSingle(" key='value'"))
        }
    }

    @Nested
    inner class AParsedElements {

        private val parsedElements = regexParser.parseAll(" key='value'\na=b")

        @Test
        fun `should render as unchanged`() {

            expectThat("$parsedElements").isEqualTo(" key='value'\na=b")
        }

        @Test
        fun `should be equal if input was the same`() {
            expectThat(parsedElements).isEqualTo(regexParser.parseAll(" key='value'\na=b"))
        }
    }
}


private fun result(margin: String, name: String, op: String, value: String): ParsedElement =
    ParsedElement.of("margin" to margin, "name" to name, "assignmentSymbol" to op, "value" to value)

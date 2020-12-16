package com.bkahlert.koodies.regex

import com.imgcstmzr.util.debug
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.matches

@Execution(CONCURRENT)
class RegularExpressionsTest {

    private data class MatchExpectations(val matchingInput: List<String>, val nonMatchingInput: List<String>)
    private data class SplitExpectations(val splitable: List<Pair<String, List<String>>>, val nonSplitable: List<String>)

    @TestFactory
    fun `should correctly match`() = listOf(
        RegularExpressions.atLeastOneWhitespaceRegex to MatchExpectations(
            matchingInput = listOf(
                " ",
                "  ",
                "\t",
                " \t ",
            ), nonMatchingInput = listOf(
                "",
                "a",
                "a ",
                " a",
                "a b",
            )),
        RegularExpressions.urlRegex to MatchExpectations(
            matchingInput = listOf(
                "http://example.net",
                "https://xn--yp9haa.io/beep/beep?signal=on&timeout=42_000#some-complex-state",
                "ftp://edu.gov/download/latest-shit",
                "file:///some/triple-slash/uri/path/to/file.sh",
            ), nonMatchingInput = listOf(
                "mailto:someone@somewhere",
                "abc://example.net",
                "crap",
            )),
        RegularExpressions.uriRegex to MatchExpectations(
            matchingInput = listOf(
                "http://example.net",
                "https://xn--yp9haa.io/beep/beep?signal=on&timeout=42_000#some-complex-state",
                "ftp://edu.gov/download/latest-shit",
                "file:///some/triple-slash/uri/path/to/file.sh",
                "mailto:someone@somewhere",
                "abc://example.net",
            ), nonMatchingInput = listOf(
                "crap",
            )),
    ).map { (regex, expectations) ->
        dynamicContainer("for ${regex.pattern}", listOf(
            dynamicContainer("should match", expectations.matchingInput.map { matchingInput ->
                dynamicTest("input: $matchingInput") {
                    expectThat(matchingInput).matches(regex)
                }
            }),
            dynamicContainer("should not match", expectations.nonMatchingInput.map { nonMatchingInput ->
                dynamicTest("input: $nonMatchingInput") {
                    expectThat(nonMatchingInput).not { matches(regex) }
                }
            }),
        ))
    }
}


fun <T : CharSequence> Assertion.Builder<T>.entirelyMatchedBy(regex: Regex) =
    get("entirely matched by $regex") { regex.matchEntire(this) }.isNotNull()

fun Assertion.Builder<Regex>.matchEntire(input: CharSequence): Assertion.Builder<MatchResult> =
    get("match entirely ${input.debug}") { matchEntire(input) }.isNotNull()

fun Assertion.Builder<Regex>.matchEntire(input: CharSequence, expected: Boolean = true): Assertion.Builder<out MatchResult?> =
    get("match entirely ${input.debug}") { matchEntire(input) }.run { if (expected) not { isNull() } else isNull() }

fun Assertion.Builder<MatchResult>.group(groupName: String) =
    get("group with name $groupName: %s") { namedGroups[groupName] }

fun Assertion.Builder<MatchResult>.group(index: Int) =
    get("group with index $index: %s") { groups[index] }

val Assertion.Builder<MatchResult>.groupValues
    get() = get("group values: %s") { groupValues }

val Assertion.Builder<MatchGroup?>.value
    get() = get("value %s") { this?.value }


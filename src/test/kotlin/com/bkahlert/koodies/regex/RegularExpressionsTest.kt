package com.bkahlert.koodies.regex

import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
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
//        RegularExpressions.pathRegex to MatchExpectations(
//            matchingInput = listOf(
//                "/abs/path",
//                "/abs/file.ext",
//                "rel/path",
//                "rel/file.ext",
//                "./same/path",
//                "./same/file.ext",
//                "../parent/path",
//                "../parent/file.ext",
//                "../././../strange/../path/./and-strange.file.ext",
//                "C:\\abs\\path",
//                "C:\\abs\\file.ext",
//                "rel\\path",
//                "rel\\file.ext",
//                ".\\same\\path",
//                ".\\same\\file.ext",
//                "..\\parent\\path",
//                "..\\parent\\file.ext",
//                "..\\.\\.\\..\\strange\\..\\path\\.\\and-strange.file.ext",
//                "..\\././../strange/../path/./and-strange.file.ext",
//                "C:/abs/path",
//                "C:/abs/file.ext",
//                "C:/abs\\file.ext",
//                "\\\\smb\\share",
//            ), nonMatchingInput = listOf(
//                "/abs/file*ext",
//                "//file.ext",
//                "/abs/file ext",
//                "text /abs/file.ext",
//                "/abs/file.ext text",
//                "////kj  oio  ijü file.ext",
//            )),
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


//    @TestFactory
//    fun `should correctly split`() = listOf(
//        RegularExpressions.digitLetterRegex to SplitExpectations(
//            splitable = listOf(
//                "1a" to listOf("1", "a"),
//                "1.1a" to listOf("1.1", "a"),
//                "111.11a" to listOf("111.11", "a"),
//                "111.a" to listOf("1", "a"),
//                "1aa" to listOf("1", "aa"),
//                ".1aa" to listOf(".1", "aa"),
//                "1.1aa" to listOf("1.1", "aa"),
//                "111.11aa" to listOf("111.11", "aa"),
//                "111.aa" to listOf("1", "aa"),
//
//                "1 a" to listOf("1", "a"),
//                "1.1 a" to listOf("1.1", "a"),
//                "111.11 a" to listOf("111.11", "a"),
//                "111. a" to listOf("1", "a"),
//                "1 aa" to listOf("1", "aa"),
//                ".1 aa" to listOf(".1", "aa"),
//                "1.1 aa" to listOf("1.1", "aa"),
//                "111.11 aa" to listOf("111.11", "aa"),
//                "111. aa" to listOf("1", "aa"),
//            ), nonSplitable = listOf(
//                "1",
//                ".a",
//                "1.a",
//                "111.11",
//                "111.*",
//            )),
//    ).map { (regex, expectations) ->
//        dynamicContainer("for ${regex.pattern}", listOf(
//            dynamicContainer("should split", expectations.splitable.map { (input, splitUp) ->
//                dynamicTest("input: $input") {
//                    expectThat(input.split(regex)).isEqualTo(splitUp)
//                }
//            }),
//            dynamicContainer("should not split", expectations.nonSplitable.map { input ->
//                dynamicTest("input: $input") {
//                    expectThat(input.split(regex)).containsExactly(input)
//                }
//            }),
//        ))
//    }
}

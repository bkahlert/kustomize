package com.bkahlert.koodies.regex

import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.matches

@Execution(CONCURRENT)
class RegularExpressionsTest {

    private data class MatchExpectations(val matchingInput: List<String>, val nonMatchingInput: List<String>)

    @ConcurrentTestFactory
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
}

package com.bkahlert.koodies.regex

import com.bkahlert.koodies.regex.SequenceOfAllMatchesKtTest.Companion.htmlLinkList
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
internal class CountMatchesKtTest {
    @Test
    fun `should find all matches`() {
        expectThat(RegularExpressions.urlRegex.countMatches(htmlLinkList)).isEqualTo(14)
    }
}

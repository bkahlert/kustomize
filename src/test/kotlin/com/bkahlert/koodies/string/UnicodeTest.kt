package com.bkahlert.koodies.string

import com.bkahlert.koodies.string.Unicode.nextLine
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.imgcstmzr.util.isEqualToStringWise
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
internal class UnicodeTest {
    @Nested
    inner class Get {

        @ConcurrentTestFactory
        internal fun `should return code point`() = listOf(
            133 to nextLine,
            119594 to Unicode.DivinationSymbols.tetragramForPurety,
        ).flatMap { (codePoint, expected) ->
            listOf(
                DynamicTest.dynamicTest("\"$expected\" ？⃔ \"$codePoint\"") {
                    val actual: CodePoint = Unicode[codePoint]
                    expectThat(actual).isEqualToStringWise(expected)
                }
            )
        }
    }
}

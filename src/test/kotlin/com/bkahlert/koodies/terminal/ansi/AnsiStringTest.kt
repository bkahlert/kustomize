package com.bkahlert.koodies.terminal.ansi

import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.ansiAwareLength
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.imgcstmzr.util.quoted
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
internal class AnsiStringTest {

    @Nested
    inner class Length {
        @ConcurrentTestFactory
        fun `should return ansi free length`(): List<DynamicTest> {
            return listOf(
                41 to "${AnsiCode.ESC}[3;36m${AnsiCode.ESC}[4mImportant:${AnsiCode.ESC}[24m This line has ${AnsiCode.ESC}[9mno${AnsiCode.ESC}[29m ANSI escapes.${AnsiCode.ESC}[23;39m",
                40 to "${AnsiCode.ESC}[3;36m${AnsiCode.ESC}[4mImportant:${AnsiCode.ESC}[24m This line has ${AnsiCode.ESC}[9mno${AnsiCode.ESC}[29m ANSI escapes${AnsiCode.ESC}[23;39m",
                26 to "${AnsiCode.ESC}[3;36m${AnsiCode.ESC}[4mImportant:${AnsiCode.ESC}[24m This line has ${AnsiCode.ESC}[9mn${AnsiCode.ESC}[23;29;39m",
                11 to "${AnsiCode.ESC}[3;36m${AnsiCode.ESC}[4mImportant:${AnsiCode.ESC}[24m ${AnsiCode.ESC}[23;39m",
                10 to "${AnsiCode.ESC}[3;36m${AnsiCode.ESC}[4mImportant:${AnsiCode.ESC}[23;24;39m",
                9 to "${AnsiCode.ESC}[3;36m${AnsiCode.ESC}[4mImportant${AnsiCode.ESC}[23;24;39m",
                0 to ""
            ).map { (expected, ansiString) ->
                DynamicTest.dynamicTest("${ansiString.quoted}.length should be $expected") {
                    expectThat(ansiString.ansiAwareLength()).isEqualTo(expected)
                }
            }
        }
    }
}

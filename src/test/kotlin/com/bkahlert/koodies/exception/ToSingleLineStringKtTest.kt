package com.bkahlert.koodies.exception

import com.bkahlert.koodies.concurrent.process.Processes.evalShellScript
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.startsWith
import java.nio.file.Path

@Execution(CONCURRENT)
class ToSingleLineStringKtTest {
    @Nested
    inner class AThrowable {

        @Test
        fun `should format as a single line`() {
            val actual = RuntimeException("test").toSingleLineString()
            expectThat(actual).startsWith("RuntimeException: test at.(ToSingleLineStringKtTest.kt:22)")
            expectThat(actual.lines()).hasSize(1)
        }
    }

    @Nested
    inner class WithException {

        @Test
        fun `should format as a single line`() {
            val actual = Result.failure<String>(RuntimeException("test")).toSingleLineString()
            expectThat(actual).startsWith("RuntimeException: test at.(ToSingleLineStringKtTest.kt:33)")
            expectThat(actual.lines()).hasSize(1)
        }
    }

    @Nested
    inner class AResult {

        @Nested
        inner class WithValue {

            @Test
            fun `should format as a single line`() {
                val actual = Result.success("good").toSingleLineString()
                expectThat(actual.removeEscapeSequences()).isEqualTo("good")
                expectThat(actual.lines()).hasSize(1)
            }

            @Test
            fun `should format Path instances as URI`() {
                val actual = Result.success(Path.of("/path")).toSingleLineString()
                expectThat(actual.removeEscapeSequences()).isEqualTo("file:///path")
                expectThat(actual.lines()).hasSize(1)
            }

            @Test
            fun `should format run processes as exit code`() {
                val actual = Result.success(evalShellScript { !"exit 42" }).toSingleLineString()
                expectThat(actual.removeEscapeSequences()).isEqualTo("42")
                expectThat(actual.lines()).hasSize(1)
            }
        }
    }
}

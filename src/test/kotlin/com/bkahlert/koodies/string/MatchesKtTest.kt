package com.bkahlert.koodies.string


import com.bkahlert.koodies.terminal.ANSI.EscapeSequences.termColors
import com.bkahlert.koodies.test.strikt.matches
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isTrue

@Execution(ExecutionMode.CONCURRENT)
internal class MatchesKtTest {

    internal fun matching_single_line_string() =
        "this is a test".matches("this is a {}")

    @Test
    internal fun `should match matching single line string`() {
        expectThat(matching_single_line_string()).isTrue()
    }

    @Test
    internal fun `should not match non-matching single line string`() {
        expectThat("this is a test").not { matches("this is also a {}") }
    }

    @Test
    internal fun `should match matching multi line string`() {
        expectThat("""
            Executing [sh, -c, >&1 echo "test output"
            >&2 echo "test error"] in /Users/bkahlert/Development/com.imgcstmzr.
            Started Process[pid=72692, exitValue=0]
            Process[pid=72692, exitValue=0] stopped with exit code 0
        """.trimIndent().matches("""
            Executing [sh, -c, >&1 echo "test output"
            >&2 echo "test error"] in {}
            Started Process[pid={}, exitValue={}]
            Process[pid={}, exitValue={}] stopped with exit code {}
        """.trimIndent())).isTrue()
    }

    @Test
    internal fun `should not match non-matching multi line string`() {
        expectThat("""
            Executing [sh, -c, >&1 echo "test output"
            >&2 echo "test error"] instantly.
            Started Process[pid=72692, exitValue=0]
            Process[pid=72692, exitValue=0] stopped with exit code 0
        """.trimIndent()).not {
            matches("""
            Executing [sh, -c, >&1 echo "test output"
            >&2 echo "test error"] in {}
            Started Process[pid={}, exitValue={}]
            Process[pid={}, exitValue={}] stopped with exit code {}
        """.trimIndent())
        }
    }

    @Test
    internal fun `should remove trailing line break by default`() {
        expectThat("abc\n").matches("abc")
    }

    @Test
    internal fun `should allow to deactivate trailing line removal`() {
        expectThat("abc\n").not { matches("abc", removeTrailingBreak = false) }
    }

    @Test
    internal fun `should remove ANSI escape sequences by default`() {
        expectThat("${termColors.red("ab")}c").matches("abc")
    }

    @Test
    internal fun `should allow to deactivate removal of ANSI escape sequences`() {
        expectThat("${termColors.red("ab")}c").not { matches("abc", removeEscapeSequences = false) }
    }

    @Test
    internal fun `should allow to ignore trailing lines`() {
        expectThat("${termColors.red("ab")}c").not { matches("abc\nxxx\nyyy", ignoreTrailingLines = true) }
    }
}

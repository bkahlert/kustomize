package com.bkahlert.koodies.terminal.ascii

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(ExecutionMode.CONCURRENT)
internal class BorderTest {

    @Test
    internal fun `should border centered text`() {
        val string = """
                   foo
              bar baz
        """.trimIndent()
        val actual = string.wrapWithBorder("╭─╮\n│*│\n╰─╯", 0, 0)
        expectThat(actual).isEqualTo("""
            ╭───────╮
            │**foo**│
            │bar baz│
            ╰───────╯
        """.trimIndent())
    }

    @Test
    internal fun `should border centered text with padding and margin`() {
        val string = """
                   foo
              bar baz
        """.trimIndent()
        val actual = string.wrapWithBorder("╭─╮\n│*│\n╰─╯", padding = 2, margin = 4)
        expectThat(actual).isEqualTo("""
            *********************
            *********************
            ****╭───────────╮****
            ****│***********│****
            ****│****foo****│****
            ****│**bar baz**│****
            ****│***********│****
            ****╰───────────╯****
            *********************
            *********************
        """.trimIndent())
    }
}

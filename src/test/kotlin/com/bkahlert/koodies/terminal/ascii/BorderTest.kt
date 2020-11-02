package com.bkahlert.koodies.terminal.ascii

import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(ExecutionMode.CONCURRENT)
class BorderTest {

    @ConcurrentTestFactory
    fun `should border centered text`(): List<DynamicTest> {
        val string = """
                   foo
              bar baz
        """.trimIndent()

        return listOf(
            string.wrapWithBorder("╭─╮\n│*│\n╰─╯", 0, 0),
            string.lines().wrapWithBorder("╭─╮\n│*│\n╰─╯", 0, 0)).map {
            dynamicTest(it) {
                expectThat(it).isEqualTo("""
                    ╭───────╮
                    │**foo**│
                    │bar baz│
                    ╰───────╯
                """.trimIndent())
            }
        }
    }

    @ConcurrentTestFactory
    fun `should border centered text with padding and margin`(): List<DynamicTest> {
        val string = """
                   foo
              bar baz
        """.trimIndent()
        return listOf(
            string.wrapWithBorder("╭─╮\n│*│\n╰─╯", padding = 2, margin = 4),
            string.lines().wrapWithBorder("╭─╮\n│*│\n╰─╯", padding = 2, margin = 4)).map {
            dynamicTest(it) {
                expectThat(it).isEqualTo("""
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
    }
}

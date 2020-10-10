package com.imgcstmzr.process

import com.bkahlert.koodies.terminal.ansi.Style.Companion.gray
import com.bkahlert.koodies.terminal.ansi.Style.Companion.italic
import com.bkahlert.koodies.terminal.removeEscapeSequences
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.imgcstmzr.process.Output.Type.META
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(ExecutionMode.CONCURRENT)
internal class OutputTest {

    @ConcurrentTestFactory
    internal fun `should leave content untouched`() = listOf("someone's output", "")
        .flatMap { rawOutput ->
            Output.Type.values().map { type ->
                dynamicTest("$rawOutput + $type") {
                    val string = (type typed rawOutput).toString()
                    expectThat(string.removeEscapeSequences<CharSequence>()).isEqualTo(rawOutput)
                }
            }
        }

    @Test
    internal fun `should properly format`() {
        val string = (META typed "raw output").toString()
        expectThat(string).isEqualTo("raw output".gray().italic())
    }
}



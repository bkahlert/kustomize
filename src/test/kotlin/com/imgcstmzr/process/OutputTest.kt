package com.imgcstmzr.process

import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.terminal.ansi.AnsiColors.gray
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.italic
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
        .flatMap { sample ->
            Output.Type.values().map { type ->
                dynamicTest("$sample + $type") {
                    val message = "$sample of type $type"
                    val string = (type typed message).toString().also { println(it) }
                    expectThat(string.removeEscapeSequences<CharSequence>()).isEqualTo(message)
                }
            }
        }

    @Test
    internal fun `should properly format`() {
        val string = (META typed "raw output").toString()
        expectThat(string).isEqualTo("raw output".gray().italic())
    }
}



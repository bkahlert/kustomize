package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.terminal.ansi.AnsiColors.gray
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.italic
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class IOTest {

    @TestFactory
    fun `should leave content untouched`() = listOf("IO", "")
        .flatMap { sample ->
            IO.Type.values().map { type ->
                dynamicTest("$sample + $type") {
                    val message = "$sample of type $type"
                    val string = (type typed message).toString()
                    expectThat(string.removeEscapeSequences()).isEqualTo(message)
                }
            }
        }

    @Test
    fun `should properly format`() {
        val string = (META typed "raw output").toString()
        expectThat(string).isEqualTo("raw output".gray().italic())
    }
}



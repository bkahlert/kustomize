package com.imgcstmzr.cli

import koodies.terminal.AnsiColors.cyan
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class FormattersKtTest {

    private val aProperty: Boolean? = null

    @Test
    fun `should format char sequence as parameter`() {
        expectThat("string".asParam()).isEqualTo("string".cyan())
    }

    @Test
    fun `should format property as parameter`() {
        val subject = ::aProperty.asParam()
        expectThat(subject).isEqualTo("a-property".cyan())
    }
}


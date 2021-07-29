package com.bkahlert.kustomize.cli

import koodies.text.ANSI.Text.Companion.ansi
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class FormattersKtTest {

    private val aProperty: Boolean? = null

    @Test
    fun `should format character sequence as parameter`() {
        expectThat("string".asParam()).isEqualTo("string".ansi.cyan.done)
    }

    @Test
    fun `should format property as parameter`() {
        val subject = ::aProperty.asParam()
        expectThat(subject).isEqualTo("a-property".ansi.cyan.done)
    }
}

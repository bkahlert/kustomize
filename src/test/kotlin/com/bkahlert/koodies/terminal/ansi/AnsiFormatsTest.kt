package com.bkahlert.koodies.terminal.ansi

import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.IDE
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.bold
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.dim
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.hidden
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.inverse
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.italic
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.strikethrough
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.underline
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class AnsiFormatsTest {
    @Test
    fun `should format bold`() {
        expectThat("bold".bold()).isEqualTo(ANSI.termColors.bold("bold"))
    }

    @Test
    fun `should format dim`() {
        expectThat("dim".dim()).isEqualTo(ANSI.termColors.dim("dim"))
    }

    @Test
    fun `should format italic`() {
        expectThat("italic".italic()).isEqualTo(ANSI.termColors.italic("italic"))
    }

    @Test
    fun `should format underline`() {
        expectThat("underline".underline()).isEqualTo(ANSI.termColors.underline("underline"))
    }

    @Test
    fun `should format inverse`() {
        expectThat("inverse".inverse()).isEqualTo(ANSI.termColors.inverse("inverse"))
    }

    @Test
    fun `should format hidden`() {
        expectThat("hidden".hidden()).isEqualTo(if (IDE.isIntelliJ) " ".repeat("hidden".length + 2) else ANSI.termColors.hidden("hidden"))
    }

    @Test
    fun `should format strikethrough`() {
        expectThat("strikethrough".strikethrough()).isEqualTo(ANSI.termColors.strikethrough("strikethrough"))
    }
}

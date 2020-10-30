package com.bkahlert.koodies.terminal.ansi

import com.bkahlert.koodies.terminal.ANSI
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
internal class AnsiFormatsTest {
    @Test
    internal fun `should format bold`() {
        expectThat("bold".bold()).isEqualTo(ANSI.termColors.bold("bold"))
    }

    @Test
    internal fun `should format dim`() {
        expectThat("dim".dim()).isEqualTo(ANSI.termColors.dim("dim"))
    }

    @Test
    internal fun `should format italic`() {
        expectThat("italic".italic()).isEqualTo(ANSI.termColors.italic("italic"))
    }

    @Test
    internal fun `should format underline`() {
        expectThat("underline".underline()).isEqualTo(ANSI.termColors.underline("underline"))
    }

    @Test
    internal fun `should format inverse`() {
        expectThat("inverse".inverse()).isEqualTo(ANSI.termColors.inverse("inverse"))
    }

    @Test
    internal fun `should format hidden`() {
        expectThat("hidden".hidden()).isEqualTo(ANSI.termColors.hidden("hidden"))
    }

    @Test
    internal fun `should format strikethrough`() {
        expectThat("strikethrough".strikethrough()).isEqualTo(ANSI.termColors.strikethrough("strikethrough"))
    }
}

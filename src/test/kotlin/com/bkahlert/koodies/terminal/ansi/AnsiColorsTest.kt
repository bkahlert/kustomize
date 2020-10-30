package com.bkahlert.koodies.terminal.ansi

import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiColors.black
import com.bkahlert.koodies.terminal.ansi.AnsiColors.blue
import com.bkahlert.koodies.terminal.ansi.AnsiColors.brightBlue
import com.bkahlert.koodies.terminal.ansi.AnsiColors.brightCyan
import com.bkahlert.koodies.terminal.ansi.AnsiColors.brightGreen
import com.bkahlert.koodies.terminal.ansi.AnsiColors.brightMagenta
import com.bkahlert.koodies.terminal.ansi.AnsiColors.brightRed
import com.bkahlert.koodies.terminal.ansi.AnsiColors.brightWhite
import com.bkahlert.koodies.terminal.ansi.AnsiColors.brightYellow
import com.bkahlert.koodies.terminal.ansi.AnsiColors.cyan
import com.bkahlert.koodies.terminal.ansi.AnsiColors.gray
import com.bkahlert.koodies.terminal.ansi.AnsiColors.green
import com.bkahlert.koodies.terminal.ansi.AnsiColors.magenta
import com.bkahlert.koodies.terminal.ansi.AnsiColors.red
import com.bkahlert.koodies.terminal.ansi.AnsiColors.white
import com.bkahlert.koodies.terminal.ansi.AnsiColors.yellow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
internal class AnsiColorsTest {

    @Test
    internal fun `should colorize magenta on cyan`() {
        expectThat("magenta on cyan".magenta { cyan }).isEqualTo((ANSI.termColors.magenta on ANSI.termColors.cyan)("magenta on cyan"))
    }

    @Test
    internal fun `should colorize black`() {
        expectThat("black".black()).isEqualTo(ANSI.termColors.black("black"))
    }

    @Test
    internal fun `should colorize red`() {
        expectThat("red".red()).isEqualTo(ANSI.termColors.red("red"))
    }

    @Test
    internal fun `should colorize green`() {
        expectThat("green".green()).isEqualTo(ANSI.termColors.green("green"))
    }

    @Test
    internal fun `should colorize yellow`() {
        expectThat("yellow".yellow()).isEqualTo(ANSI.termColors.yellow("yellow"))
    }

    @Test
    internal fun `should colorize blue`() {
        expectThat("blue".blue()).isEqualTo(ANSI.termColors.blue("blue"))
    }

    @Test
    internal fun `should colorize magenta`() {
        expectThat("magenta".magenta()).isEqualTo(ANSI.termColors.magenta("magenta"))
    }

    @Test
    internal fun `should colorize cyan`() {
        expectThat("cyan".cyan()).isEqualTo(ANSI.termColors.cyan("cyan"))
    }

    @Test
    internal fun `should colorize white`() {
        expectThat("white".white()).isEqualTo(ANSI.termColors.white("white"))
    }

    @Test
    internal fun `should colorize gray`() {
        expectThat("gray".gray()).isEqualTo(ANSI.termColors.gray("gray"))
    }

    @Test
    internal fun `should colorize brightRed`() {
        expectThat("brightRed".brightRed()).isEqualTo(ANSI.termColors.brightRed("brightRed"))
    }

    @Test
    internal fun `should colorize brightGreen`() {
        expectThat("brightGreen".brightGreen()).isEqualTo(ANSI.termColors.brightGreen("brightGreen"))
    }

    @Test
    internal fun `should colorize brightYellow`() {
        expectThat("brightYellow".brightYellow()).isEqualTo(ANSI.termColors.brightYellow("brightYellow"))
    }

    @Test
    internal fun `should colorize brightBlue`() {
        expectThat("brightBlue".brightBlue()).isEqualTo(ANSI.termColors.brightBlue("brightBlue"))
    }

    @Test
    internal fun `should colorize brightMagenta`() {
        expectThat("brightMagenta".brightMagenta()).isEqualTo(ANSI.termColors.brightMagenta("brightMagenta"))
    }

    @Test
    internal fun `should colorize brightCyan`() {
        expectThat("brightCyan".brightCyan()).isEqualTo(ANSI.termColors.brightCyan("brightCyan"))
    }

    @Test
    internal fun `should colorize brightWhite`() {
        expectThat("brightWhite".brightWhite()).isEqualTo(ANSI.termColors.brightWhite("brightWhite"))
    }
}

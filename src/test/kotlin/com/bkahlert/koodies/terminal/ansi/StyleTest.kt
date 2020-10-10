package com.bkahlert.koodies.terminal.ansi

import com.bkahlert.koodies.terminal.ansi.Style.Companion.black
import com.bkahlert.koodies.terminal.ansi.Style.Companion.blue
import com.bkahlert.koodies.terminal.ansi.Style.Companion.brightBlue
import com.bkahlert.koodies.terminal.ansi.Style.Companion.brightCyan
import com.bkahlert.koodies.terminal.ansi.Style.Companion.brightGreen
import com.bkahlert.koodies.terminal.ansi.Style.Companion.brightMagenta
import com.bkahlert.koodies.terminal.ansi.Style.Companion.brightRed
import com.bkahlert.koodies.terminal.ansi.Style.Companion.brightWhite
import com.bkahlert.koodies.terminal.ansi.Style.Companion.brightYellow
import com.bkahlert.koodies.terminal.ansi.Style.Companion.cyan
import com.bkahlert.koodies.terminal.ansi.Style.Companion.gray
import com.bkahlert.koodies.terminal.ansi.Style.Companion.green
import com.bkahlert.koodies.terminal.ansi.Style.Companion.magenta
import com.bkahlert.koodies.terminal.ansi.Style.Companion.red
import com.bkahlert.koodies.terminal.ansi.Style.Companion.style
import com.bkahlert.koodies.terminal.ansi.Style.Companion.white
import com.bkahlert.koodies.terminal.ansi.Style.Companion.yellow
import com.imgcstmzr.patch.isEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import com.bkahlert.koodies.terminal.ANSI.EscapeSequences.termColors as tc

@Execution(CONCURRENT)
internal class StyleTest {
    @Test
    internal fun `should colorize black`() {
        expectThat("black".black()).isEqualTo(tc.black("black"))
    }

    @Test
    internal fun `should colorize red`() {
        expectThat("red".red()).isEqualTo(tc.red("red"))
    }

    @Test
    internal fun `should colorize green`() {
        expectThat("green".green()).isEqualTo(tc.green("green"))
    }

    @Test
    internal fun `should colorize yellow`() {
        expectThat("yellow".yellow()).isEqualTo(tc.yellow("yellow"))
    }

    @Test
    internal fun `should colorize blue`() {
        expectThat("blue".blue()).isEqualTo(tc.blue("blue"))
    }

    @Test
    internal fun `should colorize magenta`() {
        expectThat("magenta".magenta()).isEqualTo(tc.magenta("magenta"))
    }

    @Test
    internal fun `should colorize cyan`() {
        expectThat("cyan".cyan()).isEqualTo(tc.cyan("cyan"))
    }

    @Test
    internal fun `should colorize white`() {
        expectThat("white".white()).isEqualTo(tc.white("white"))
    }

    @Test
    internal fun `should colorize gray`() {
        expectThat("gray".gray()).isEqualTo(tc.gray("gray"))
    }

    @Test
    internal fun `should colorize brightRed`() {
        expectThat("brightRed".brightRed()).isEqualTo(tc.brightRed("brightRed"))
    }

    @Test
    internal fun `should colorize brightGreen`() {
        expectThat("brightGreen".brightGreen()).isEqualTo(tc.brightGreen("brightGreen"))
    }

    @Test
    internal fun `should colorize brightYellow`() {
        expectThat("brightYellow".brightYellow()).isEqualTo(tc.brightYellow("brightYellow"))
    }

    @Test
    internal fun `should colorize brightBlue`() {
        expectThat("brightBlue".brightBlue()).isEqualTo(tc.brightBlue("brightBlue"))
    }

    @Test
    internal fun `should colorize brightMagenta`() {
        expectThat("brightMagenta".brightMagenta()).isEqualTo(tc.brightMagenta("brightMagenta"))
    }

    @Test
    internal fun `should colorize brightCyan`() {
        expectThat("brightCyan".brightCyan()).isEqualTo(tc.brightCyan("brightCyan"))
    }

    @Test
    internal fun `should colorize brightWhite`() {
        expectThat("brightWhite".brightWhite()).isEqualTo(tc.brightWhite("brightWhite"))
    }

    @Nested
    inner class Style {
        @Test
        internal fun `should colorize black`() {
            expectThat("black".style.colorize.black()).isEqualTo(tc.black("black"))
        }

        @Test
        internal fun `should colorize red`() {
            expectThat("red".style.colorize.red()).isEqualTo(tc.red("red"))
        }

        @Test
        internal fun `should colorize green`() {
            expectThat("green".style.colorize.green()).isEqualTo(tc.green("green"))
        }

        @Test
        internal fun `should colorize yellow`() {
            expectThat("yellow".style.colorize.yellow()).isEqualTo(tc.yellow("yellow"))
        }

        @Test
        internal fun `should colorize blue`() {
            expectThat("blue".style.colorize.blue()).isEqualTo(tc.blue("blue"))
        }

        @Test
        internal fun `should colorize magenta`() {
            expectThat("magenta".style.colorize.magenta()).isEqualTo(tc.magenta("magenta"))
        }

        @Test
        internal fun `should colorize cyan`() {
            expectThat("cyan".style.colorize.cyan()).isEqualTo(tc.cyan("cyan"))
        }

        @Test
        internal fun `should colorize white`() {
            expectThat("white".style.colorize.white()).isEqualTo(tc.white("white"))
        }

        @Test
        internal fun `should colorize gray`() {
            expectThat("gray".style.colorize.gray()).isEqualTo(tc.gray("gray"))
        }

        @Test
        internal fun `should colorize brightRed`() {
            expectThat("brightRed".style.colorize.brightRed()).isEqualTo(tc.brightRed("brightRed"))
        }

        @Test
        internal fun `should colorize brightGreen`() {
            expectThat("brightGreen".style.colorize.brightGreen()).isEqualTo(tc.brightGreen("brightGreen"))
        }

        @Test
        internal fun `should colorize brightYellow`() {
            expectThat("brightYellow".style.colorize.brightYellow()).isEqualTo(tc.brightYellow("brightYellow"))
        }

        @Test
        internal fun `should colorize brightBlue`() {
            expectThat("brightBlue".style.colorize.brightBlue()).isEqualTo(tc.brightBlue("brightBlue"))
        }

        @Test
        internal fun `should colorize brightMagenta`() {
            expectThat("brightMagenta".style.colorize.brightMagenta()).isEqualTo(tc.brightMagenta("brightMagenta"))
        }

        @Test
        internal fun `should colorize brightCyan`() {
            expectThat("brightCyan".style.colorize.brightCyan()).isEqualTo(tc.brightCyan("brightCyan"))
        }

        @Test
        internal fun `should colorize brightWhite`() {
            expectThat("brightWhite".style.colorize.brightWhite()).isEqualTo(tc.brightWhite("brightWhite"))
        }
    }
}

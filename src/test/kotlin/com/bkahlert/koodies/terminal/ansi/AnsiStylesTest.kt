package com.bkahlert.koodies.terminal.ansi

import com.bkahlert.koodies.terminal.ansi.AnsiStyles.echo
import com.bkahlert.koodies.terminal.ansi.AnsiStyles.saying
import com.bkahlert.koodies.terminal.ansi.AnsiStyles.tag
import com.bkahlert.koodies.terminal.ansi.AnsiStyles.unit
import com.imgcstmzr.patch.isEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
class AnsiStylesTest {
    @Test
    fun `should style echo`() {
        expectThat("echo".echo()).isEqualTo("·<❮❰❰❰ echo ❱❱❱❯>·")
    }

    @Test
    fun `should style saying`() {
        expectThat("saying".saying()).isEqualTo("͔˱❮❰( saying")
    }

    @Test
    fun `should style tag`() {
        expectThat("tag".tag()).isEqualTo("【tag】")
    }

    @Nested
    inner class UnitStyle {
        @Test
        fun `should style empty string`() {
            expectThat("".unit()).isEqualTo("❲❳")
        }

        @Test
        fun `should style single char string`() {
            expectThat("x".unit()).isEqualTo("❲x❳")
        }

        @Test
        fun `should style two char string`() {
            expectThat("az".unit()).isEqualTo("❲az❳")
        }

        @Test
        fun `should style multi char string`() {
            expectThat("unit".unit()).isEqualTo("❲unit❳")
        }
    }
}

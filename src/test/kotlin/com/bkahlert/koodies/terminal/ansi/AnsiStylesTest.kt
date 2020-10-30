package com.bkahlert.koodies.terminal.ansi

import com.bkahlert.koodies.terminal.ansi.AnsiStyles.echo
import com.bkahlert.koodies.terminal.ansi.AnsiStyles.saying
import com.bkahlert.koodies.terminal.ansi.AnsiStyles.tag
import com.imgcstmzr.patch.isEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
internal class AnsiStylesTest {
    @Test
    internal fun `should style echo`() {
        expectThat("echo".echo()).isEqualTo("·<❮❰❰❰ echo ❱❱❱❯>·")
    }

    @Test
    internal fun `should style saying`() {
        expectThat("saying".saying()).isEqualTo("͔˱❮❰( saying")
    }

    @Test
    internal fun `should style tag`() {
        expectThat("tag".tag()).isEqualTo("【tag】")
    }
}

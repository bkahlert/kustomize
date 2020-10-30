package com.bkahlert.koodies.string

import com.imgcstmzr.patch.isEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
internal class PrefixWithKtTest {
    @Test
    internal fun `should add prefix if not there`() {
        @Suppress("SpellCheckingInspection")
        expectThat("foo".prefixWith("bar")).isEqualTo("barfoo")
    }

    @Test
    internal fun `should add prefix if already there`() {
        @Suppress("SpellCheckingInspection")
        expectThat("barfoo".prefixWith("bar")).isEqualTo("barbarfoo")
    }
}

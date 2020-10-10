package com.bkahlert.koodies.functional

import com.imgcstmzr.patch.isEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
internal class ComposeKtTest {

    fun a(arg: String): String = "${arg.length}"
    fun b(arg: String): String = "$arg$arg"
    fun c(arg: String): String = "${arg}c"

    @Test
    internal fun `a + b should call b with result of a`() {
        val ab = ::a.compose(::b)
        expectThat(ab("abc")).isEqualTo("33")
    }

    @Test
    internal fun `b + a should call a with result of b`() {
        val ba = ::b.compose(::a)
        expectThat(ba("abc")).isEqualTo("6")
    }

    @Test
    internal fun `compose should have + infix function+`() {
        val ab = ::a.plus(::b)
        expectThat(ab("abc")).isEqualTo("33")
    }

    @Test
    internal fun `should with three functions`() {
        val ab = ::a.compose(::b, ::c)
        expectThat(ab("abc")).isEqualTo("33c")
    }

    @Test
    internal fun `should allow composition without receiver`() {
        val ab = compositionOf(::a, ::b, ::c)
        expectThat(ab("abc")).isEqualTo("33c")
    }

    @Test
    internal fun `should allow conditional composition without receiver`() {
        val ab = compositionOf(true to ::a, false to ::b, true to ::c)
        expectThat(ab("abc")).isEqualTo("3c")
    }
}

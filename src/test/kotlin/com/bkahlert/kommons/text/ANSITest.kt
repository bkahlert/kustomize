package com.bkahlert.kommons.text

import com.bkahlert.kommons.text.ANSI.ansiRemoved
import com.bkahlert.kommons.text.ANSI.containsAnsi
import strikt.api.Assertion.Builder
import strikt.api.DescribeableBuilder

fun <T : CharSequence> Builder<T>.containsAnsi(): Builder<T> =
    assert("contains ANSI escape sequences") {
        when (val actual = it.toString().containsAnsi) {
            true -> pass()
            else -> fail(actual = actual)
        }
    }

inline val <reified T : CharSequence> Builder<T>.ansiRemoved: DescribeableBuilder<String>
    get() = get("escape sequences removed") { ansiRemoved }

inline fun <reified T : CharSequence> Builder<T>.ansiRemoved(noinline assertions: Builder<String>.() -> Unit): Builder<T> =
    with("escape sequences removed", { ansiRemoved }, assertions)

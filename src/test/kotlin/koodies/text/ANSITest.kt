package koodies.text

import koodies.text.ANSI.ansiRemoved
import koodies.text.ANSI.containsAnsi
import strikt.api.Assertion
import strikt.api.DescribeableBuilder

fun <T : CharSequence> Assertion.Builder<T>.containsAnsi(): Assertion.Builder<T> =
    assert("contains ANSI escape sequences") {
        when (val actual = it.toString().containsAnsi) {
            true -> pass()
            else -> fail(actual = actual)
        }
    }

inline val <reified T : CharSequence> Assertion.Builder<T>.ansiRemoved: DescribeableBuilder<String>
    get() = get("escape sequences removed") { ansiRemoved }

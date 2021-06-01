package koodies.logging

import koodies.logging.InMemoryLogger.Companion.SUCCESSFUL_RETURN_VALUE
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import strikt.api.Assertion.Builder
import strikt.api.expectThat


val <T : InMemoryLogger> T.expectLogged
    get() = expectThat(toString(fallbackReturnValue = null,
        keepEscapeSequences = false,
        lineSkip = 1).withoutTrailingLineSeparator)

fun <T : InMemoryLogger> T.expectThatLogged(closeIfOpen: Boolean = true) =
    expectThat(toString(SUCCESSFUL_RETURN_VALUE.takeIf { closeIfOpen }))

fun <T : InMemoryLogger> T.expectThatLogged(closeIfOpen: Boolean = true, block: Builder<String>.() -> Unit) =
    expectThat(toString(SUCCESSFUL_RETURN_VALUE.takeIf { closeIfOpen }), block)

package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.test.strikt.matches
import com.imgcstmzr.util.logging.InMemoryLogger
import strikt.api.Assertion


fun Assertion.Builder<InMemoryLogger<Unit>>.matches(
    curlyPattern: String,
    removeTrailingBreak: Boolean = true,
    removeEscapeSequences: Boolean = true,
    trimmed: Boolean = true,
) = get("logged content") {
    finalizedDump(Result.success(Unit))
}.matches(curlyPattern, removeTrailingBreak, removeEscapeSequences, trimmed)

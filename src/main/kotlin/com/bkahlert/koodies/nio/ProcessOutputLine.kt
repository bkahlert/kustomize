package com.bkahlert.koodies.nio

import com.bkahlert.koodies.string.LineSeparators.hasTrailingLineSeparator

/**
 * A single (possibly not yet finalized) line of process output.
 */
inline class ProcessOutputLine(val text: String?) {
    override fun toString(): String = text ?: ""
    val isInitializing: Boolean get() = text?.length == 1 && text[0] == `␀`
    val isFinalized: Boolean get() = text?.hasTrailingLineSeparator ?: false
    val isEOF: Boolean get() = text == null

    companion object {
        @Suppress("ObjectPropertyName", "NonAsciiCharacters")
        private const val `␀`: Char = '\u0000'
    }
}

package com.imgcstmzr.runtime

import com.bkahlert.koodies.terminal.ANSI
import com.imgcstmzr.util.joinToTruncatedString

interface HasStatus {

    /**
     * Renders the status.
     */
    fun status(): String

    companion object {
        private val pauseSymbol = ANSI.EscapeSequences.termColors.gray("▮▮")
        private val playSymbol = ANSI.EscapeSequences.termColors.gray("◀")
        private val fastForwardSymbol = ANSI.EscapeSequences.termColors.green("◀◀")

        /**
         * Default implementation to render the status of a [List] of [HasStatus] instances.
         */
        fun List<HasStatus>.status(): String {
            if (size == 0) return pauseSymbol
            return joinToTruncatedString("  $playSymbol ", "$fastForwardSymbol ",
                truncated = "…",
                transform = { element -> ANSI.EscapeSequences.termColors.bold(element.status()) },
                transformEnd = { lastElement -> ANSI.EscapeSequences.termColors.gray(lastElement.status()) })
        }

        /**
         * Default implementation to render the status of a [List] of [HasStatus] instances.
         */
        fun List<String>.asStatus(): String {
            if (size == 0) return pauseSymbol
            return joinToTruncatedString("  $playSymbol ", "$fastForwardSymbol ",
                truncated = "…",
                transform = { element -> ANSI.EscapeSequences.termColors.bold(element) },
                transformEnd = { lastElement -> ANSI.EscapeSequences.termColors.gray(lastElement) })
        }
    }
}

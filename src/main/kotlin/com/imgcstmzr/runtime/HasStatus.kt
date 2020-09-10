package com.imgcstmzr.runtime

import com.imgcstmzr.cli.ColorHelpFormatter.Companion.tc
import com.imgcstmzr.util.joinToTruncatedString

interface HasStatus {

    /**
     * Renders the status.
     */
    fun status(): String

    companion object {
        private val pauseSymbol = tc.gray("▮▮")
        private val playSymbol = tc.gray("◀")
        private val fastForwardSymbol = tc.green("◀◀")

        /**
         * Default implementation to render the status of a [List] of [HasStatus] instances.
         */
        fun List<HasStatus>.status(): String {
            if (size == 0) return pauseSymbol
            return joinToTruncatedString("  $playSymbol ", "$fastForwardSymbol ",
                truncated = "…",
                transform = { x -> tc.bold(x.status()) },
                transformEnd = { x -> tc.gray(x.status()) })
        }
    }
}

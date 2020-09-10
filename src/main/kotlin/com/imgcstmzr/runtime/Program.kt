package com.imgcstmzr.runtime

import com.imgcstmzr.cli.ColorHelpFormatter.Companion.tc
import com.imgcstmzr.process.Output

/**
 * A program that can be executed using an [OS] and a [Runtime].
 */
interface Program<P : Program<P>> : HasStatus {
    val name: String
    val state: String?
    val stateCount: Int

    /**
     * Given an [output] of the [OS] conducts calculations until new feedback in the form of [output] is needed.
     * @return `true` if the calculation if ongoing; otherwise return `false`
     */
    fun calc(process: RunningOS<P>, output: Output): Boolean

    /**
     * Renders the status of this [Program].
     */
    override fun status(): String = when (state) {
        null -> tc.gray("◀ $name")
        else -> when (stateCount) {
            0 -> tc.green("◀◀ ") + tc.bold(name)
            else -> tc.green("◀◀ ") + tc.bold(name) + tc.gray("❬${state.toString()}❭")
        }
    }

    companion object {
        /**
         * Given an [output] of the [OS] conducts calculations until new feedback in the form of [output] is needed.
         * @return `true` if the calculation if ongoing; otherwise return `false`
         */
        fun <P : Program<P>> Collection<P>.calc(runningOS: RunningOS<P>, output: Output): Boolean =
            this.firstOrNull()?.calc(runningOS, output) ?: false
    }
}

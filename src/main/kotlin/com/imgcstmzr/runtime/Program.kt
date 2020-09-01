package com.imgcstmzr.runtime

import com.imgcstmzr.cli.ColorHelpFormatter.Companion.tc
import com.imgcstmzr.process.Output

/**
 * A program that can be executed using an [OS] and a [Runtime].
 */
interface Program<P : Program<P>> {
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
    fun status(): String = when (state) {
        null -> tc.gray("◀ $name")
        else -> when (stateCount) {
            0 -> tc.green("◀◀ ") + tc.bold(name)
            else -> tc.green("◀◀ ") + tc.bold(name) + tc.gray("❬${state.toString()}❭")
        }
    }

    companion object {
        private val noRunningWorkflowsIndicator = (tc.gray + tc.italic)("no active program")

        /**
         * Renders the status of these [Program] instances.
         */
        fun <P : Program<*>> List<P>.status(): String {
            return when (size) {
                0 -> noRunningWorkflowsIndicator
                1, 2, 3 -> this.joinToString(" ")
                else -> {
                    val firstWorkflows = subList(0, 2).joinToString(" ") { it.status() }
                    val hiddenWorkflows = tc.gray("◀ …")
                    val lastWorkflows = subList(size - 1, size).joinToString(" ") { it.status() }
                    listOf(firstWorkflows, hiddenWorkflows, lastWorkflows).joinToString(" ")
                }
            }
        }

        /**
         * Given an [output] of the [OS] conducts calculations until new feedback in the form of [output] is needed.
         * @return `true` if the calculation if ongoing; otherwise return `false`
         */
        fun <P : Program<P>> Collection<P>.calc(runningOS: RunningOS<P>, output: Output): Boolean =
            this.firstOrNull()?.calc(runningOS, output) ?: false
    }
}

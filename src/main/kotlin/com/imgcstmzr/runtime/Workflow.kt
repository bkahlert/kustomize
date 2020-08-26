package com.imgcstmzr.runtime

import com.github.ajalt.clikt.output.TermUi.echo
import com.github.ajalt.mordant.TermColors
import com.imgcstmzr.cli.ColorHelpFormatter
import com.imgcstmzr.replaceNonPrintableCharacters
import com.imgcstmzr.stripOffAnsi

/**
 * Instances of this class can interact with a process based on a state machine for the given [purpose].
 * The workflow is as follows:
 * 1. The handler of the [initialState] is called whenever the [Process] generates an output.
 *    Based on the current state and the output the handler has to return with the new state whereas the new state can be the old state.
 *    Furthermore the handler is free to interact with the [Process], e.g. using [Process.fakeInput].
 * 2. Everytime an output is generated the currently active handler based on the provided [stateMachine] is used to process that new output.
 * 3. Step 1-2 occur as long as input is generated or a handler returns `null` as the new state.
 *    `null` signifies the workflows successful accomplishment.
 */
class Workflow(
    val purpose: String,
    private val initialState: (Process, String) -> String?,
    states: List<Pair<String, (Process, String) -> String?>>,
) {
    constructor(
        purpose: String,
        initialState: (Process, String) -> String?,
        vararg states: Pair<String, (Process, String) -> String?>,
    ) : this(purpose, initialState, states.toList())


    private var logging = false
    private var initiating: Boolean = true
    var state: String? = null
    val halted: Boolean
        get() = !initiating && state == null

    private val stateMachine: Map<String, (Process, String) -> String?> = states.associate { it }
    private var stateHistory: MutableList<HistoryElement> = mutableListOf()

    private fun handler(): (Process, String) -> String? {
        if (initiating) {
            initiating = false
            return initialState
        }
        if (halted) {
            return { process, output -> echo(ColorHelpFormatter.INSTANCE.tc.red("Execution already stopped. No more processing.")); null }
        }
        return stateMachine[state] ?: throw IllegalStateException("Unknown state $state. Available: ${stateMachine.keys}. History: $stateHistory")
    }

    fun process(process: Process, output: String): Boolean {
        val oldState = state
        state = handler().invoke(process, output.stripOffAnsi())
        val historyElement = HistoryElement(oldState, output, state)
        if (logging) println("$purpose: $historyElement")
        stateHistory.add(historyElement)
        return state != null
    }

    private data class HistoryElement(private val oldState: String?, private val output: String, private val newState: String?) {
        override fun toString(): String = when (oldState) {
            null -> "▶️❬${newState}❭"
            else -> {
                val visualizedOutput = when (output) {
                    "" -> "[nothing]"
                    else -> output.replaceNonPrintableCharacters()
                }
                when (newState) {
                    oldState -> "❬$oldState❭🔁: $visualizedOutput"
                    null -> "⏹️"
                    else -> "❬$oldState❭⏩️❬$newState❭ $visualizedOutput"
                }
            }
        }
    }

    private val tc = TermColors()
    override fun toString(): String = when (state) {
        null -> tc.gray("◀ $purpose")
        else -> when (stateMachine.size) {
            0 -> tc.green("◀◀ ") + tc.bold(purpose)
            else -> tc.green("◀◀ ") + tc.bold(purpose) + tc.gray("❬${state.toString()}❭")
        }
    }

    fun logging(): Workflow {
        logging = true
        return this
    }
}


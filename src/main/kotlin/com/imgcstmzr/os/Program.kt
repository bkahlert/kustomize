package com.imgcstmzr.os

import com.github.ajalt.clikt.output.TermUi.echo
import koodies.debug.replaceNonPrintableCharacters
import koodies.exec.IO
import koodies.runtime.isDebugging
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.Semantics.formattedAs
import koodies.text.quoted
import koodies.text.truncate

/**
 * Instances of this class can interact with a process based on a state machine for the given [name].
 * Or in other words: A [Program] does not run in a [OperatingSystemProcess] but on it, like somehow a human interacts with a machine.
 *
 * The workflow is as follows:
 * 1. The handler of the [initialState] is called whenever the [Process] generates an output.
 *    Based on the current state and the output the handler has to return with the new state whereas the new state can be the old state.
 *    Furthermore the handler is free to interact with the [Process], e.g. using [RunningOperatingSystem.enter] to simulate a user input.
 * 2. Everytime an output is generated the currently active handler based on the provided [stateMachine] is used to process that new output.
 * 3. Step 1-2 occur as long as input is generated and a handler does not return `null` as the new state.
 *    - `null` signifies the workflows successful completion.
 *    - To signify a problem an exception can be thrown.
 */
class Program(
    val name: String,
    private val initialState: OperatingSystemProcess.() -> String?,
    private val states: List<ProgramState>,
) {

    constructor(
        purpose: String,
        initialState: OperatingSystemProcess.() -> String?,
        vararg states: ProgramState,
    ) : this(purpose, initialState, states.toList())

    private var logging = isDebugging
    private var initiating: Boolean = true
    private val stateCount: Int
        get() = states.size
    var state: String? = null
    val halted: Boolean get() = !initiating && state == null

    private val stateMachine: Map<String, OperatingSystemProcess.(String) -> String?> = states.associate { it }
    private var stateHistory: MutableList<HistoryElement> = mutableListOf()

    @Suppress("SpellCheckingInspection")
    private fun handler(operatingSystemProcess: OperatingSystemProcess): OperatingSystemProcess.(String) -> String? {
        if (initiating) {
            initiating = false
            state = initialState(operatingSystemProcess)
        }
        if (halted) {
            return { output -> echo("Execution of ${this@Program.name.quoted} already stopped. No more processing ${output.quoted}".ansi.red); null }
        }
        return stateMachine[state] ?: throw IllegalStateException("Unknown state $state. Available: ${stateMachine.keys}. History: $stateHistory")
    }

    /**
     * Triggers one computation using the provided [operatingSystemProcess]
     * and the [io] to handle.
     *
     * @return whether the computation did not result in a final state, that is `true` if this program needs more computation,
     * `false` if this program finished.
     */
    fun compute(operatingSystemProcess: OperatingSystemProcess, io: IO): Boolean {
        val oldState = state
        state = handler(operatingSystemProcess).invoke(operatingSystemProcess, io.ansiRemoved)
        val historyElement = HistoryElement(oldState, io, state)
        if (logging) echo("$name execution step #${stateHistory.size}: $historyElement")
        stateHistory.add(historyElement)
        return state != null
    }

    private data class HistoryElement(private val oldState: String?, private val io: IO, private val newState: String?) {
        val leftBracket = "〘"
        val rightBracket = "〙"
        override fun toString(): String = when (oldState) {
            null -> " ▶️ $leftBracket$newState$rightBracket"
            else -> {
                val visualizedOutput = if (io.isBlank()) "\u2400" else io.ansiRemoved.replaceNonPrintableCharacters().formattedAs.input
                when (newState) {
                    oldState -> "$leftBracket$oldState$rightBracket 🔁 $visualizedOutput"
                    null -> " ⏹️ "
                    else -> "$leftBracket$oldState$rightBracket ⏩️ $leftBracket$newState$rightBracket $visualizedOutput"
                }
            }
        }
    }

    fun logging(): Program {
        logging = true
        return this
    }

    override fun toString(): String = when (state) {
        null -> name.ansi.gray.done
        else -> when (stateCount) {
            0 -> name.ansi.bold.done
            else -> name.ansi.bold.done + "❬${state.toString()}❭".ansi.gray.done
        }
    }

    companion object {

        /**
         * Utility version of [Program.compute] that delegates the output to the first [Program] of this [Collection].
         *
         * @return `true` if the calculation is ongoing; otherwise return `false`
         */
        fun Collection<Program>.compute(operatingSystemProcess: OperatingSystemProcess, IO: IO): Boolean =
            this.firstOrNull()?.compute(operatingSystemProcess, IO) ?: false

        /**
         * Formats an array of programs
         */
        fun Array<out Program>.format(): CharSequence = if (size > 0) map { it.name }.asExtra() else "no programs".formattedAs.meta

        private fun stateName(index: Int, commands: Array<out String>): String {
            val commandLine = commands[index]
            val command = commandLine.split(' ').first()
            return "${index + 1}/${commands.size}: $command"
        }

        /**
         * Converts a setup script to an [Array] of [Program]s.
         *
         * The following script is an example of such a script that consist of multiple blocks
         * each with a description precedes by a colon about the purpose of this code block.
         *
         * Example:
         * ```shell script
         * sudo -i
         *
         * : configure SSH port
         * sed -i 's/^\#Port 22$/Port 1234/g' /etc/ssh/sshd_config
         *
         * : configure
         * systemctl enable serial-getty@ttyGS0.service
         *
         * : remove unused DHCP clients
         * apt-get purge -qq -m isc-dhcp-client
         * apt-get purge -y -m udhcpd
         * apt-get autoremove -y -m
         * ```
         */
        @Deprecated("delete")
        @Suppress("SpellCheckingInspection")
        fun fromSetupScript(name: String, readyPattern: Regex, labelledScripts: String): Array<Program> {
            return splitScripts(labelledScripts).map { labelledScript ->
                splitLabel(labelledScript).let { (label, script) -> fromScript(label ?: "$name—$script", readyPattern, script) }
            }.toTypedArray()
        }

        private fun splitScripts(commandBlocks: String) = commandBlocks.trimIndent().split("\\r\n:", "\r:", "\n:")

        /**
         * Splits a [labelledScript] into label and script, whereas the label is `null` if there was none.
         */
        private fun splitLabel(labelledScript: String): Pair<String?, String> = labelledScript.lines().let {
            if (it.size == 1) null to it[0] else it[0] to it[1]
        }

        fun fromScript(name: String, readyPattern: Regex, vararg commands: String): Program {
            val completionState: ProgramState = commands.completionState(readyPattern)

            val states: List<ProgramState> = commands.flatMapIndexed { index, command ->
                val currentStateName: String = stateName(index, commands)
                val currentCompletionStateName: String = stateName(index, commands) + " …"
                val nextStateName: String =
                    if (index + 1 < commands.size) stateName(index + 1, commands)
                    else completionState.first

                val step: OperatingSystemProcess.(String) -> String? = { output: String ->
                    if (output.matches(readyPattern)) {
                        enter("$command\r")
                        currentCompletionStateName
                    } else currentStateName
                }

                val completionStep: OperatingSystemProcess.(String) -> String? = { output: String ->
                    if (output.matches(readyPattern)) {
                        nextStateName
                    } else currentCompletionStateName
                }

                listOf(
                    currentStateName to step,
                    currentCompletionStateName to completionStep,
                )
            }.plusElement(completionState)

            return Program(
                name = name.removePrefix(":").trim(),
                initialState = { if (commands.isEmpty()) null else stateName(0, commands) },
                states = states
            )
        }

        /**
         * Returns the [ProgramState] to awaits the current command finish execution
         * by skipping the [IO] until [readyPattern] is matched.
         */
        private fun Array<out String>.completionState(readyPattern: Regex): ProgramState =
            "waiting for ${last().truncate()} to finish".let { stateName ->
                stateName to { output: String ->
                    if (output.matches(readyPattern)) {
                        null
                    } else stateName
                }
            }
    }
}

typealias ProgramState = Pair<String, OperatingSystemProcess.(String) -> String?>

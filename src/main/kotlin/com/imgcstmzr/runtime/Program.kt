package com.imgcstmzr.runtime

import com.bkahlert.koodies.string.TruncationStrategy.MIDDLE
import com.bkahlert.koodies.string.replaceNonPrintableCharacters
import com.bkahlert.koodies.string.truncate
import com.bkahlert.koodies.terminal.ansi.AnsiColors.cyan
import com.bkahlert.koodies.terminal.ansi.AnsiColors.gray
import com.bkahlert.koodies.terminal.ansi.AnsiColors.red
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.bold
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.process.Output
import com.imgcstmzr.util.debug
import com.imgcstmzr.util.quoted

/**
 * Instances of this class can interact with a process based on a state machine for the given [name].
 * Or in other words: A [Program] does not run in a [RunningOperatingSystem] but on it, like somehow a human interacts with his machine.
 *
 * The workflow is as follows:
 * 1. The handler of the [initialState] is called whenever the [Process] generates an output.
 *    Based on the current state and the output the handler has to return with the new state whereas the new state can be the old state.
 *    Furthermore the handler is free to interact with the [Process], e.g. using [Process.fakeInput].
 * 2. Everytime an output is generated the currently active handler based on the provided [stateMachine] is used to process that new output.
 * 3. Step 1-2 occur as long as input is generated or a handler returns `null` as the new state.
 *    `null` signifies the workflows successful accomplishment.
 */
class Program(
    val name: String,
    private val initialState: RunningOperatingSystem.(String) -> String?,
    private val states: List<ProgramState>,
) : HasStatus {

    constructor(
        purpose: String,
        initialState: RunningOperatingSystem.(String) -> String?,
        vararg states: ProgramState,
    ) : this(purpose, initialState, states.toList())

    private var logging = false
    private var initiating: Boolean = true
    val stateCount: Int
        get() = states.size
    var state: String? = null
    val halted: Boolean
        get() = !initiating && state == null

    private val stateMachine: Map<String, RunningOperatingSystem.(String) -> String?> = states.associate { it }
    private var stateHistory: MutableList<HistoryElement> = mutableListOf()

    private fun handler(): RunningOperatingSystem.(String) -> String? {
        if (initiating) {
            initiating = false
            return initialState
        }
        if (halted) {
            return { output -> echo("Execution of ${name.quoted} already stopped. No more processing.".red()); null }
        }
        return stateMachine[state] ?: throw IllegalStateException("Unknown state $state. Available: ${stateMachine.keys}. History: $stateHistory")
    }

    fun compute(runningOperatingSystem: RunningOperatingSystem, output: Output): Boolean {
        val oldState = state
        state = handler().invoke(runningOperatingSystem, output.unformatted)
        val historyElement = HistoryElement(oldState, output, state)
        if (logging) TermUi.debug("$name execution step #${stateHistory.size}: $historyElement")
        stateHistory.add(historyElement)
        return state != null
    }

    private data class HistoryElement(private val oldState: String?, private val output: Output, private val newState: String?) {
        val leftBracket = "〘"
        val rightBracket = "〙"
        override fun toString(): String = when (oldState) {
            null -> " ▶️ $leftBracket$newState$rightBracket"
            else -> {
                val visualizedOutput = if (output.isBlank) '\u2400' else output.output.replaceNonPrintableCharacters().cyan()
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

    override fun toString(): String = status()

    /**
     * Renders the status of this [Program].
     */
    override fun status(): String = when (state) {
        null -> name.gray()
        else -> when (stateCount) {
            0 -> name.bold()
            else -> name.bold() + "❬${state.toString()}❭".gray()
        }
    }

    companion object {
        /**
         * Utility version of [Program.compute] that delegates the output to the first [Program] of this [Collection].
         *
         * @return `true` if the calculation is ongoing; otherwise return `false`
         */
        fun Collection<Program>.compute(runningOperatingSystem: RunningOperatingSystem, output: Output): Boolean =
            this.firstOrNull()?.compute(runningOperatingSystem, output) ?: false

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
         * systemctl enable getty@ttyGS0.service
         *
         * : remove unused DHCP clients
         * apt-get purge -qq -m isc-dhcp-client
         * apt-get purge -y -m udhcpd
         * apt-get autoremove -y -m
         * ```
         */
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
                val currentCompletionStateName: String = stateName(index, commands) + "…"
                val nextStateName: String? =
                    if (index + 1 < commands.size) stateName(index + 1, commands)
                    else completionState.first

                val step: RunningOperatingSystem.(String) -> String? = { output: String ->
                    if (output.matches(readyPattern)) {
                        input("$command\r")
                        currentCompletionStateName
                    } else currentStateName
                }

                val completionStep: RunningOperatingSystem.(String) -> String? = { output: String ->
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
                initialState = { output -> if (commands.isEmpty()) null else stateName(0, commands) },
                states = states
            )
        }

        /**
         * Returns the [ProgramState] to awaits the current command finish execution
         * by skipping the [Output] until [readyPattern] is matched.
         */
        private fun Array<out String>.completionState(readyPattern: Regex): ProgramState =
            "waiting for ${last().truncate(strategy = MIDDLE)} to finish".let { stateName ->
                stateName to { output: String ->
                    if (output.matches(readyPattern)) {
                        null
                    } else stateName
                }
            }
    }
}

typealias ProgramState = Pair<String, RunningOperatingSystem.(String) -> String?>

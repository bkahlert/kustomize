package com.imgcstmzr.runtime

import com.bkahlert.koodies.terminal.ansi.Style.Companion.red
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.output.TermUi.echo
import com.github.ajalt.mordant.TermColors
import com.imgcstmzr.process.Output
import com.imgcstmzr.util.debug
import com.imgcstmzr.util.quoted
import com.imgcstmzr.util.replaceNonPrintableCharacters

/**
 * Instances of this class can interact with a process based on a state machine for the given [name].
 * Or in other words: A [Program] does not run in a [RunningOS] but on it, like somehow a human interacts with his machine.
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
    private val initialState: RunningOS.(String) -> String?,
    private val states: List<Pair<String, RunningOS.(String) -> String?>>,
    private val tc: TermColors = TermColors(),
) : HasStatus {

    constructor(
        purpose: String,
        initialState: RunningOS.(String) -> String?,
        vararg states: Pair<String, RunningOS.(String) -> String?>,
    ) : this(purpose, initialState, states.toList())

    private var logging = false
    private var initiating: Boolean = true
    val stateCount: Int
        get() = states.size
    var state: String? = null
    val halted: Boolean
        get() = !initiating && state == null

    private val stateMachine: Map<String, RunningOS.(String) -> String?> = states.associate { it }
    private var stateHistory: MutableList<HistoryElement> = mutableListOf()

    private fun handler(): RunningOS.(String) -> String? {
        if (initiating) {
            initiating = false
            return initialState
        }
        if (halted) {
            return { output -> echo("Execution of ${name.quoted} already stopped. No more processing.".red()); null }
        }
        return stateMachine[state] ?: throw IllegalStateException("Unknown state $state. Available: ${stateMachine.keys}. History: $stateHistory")
    }

    fun compute(runningOS: RunningOS, output: Output): Boolean {
        val oldState = state
        state = handler().invoke(runningOS, output.unformatted)
        val historyElement = HistoryElement(oldState, output, state)
        if (logging) TermUi.debug("$name: $historyElement")
        stateHistory.add(historyElement)
        return state != null
    }

    private data class HistoryElement(private val oldState: String?, private val output: Output, private val newState: String?) {
        override fun toString(): String = when (oldState) {
            null -> "▶️❬${newState}❭"
            else -> {
                val visualizedOutput = if (output.isBlank) "[nothing]" else output.raw.replaceNonPrintableCharacters()
                when (newState) {
                    oldState -> "❬$oldState❭🔁: $visualizedOutput"
                    null -> "⏹️"
                    else -> "❬$oldState❭⏩️❬$newState❭ $visualizedOutput"
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
        null -> tc.gray(name)
        else -> when (stateCount) {
            0 -> tc.bold(name)
            else -> tc.bold(name) + tc.gray("❬${state.toString()}❭")
        }
    }

    companion object {
        /**
         * Utility version of [Program.compute] that delegates the output to the first [Program] of this [Collection].
         *
         * @return `true` if the calculation is ongoing; otherwise return `false`
         */
        fun Collection<Program>.compute(runningOS: RunningOS, output: Output): Boolean =
            this.firstOrNull()?.compute(runningOS, output) ?: false

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

        fun fromScript(name: String, readyPattern: Regex, vararg commands: String): Program = Program(
            name.removePrefix(":").trim(),
            { output -> if (commands.isEmpty()) null else stateName(0, commands) },
            commands.mapIndexed { index, command ->
                val currentStateName: String = stateName(index, commands)
                val nextStateName: String? = if (index + 1 < commands.size) stateName(index + 1, commands) else null
                val step: RunningOS.(String) -> String? = { output: String ->
                    if (output.matches(readyPattern)) {
                        input("$command\r")
                        nextStateName
                    } else currentStateName
                }
                currentStateName to step
            }
        )
    }
}

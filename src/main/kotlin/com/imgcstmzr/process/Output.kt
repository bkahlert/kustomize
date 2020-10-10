package com.imgcstmzr.process

import com.bkahlert.koodies.string.mapLines
import com.bkahlert.koodies.terminal.ansi.Style.Companion.bold
import com.bkahlert.koodies.terminal.ansi.Style.Companion.gray
import com.bkahlert.koodies.terminal.ansi.Style.Companion.italic
import com.bkahlert.koodies.terminal.ansi.Style.Companion.red
import com.bkahlert.koodies.terminal.ansi.Style.Companion.yellow
import com.bkahlert.koodies.terminal.removeEscapeSequences
import com.imgcstmzr.process.Output.Type
import com.imgcstmzr.process.Output.Type.ERR

/**
 * Instances are [raw] output with a certain [Type].
 */
data class Output(val raw: String, val type: Type) : CharSequence by raw {
    val unformatted: String by lazy { raw.removeEscapeSequences<CharSequence>() }
    val formatted: String by lazy { type.format(raw) }
    val formattedLines: List<String> by lazy { raw.lines().map { line -> type.format(line) } }
    val isBlank: Boolean = unformatted.isBlank()

    override fun toString(): String = formatted

    companion object {
        fun Throwable.format() = ERR.format(stackTraceToString())
    }

    /**
     * Classifier for different types of [Output].
     */
    enum class Type(val symbol: String, val format: (String) -> String) {
        META("𝕄", { value -> value.removeEscapeSequences().mapLines { it.gray().italic() } }),
        OUT("𝕆", { value -> value.mapLines { it.yellow() } }),
        ERR("𝔼", { value -> value.removeEscapeSequences().mapLines { it.red().bold() } }) {
            infix fun typed(value: Result<*>): Output {
                require(value.isFailure)
                val message = value.exceptionOrNull()?.stackTraceToString() ?: throw IllegalStateException("Exception was unexpectedly null")
                return Output(message, ERR)
            }
        };

        val EMPTY_INSTANCE: Output by lazy { Output("", this) }

        infix fun typed(value: String?): Output = if (value?.isEmpty() == true) EMPTY_INSTANCE else Output(value ?: "❔", this)
    }


}

package com.imgcstmzr.process

import com.github.ajalt.clikt.output.TermUi
import com.imgcstmzr.cli.ColorHelpFormatter.Companion.tc
import com.imgcstmzr.process.Output.Type
import com.imgcstmzr.util.splitLineBreaks
import com.imgcstmzr.util.stripOffAnsi
import com.imgcstmzr.util.trace

/**
 * Instances are [raw] output with a certain [Type].
 */
class Output private constructor(val raw: String, val type: Type) : CharSequence by raw {
    val unformatted: String by lazy { raw.stripOffAnsi() }
    val formatted: String by lazy { type.format(raw) }
    val formattedLines: List<String> by lazy { raw.splitLineBreaks().map { line -> type.format(line) } }
    val isBlank: Boolean = unformatted.isBlank()

    override fun toString(): String = formatted

    companion object {
        val LOGGING_PROCESSOR: Process.(Output) -> Unit = { output ->
            when (output.type) {
                Type.META -> TermUi.trace(output.formatted)
                else -> TermUi.echo(output.formatted)
            }
        }
    }

    /**
     * Classifier for different types of [Output].
     */
    enum class Type(val symbol: String, val format: (String) -> String) {
        META("𝕄", { value -> (tc.gray + tc.italic)(value.stripOffAnsi()) }),
        OUT("𝕆", { value -> value }),
        ERR("𝔼", { value -> (tc.red)(value.stripOffAnsi()) }) {
            infix fun typed(value: Result<*>): Output {
                require(value.isFailure)
                val message = value.exceptionOrNull()?.message ?: throw IllegalStateException("Exception was unexpectedly null")
                return Output(message, ERR)
            }
        };

        infix fun typed(value: String?): Output = Output(value ?: "❔", this)
    }

}

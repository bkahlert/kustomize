package com.imgcstmzr.process

import com.bkahlert.koodies.string.asString
import com.bkahlert.koodies.string.mapLines
import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.ansiAwareLineSequence
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.ansiAwareMapLines
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.terminal.ansi.AnsiColors.brightBlue
import com.bkahlert.koodies.terminal.ansi.AnsiColors.gray
import com.bkahlert.koodies.terminal.ansi.AnsiColors.red
import com.bkahlert.koodies.terminal.ansi.AnsiColors.yellow
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.bold
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.dim
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.italic
import com.imgcstmzr.process.Output.Type
import com.imgcstmzr.process.Output.Type.ERR

/**
 * Instances are [output] output with a certain [Type].
 */
data class Output(
    // TODO rename to IO
    /**
     * Contains the originally encountered [Output].
     */
    val output: String,
    /**
     * Contains the [Type] of this [Output].
     */
    val type: Type,
) : CharSequence by output {

    /**
     * Contains this [output] with all [ANSI] escape sequences removed.
     */
    @Suppress("SpellCheckingInspection")
    val unformatted: String by lazy { output.removeEscapeSequences<CharSequence>() }

    /**
     * Contains this [output] with the format of it's [type] applied.
     */
    val formatted: String by lazy { type.format(output) }

    /**
     * Splits this [Output] into separate lines while keeping the ANSI formatting intact.
     */
    fun lines(): List<Output> = output.ansiAwareLineSequence().map { type typed it }.toList()

    /**
     * Whether this [output] (ignoring eventually existing [ANSI] escape sequences)
     * is blank (≝ is empty or consists of nothing but whitespaces).
     */
    val isBlank: Boolean = unformatted.isBlank()

    override fun toString(): String = formatted

    companion object {
        /**
         * Formats a [Throwable] as an [ERR].
         */
        fun Throwable.format(): String = ERR.format(stackTraceToString())
    }

    /**
     * Classifier for different types of [Output].
     */
    enum class Type(
        @Suppress("unused") private val symbol: String,
        /**
         * Formats a strings to like an output of this [Type].
         */
        val format: (String) -> String,
    ) {
        /**
         * An [Output] that represents information about a [Process].
         */
        META("𝕄", { value -> value.ansiAwareMapLines { it.gray().italic() } }),

        /**
         * An [Output] (of another process) serving as an input.
         */
        IN("𝕀", { value -> value.ansiAwareMapLines { it.brightBlue().dim().italic() } }),

        /**
         * An [Output] that is neither [META], [IN] nor [ERR].
         */
        OUT("𝕆", { value -> value.ansiAwareMapLines { it.yellow() } }),

        /**
         * An [Output] that represents a errors.
         */
        ERR("𝔼", { value -> value.removeEscapeSequences().mapLines { it.red().bold() } }) {
            /**
             * Factory to classify an [ERR] [Output].
             */
            infix fun typed(value: Result<*>): Output {
                require(value.isFailure)
                val message = value.exceptionOrNull()?.stackTraceToString() ?: throw IllegalStateException("Exception was unexpectedly null")
                return Output(message, ERR)
            }
        };

        /**
         * Instance representing an empty [Output].
         */
        private val empty: Output by lazy { Output("", this) }

        /**
         * Factory to classify different [Type]s of [Output].
         */
        infix fun typed(value: CharSequence?): Output = if (value?.isEmpty() == true) empty else Output(value?.asString() ?: "❔", this)

        /**
         * Factory to classify different [Type]s of [Output]s.
         */
        infix fun <T : CharSequence> typed(value: Iterable<T>): List<Output> = value.map { typed(it) }
    }
}

package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.IO.Type
import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
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

// TODO make sealed class and refactor types to inherited IOs
/**
 * Instances are [text] output with a certain [Type].
 */
data class IO(
    /**
     * Contains the originally encountered [IO].
     */
    val text: String,
    /**
     * Contains the [Type] of this [IO].
     */
    val type: Type,
) : CharSequence by text {

    /**
     * Contains this [text] with all [ANSI] escape sequences removed.
     */
    @Suppress("SpellCheckingInspection")
    val unformatted: String by lazy { text.removeEscapeSequences<CharSequence>() }

    /**
     * Contains this [text] with the format of it's [type] applied.
     */
    val formatted: String by lazy { type.format(text) }

    /**
     * Splits this [IO] into separate lines while keeping the ANSI formatting intact.
     */
    fun lines(): List<IO> = text.ansiAwareLineSequence().map { type typed it }.toList()

    /**
     * Whether this [text] (ignoring eventually existing [ANSI] escape sequences)
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
     * Classifier for different types of [IO].
     */
    enum class Type(
        @Suppress("unused") private val symbol: String,
        /**
         * Formats a strings to like an output of this [Type].
         */
        val format: (String) -> String,
    ) {
        /**
         * An [IO] that represents information about a [Process].
         */
        META("𝕄", { value -> value.ansiAwareMapLines { it.gray().italic() } }),

        /**
         * An [IO] (of another process) serving as an input.
         */
        IN("𝕀", { value -> value.ansiAwareMapLines { it.brightBlue().dim().italic() } }),

        /**
         * An [IO] that is neither [META], [IN] nor [ERR].
         */
        OUT("𝕆", { value -> value.ansiAwareMapLines { it.yellow() } }),

        /**
         * An [IO] that represents a errors.
         */
        ERR("𝔼", { value -> value.removeEscapeSequences().mapLines { it.red().bold() } }) {
            /**
             * Factory to classify an [ERR] [IO].
             */
            infix fun typed(value: Result<*>): IO {
                require(value.isFailure)
                val message = value.exceptionOrNull()?.stackTraceToString() ?: throw IllegalStateException("Exception was unexpectedly null")
                return IO(message, ERR)
            }
        };

        /**
         * Instance representing an empty [IO].
         */
        private val empty: IO by lazy { IO("", this) }

        /**
         * Factory to classify different [Type]s of [IO].
         */
        infix fun typed(value: CharSequence?): IO = if (value?.isEmpty() == true) empty else IO(value?.asString() ?: "❔", this)

        /**
         * Factory to classify different [Type]s of [IO]s.
         */
        infix fun <T : CharSequence> typed(value: Iterable<T>): List<IO> = value.map { typed(it) }
    }
}

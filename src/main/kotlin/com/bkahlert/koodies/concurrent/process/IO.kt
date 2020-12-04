package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.IO.Type
import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.string.mapLines
import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiColors.brightBlue
import com.bkahlert.koodies.terminal.ansi.AnsiColors.gray
import com.bkahlert.koodies.terminal.ansi.AnsiColors.red
import com.bkahlert.koodies.terminal.ansi.AnsiColors.yellow
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.bold
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.dim
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.italic
import com.bkahlert.koodies.terminal.ansi.AnsiString

// TODO make sealed class and refactor types to inherited IOs
/**
 * Instances are ANSI formatted output with a certain [Type].
 */
class IO(
    /**
     * Contains the originally encountered [IO].
     */
    val text: AnsiString,
    /**
     * Contains the [Type] of this [IO].
     */
    val type: Type,
) : AnsiString(text.toString(withoutAnsi = false)) {

    /**
     * Contains this [text] with the format of it's [type] applied.
     */
    val formatted: String by lazy { type.format(this) }

    private val lines by lazy { text.lines().map { type typed it }.toList() }

    /**
     * Splits this [IO] into separate lines while keeping the ANSI formatting intact.
     */
    fun lines(): List<IO> = lines

    /**
     * Whether this [text] (ignoring eventually existing [ANSI] escape sequences)
     * is blank (≝ is empty or consists of nothing but whitespaces).
     */
    val isBlank: Boolean by lazy { unformatted.isBlank() }

    override fun toString(): String = formatted

    companion object {
        /**
         * Formats a [Throwable] as an [ERR].
         */
        fun Throwable.format(): String = ERR.format(stackTraceToString().asAnsiString())
    }

    /**
     * Classifier for different types of [IO].
     */
    enum class Type(
        @Suppress("unused") private val symbol: String,
        /**
         * Formats a strings to like an output of this [Type].
         */
        val formatAnsi: (AnsiString) -> String,
    ) {

        /**
         * An [IO] that represents information about a [Process].
         */
        META("𝕄", { value -> value.mapLines { it.gray().italic() } }),

        /**
         * An [IO] (of another process) serving as an input.
         */
        IN("𝕀", { value -> value.mapLines { it.brightBlue().dim().italic() } }),

        /**
         * An [IO] that is neither [META], [IN] nor [ERR].
         */
        OUT("𝕆", { value -> value.mapLines { it.yellow() } }),

        /**
         * An [IO] that represents a errors.
         */
        ERR("𝔼", { value -> value.unformatted.mapLines { it.red().bold() } }) {
            /**
             * Factory to classify an [ERR] [IO].
             */
            infix fun typed(value: Result<*>): IO {
                require(value.isFailure)
                val message = value.exceptionOrNull()?.stackTraceToString() ?: throw IllegalStateException("Exception was unexpectedly null")
                return IO(message.asAnsiString(), ERR)
            }
        };

        /**
         * Instance representing an empty [IO].
         */
        private val EMPTY: IO by lazy { IO(AnsiString.EMPTY, this) }

        /**
         * Factory to classify different [Type]s of [IO].
         */
        infix fun typed(value: CharSequence?): IO = if (value?.isEmpty() == true) EMPTY else IO(value?.asAnsiString() ?: "❔".asAnsiString(), this)

        /**
         * Factory to classify different [Type]s of [IO]s.
         */
        infix fun <T : CharSequence> typed(value: Iterable<T>): List<IO> = value.map { typed(it) }

        infix fun formatted(string: String): String = formatAnsi(string.asAnsiString())
        infix fun formatted(string: AnsiString): String = formatAnsi(string)
        fun format(string: String): String = formatAnsi(string.asAnsiString())
        fun format(string: AnsiString): String = formatAnsi(string)
    }
}
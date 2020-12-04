package com.bkahlert.koodies.shell

import com.bkahlert.koodies.builder.ListBuilderInit
import com.bkahlert.koodies.builder.build
import com.bkahlert.koodies.string.LineSeparators
import com.bkahlert.koodies.string.random

object HereDocBuilder {
    /**
     * Returns a random—most likely unique—label to be used for a [HereDoc].
     */
    fun randomLabel(): String = "HERE-" + String.random(8, allowedCharacters = String.random.alphanumericCapitalCharacters).toUpperCase()

    /**
     * The line separator used by default to separate lines in a [HereDoc].
     */
    const val DEFAULT_LINE_SEPARATOR: String = LineSeparators.LF

    operator fun String.plus(line: String): List<String> = listOf(this, line)

    fun ListBuilderInit<String>.hereDoc(
        label: String = randomLabel(),
        lineSeparator: String = DEFAULT_LINE_SEPARATOR,
    ) = hereDoc(label = label, lineSeparator = lineSeparator, init = this)

    fun hereDoc(
        label: String = randomLabel(),
        lineSeparator: String = DEFAULT_LINE_SEPARATOR,
        init: ListBuilderInit<String>,
    ): String = init.build().let { lines ->
        mutableListOf("<<$label").apply { addAll(lines) }.apply { add(label) }.joinToString(separator = lineSeparator)
    }
}

package com.bkahlert.koodies.shell

import com.bkahlert.koodies.string.random

/**
 * Creates a [here document](https://en.wikipedia.org/wiki/Here_document) consisting of the given [lines], a customizable [label] and [lineSeparator].
 */
fun <T : CharSequence> List<T>.toHereDoc(
    label: String = "HERE-" + String.random(8, allowedCharacters = String.random.alphanumericCapitalCharacters).toUpperCase(),
    lineSeparator: String = "\n",
): String = listOf("<<$label").plus(this).plus(label).joinToString(lineSeparator)

/**
 * Creates a [here document](https://en.wikipedia.org/wiki/Here_document) consisting of the given [lines], a customizable [label] and [lineSeparator].
 */
fun <T : CharSequence> Iterable<T>.toHereDoc(
    label: String = "HERE-" + String.random(8, allowedCharacters = String.random.alphanumericCapitalCharacters).toUpperCase(),
    lineSeparator: String = "\n",
) = toList().run { toHereDoc(label, lineSeparator) }

/**
 * Creates a [here document](https://en.wikipedia.org/wiki/Here_document) consisting of the given [lines], a customizable [label] and [lineSeparator].
 */
fun <T : CharSequence> Array<T>.toHereDoc(
    label: String = "HERE-" + String.random(8, allowedCharacters = String.random.alphanumericCapitalCharacters).toUpperCase(),
    lineSeparator: String = "\n",
) = toList().run { toHereDoc(label, lineSeparator) }

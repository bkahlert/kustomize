package com.bkahlert.koodies.string

import com.bkahlert.koodies.regex.RegexBuilder
import kotlin.random.Random

class Random {
    val alphanumericCharacters = RegexBuilder.alphanumericCharacters
    val alphanumericCapitalCharacters = RegexBuilder.alphanumericCapitalCharacters

    operator fun invoke(
        length: Int = 16,
        allowedCharacters: CharArray = alphanumericCharacters,
    ): String =
        (1..length).map { allowedCharacters[Random.nextInt(0, allowedCharacters.size)] }
            .joinToString("")

    fun cryptSalt() = this(2, alphanumericCharacters)
}

val String.Companion.random by lazy { com.bkahlert.koodies.string.Random() }

package com.bkahlert.koodies.string

import com.bkahlert.koodies.regex.RegexBuilder
import kotlin.random.Random

object Random {
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

val String.Companion.random by lazy { com.bkahlert.koodies.string.Random }

val CodePoint.Companion.random
    get() : CodePoint {
        var possibleCodePoint = Random.nextInt(Character.MIN_CODE_POINT, Character.MAX_CODE_POINT)
        while (!possibleCodePoint.isValidCodePoint()) possibleCodePoint =
            Random.nextInt(Character.MIN_CODE_POINT, Character.MAX_CODE_POINT)
        return CodePoint(possibleCodePoint)
    }

val Char.Companion.random get() : String = CodePoint.random.toString()

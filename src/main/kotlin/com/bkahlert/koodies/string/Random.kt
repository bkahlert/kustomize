package com.bkahlert.koodies.string

import kotlin.random.Random

private val randomCharacterPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

fun String.Companion.random(length: Int = 16): String = (1..length).map { randomCharacterPool[Random.nextInt(0, randomCharacterPool.size)] }.joinToString("")

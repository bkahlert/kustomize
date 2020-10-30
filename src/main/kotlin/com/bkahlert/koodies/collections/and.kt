package com.bkahlert.koodies.collections

infix fun <T> T.and(second: T): List<T> = listOf(this, second)

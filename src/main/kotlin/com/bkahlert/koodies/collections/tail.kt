package com.bkahlert.koodies.collections

val <T> List<T>.tail: List<T>
    get() = drop(1)

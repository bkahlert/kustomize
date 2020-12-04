package com.bkahlert.koodies.concurrent.process

/**
 * Function that processes the [IO] of a [Process].
 */
typealias Processor = Process.(IO) -> Unit

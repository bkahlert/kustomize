package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.IN
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT

/**
 * What is left of a completed [Process].
 *
 * There are three ways to access the [IO] of a completed process:
 * a) use this instance's properties [exitCode], [all], [meta]. [input], [output] and [error]
 * b) use this instance like a char sequence comprising all logged output
 * c) use a destructuring declaration
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class CompletedProcess(
    /**
     * Exit code of the completed [Process].
     */
    val exitCode: Int,
    /**
     * All [output] of the completed [Process].
     */
    val all: List<IO>,
    /**
     * All [output] of type [META] of the completed [Process].
     */
    val meta: IO,
    /**
     * All [output] of type [IN] of the completed [Process].
     */
    val input: IO,
    /**
     * All [output] of type [OUT] of the completed [Process].
     */
    val output: IO,
    /**
     * All [output] of type [ERR] of the completed [Process].
     */
    val error: IO,
) : CharSequence by all.joinToString("\n") {
    constructor(exitCode: Int, io: List<IO>) : this(
        exitCode,
        io,
        META typed io.filter { it.type == META }.joinToString("\n") { it.unformatted },
        IN typed io.filter { it.type == IN }.joinToString("\n") { it.unformatted },
        OUT typed io.filter { it.type == OUT }.joinToString("\n") { it.unformatted },
        ERR typed io.filter { it.type == ERR }.joinToString("\n") { it.unformatted },
    )

    /**
     * Exit code of the completed [Process].
     */
    operator fun component1(): Int = exitCode

    /**
     * All [output] of the completed [Process].
     */
    operator fun component2(): List<IO> = all

    /**
     * All [output] of the completed [Process].
     */
    operator fun component3(): IO = meta

    /**
     * All [output] of type [IN] of the completed [Process].
     */
    operator fun component4(): IO = input

    /**
     * All [output] of type [OUT] of the completed [Process].
     */
    operator fun component5(): IO = output

    /**
     * All [output] of type [ERR] of the completed [Process].
     */
    operator fun component6(): IO = error
}

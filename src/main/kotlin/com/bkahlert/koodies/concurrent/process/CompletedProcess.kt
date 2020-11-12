package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.IN
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.exception.dump
import com.bkahlert.koodies.exception.persistDump
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.string.joinLinesToString
import java.nio.file.Path

/**
 * What is left of a completed [Process].
 *
 * There are three ways to access the [IO] of a completed process:
 * * use this instance's properties [pid], [exitCode], [all], [meta]. [input], [output] and [error]
 * * use this instance like a char sequence comprising all logged output,
 *   e.g. `completedProcess.contains("expected output")`.
 * * use a destructuring declaration
 *   e.g. `val (_, exitCode, _, _, _, output, _) = completedProcess`
 *
 * **No matter how the I/O is accessed only [all] contains the original ANSI escape sequences.**
 *
 * **All other means are ANSI free.**
 */
class CompletedProcess private constructor(
    /**
     * PID of the completed [Process] when it was running.
     */
    val pid: Long,
    /**
     * Exit code of the completed [Process].
     */
    val exitCode: Int,
    /**
     * All [output] of the completed [Process].
     *
     * This field contains all original ANSI escapes sequences.
     */
    val all: List<IO>,
    /**
     * All [output] of type [META] of the completed [Process].
     *
     * In contrast to [all] this field contains no ANSI escapes sequences.
     */
    val meta: IO,
    /**
     * All [output] of type [IN] of the completed [Process].
     *
     * In contrast to [all] this field contains no ANSI escapes sequences.
     */
    val input: IO,
    /**
     * All [output] of type [OUT] of the completed [Process].
     *
     * In contrast to [all] this field contains no ANSI escapes sequences.
     */
    val output: IO,
    /**
     * All [output] of type [ERR] of the completed [Process].
     *
     * In contrast to [all] this field contains no ANSI escapes sequences.
     */
    val error: IO,
) : CharSequence by all.joinToString("\n", transform = { it.unformatted }) {
    constructor(pid: Long, exitCode: Int, io: List<IO>) : this(
        pid = pid,
        exitCode = exitCode,
        all = io,
        meta = META typed io.filter { it.type == META }.joinToString("\n") { it.unformatted },
        input = IN typed io.filter { it.type == IN }.joinToString("\n") { it.unformatted },
        output = OUT typed io.filter { it.type == OUT }.joinToString("\n") { it.unformatted },
        error = ERR typed io.filter { it.type == ERR }.joinToString("\n") { it.unformatted },
    )

    /**
     * PID of the completed [Process] when it was running.
     */
    operator fun component1(): Long = pid

    /**
     * Exit code of the completed [Process].
     */
    operator fun component2(): Int = exitCode

    /**
     * All [output] of the completed [Process].
     *
     * This field contains all original ANSI escapes sequences.
     */
    operator fun component3(): List<IO> = all

    /**
     * All [output] of type [META] the completed [Process].
     *
     * In contrast to [all] this field contains no ANSI escapes sequences.
     */
    operator fun component4(): IO = meta

    /**
     * All [output] of type [IN] of the completed [Process].
     *
     * In contrast to [all] this field contains no ANSI escapes sequences.
     */
    operator fun component5(): IO = input

    /**
     * All [output] of type [OUT] of the completed [Process].
     *
     * In contrast to [all] this field contains no ANSI escapes sequences.
     */
    operator fun component6(): IO = output

    /**
     * All [output] of type [ERR] of the completed [Process].
     *
     * In contrast to [all] this field contains no ANSI escapes sequences.
     */
    operator fun component7(): IO = error

    /**
     * Saves the IO log to the specified [path].
     */
    fun saveIO(path: Path = tempFile("koodies.process.$pid.", ".log")): Map<String, Path> = persistDump(path) { all.joinLinesToString { it.formatted } }

    /**
     * Checks if the specified [requiredExitCode] was returned.
     *
     * If the [exitCode] differs from the expectation, an [IllegalStateException]
     * is thrown with [lazyMessage] as its message.
     *
     * @throws IllegalStateException if a different than the [requiredExitCode]
     *         was returned comprising the [lazyMessage].
     */
    fun checkExitCode(requiredExitCode: Int = 0, lazyMessage: CompletedProcess.() -> String = defaultLazyMessage(requiredExitCode)): CompletedProcess =
        apply { check(exitCode == requiredExitCode) { lazyMessage(this) } }

    /**
     * Returns the default lazy message used by [checkExitCode] to
     * check the [requiredExitCode].
     */
    fun defaultLazyMessage(requiredExitCode: Int): CompletedProcess.() -> String = {
        dump("An error occurred which led to exit code $exitCode although $requiredExitCode was expected") {
            all.joinLinesToString { it.formatted }
        }
    }

    override fun toString(): String = all.joinToString("\n")
}

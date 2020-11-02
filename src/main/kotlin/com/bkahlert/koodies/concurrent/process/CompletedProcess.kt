package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.IN
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.exception.toSingleLineString
import com.bkahlert.koodies.string.LineSeparators.LF
import com.bkahlert.koodies.string.joinLinesToString
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.withExtension
import com.imgcstmzr.util.writeText
import java.io.IOException
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
 */
class CompletedProcess(
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
     */
    operator fun component3(): List<IO> = all

    /**
     * All [output] of the completed [Process].
     */
    operator fun component4(): IO = meta

    /**
     * All [output] of type [IN] of the completed [Process].
     */
    operator fun component5(): IO = input

    /**
     * All [output] of type [OUT] of the completed [Process].
     */
    operator fun component6(): IO = output

    /**
     * All [output] of type [ERR] of the completed [Process].
     */
    operator fun component7(): IO = error

    /**
     * Saves the IO log to the specified [path].
     *
     * The log is written twice:
     * 1) [path] with `.log` as its extension contains the "black/white" version
     * 2) [path] with `.ansi.log` contains the version if format relevant ANSI control sequences intact
     */
    fun saveIO(path: Path = Paths.tempFile("koodies.process.$pid.", ".log")): List<Path> =
        kotlin.runCatching {
            listOf(
                path.withExtension("ansi.log").writeText(all.joinLinesToString {
                    it.formatted
                }),
                path.withExtension("log").writeText(all.joinLinesToString {
                    it.unformatted
                })
            )
        }.getOrElse {
            if (it is IOException) throw it
            throw IOException(it)
        }

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
    fun defaultLazyMessage(requiredExitCode: Int): CompletedProcess.() -> String {
        return {
            kotlin.runCatching {
                saveIO().let { logPaths ->
                    all.size.coerceAtMost(10).let { recentLineCount ->
                        "An error occurred which led to exit code $exitCode although $requiredExitCode was expected.$LF" +
                            "➜ The I/O log has been written once with and once without ANSI control sequences to:$LF" +
                            logPaths.joinLinesToString(postfix = LF) { "  - ${it.toUri()}" } +
                            "➜ The last $recentLineCount I/O lines were:$LF" +
                            all.take(recentLineCount).map { "  $it" }.joinLinesToString(postfix = LF)
                    }
                }
            }.recover {
                "An error occurred which led to exit code $exitCode although $requiredExitCode was expected.$LF" +
                    "➜ Unfortunately also the I/O log could not be stored (${it.toSingleLineString()}).$LF" +
                    "➜ Therefore the complete I/O log will be printed here:$LF" +
                    all.map { "  $it" }.joinLinesToString(postfix = LF)
            }.getOrThrow()
        }
    }

    override fun toString(): String = all.joinToString("\n")
}

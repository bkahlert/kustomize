package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.IN
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

/**
 * What is left of a completed [LoggingProcess].
 *
 * There are three ways to access the [IO] of a completed process:
 * * use this instance's properties [all], [meta]. [input], [output] and [error]
 * * use this instance like a char sequence comprising all logged output,
 *   e.g. `completedProcess.contains("expected output")`.
 * * use a destructuring declaration
 *   e.g. `val (_, _, _, output, _) = completedProcess`
 *
 * **No matter how the I/O is accessed only [all] contains the original ANSI escape sequences.**
 *
 * **All other means are ANSI free.**
 */
class LoggedProcess private constructor(
    private val process: Process,
    /**
     * All [output] of the completed [Process].
     *
     * This field contains all original ANSI escapes sequences.
     */
    val all: List<IO>,
) : CharSequence by all.joinToString("\n", transform = { it.unformatted }), Process() {
    constructor(process: LoggingProcess) : this(process = process, all = process.ioLog.logged)

    /**
     * All [output] of type [META] of the completed [Process].
     *
     * In contrast to [all] this field contains no ANSI escapes sequences.
     */
    val meta: IO by lazy { META typed all.filter { it.type == META }.joinToString("\n") { it.unformatted } }

    /**
     * All [output] of type [IN] of the completed [Process].
     *
     * In contrast to [all] this field contains no ANSI escapes sequences.
     */
    val input: IO by lazy { IN typed all.filter { it.type == IN }.joinToString("\n") { it.unformatted } }

    /**
     * All [output] of type [OUT] of the completed [Process].
     *
     * In contrast to [all] this field contains no ANSI escapes sequences.
     */
    val output: IO by lazy { OUT typed all.filter { it.type == OUT }.joinToString("\n") { it.unformatted } }

    /**
     * All [output] of type [ERR] of the completed [Process].
     *
     * In contrast to [all] this field contains no ANSI escapes sequences.
     */
    val error: IO by lazy { ERR typed all.filter { it.type == ERR }.joinToString("\n") { it.unformatted } }

    /**
     * All [output] of the completed [Process].
     *
     * This field contains all original ANSI escapes sequences.
     */
    operator fun component1(): List<IO> = all

    /**
     * All [output] of type [META] the completed [Process].
     *
     * In contrast to [all] this field contains no ANSI escapes sequences.
     */
    operator fun component2(): IO = meta

    /**
     * All [output] of type [IN] of the completed [Process].
     *
     * In contrast to [all] this field contains no ANSI escapes sequences.
     */
    operator fun component3(): IO = input

    /**
     * All [output] of type [OUT] of the completed [Process].
     *
     * In contrast to [all] this field contains no ANSI escapes sequences.
     */
    operator fun component4(): IO = output

    /**
     * All [output] of type [ERR] of the completed [Process].
     *
     * In contrast to [all] this field contains no ANSI escapes sequences.
     */
    operator fun component5(): IO = error

    override fun toString(): String = all.joinToString("\n")

    override fun getOutputStream(): OutputStream = process.outputStream
    override fun getInputStream(): InputStream = process.inputStream
    override fun getErrorStream(): InputStream = process.errorStream
    override fun waitFor(): Int = process.waitFor()
    override fun waitFor(timeout: Long, unit: TimeUnit?): Boolean = waitFor(timeout, unit)
    override fun exitValue(): Int = process.exitValue()
    override fun destroy(): Unit = process.destroy()
    override fun destroyForcibly(): Process = process.destroyForcibly()
    override fun supportsNormalTermination(): Boolean = process.supportsNormalTermination()
    override fun isAlive(): Boolean = process.isAlive
    override fun pid(): Long = process.pid()
    override fun onExit(): CompletableFuture<Process> = process.onExit()
    override fun toHandle(): ProcessHandle = process.toHandle()
    override fun info(): ProcessHandle.Info = process.info()
    override fun children(): Stream<ProcessHandle> = process.children()
    override fun descendants(): Stream<ProcessHandle> = process.descendants()
}

package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.IN
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import java.io.InputStream
import java.io.OutputStream
import java.lang.ProcessHandle.Info
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
    val exitValue: Int,
    val supportsNormalTermination: Boolean,
    val pid: Long,
    val handle: ProcessHandle,
    val info: Info,
    /**
     * All [output] of the completed [Process].
     *
     * This field contains all original ANSI escapes sequences.
     */
    val all: List<IO>,
) : CharSequence by all.joinToString("\n", transform = { it.unformatted }), Process() {
    constructor(process: LoggingProcess) : this(
        exitValue = process.exitValue(),
        supportsNormalTermination = process.supportsNormalTermination(),
        pid = process.pid(),
        handle = process.toHandle(),
        info = process.info(),
        all = process.ioLog.logged
    )

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

    override fun getOutputStream(): OutputStream = OutputStream.nullOutputStream()
    override fun getInputStream(): InputStream = InputStream.nullInputStream()
    override fun getErrorStream(): InputStream = InputStream.nullInputStream()
    override fun waitFor(): Int = exitValue
    override fun waitFor(timeout: Long, unit: TimeUnit?) = true
    override fun exitValue(): Int = exitValue
    override fun destroy() {}
    override fun destroyForcibly(): Process = this
    override fun supportsNormalTermination(): Boolean = supportsNormalTermination
    override fun isAlive(): Boolean = false
    override fun pid(): Long = pid
    override fun onExit(): CompletableFuture<Process> = CompletableFuture.completedFuture(this)
    override fun toHandle(): ProcessHandle = handle
    override fun info(): Info = info
    override fun children(): Stream<ProcessHandle> = Stream.empty()
    override fun descendants(): Stream<ProcessHandle> = Stream.empty()
}

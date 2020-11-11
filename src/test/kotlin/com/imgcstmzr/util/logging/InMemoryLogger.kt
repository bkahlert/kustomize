package com.imgcstmzr.util.logging

import com.bkahlert.koodies.collections.withNegativeIndices
import com.bkahlert.koodies.string.LineSeparators.withoutTrailingLineSeparator
import com.bkahlert.koodies.string.TruncationStrategy.MIDDLE
import com.bkahlert.koodies.string.asString
import com.bkahlert.koodies.string.padStartFixedLength
import com.bkahlert.koodies.string.prefixLinesWith
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.time.Now
import com.bkahlert.koodies.tracing.MacroTracer
import com.bkahlert.koodies.tracing.Tracer
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import org.apache.commons.io.output.TeeOutputStream
import java.io.OutputStream
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3

open class InMemoryLogger<T> private constructor(
    caption: CharSequence,
    borderedOutput: Boolean = false,
    statusInformationColumn: Int = -1,
    private val outputStream: TeeOutputStream,
    private val captured: MutableList<CharSequence> = mutableListOf(),
    private val start: Long = System.currentTimeMillis(),
) : BlockRenderingLogger<T>(
    caption = caption,
    borderedOutput = borderedOutput,
    statusInformationColumn = if (statusInformationColumn > 0) statusInformationColumn else 60,
    log = { message: CharSequence ->
        val time = Thread.currentThread().name.padStartFixedLength(30, strategy = MIDDLE).asString() + ":" + " ${Now.passedSince(start)}".padStartFixedLength(7)
        outputStream.write(message.prefixLinesWith(prefix = "$time: ").toByteArray())
        captured.add(message.withoutTrailingLineSeparator)
    }
), Tracer<T> {
    constructor(
        caption: String,
        borderedOutput: Boolean = true,
        statusInformationColumn: Int = -1,
        outputStreams: List<OutputStream>,
    ) : this(
        caption = caption,
        borderedOutput = borderedOutput,
        statusInformationColumn = statusInformationColumn,
        outputStream = outputStreams.foldRight(TeeOutputStream(OutputStream.nullOutputStream(), OutputStream.nullOutputStream()),
            { os, tos -> TeeOutputStream(os, tos) })
    )

    constructor() : this("Test", true, -1, emptyList())

    val messages: List<CharSequence> by withNegativeIndices { captured }
    val raw: String get() = messages.joinToString("\n")
    val logged: String get() = messages.joinToString("\n").removeEscapeSequences().withoutTrailingLineSeparator.trim()

    /**
     * Returns the so far rendered content—pretending this block was finished with [result].
     */
    fun finalizedDump(result: Result<T>): String = raw + "\n" + getBlockEnd(result)

    @Suppress("UNCHECKED_CAST")
    override fun toString(): String = finalizedDump(Result.success(Unit) as Result<Nothing>)

    override fun trace(input: String) {
        logLine { input }
    }

    override fun <MR> macroTrace(f: String, block: MacroTracer<MR>.() -> MR): MR {
        TODO()
    }

    override fun <MR> macroTrace(f: KCallable<MR>, block: MacroTracer<MR>.() -> MR): MR {
        TODO()
    }

    override fun <MR> macroTrace(f: KFunction0<MR>, block: MacroTracer<MR>.() -> MR): MR {
        TODO()
    }

    override fun <MR> macroTrace1(f: KFunction1<*, MR>, block: MacroTracer<MR>.() -> MR): MR {
        TODO()
    }

    override fun <MR> macroTrace2(f: KFunction2<*, *, MR>, block: MacroTracer<MR>.() -> MR): MR {
        TODO()
    }

    override fun <MR> macroTrace3(f: KFunction3<*, *, *, MR>, block: MacroTracer<MR>.() -> MR): MR {
        TODO()
    }
}

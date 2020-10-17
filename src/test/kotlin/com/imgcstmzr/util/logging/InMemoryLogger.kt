package com.imgcstmzr.util.logging

import com.bkahlert.koodies.collections.withNegativeIndices
import com.bkahlert.koodies.string.LineSeparators.withoutTrailingLineSeparator
import com.bkahlert.koodies.string.TruncationStrategy.MIDDLE
import com.bkahlert.koodies.string.padStartFixedLength
import com.bkahlert.koodies.string.prefixLinesWith
import com.bkahlert.koodies.terminal.removeEscapeSequences
import com.bkahlert.koodies.tracing.MacroTracer
import com.bkahlert.koodies.tracing.Tracer
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.util.Now
import org.apache.commons.io.output.TeeOutputStream
import java.io.OutputStream
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3

class InMemoryLogger<T> private constructor(
    caption: String,
    borderedOutput: Boolean = false,
    private val outputStream: TeeOutputStream,
    private val captured: MutableList<CharSequence> = mutableListOf(),
    private val start: Long = System.currentTimeMillis(),
) : BlockRenderingLogger<T>(
    caption = caption,
    borderedOutput = borderedOutput,
    log = { message: String ->
        val time = Thread.currentThread().name.padStartFixedLength(30, strategy = MIDDLE) + ":" + " ${Now.passedSince(start)}".padStartFixedLength(7)
        outputStream.write(message.prefixLinesWith("$time: ").toByteArray())
        captured.add(message.withoutTrailingLineSeparator)
    }
), Tracer<T> {
    constructor(caption: String, borderedOutput: Boolean = true, outputStreams: List<OutputStream>) : this(
        caption,
        borderedOutput,
        outputStreams.foldRight(TeeOutputStream(OutputStream.nullOutputStream(), OutputStream.nullOutputStream()), { os, tos -> TeeOutputStream(os, tos) })
    )

    constructor() : this("Test", true, emptyList())

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
        log(input, true)
    }

    override fun <MR> Tracer<T>.macroTrace(f: String, block: MacroTracer<MR>.() -> MR): MR {
        TODO()
    }

    override fun <MR> Tracer<T>.macroTrace(f: KCallable<MR>, block: MacroTracer<MR>.() -> MR): MR {
        TODO()
    }

    override fun <MR> Tracer<T>.macroTrace(f: KFunction0<MR>, block: MacroTracer<MR>.() -> MR): MR {
        TODO()
    }

    override fun <MR> Tracer<T>.macroTrace1(f: KFunction1<*, MR>, block: MacroTracer<MR>.() -> MR): MR {
        TODO()
    }

    override fun <MR> Tracer<T>.macroTrace2(f: KFunction2<*, *, MR>, block: MacroTracer<MR>.() -> MR): MR {
        TODO()
    }

    override fun <MR> Tracer<T>.macroTrace3(f: KFunction3<*, *, *, MR>, block: MacroTracer<MR>.() -> MR): MR {
        TODO()
    }
}

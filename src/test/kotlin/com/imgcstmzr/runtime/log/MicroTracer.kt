package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.string.Grapheme
import com.bkahlert.koodies.tracing.MicroTracer
import com.bkahlert.koodies.tracing.MiniTracer
import com.imgcstmzr.process.Output.Type.OUT


fun <R> SingleLineLogger<R>.microSequence(symbol: Grapheme, block: MicroTracer<R>.() -> R): R {
    val traces = mutableListOf<String>()
    val r: R = (object : MicroTracer<R> {
        override fun trace(input: String) {
            traces.add(input)
        }
    }).let(block)
    logLine(OUT typed traces.joinToString(prefix = "($symbol ", separator = " ˃ ", postfix = ")"))
    return r
}

fun <R> MiniTracer<R>.microTrace(symbol: Grapheme, block: MicroTracer<R>.() -> R): R {
    val traces = mutableListOf<String>()
    val r: R = (object : MicroTracer<R> {
        override fun trace(input: String) {
            traces.add(input)
        }
    }).let(block)
    trace(traces.joinToString(prefix = "($symbol ", separator = " ˃ ", postfix = ")"))
    return r
}

@Suppress("NonAsciiCharacters") fun <mR, μR> MiniTracer<mR>.microTraceX(symbol: Grapheme, block: MicroTracer<μR>.() -> μR): μR {
    val traces = mutableListOf<String>()
    val r: μR = (object : MicroTracer<μR> {
        override fun trace(input: String) {
            traces.add(input)
        }
    }).let(block)
    trace(traces.joinToString(prefix = "($symbol ", separator = " ˃ ", postfix = ")"))
    return r
}

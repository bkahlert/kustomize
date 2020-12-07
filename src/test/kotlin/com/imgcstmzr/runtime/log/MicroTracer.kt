package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.string.Grapheme
import com.bkahlert.koodies.tracing.MicroTracer
import com.bkahlert.koodies.tracing.MiniTracer

class SimpleMicroTracer(private val symbol: Grapheme) : MicroTracer {
    private val traces = mutableListOf<String>()
    override fun trace(input: String) {
        traces.add(input)
    }

    fun render(): String = traces.joinToString(prefix = "($symbol ", separator = " ˃ ", postfix = ")")
}

fun <R> MiniTracer?.microTrace(symbol: Grapheme, block: MicroTracer?.() -> R): R {
    val simpleMicroTracer = SimpleMicroTracer(symbol)
    val returnValue: R = simpleMicroTracer.run(block)
    this?.trace(simpleMicroTracer.render())
    return returnValue
}

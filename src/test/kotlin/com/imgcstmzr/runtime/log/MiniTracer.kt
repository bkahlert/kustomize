package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.string.Grapheme
import com.bkahlert.koodies.tracing.MicroTracer
import com.bkahlert.koodies.tracing.MiniTracer
import com.imgcstmzr.util.format
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3

@Suppress("NonAsciiCharacters")
inline fun <reified MR, reified mR> BlockRenderingLogger<MR>?._segment(
    f: String,
    crossinline block: MiniTracer<mR>.() -> mR,
): mR = miniSegment(f.format()) {
    object : MiniTracer<mR> {
        override fun trace(input: String) {
            if (strings != null) {
                strings = strings?.plus(input.lines().joinToString(", "))
            }
        }

        override fun <μR> MiniTracer<mR>.microTrace(grapheme: Grapheme, block: MicroTracer<μR>.() -> μR): μR =
            microTraceX(grapheme, block)

        override fun <μR> MiniTracer<mR>.microTrace(f: String, block: MicroTracer<μR>.() -> μR): μR =
            microTrace(Grapheme("𝙛"), block)

        override fun <μR> MiniTracer<mR>.microTrace(f: KCallable<μR>, block: MicroTracer<μR>.() -> μR): μR =
            microTrace(Grapheme("𝙛"), block)

        override fun <μR> MiniTracer<mR>.microTrace(f: KFunction0<μR>, block: MicroTracer<μR>.() -> μR): μR =
            microTrace(Grapheme("𝙛"), block)

        override fun <μR> MiniTracer<mR>.microTrace1(f: KFunction1<*, μR>, block: MicroTracer<μR>.() -> μR): μR =
            microTrace(Grapheme("𝙛"), block)

        override fun <μR> MiniTracer<mR>.microTrace2(f: KFunction2<*, *, μR>, block: MicroTracer<μR>.() -> μR): μR =
            microTrace(Grapheme("𝙛"), block)

        override fun <μR> MiniTracer<mR>.microTrace3(f: KFunction3<*, *, *, μR>, block: MicroTracer<μR>.() -> μR): μR =
            microTrace(Grapheme("𝙛"), block)
    }.let(block)
}

@Suppress("NonAsciiCharacters")
inline fun <reified mR, reified μR> BlockRenderingLogger<mR>?.miniTrace(f: String, crossinline block: MiniTracer<μR>.() -> μR): μR =
    _segment(f.format(), block)

@Suppress("NonAsciiCharacters")
inline fun <reified mR, reified μR> BlockRenderingLogger<mR>?.miniTrace(f: KCallable<μR>, crossinline block: MiniTracer<μR>.() -> μR): μR =
    _segment(f.format(), block)

@Suppress("NonAsciiCharacters")
inline fun <reified mR, reified μR> BlockRenderingLogger<mR>?.miniTrace(f: KFunction0<μR>, crossinline block: MiniTracer<μR>.() -> μR): μR =
    _segment(f.format(), block)

@Suppress("NonAsciiCharacters")
inline fun <reified mR, reified μR> BlockRenderingLogger<mR>?.miniTrace1(f: KFunction1<*, μR>, crossinline block: MiniTracer<μR>.() -> μR): μR =
    _segment(f.format(), block)

@Suppress("NonAsciiCharacters")
inline fun <reified mR, reified μR> BlockRenderingLogger<mR>?.miniTrace2(f: KFunction2<*, *, μR>, crossinline block: MiniTracer<μR>.() -> μR): μR =
    _segment(f.format(), block)

@Suppress("NonAsciiCharacters")
inline fun <reified mR, reified μR> BlockRenderingLogger<mR>?.miniTrace3(
    f: KFunction3<*, *, *, μR>,
    crossinline block: MiniTracer<μR>.() -> μR,
): μR = _segment(f.format(), block)

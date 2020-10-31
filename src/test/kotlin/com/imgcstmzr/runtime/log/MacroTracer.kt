package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.tracing.MacroTracer
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
    crossinline block: MacroTracer<mR>.() -> mR,
): mR = segment(f, null, false, null) {
    object : MacroTracer<mR> {
        override fun trace(input: String) {
            this@segment.logStatus { META typed input }
        }

        override fun <μR> MacroTracer<mR>.miniTrace(f: String, block: MiniTracer<μR>.() -> μR): μR =
            miniTrace(f, block)

        override fun <μR> MacroTracer<mR>.miniTrace(f: KCallable<μR>, block: MiniTracer<μR>.() -> μR): μR =
            miniTrace(f.format(), block)

        override fun <μR> MacroTracer<mR>.miniTrace(f: KFunction0<μR>, block: MiniTracer<μR>.() -> μR): μR =
            miniTrace(f.format(), block)

        override fun <μR> MacroTracer<mR>.miniTrace1(f: KFunction1<*, μR>, block: MiniTracer<μR>.() -> μR): μR =
            miniTrace(f.format(), block)

        override fun <μR> MacroTracer<mR>.miniTrace2(f: KFunction2<*, *, μR>, block: MiniTracer<μR>.() -> μR): μR =
            miniTrace(f.format(), block)

        override fun <μR> MacroTracer<mR>.miniTrace3(f: KFunction3<*, *, *, μR>, block: MiniTracer<μR>.() -> μR): μR =
            miniTrace(f.format(), block)
    }.let(block)
}

inline fun <reified MR, reified mR> BlockRenderingLogger<MR>?.macroTrace(f: String, crossinline block: MacroTracer<mR>.() -> mR): mR =
    _segment(f.format(), block)

inline fun <reified MR, reified mR> BlockRenderingLogger<MR>?.macroTrace(f: KCallable<mR>, crossinline block: MacroTracer<mR>.() -> mR): mR =
    _segment(f.format(), block)

inline fun <reified MR, reified mR> BlockRenderingLogger<MR>?.macroTrace(f: KFunction0<mR>, crossinline block: MacroTracer<mR>.() -> mR): mR =
    _segment(f.format(), block)

inline fun <reified MR, reified mR> BlockRenderingLogger<MR>?.macroTrace1(f: KFunction1<*, mR>, crossinline block: MacroTracer<mR>.() -> mR): mR =
    _segment(f.format(), block)

inline fun <reified MR, reified mR> BlockRenderingLogger<MR>?.macroTrace2(f: KFunction2<*, *, mR>, crossinline block: MacroTracer<mR>.() -> mR): mR =
    _segment(f.format(), block)

inline fun <reified MR, reified mR> BlockRenderingLogger<MR>?.macroTrace3(
    f: KFunction3<*, *, *, mR>,
    crossinline block: MacroTracer<mR>.() -> mR,
): mR =
    _segment(f.format(), block)

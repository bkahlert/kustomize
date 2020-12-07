@file:Suppress("NonAsciiCharacters")

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

class RenderingLoggerBasedMacroTracer(private val logger: RenderingLogger) : MacroTracer {
    override fun trace(input: String) = logger.logStatus { META typed input }
    override fun <μR> miniTrace(f: String, block: MiniTracer.() -> μR): μR = miniTrace(f, block)
    override fun <μR> miniTrace(f: KCallable<μR>, block: MiniTracer.() -> μR): μR = miniTrace(f.format(), block)
    override fun <μR> miniTrace(f: KFunction0<μR>, block: MiniTracer.() -> μR): μR = miniTrace(f.format(), block)
    override fun <μR> miniTrace1(f: KFunction1<*, μR>, block: MiniTracer.() -> μR): μR = miniTrace(f.format(), block)
    override fun <μR> miniTrace2(f: KFunction2<*, *, μR>, block: MiniTracer.() -> μR): μR = miniTrace(f.format(), block)
    override fun <μR> miniTrace3(f: KFunction3<*, *, *, μR>, block: MiniTracer.() -> μR): μR = miniTrace(f.format(), block)
}

inline fun <reified mR> RenderingLogger?._segment(f: String, crossinline block: MacroTracer.() -> mR): mR =
    subLogger(f, null, false) { RenderingLoggerBasedMacroTracer(this).run(block) }

inline fun <reified mR> RenderingLogger?.macroTrace(f: String, crossinline block: MacroTracer?.() -> mR): mR = _segment(f.format(), block)
inline fun <reified mR> RenderingLogger?.macroTrace(f: KCallable<mR>, crossinline block: MacroTracer?.() -> mR): mR = _segment(f.format(), block)
inline fun <reified mR> RenderingLogger?.macroTrace(f: KFunction0<mR>, crossinline block: MacroTracer?.() -> mR): mR = _segment(f.format(), block)
inline fun <reified mR> RenderingLogger?.macroTrace1(f: KFunction1<*, mR>, crossinline block: MacroTracer?.() -> mR): mR = _segment(f.format(), block)
inline fun <reified mR> RenderingLogger?.macroTrace2(f: KFunction2<*, *, mR>, crossinline block: MacroTracer?.() -> mR): mR = _segment(f.format(), block)
inline fun <reified mR> RenderingLogger?.macroTrace3(f: KFunction3<*, *, *, mR>, crossinline block: MacroTracer?.() -> mR): mR =
    _segment(f.format(), block)

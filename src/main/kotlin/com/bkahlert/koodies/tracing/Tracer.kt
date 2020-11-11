@file:Suppress("NonAsciiCharacters")

package com.bkahlert.koodies.tracing

import com.bkahlert.koodies.string.Grapheme
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3

interface Tracer<R> {
    fun trace(input: String)
    fun <MR> macroTrace(f: String, block: MacroTracer<MR>.() -> MR): MR
    fun <MR> macroTrace(f: KCallable<MR>, block: MacroTracer<MR>.() -> MR): MR
    fun <MR> macroTrace(f: KFunction0<MR>, block: MacroTracer<MR>.() -> MR): MR
    fun <MR> macroTrace1(f: KFunction1<*, MR>, block: MacroTracer<MR>.() -> MR): MR
    fun <MR> macroTrace2(f: KFunction2<*, *, MR>, block: MacroTracer<MR>.() -> MR): MR
    fun <MR> macroTrace3(f: KFunction3<*, *, *, MR>, block: MacroTracer<MR>.() -> MR): MR
}

fun Tracer<*>?.trace(input: String) = this?.trace(input)
fun <mR> Tracer<*>?.macroTrace(f: String, block: MacroTracer<mR>?.() -> mR) = this@macroTrace?.macroTrace(f, block)
fun <mR> Tracer<*>?.macroTrace(f: KCallable<mR>, block: MacroTracer<mR>?.() -> mR) = this@macroTrace?.macroTrace(f, block)
fun <mR> Tracer<*>?.macroTrace(f: KFunction0<mR>, block: MacroTracer<mR>?.() -> mR) = this@macroTrace?.macroTrace(f, block)
fun <mR> Tracer<*>?.macroTrace1(f: KFunction1<*, mR>, block: MacroTracer<mR>?.() -> mR) = this@macroTrace1?.macroTrace1(f, block)
fun <mR> Tracer<*>?.macroTrace2(f: KFunction2<*, *, mR>, block: MacroTracer<mR>?.() -> mR) = this@macroTrace2?.macroTrace2(f, block)
fun <mR> Tracer<*>?.macroTrace3(f: KFunction3<*, *, *, mR>, block: MacroTracer<mR>?.() -> mR) = this@macroTrace3?.macroTrace3(f, block)


interface MacroTracer<MR> {
    fun trace(input: String)
    fun <mR> miniTrace(f: String, block: MiniTracer<mR>.() -> mR): mR
    fun <mR> miniTrace(f: KCallable<mR>, block: MiniTracer<mR>.() -> mR): mR
    fun <mR> miniTrace(f: KFunction0<mR>, block: MiniTracer<mR>.() -> mR): mR
    fun <mR> miniTrace1(f: KFunction1<*, mR>, block: MiniTracer<mR>.() -> mR): mR
    fun <mR> miniTrace2(f: KFunction2<*, *, mR>, block: MiniTracer<mR>.() -> mR): mR
    fun <mR> miniTrace3(f: KFunction3<*, *, *, mR>, block: MiniTracer<mR>.() -> mR): mR
}

fun MacroTracer<*>?.trace(input: String) = this?.trace(input)
fun <mR> MacroTracer<*>?.miniTrace(f: String, block: MiniTracer<mR>?.() -> mR) = this@miniTrace?.miniTrace(f, block)
fun <mR> MacroTracer<*>?.miniTrace(f: KCallable<mR>, block: MiniTracer<mR>?.() -> mR) = this@miniTrace?.miniTrace(f, block)
fun <mR> MacroTracer<*>?.miniTrace(f: KFunction0<mR>, block: MiniTracer<mR>?.() -> mR) = this@miniTrace?.miniTrace(f, block)
fun <mR> MacroTracer<*>?.miniTrace1(f: KFunction1<*, mR>, block: MiniTracer<mR>?.() -> mR) = this@miniTrace1?.miniTrace1(f, block)
fun <mR> MacroTracer<*>?.miniTrace2(f: KFunction2<*, *, mR>, block: MiniTracer<mR>?.() -> mR) = this@miniTrace2?.miniTrace2(f, block)
fun <mR> MacroTracer<*>?.miniTrace3(f: KFunction3<*, *, *, mR>, block: MiniTracer<mR>?.() -> mR) = this@miniTrace3?.miniTrace3(f, block)


@Suppress("NonAsciiCharacters")
interface MiniTracer<mR> {
    fun trace(input: String)
    fun <μR> microTrace(grapheme: Grapheme, block: MicroTracer<μR>.() -> μR): μR
    fun <μR> microTrace(f: String, block: MicroTracer<μR>.() -> μR): μR
    fun <μR> microTrace(f: KCallable<μR>, block: MicroTracer<μR>.() -> μR): μR
    fun <μR> microTrace(f: KFunction0<μR>, block: MicroTracer<μR>.() -> μR): μR
    fun <μR> microTrace1(f: KFunction1<*, μR>, block: MicroTracer<μR>.() -> μR): μR
    fun <μR> microTrace2(f: KFunction2<*, *, μR>, block: MicroTracer<μR>.() -> μR): μR
    fun <μR> microTrace3(f: KFunction3<*, *, *, μR>, block: MicroTracer<μR>.() -> μR): μR
}

fun MiniTracer<*>?.trace(input: String) = this?.trace(input)
fun <μR> MiniTracer<*>?.microTrace(grapheme: Grapheme, block: MicroTracer<μR>?.() -> μR) = this@microTrace?.microTrace(grapheme, block)
fun <μR> MiniTracer<*>?.microTrace(f: String, block: MicroTracer<μR>?.() -> μR) = this@microTrace?.microTrace(f, block)
fun <μR> MiniTracer<*>?.microTrace(f: KCallable<μR>, block: MicroTracer<μR>?.() -> μR) = this@microTrace?.microTrace(f, block)
fun <μR> MiniTracer<*>?.microTrace(f: KFunction0<μR>, block: MicroTracer<μR>?.() -> μR) = this@microTrace?.microTrace(f, block)
fun <μR> MiniTracer<*>?.microTrace1(f: KFunction1<*, μR>, block: MicroTracer<μR>?.() -> μR) = this@microTrace1?.microTrace1(f, block)
fun <μR> MiniTracer<*>?.microTrace2(f: KFunction2<*, *, μR>, block: MicroTracer<μR>?.() -> μR) = this@microTrace2?.microTrace2(f, block)
fun <μR> MiniTracer<*>?.microTrace3(f: KFunction3<*, *, *, μR>, block: MicroTracer<μR>?.() -> μR) = this@microTrace3?.microTrace3(f, block)


interface MicroTracer<μR> {
    fun trace(input: String)
}

fun MicroTracer<*>?.trace(input: String) = this?.trace(input)

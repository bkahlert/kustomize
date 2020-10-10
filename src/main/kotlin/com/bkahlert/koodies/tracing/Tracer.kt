package com.bkahlert.koodies.tracing

import com.bkahlert.koodies.string.Grapheme
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3


interface Tracer<R> {
    fun trace(input: String)
    fun <MR> Tracer<R>.macroTrace(f: String, block: MacroTracer<MR>.() -> MR): MR
    fun <MR> Tracer<R>.macroTrace(f: KCallable<MR>, block: MacroTracer<MR>.() -> MR): MR
    fun <MR> Tracer<R>.macroTrace(f: KFunction0<MR>, block: MacroTracer<MR>.() -> MR): MR
    fun <MR> Tracer<R>.macroTrace1(f: KFunction1<*, MR>, block: MacroTracer<MR>.() -> MR): MR
    fun <MR> Tracer<R>.macroTrace2(f: KFunction2<*, *, MR>, block: MacroTracer<MR>.() -> MR): MR
    fun <MR> Tracer<R>.macroTrace3(f: KFunction3<*, *, *, MR>, block: MacroTracer<MR>.() -> MR): MR

    companion object {
        fun <R> nullTracer() = object : Tracer<R> {
            override fun trace(input: String) {}
            override fun <MR> Tracer<R>.macroTrace(f: String, block: MacroTracer<MR>.() -> MR): MR =
                MacroTracer.nullMacroTracer<MR>().block()

            override fun <MR> Tracer<R>.macroTrace(f: KCallable<MR>, block: MacroTracer<MR>.() -> MR): MR =
                MacroTracer.nullMacroTracer<MR>().block()

            override fun <MR> Tracer<R>.macroTrace(f: KFunction0<MR>, block: MacroTracer<MR>.() -> MR): MR =
                MacroTracer.nullMacroTracer<MR>().block()

            override fun <MR> Tracer<R>.macroTrace1(f: KFunction1<*, MR>, block: MacroTracer<MR>.() -> MR): MR =
                MacroTracer.nullMacroTracer<MR>().block()

            override fun <MR> Tracer<R>.macroTrace2(f: KFunction2<*, *, MR>, block: MacroTracer<MR>.() -> MR): MR =
                MacroTracer.nullMacroTracer<MR>().block()

            override fun <MR> Tracer<R>.macroTrace3(f: KFunction3<*, *, *, MR>, block: MacroTracer<MR>.() -> MR): MR =
                MacroTracer.nullMacroTracer<MR>().block()
        }
    }
}

fun <R> Tracer<R>?.trace(input: String) = this?.trace(input)

fun <MR, mR> Tracer<MR>?.macroTrace(f: String, block: MacroTracer<mR>.() -> mR) =
    if (this != null) this@macroTrace.macroTrace(f, block) else MacroTracer.nullMacroTracer<mR>().block()

fun <MR, mR> Tracer<MR>?.macroTrace(f: KCallable<mR>, block: MacroTracer<mR>.() -> mR) =
    if (this != null) this@macroTrace.macroTrace(f, block) else MacroTracer.nullMacroTracer<mR>().block()

fun <MR, mR> Tracer<MR>?.macroTrace(f: KFunction0<mR>, block: MacroTracer<mR>.() -> mR) =
    if (this != null) this@macroTrace.macroTrace(f, block) else MacroTracer.nullMacroTracer<mR>().block()

fun <MR, mR> Tracer<MR>?.macroTrace1(f: KFunction1<*, mR>, block: MacroTracer<mR>.() -> mR) =
    if (this != null) this@macroTrace1.macroTrace1(f, block) else MacroTracer.nullMacroTracer<mR>().block()

fun <MR, mR> Tracer<MR>?.macroTrace2(f: KFunction2<*, *, mR>, block: MacroTracer<mR>.() -> mR) =
    if (this != null) this@macroTrace2.macroTrace2(f, block) else MacroTracer.nullMacroTracer<mR>().block()

fun <MR, mR> Tracer<MR>?.macroTrace3(f: KFunction3<*, *, *, mR>, block: MacroTracer<mR>.() -> mR) =
    if (this != null) this@macroTrace3.macroTrace3(f, block) else MacroTracer.nullMacroTracer<mR>().block()


interface MacroTracer<MR> {
    fun trace(input: String)
    fun <mR> MacroTracer<MR>.miniTrace(f: String, block: MiniTracer<mR>.() -> mR): mR
    fun <mR> MacroTracer<MR>.miniTrace(f: KCallable<mR>, block: MiniTracer<mR>.() -> mR): mR
    fun <mR> MacroTracer<MR>.miniTrace(f: KFunction0<mR>, block: MiniTracer<mR>.() -> mR): mR
    fun <mR> MacroTracer<MR>.miniTrace1(f: KFunction1<*, mR>, block: MiniTracer<mR>.() -> mR): mR
    fun <mR> MacroTracer<MR>.miniTrace2(f: KFunction2<*, *, mR>, block: MiniTracer<mR>.() -> mR): mR
    fun <mR> MacroTracer<MR>.miniTrace3(f: KFunction3<*, *, *, mR>, block: MiniTracer<mR>.() -> mR): mR

    companion object {
        fun <MR> nullMacroTracer() = object : MacroTracer<MR> {
            override fun trace(input: String) {}
            override fun <mR> MacroTracer<MR>.miniTrace(f: String, block: MiniTracer<mR>.() -> mR): mR =
                MiniTracer.nullMiniTracer<mR>().block()

            override fun <mR> MacroTracer<MR>.miniTrace(f: KCallable<mR>, block: MiniTracer<mR>.() -> mR): mR =
                MiniTracer.nullMiniTracer<mR>().block()

            override fun <mR> MacroTracer<MR>.miniTrace(f: KFunction0<mR>, block: MiniTracer<mR>.() -> mR): mR =
                MiniTracer.nullMiniTracer<mR>().block()

            override fun <mR> MacroTracer<MR>.miniTrace1(f: KFunction1<*, mR>, block: MiniTracer<mR>.() -> mR): mR =
                MiniTracer.nullMiniTracer<mR>().block()

            override fun <mR> MacroTracer<MR>.miniTrace2(f: KFunction2<*, *, mR>, block: MiniTracer<mR>.() -> mR): mR =
                MiniTracer.nullMiniTracer<mR>().block()

            override fun <mR> MacroTracer<MR>.miniTrace3(f: KFunction3<*, *, *, mR>, block: MiniTracer<mR>.() -> mR): mR =
                MiniTracer.nullMiniTracer<mR>().block()
        }
    }
}

fun <MR, mR> MacroTracer<MR>?.miniTrace(f: String, block: MiniTracer<mR>.() -> mR) =
    if (this != null) this@miniTrace.miniTrace(f, block) else MiniTracer.nullMiniTracer<mR>().block()

fun <MR, mR> MacroTracer<MR>?.miniTrace(f: KCallable<mR>, block: MiniTracer<mR>.() -> mR) =
    if (this != null) this@miniTrace.miniTrace(f, block) else MiniTracer.nullMiniTracer<mR>().block()

fun <MR, mR> MacroTracer<MR>?.miniTrace(f: KFunction0<mR>, block: MiniTracer<mR>.() -> mR) =
    if (this != null) this@miniTrace.miniTrace(f, block) else MiniTracer.nullMiniTracer<mR>().block()

fun <MR, mR> MacroTracer<MR>?.miniTrace1(f: KFunction1<*, mR>, block: MiniTracer<mR>.() -> mR) =
    if (this != null) this@miniTrace1.miniTrace1(f, block) else MiniTracer.nullMiniTracer<mR>().block()

fun <MR, mR> MacroTracer<MR>?.miniTrace2(f: KFunction2<*, *, mR>, block: MiniTracer<mR>.() -> mR) =
    if (this != null) this@miniTrace2.miniTrace2(f, block) else MiniTracer.nullMiniTracer<mR>().block()

fun <MR, mR> MacroTracer<MR>?.miniTrace3(f: KFunction3<*, *, *, mR>, block: MiniTracer<mR>.() -> mR) =
    if (this != null) this@miniTrace3.miniTrace3(f, block) else MiniTracer.nullMiniTracer<mR>().block()


@Suppress("NonAsciiCharacters")
interface MiniTracer<mR> {
    fun trace(input: String)
    fun <μR> MiniTracer<mR>.microTrace(grapheme: Grapheme, block: MicroTracer<μR>.() -> μR): μR
    fun <μR> MiniTracer<mR>.microTrace(f: String, block: MicroTracer<μR>.() -> μR): μR
    fun <μR> MiniTracer<mR>.microTrace(f: KCallable<μR>, block: MicroTracer<μR>.() -> μR): μR
    fun <μR> MiniTracer<mR>.microTrace(f: KFunction0<μR>, block: MicroTracer<μR>.() -> μR): μR
    fun <μR> MiniTracer<mR>.microTrace1(f: KFunction1<*, μR>, block: MicroTracer<μR>.() -> μR): μR
    fun <μR> MiniTracer<mR>.microTrace2(f: KFunction2<*, *, μR>, block: MicroTracer<μR>.() -> μR): μR
    fun <μR> MiniTracer<mR>.microTrace3(f: KFunction3<*, *, *, μR>, block: MicroTracer<μR>.() -> μR): μR

    companion object {
        fun <mR> nullMiniTracer() = object : MiniTracer<mR> {
            override fun trace(input: String) {}
            override fun <μR> MiniTracer<mR>.microTrace(grapheme: Grapheme, block: MicroTracer<μR>.() -> μR): μR =
                MicroTracer.nullMicroTracer<μR>().block()

            override fun <μR> MiniTracer<mR>.microTrace(f: String, block: MicroTracer<μR>.() -> μR): μR =
                MicroTracer.nullMicroTracer<μR>().block()

            override fun <μR> MiniTracer<mR>.microTrace(f: KCallable<μR>, block: MicroTracer<μR>.() -> μR): μR =
                MicroTracer.nullMicroTracer<μR>().block()

            override fun <μR> MiniTracer<mR>.microTrace(f: KFunction0<μR>, block: MicroTracer<μR>.() -> μR): μR =
                MicroTracer.nullMicroTracer<μR>().block()

            override fun <μR> MiniTracer<mR>.microTrace1(f: KFunction1<*, μR>, block: MicroTracer<μR>.() -> μR): μR =
                MicroTracer.nullMicroTracer<μR>().block()

            override fun <μR> MiniTracer<mR>.microTrace2(f: KFunction2<*, *, μR>, block: MicroTracer<μR>.() -> μR): μR =
                MicroTracer.nullMicroTracer<μR>().block()

            override fun <μR> MiniTracer<mR>.microTrace3(f: KFunction3<*, *, *, μR>, block: MicroTracer<μR>.() -> μR): μR =
                MicroTracer.nullMicroTracer<μR>().block()
        }
    }
}

@Suppress("NonAsciiCharacters")
fun <mR, μR> MiniTracer<mR>?.microTrace(grapheme: Grapheme, block: MicroTracer<μR>.() -> μR) =
    if (this != null) this@microTrace.microTrace(grapheme, block) else MicroTracer.nullMicroTracer<μR>().block()

@Suppress("NonAsciiCharacters")
fun <mR, μR> MiniTracer<mR>?.microTrace(f: String, block: MicroTracer<μR>.() -> μR) =
    if (this != null) this@microTrace.microTrace(f, block) else MicroTracer.nullMicroTracer<μR>().block()

@Suppress("NonAsciiCharacters")
fun <mR, μR> MiniTracer<mR>?.microTrace(f: KCallable<μR>, block: MicroTracer<μR>.() -> μR) =
    if (this != null) this@microTrace.microTrace(f, block) else MicroTracer.nullMicroTracer<μR>().block()

@Suppress("NonAsciiCharacters")
fun <mR, μR> MiniTracer<mR>?.microTrace(f: KFunction0<μR>, block: MicroTracer<μR>.() -> μR) =
    if (this != null) this@microTrace.microTrace(f, block) else MicroTracer.nullMicroTracer<μR>().block()

@Suppress("NonAsciiCharacters")
fun <mR, μR> MiniTracer<mR>?.microTrace1(f: KFunction1<*, μR>, block: MicroTracer<μR>.() -> μR) =
    if (this != null) this@microTrace1.microTrace1(f, block) else MicroTracer.nullMicroTracer<μR>().block()

@Suppress("NonAsciiCharacters")
fun <mR, μR> MiniTracer<mR>?.microTrace2(f: KFunction2<*, *, μR>, block: MicroTracer<μR>.() -> μR) =
    if (this != null) this@microTrace2.microTrace2(f, block) else MicroTracer.nullMicroTracer<μR>().block()

@Suppress("NonAsciiCharacters")
fun <mR, μR> MiniTracer<mR>?.microTrace3(f: KFunction3<*, *, *, μR>, block: MicroTracer<μR>.() -> μR) =
    if (this != null) this@microTrace3.microTrace3(f, block) else MicroTracer.nullMicroTracer<μR>().block()

@Suppress("NonAsciiCharacters")
interface MicroTracer<μR> {
    fun trace(input: String)

    companion object {
        fun <μR> nullMicroTracer() = object : MicroTracer<μR> {
            override fun trace(input: String) {}
        }
    }
}

package com.imgcstmzr.runtime.log

//
//@Suppress("NonAsciiCharacters")
//inline fun <reified MR> BlockRenderingLogger<MR, HasStatus>?._segment(
//    f: String,
//    crossinline block: Tracer<MR>.() -> MR,
//): MR = segment<*, MR>(f, null, false, null) {
//    object : Tracer<MR> {
//        override fun trace(input: String) {
//            this@_segment?.logLine(META typed input)
//        }
//
//        override fun <mR> Tracer<MR>.macroTrace(f: String, block: MacroTracer<mR>.() -> mR): mR =
//            macroTrace(f, block)
//
//        override fun <mR> Tracer<MR>.macroTrace(f: KCallable<mR>, block: MacroTracer<mR>.() -> mR): mR =
//            macroTrace(f.format(), block)
//
//        override fun <mR> Tracer<MR>.macroTrace(f: KFunction0<mR>, block: MacroTracer<mR>.() -> mR): mR =
//            macroTrace(f.format(), block)
//
//        override fun <mR> Tracer<MR>.macroTrace1(f: KFunction1<*, mR>, block: MacroTracer<mR>.() -> mR): mR =
//            macroTrace(f.format(), block)
//
//        override fun <mR> Tracer<MR>.macroTrace2(f: KFunction2<*, *, mR>, block: MacroTracer<mR>.() -> mR): mR =
//            macroTrace(f.format(), block)
//
//        override fun <mR> Tracer<MR>.macroTrace3(f: KFunction3<*, *, *, mR>, block: MacroTracer<mR>.() -> mR): mR =
//            macroTrace(f.format(), block)
//        }
//    }.let(block)
//}
//
//inline fun <reified R, reified MR> BlockRenderingLogger<R, HasStatus>?.trace(f: String, crossinline block: Tracer<MR>.() -> MR): MR =
//    _segment(f.format(), block)
//
//inline fun <reified R, reified MR> BlockRenderingLogger<R, HasStatus>?.trace(f: KCallable<MR>, crossinline block: Tracer<MR>.() -> MR): MR =
//    _segment(f.format(), block)
//
//@JvmName("trace0")
//inline fun <reified R, reified MR> BlockRenderingLogger<R, HasStatus>?.trace(f: KFunction0<MR>, crossinline block: Tracer<MR>.() -> MR): MR =
//    _segment(f.format(), block)
//
//@JvmName("trace1")
//inline fun <reified R, reified MR> BlockRenderingLogger<R, HasStatus>?.trace(f: KFunction1<*, MR>, crossinline block: Tracer<MR>.() -> MR): MR =
//    _segment(f.format(), block)
//
//@JvmName("trace2")
//inline fun <reified R, reified MR> BlockRenderingLogger<R, HasStatus>?.trace(f: KFunction2<*, *, MR>, crossinline block: Tracer<MR>.() -> MR): MR =
//    _segment(f.format(), block)
//
//@JvmName("trace3")
//inline fun <reified R, reified MR> BlockRenderingLogger<R, HasStatus>?.trace(f: KFunction3<*, *, *, MR>, crossinline block: Tracer<MR>.() -> MR): MR =
//    _segment(f.format(), block)

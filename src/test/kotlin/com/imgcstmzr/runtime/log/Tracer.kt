package com.imgcstmzr.runtime.log

//
//@Suppress("NonAsciiCharacters")
//inline fun <reified MR> BlockRenderingLogger?._segment(
//    f: String,
//    crossinline block: Tracer.() -> MR,
//): MR = segment<*, MR>(f, null, false, null) {
//    object : Tracer {
//        override fun trace(input: String) {
//            this@_segment?.logStatus { META typed input }
//        }
//
//        override fun <mR> Tracer.macroTrace(f: String, block: MacroTracer.() -> mR): mR =
//            macroTrace(f, block)
//
//        override fun <mR> Tracer.macroTrace(f: KCallable<mR>, block: MacroTracer.() -> mR): mR =
//            macroTrace(f.format(), block)
//
//        override fun <mR> Tracer.macroTrace(f: KFunction0<mR>, block: MacroTracer.() -> mR): mR =
//            macroTrace(f.format(), block)
//
//        override fun <mR> Tracer.macroTrace1(f: KFunction1<*, mR>, block: MacroTracer.() -> mR): mR =
//            macroTrace(f.format(), block)
//
//        override fun <mR> Tracer.macroTrace2(f: KFunction2<*, *, mR>, block: MacroTracer.() -> mR): mR =
//            macroTrace(f.format(), block)
//
//        override fun <mR> Tracer.macroTrace3(f: KFunction3<*, *, *, mR>, block: MacroTracer.() -> mR): mR =
//            macroTrace(f.format(), block)
//        }
//    }.let(block)
//}
//
//inline fun <reified R, reified MR> BlockRenderingLogger?.trace(f: String, crossinline block: Tracer.() -> MR): MR =
//    _segment(f.format(), block)
//
//inline fun <reified R, reified MR> BlockRenderingLogger?.trace(f: KCallable<MR>, crossinline block: Tracer.() -> MR): MR =
//    _segment(f.format(), block)
//
//@JvmName("trace0")
//inline fun <reified R, reified MR> BlockRenderingLogger?.trace(f: KFunction0<MR>, crossinline block: Tracer.() -> MR): MR =
//    _segment(f.format(), block)
//
//@JvmName("trace1")
//inline fun <reified R, reified MR> BlockRenderingLogger?.trace(f: KFunction1<*, MR>, crossinline block: Tracer.() -> MR): MR =
//    _segment(f.format(), block)
//
//@JvmName("trace2")
//inline fun <reified R, reified MR> BlockRenderingLogger?.trace(f: KFunction2<*, *, MR>, crossinline block: Tracer.() -> MR): MR =
//    _segment(f.format(), block)
//
//@JvmName("trace3")
//inline fun <reified R, reified MR> BlockRenderingLogger?.trace(f: KFunction3<*, *, *, MR>, crossinline block: Tracer.() -> MR): MR =
//    _segment(f.format(), block)

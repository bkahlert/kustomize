package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.string.Grapheme
import com.bkahlert.koodies.tracing.MicroTracer
import com.bkahlert.koodies.tracing.MiniTracer
import com.imgcstmzr.util.format
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3

class RenderingLoggerBasedMiniTracer(private val renderingLogger: RenderingLogger) : MiniTracer {
    override fun trace(input: String) = renderingLogger.logStatus { IO.Type.META typed input }
    override fun <U> microTrace(grapheme: Grapheme, block: MicroTracer.() -> U): U = microTrace(grapheme, block)
    override fun <U> microTrace(f: String, block: MicroTracer.() -> U): U = microTrace(Grapheme("𝙛"), block)
    override fun <U> microTrace(f: KCallable<U>, block: MicroTracer.() -> U): U = microTrace(Grapheme("𝙛"), block)
    override fun <U> microTrace(f: KFunction0<U>, block: MicroTracer.() -> U): U = microTrace(Grapheme("𝙛"), block)
    override fun <U> microTrace1(f: KFunction1<*, U>, block: MicroTracer.() -> U): U = microTrace(Grapheme("𝙛"), block)
    override fun <U> microTrace2(f: KFunction2<*, *, U>, block: MicroTracer.() -> U): U = microTrace(Grapheme("𝙛"), block)
    override fun <U> microTrace3(f: KFunction3<*, *, *, U>, block: MicroTracer.() -> U): U = microTrace(Grapheme("𝙛"), block)
}

inline fun <reified R> BlockRenderingLogger?.subTrace(f: String, crossinline block: MiniTracer?.() -> R): R =
    singleLineLogging(f.format()) { RenderingLoggerBasedMiniTracer(this).run(block) }

inline fun <reified U> BlockRenderingLogger?.miniTrace(f: String, crossinline block: MiniTracer?.() -> U): U = subTrace(f.format(), block)
inline fun <reified U> BlockRenderingLogger?.miniTrace(f: KCallable<U>, crossinline block: MiniTracer?.() -> U): U = subTrace(f.format(), block)
inline fun <reified U> BlockRenderingLogger?.miniTrace(f: KFunction0<U>, crossinline block: MiniTracer?.() -> U): U = subTrace(f.format(), block)
inline fun <reified U> BlockRenderingLogger?.miniTrace1(f: KFunction1<*, U>, crossinline block: MiniTracer?.() -> U): U = subTrace(f.format(), block)
inline fun <reified U> BlockRenderingLogger?.miniTrace2(f: KFunction2<*, *, U>, crossinline block: MiniTracer?.() -> U): U = subTrace(f.format(), block)
inline fun <reified U> BlockRenderingLogger?.miniTrace3(f: KFunction3<*, *, *, U>, crossinline block: MiniTracer?.() -> U): U = subTrace(f.format(), block)

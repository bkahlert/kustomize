package com.bkahlert.kustomize

import com.bkahlert.kommons.collections.synchronizedMapOf
import com.bkahlert.kommons.junit.TestName.Companion.testName
import com.bkahlert.kommons.junit.Verbosity.Companion.isVerbose
import com.bkahlert.kommons.text.Banner
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.tracing.SpanId
import com.bkahlert.kommons.tracing.TraceId
import com.bkahlert.kommons.tracing.rendering.CompactRenderer
import com.bkahlert.kommons.tracing.rendering.InMemoryPrinter
import com.bkahlert.kommons.tracing.rendering.Printer
import com.bkahlert.kommons.tracing.rendering.RenderableAttributes
import com.bkahlert.kommons.tracing.rendering.Renderer
import com.bkahlert.kommons.tracing.rendering.RendererProvider
import com.bkahlert.kommons.tracing.rendering.Settings
import com.bkahlert.kommons.tracing.rendering.Styles
import com.bkahlert.kommons.tracing.runSpanning
import com.bkahlert.kustomize.cli.Layouts
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation
import org.junit.jupiter.api.extension.ReflectiveInvocationContext
import strikt.api.Assertion.Builder
import strikt.api.DescribeableBuilder
import strikt.api.expectThat
import java.lang.reflect.Method

/**
 * JUnit extension that provides access to what was rendered / output
 * during the execution of a trace.
 */
class TestSpanOutputExtension : InvocationInterceptor {

    override fun interceptTestMethod(
        invocation: Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        var linkedToTrace: TraceId? = null
        val capture = InMemoryPrinter()
        runSpanning(
            extensionContext.testName,
            style = Styles.Dotted,
            layout = Layouts.DESCRIPTION,
            renderer = {
                val print: Printer = if (extensionContext.isVerbose()) printer else run { {} }
                object : Renderer {
                    override fun start(traceId: TraceId, spanId: SpanId, name: CharSequence): Unit = print(LF + Banner.banner(name) + LF)
                    override fun event(name: CharSequence, attributes: RenderableAttributes): Unit = Unit
                    override fun exception(exception: Throwable, attributes: RenderableAttributes): Unit = Unit
                    override fun <R> end(result: Result<R>): Unit = Unit
                    override fun childRenderer(renderer: RendererProvider): Renderer = renderer(Settings { message ->
                        if (linkedToTrace == null) linkedToTrace = TraceId.current.takeIf { it.valid }?.also { traceId -> outputs[traceId] = capture }
                        capture(message)
                        print(message)
                    }) { CompactRenderer(it) }

                    override fun printChild(text: CharSequence): Unit = Unit
                }
            },
        ) {
            kotlin.runCatching {
                invocation.proceed()
            }.also {
                linkedToTrace?.also { outputs.remove(it) }
            }.getOrThrow()
        }
    }

    companion object {

        /** Stores rendered outputs to allow assertions. */
        private val outputs = synchronizedMapOf<TraceId, InMemoryPrinter>()

        /**
         * Returns the output rendered for the given [traceId].
         */
        operator fun get(traceId: TraceId): String =
            outputs[traceId].toString()
    }
}

/** Returns a [Builder] to run assertions on the rendered output. */
fun expectRendered(): DescribeableBuilder<String> = expectThat(TestSpanOutputExtension[TraceId.current])

/** Ends this spans and runs the specified [assertions] on the rendered output. */
fun expectRendered(assertions: Builder<String>.() -> Unit): Unit = expectThat(TestSpanOutputExtension[TraceId.current], assertions)

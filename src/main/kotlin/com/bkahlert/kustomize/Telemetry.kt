package com.bkahlert.kustomize

import com.bkahlert.kommons.tracing.Jaeger
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.SpanLimits
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import java.net.URI

/**
 * [OpenTelemetry](https://opentelemetry.io) instance used.
 */
object Telemetry {
    internal var started: Boolean = false
    internal var uri: URI? = null
    fun start(jaegerHostname: String?): URI? {
        if (started) return uri

        val jaeger = jaegerHostname?.let { Jaeger(it).apply { startLocally() } }

        OpenTelemetrySdk.builder()
            .setTracerProvider(SdkTracerProvider.builder()
                .apply {
                    if (jaeger != null) {
                        addSpanProcessor(BatchSpanProcessor.builder(JaegerGrpcSpanExporter.builder()
                            .setEndpoint(jaeger.protobufEndpoint.toString())
                            .build()).build())
                    }
                }
                .setResource(Resource.create(Attributes.of(
                    AttributeKey.stringKey("service.name"), Kustomize.name,
                    AttributeKey.stringKey("service.version"), Kustomize.version.toString(),
                )))
                .setSpanLimits { SpanLimits.builder().setMaxNumberOfEvents(2500).build() }
                .build())
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .buildAndRegisterGlobal()

        return jaeger?.uiEndpoint.also {
            started = true
            uri = it
        }
    }
}

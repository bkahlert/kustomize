package com.bkahlert.kustomize

import io.opentelemetry.api.OpenTelemetry
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
import koodies.tracing.Jaeger
import java.net.URI

/**
 * [OpenTelemetry](https://opentelemetry.io) instance used.
 */
object KustomizeTelemetry : OpenTelemetry by OpenTelemetrySdk.builder()
    .setTracerProvider(SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(JaegerGrpcSpanExporter.builder()
            .setEndpoint(Jaeger.startLocally())
            .build()).build())
        .setResource(Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), "kustomize")))
        .setSpanLimits { SpanLimits.builder().setMaxNumberOfEvents(2500).build() }
        .build())
    .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
    .buildAndRegisterGlobal() {

    val tracerUI: URI = Jaeger.uiEndpoint
}

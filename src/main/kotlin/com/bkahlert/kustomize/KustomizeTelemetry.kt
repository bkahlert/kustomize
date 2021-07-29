package com.bkahlert.kustomize

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import koodies.tracing.Jaeger
import java.net.URI

/**
 * [OpenTelemetry](https://opentelemetry.io) instance used.
 */
object KustomizeTelemetry : OpenTelemetry by OpenTelemetrySdk.builder()
//    .setTracerProvider(SdkTracerProvider.builder()
//        .addSpanProcessor(BatchSpanProcessor.builder(JaegerGrpcSpanExporter.builder()
//            .setEndpoint(Jaeger.startLocally())
//            .build()).build())
//        .setResource(Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), "kustomize")))
//        .setSpanLimits { SpanLimits.builder().setMaxNumberOfEvents(2500).build() }
//        .build())
//    .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
    .buildAndRegisterGlobal() {

    val tracerUI: URI = Jaeger.uiEndpoint
}

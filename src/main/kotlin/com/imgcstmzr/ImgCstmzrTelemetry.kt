package com.imgcstmzr

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import koodies.tracing.Jaeger
import java.net.URI

/**
 * [OpenTelemetry](https://opentelemetry.io) instance used.
 */
object ImgCstmzrTelemetry : OpenTelemetry by OpenTelemetrySdk.builder()
    .setTracerProvider(SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(JaegerGrpcSpanExporter.builder()
            .setEndpoint(Jaeger.startLocally())
            .build()).build())
        .setResource(Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), "imgcstmzr")))
        .setSpanLimits { SpanLimits.builder().setMaxNumberOfEvents(2500).build() }
        .build())
    .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
    .buildAndRegisterGlobal() {

    val tracerUI: URI = Jaeger.uiEndpoint
}

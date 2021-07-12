package com.imgcstmzr

import com.imgcstmzr.OpenTelemetry.NOOP
import io.opentelemetry.api.GlobalOpenTelemetry

/**
 * [OpenTelemetry](https://opentelemetry.io) instance used.
 *
 * For an implementation that never traces, use [NOOP].
 */
object OpenTelemetry : io.opentelemetry.api.OpenTelemetry by GlobalOpenTelemetry.get() {
    init {
        koodies.tracing.OpenTelemetry.register(this)
    }

    /**
     * [io.opentelemetry.api.OpenTelemetry] that does nothing.
     */
    object NOOP : io.opentelemetry.api.OpenTelemetry by io.opentelemetry.api.OpenTelemetry.noop()
}

/**
 * [OpenTelemetry](https://opentelemetry.io) [io.opentelemetry.api.trace.Tracer] used for tracing.
 */
object Tracer : io.opentelemetry.api.trace.Tracer by OpenTelemetry.tracerProvider.get("com.imgcstmzr", "1.0") {

    /**
     * [io.opentelemetry.api.trace.Tracer] that does nothing.
     */
    object NOOP : io.opentelemetry.api.trace.Tracer by OpenTelemetry.NOOP.tracerProvider.get("com.imgcstmzr", "1.0")
}

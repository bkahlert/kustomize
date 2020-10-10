package com.bkahlert.koodies.concurrent

import com.bkahlert.koodies.tracing.Tracer
import com.bkahlert.koodies.tracing.trace
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [Thread] that drains your battery and can only be stopped by calling [stop].
 */
class BusyThread private constructor(private var stopped: AtomicBoolean, private val tracer: Tracer<*>? = null) : Thread({
    while (!stopped.get()) {
        tracer.trace("THREAD $stopped")
        try {
            tracer.trace("busy")
        } catch (e: InterruptedException) {
            if (!stopped.get()) currentThread().interrupt()
        }
    }
}) {
    constructor(tracer: Tracer<*>? = null) : this(AtomicBoolean(false), tracer)

    init {
        start()
    }

    fun stopFussFree() {
        tracer.trace("stopping")
        stopped.set(true)
        interrupt()
        tracer.trace("stopped")
    }
}

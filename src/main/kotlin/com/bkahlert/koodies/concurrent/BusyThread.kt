package com.bkahlert.koodies.concurrent

import com.imgcstmzr.runtime.log.RenderingLogger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [Thread] that drains your battery and can only be stopped by calling [stop].
 */
class BusyThread private constructor(private var stopped: AtomicBoolean, private val logger: RenderingLogger<*>? = null) : Thread({
    while (!stopped.get()) {
        logger?.logLine { "THREAD $stopped" }
        try {
            logger?.logLine { "busy" }
        } catch (e: InterruptedException) {
            if (!stopped.get()) currentThread().interrupt()
        }
    }
}) {
    constructor(logger: RenderingLogger<*>? = null) : this(AtomicBoolean(false), logger)

    init {
        start()
    }

    fun stopFussFree() {
        logger?.logLine { "stopping" }
        stopped.set(true)
        interrupt()
        logger?.logLine { "stopped" }
    }
}

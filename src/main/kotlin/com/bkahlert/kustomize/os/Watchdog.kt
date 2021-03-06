package com.bkahlert.kustomize.os

import com.bkahlert.kommons.runtime.thread
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.time.Now
import com.bkahlert.kommons.tracing.SpanScope
import com.bkahlert.kustomize.os.Watchdog.Command.RESET
import com.bkahlert.kustomize.os.Watchdog.Command.STOP
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.time.Duration

/**
 * Watchdog implementation that works like a dead man's switch.
 * [timedOut] is **not** triggered as long as [reset] was called in time.
 * Should the time between the start of this watchdog or the last [reset] call surpass the [timeout]
 * the watchdog calls [timedOut].
 */
open class Watchdog(
    /**
     * Duration that needs to pass until [timeout] is called.
     */
    private val timeout: Duration,
    /**
     * If set to `true` this watchdog does not stop working after having been triggered.
     * Instead the watch period starts again after [timedOut] finished.
     */
    private val repeating: Boolean = false,
    /**
     * Logger that can be accessed in [timedOut].
     */
    private val span: SpanScope? = null,
    /**
     * Gets called after more time has passed between the start of this watchdog and/or two consecutive [reset] calls.
     */
    val timedOut: SpanScope.() -> Any,
) {
    private var timeoutStart: Long = -1L
    private val blockingQueue = LinkedBlockingQueue<Command>()
    private val thread = thread {
        var lastEvent: Command? = null
        while (true) {
            try {
                when (blockingQueue.poll(timeout.toLong(MILLISECONDS).also {
                    timeoutStart = System.currentTimeMillis()
                    if (lastEvent == null) {
                        span?.log("Watchdog started. Timing out in $remaining.")
                        lastEvent = RESET
                    }
                }, MILLISECONDS)) {
                    RESET -> {
//                        logger?.logLine { IO.Type.META typed "Watchdog reset. Timing out in $timeout." }
                        lastEvent = RESET
                    }
                    STOP -> {
                        span?.log("Watchdog stopped.")
                        lastEvent = STOP
                        break
                    }
                    null -> {
                        if (lastEvent != null) {
                            span?.log("Watchdog timed out. Invoking $timedOut.")
                            span?.timedOut()
                        }
                        lastEvent = null
                        if (!repeating) break
                    }
                }
            } catch (e: InterruptedException) {
                "Watchdog was interrupted (last event: $lastEvent). Stopping.".ansi.red
                break
            }
        }
    }

    val remaining: Duration get() = timeout - Now.passedSince(timeoutStart)

    /**
     * A call to [reset] resets the timer that ticks against [timeout].
     * E.g. if [timeout] was 15s and the watchdog is running since 10s the [timedOut] would be called in 5s.
     * After having called [reset] the time it takes until [timedOut] is called, is again 15s.
     */
    open fun reset() {
        if (thread.isAlive) {
            blockingQueue.put(RESET)
        }
    }

    /**
     * Stops this watchdog. Any call to further call to [reset] or [stop] is ignored. A call to [timedOut] will
     * never occur.
     */
    fun stop() {
        if (thread.isAlive) {
            blockingQueue.put(STOP)
        }
    }

    private enum class Command {
        RESET, STOP
    }
}

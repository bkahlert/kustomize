package com.imgcstmzr.runtime

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.startAsThread
import com.bkahlert.koodies.terminal.ansi.AnsiColors.red
import com.bkahlert.koodies.time.Now
import com.imgcstmzr.runtime.Watchdog.Command.RESET
import com.imgcstmzr.runtime.Watchdog.Command.STOP
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.RenderingLogger.Companion.DEFAULT
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

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
    private val timeout: kotlin.time.Duration,
    /**
     * If set to `true` this watchdog does not stop working after having been triggered.
     * Instead the watch period starts again after [timedOut] finished.
     */
    private val repeating: Boolean = false,
    /**
     * Logger that can be accessed in [timedOut].
     */
    private val logger: RenderingLogger? = DEFAULT,
    /**
     * Gets called after more time has passed between the start of this watchdog and/or two consecutive [reset] calls.
     */
    val timedOut: RenderingLogger.() -> Any,
) {
    private var timeoutStart: Long = -1L
    private val blockingQueue = LinkedBlockingQueue<Command>()
    private val thread = startAsThread {
        var lastEvent: Command? = RESET
        while (true) {
            try {
                when (blockingQueue.poll(timeout.toLongMilliseconds().also {
                    timeoutStart = System.currentTimeMillis()
                    logger?.logLine { IO.Type.META typed "Watchdog started. Timing out in $remaining." }
                }, TimeUnit.MILLISECONDS)) {
                    RESET -> {
                        logger?.logLine { IO.Type.META typed "Watchdog reset. Timing out in $timeout." }
                        lastEvent = RESET
                    }
                    STOP -> {
                        logger?.logLine { IO.Type.META typed "Watchdog stopped." }
                        lastEvent = STOP
                        break
                    }
                    null -> {
                        if (lastEvent != null) {
                            logger?.logLine { IO.Type.META typed "Watchdog timed out. Invoking $timedOut." }
                            logger?.timedOut()
                        }
                        lastEvent = null
                        if (!repeating) break
                    }
                }
            } catch (e: InterruptedException) {
                "Watchdog was interrupted (last event: $lastEvent). Stopping.".red()
                break
            }
        }
    }

    val remaining: kotlin.time.Duration get() = timeout - Now.passedSince(timeoutStart)

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

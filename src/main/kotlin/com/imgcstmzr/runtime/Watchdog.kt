package com.imgcstmzr.runtime

import com.bkahlert.koodies.concurrent.process.IO
import com.github.ajalt.clikt.output.TermUi.echo
import com.github.ajalt.mordant.TermColors
import com.imgcstmzr.runtime.Watchdog.Command.RESET
import com.imgcstmzr.runtime.Watchdog.Command.STOP
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.RenderingLogger.Companion.DEFAULT
import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Watchdog implementation that works like a dead man's switch.
 * [timedOut] is **not** triggered as long as [reset] was called in time.
 * Should the time between the start of this watchdog or the last [reset] call surpass the [timeout]
 * the watchdog calls [timedOut].
 */
class Watchdog(
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
    private val renderingLogger: RenderingLogger<*> = DEFAULT,
    /**
     * Gets called after more time has passed between the start of this watchdog and/or two consecutive [reset] calls.
     */
    val timedOut: RenderingLogger<*>.() -> Any,
) {
    private val blockingQueue = LinkedBlockingQueue<Command>()
    private val thread = Thread {
        var lastEvent: Command? = RESET
        while (true) {
            try {
                when (blockingQueue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    RESET -> {
                        // start another poll
                        lastEvent = RESET
                    }
                    STOP -> {
                        renderingLogger.logLine { IO.Type.META typed "Watchdog stopped." }
                        lastEvent = STOP
                        return@Thread
                    }
                    null -> {
                        if (lastEvent != null) renderingLogger.timedOut()
                        lastEvent = null
                        if (!repeating) return@Thread
                    }
                }
            } catch (e: InterruptedException) {
                with(TC) {
                    echo(red("Watchdog was interrupted (last event: $lastEvent). Stopping."))
                }
                return@Thread
            }
        }
    }.apply { start() }

    /**
     * A call to [reset] resets the timer that ticks against [timeout].
     * E.g. if [timeout] was 15s and the watchdog is running since 10s the [timedOut] would be called in 5s.
     * After having called [reset] the time it takes until [timedOut] is called, is again 15s.
     */
    fun reset() {
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

    companion object {
        private val TC = TermColors(TermColors.Level.ANSI16)
    }
}

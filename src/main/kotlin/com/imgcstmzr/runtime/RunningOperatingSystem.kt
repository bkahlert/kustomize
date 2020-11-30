package com.imgcstmzr.runtime

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.process.UserInput.enter
import com.bkahlert.koodies.kaomoji.Kaomojis
import com.bkahlert.koodies.kaomoji.Kaomojis.thinking
import com.bkahlert.koodies.string.LineSeparators.LF
import com.bkahlert.koodies.string.LineSeparators.withoutTrailingLineSeparator
import com.bkahlert.koodies.string.quoted
import com.bkahlert.koodies.terminal.ansi.AnsiColors.green
import com.imgcstmzr.runtime.log.RenderingLogger
import kotlin.properties.Delegates.observable
import kotlin.time.Duration
import kotlin.time.milliseconds

abstract class RunningOperatingSystem(val shutdownCommand: String) {
    abstract val logger: RenderingLogger<*>
    abstract val process: Process

    /**
     * Forwards the [values] to the OS running process.
     */
    fun enter(vararg values: String, delay: Duration = 10.milliseconds) {
        if (process.isAlive) {
            feedback("Entering ${values.joinToString { it.withoutTrailingLineSeparator }.quoted}")
            process.enter(*values, delay = delay)
        } else {
            feedback("Process $process is not alive.")
        }
    }

    /**
     * Prints [value] on the output without actually forwarding it
     * to the OS running process.
     */
    fun feedback(
        value: String,
        kaomoji: CharSequence = listOf(Kaomojis.Happy, Kaomojis.PeaceSign, Kaomojis.Smile, Kaomojis.ThumbsUp, Kaomojis.Proud).random().random(),
    ) {
        logger.logLine { LF + kaomoji.thinking(value.capitalize().green()) + LF }
    }

    /**
     * Prints [value] on the output without actually forwarding it
     * to the OS running process.
     */
    fun negativeFeedback(
        value: String,
        kaomoji: CharSequence = listOf(Kaomojis.Cry, Kaomojis.Depressed, Kaomojis.Disappointed, Kaomojis.Sad).random().random(),
    ) {
        logger.logLine { LF + kaomoji.thinking(value.capitalize().green()) + LF }
    }


    val shuttingDownStatus: List<HasStatus> = listOf(object : HasStatus {
        override fun status(): String = "shutting down"
    })

    /**
     * Logs the current execution status given the [io] and [unfinished].
     */
    fun status(io: IO, unfinished: List<Program>) {
        logger.logStatus(items = if (shuttingDown) shuttingDownStatus else unfinished) { io }
    }

    fun command(input: String) {
        enter(input)
    }

    /**
     * Initiates the systems immediate shutdown.
     */
    fun shutdown() {
        enter(shutdownCommand)
        shuttingDown = true
    }

    fun kill() {
        feedback("Kill invoked")
        process.destroyForcibly()
    }

    var shuttingDown: Boolean by observable(false) { _, _, _ ->
        feedback("Shutdown invoked")
    }

    override fun toString(): String =
        "RunningOperatingSystem(renderer=${logger.javaClass}, process=$process, shutdownCommand='$shutdownCommand', shuttingDownStatus=$shuttingDownStatus)"
}

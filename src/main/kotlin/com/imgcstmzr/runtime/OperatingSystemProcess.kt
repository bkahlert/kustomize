package com.imgcstmzr.runtime

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.IN
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.concurrent.process.Process
import com.bkahlert.koodies.concurrent.process.Processor
import com.bkahlert.koodies.concurrent.process.UserInput.input
import com.bkahlert.koodies.concurrent.process.process
import com.bkahlert.koodies.concurrent.process.waitForTermination
import com.bkahlert.koodies.concurrent.startAsThread
import com.bkahlert.koodies.docker.DockerContainerName.Companion.toUniqueContainerName
import com.bkahlert.koodies.docker.DockerImage
import com.bkahlert.koodies.docker.DockerImageBuilder.Companion.build
import com.bkahlert.koodies.docker.DockerProcess
import com.bkahlert.koodies.docker.DockerRunCommandLineBuilder.Companion.buildRunCommand
import com.bkahlert.koodies.kaomoji.Kaomojis
import com.bkahlert.koodies.kaomoji.Kaomojis.thinking
import com.bkahlert.koodies.string.LineSeparators
import com.bkahlert.koodies.string.LineSeparators.withoutTrailingLineSeparator
import com.bkahlert.koodies.string.quoted
import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiColors.green
import com.imgcstmzr.runtime.Program.Companion.compute
import com.imgcstmzr.runtime.Program.Companion.format
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.subLogger
import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlin.time.milliseconds

/**
 * Function that processes the [IO] of a [OperatingSystemProcess].
 */
typealias OperatingSystemProcessor = Processor<OperatingSystemProcess>

fun stuckCheckingProcessor(processor: OperatingSystemProcessor): OperatingSystemProcessor = processor.run {
    var stuck = false
    { io: IO ->
        if (!stuck) {
            stuck = isStuck(io)
            if (!stuck) processor(io)
        }
    }
}


/**
 * A [Process] running an emulator for [ARM based](https://en.wikipedia.org/wiki/ARM_architecture)
 * [OperatingSystem] such as [OperatingSystems.RaspberryPiLite] provided
 * through an [OperatingSystemImage].
 *
 * Changes to the [OperatingSystemImage] are permanent. The manipulated
 * image file can be used to to flash a memory card and to run it
 * non-virtually on actual hardware.
 */
open class OperatingSystemProcess(
    name: String,
    val osImage: OperatingSystemImage,
    val logger: RenderingLogger,
) : DockerProcess(
    commandLine = DOCKER_IMAGE.buildRunCommand {
        options {
            name { name }
            mounts { osImage.file mountAt "/sdcard/filesystem.img" }
        }
    },
) {
    companion object {
        @Suppress("SpellCheckingInspection")
        private val DOCKER_IMAGE: DockerImage = build { "lukechilds" / "dockerpi" tag "vm" }
    }

    /**
     * Forwards the [values] to the OS running process.
     */
    fun enter(vararg values: String, delay: Duration = 10.milliseconds) {
        if (alive) {
            feedback("Entering ${values.joinToString { it.withoutTrailingLineSeparator }.quoted}")
            input(*values, delay = delay)
        } else {
            feedback("Process $this is not alive.")
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
        logger.logLine { LineSeparators.LF + kaomoji.thinking(value.capitalize().green()) + LineSeparators.LF }
    }

    /**
     * Prints [value] on the output without actually forwarding it
     * to the OS running process.
     */
    fun negativeFeedback(
        value: String,
        kaomoji: CharSequence = listOf(Kaomojis.Cry, Kaomojis.Depressed, Kaomojis.Disappointed, Kaomojis.Sad).random().random(),
    ) {
        logger.logLine { LineSeparators.LF + kaomoji.thinking(value.capitalize().green()) + LineSeparators.LF }
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

    fun isStuck(io: IO): Boolean {
        val stuck = osImage.deadEndPattern?.matches(io.unformatted) == true
        if (stuck) {
            startAsThread {
                negativeFeedback("The VM is stuck. Chances are the VM starts correctly with less load on this machine.")
            }
            stop()
            throw IllegalStateException(io.unformatted)
        }
        return stuck
    }

    /**
     * Initiates the systems immediate shutdown.
     */
    fun shutdown() {
        enter(osImage.shutdownCommand)
        shuttingDown = true
    }

    var shuttingDown: Boolean by Delegates.observable(false) { _, _, _ ->
        feedback("Shutdown invoked")
    }
}

/**
 * Starts a [DockerProcess] with [name] that boots the [this@run] and
 * runs all provided [programs].
 *
 * Before the [programs] the [this@run] will be booted and an [autoLogin] takes place.
 * After the execution of all [programs] finishes the [this@run] will
 * be shutdown.
 *
 * @return exit code `0` if no errors occurred.
 */
fun OperatingSystemImage.execute(
    name: String = file.toUniqueContainerName().sanitized,
    logger: RenderingLogger,
    autoLogin: Boolean = true,
    vararg programs: Program,
): Int = logger.subLogger("Running $shortName with ${programs.format()}", ansiCode = ANSI.termColors.cyan) {
    val unfinished: MutableList<Program> = mutableListOf(
        *(if (autoLogin) arrayOf(loginProgram(credentials)/*.logging()*/) else emptyArray()),
        *programs,
        shutdownProgram(),/*.logging()*/
    )

    val operatingSystemProcess = OperatingSystemProcess(name, this@execute, this@subLogger)
    operatingSystemProcess.process(stuckCheckingProcessor { io ->
        when (io.type) {
            META -> feedback(io.unformatted)
            IN -> logStatus(items = unfinished) { io }
            OUT -> logStatus(items = unfinished) { io }
            ERR -> feedback("Unfortunately an error occurred: ${io.formatted}")
        }
        if (!unfinished.compute(this, io)) unfinished.takeIf { it.isNotEmpty() }?.removeAt(0)
    }).waitForTermination()
}

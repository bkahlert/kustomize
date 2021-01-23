package com.imgcstmzr.runtime

import com.imgcstmzr.runtime.Program.Companion.compute
import com.imgcstmzr.runtime.Program.Companion.format
import koodies.builder.ListBuilder.Companion.buildList
import koodies.concurrent.process.IO
import koodies.concurrent.process.IO.Type.ERR
import koodies.concurrent.process.IO.Type.IN
import koodies.concurrent.process.IO.Type.META
import koodies.concurrent.process.IO.Type.OUT
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Process
import koodies.concurrent.process.Processor
import koodies.concurrent.process.UserInput.input
import koodies.concurrent.process.process
import koodies.concurrent.thread
import koodies.docker.DockerContainerName.Companion.toUniqueContainerName
import koodies.docker.DockerImage
import koodies.docker.DockerImageBuilder.Companion.build
import koodies.docker.DockerProcess
import koodies.docker.buildCommandLine
import koodies.kaomoji.Kaomojis
import koodies.kaomoji.Kaomojis.thinking
import koodies.logging.HasStatus
import koodies.logging.RenderingLogger
import koodies.logging.logging
import koodies.terminal.ANSI
import koodies.terminal.AnsiColors.green
import koodies.text.LineSeparators
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.quoted
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
    private val os: OperatingSystem,
    private val process: ManagedProcess,
    internal open val logger: RenderingLogger,
) : ManagedProcess by process {

    constructor(name: String, osImage: OperatingSystemImage, logger: RenderingLogger) : this(
        os = osImage.operatingSystem,
        process = DOCKER_IMAGE.buildCommandLine {
            options {
                name { name }
                mounts { osImage.file mountAt "/sdcard/filesystem.img" }
            }
        }.execute(),
        logger = logger,
    )

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
        override fun renderStatus(): String = "shutting down"
    })

    /**
     * Logs the current execution status given the [io] and [unfinished].
     */
    fun status(io: IO, unfinished: List<Program>) {
        logger.logStatus(items = if (shuttingDown) shuttingDownStatus else unfinished) { io }
    }

    fun command(input: String) {
        enter(input, delay = Duration.ZERO)
    }

    fun isStuck(io: IO): Boolean {
        val stuck = os.deadEndPattern?.matches(io.unformatted) == true
        if (stuck) {
            thread { negativeFeedback("The VM is stuck. Chances are the VM starts correctly with less load on this machine.") }
            stop()
            throw IllegalStateException(io.unformatted)
        }
        return stuck
    }

    /**
     * Initiates the systems immediate shutdown.
     */
    fun shutdown() {
        enter(os.shutdownCommand)
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
 * Before the [programs] the [this@run] will be booted an [autoLogin] takes place.
 * After the execution of all [programs] finishes the [this@run] will
 * be [autoShutdown].
 *
 * @return exit code `0` if no errors occurred.
 */
fun OperatingSystemImage.execute(
    name: String = file.toUniqueContainerName().sanitized,
    logger: RenderingLogger,
    autoLogin: Boolean = true,
    autoShutdown: Boolean = true,
    vararg programs: Program,
): Int = logger.logging("Running $shortName with ${programs.format()}", ansiCode = ANSI.termColors.cyan) {
    val unfinished: MutableList<Program> = buildList<Program> {
        if (autoLogin) +loginProgram(credentials)/*.logging()*/
        +programs
        if (autoShutdown) +shutdownProgram()/*.logging()*/
    }.toMutableList()

    val operatingSystemProcess = OperatingSystemProcess(name, this@execute, this@logging)
    operatingSystemProcess.process(stuckCheckingProcessor { io ->
        when (io.type) {
            META -> feedback(io.unformatted)
            IN -> logStatus(items = unfinished) { io }
            OUT -> logStatus(items = unfinished) { io }
            ERR -> feedback("Unfortunately an error occurred: ${io.formatted}")
        }
        if (!unfinished.compute(this, io)) {
            if (unfinished.isNotEmpty()) unfinished.removeFirst()

            // if the OS was ready and the previous program "just" waited to confirm successful execution
            // pass this IO also to the next program. Otherwise the execution might get stuck should more
            // IO be emitted, like `bkahlert@bother-you-apYr:~$ [  OK  ] Started User Manager for UID 1000`
            //                                 ready till here ↑ now, no more
            if (unfinished.isNotEmpty() && !unfinished.compute(this, io)) unfinished.removeFirst()
        }
    }).waitForTermination()
}

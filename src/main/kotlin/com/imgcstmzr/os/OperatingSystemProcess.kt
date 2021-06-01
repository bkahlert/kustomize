package com.imgcstmzr.os

import com.imgcstmzr.os.Program.Companion.compute
import com.imgcstmzr.os.Program.Companion.format
import koodies.builder.buildList
import koodies.docker.DockerContainer
import koodies.docker.DockerImage
import koodies.docker.dockerPi
import koodies.exec.Exec
import koodies.exec.IO
import koodies.exec.Process.State
import koodies.exec.alive
import koodies.exec.input
import koodies.jvm.thread
import koodies.kaomoji.Kaomoji
import koodies.logging.FixedWidthRenderingLogger
import koodies.logging.FixedWidthRenderingLogger.Border
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.Semantics.formattedAs
import koodies.text.quoted
import koodies.time.seconds
import koodies.unit.milli
import kotlin.properties.Delegates
import kotlin.time.Duration

/**
 * Function that processes the [IO] of a [OperatingSystemProcess].
 */
typealias OperatingSystemProcessor = OperatingSystemProcess.(IO) -> Unit

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
    private val exec: Exec,
    open val logger: FixedWidthRenderingLogger,
) : Exec by exec {

    /**
     * Forwards the [values] to the OS running process.
     */
    fun enter(vararg values: String, delay: Duration = 10.milli.seconds) {
        if (alive) {
            feedback("Entering ${values.joinToString { it.withoutTrailingLineSeparator }.quoted}")
            input(*values, delay = delay)
        } else {
            feedback("Process $this is not alive.")
        }
    }

    /**
     * Prints [message] on the output without actually forwarding it
     * to the OS running process.
     */
    fun feedback(
        message: String,
        kaomoji: Kaomoji = Kaomoji.random(Kaomoji.Happy, Kaomoji.PeaceSign, Kaomoji.Smiling, Kaomoji.ThumbsUp, Kaomoji.Proud),
    ) {
        logger.logLine { LF + kaomoji.thinking(message) + LF }
    }

    private val ignoredErrors = listOf(
        "WARNING: Image format was not specified for '/sdcard/filesystem.img' and probing guessed raw.",
        "Automatically detecting the format is dangerous for raw images, write operations on block 0 will be restricted.",
        "Specify the 'raw' format explicitly to remove the restrictions.",
    )

    /**
     * Prints [message] on the output without actually forwarding it
     * to the OS running process.
     */
    fun negativeFeedback(
        message: String,
        kaomoji: Kaomoji = Kaomoji.random(Kaomoji.Crying, Kaomoji.Depressed, Kaomoji.Disappointed, Kaomoji.Sad, Kaomoji.Screaming).random(),
    ) {
        if (ignoredErrors.any { message.contains(it, ignoreCase = true) }) return
        logger.logLine { LF + kaomoji.thinking(message.formattedAs.error) + LF }
    }

    val shuttingDownStatus: List<String> = listOf("shutting down")

    /**
     * Logs the current execution status given the [io] and [unfinished].
     */
    fun status(io: IO, unfinished: List<Program>) {
        if (shuttingDown) {
            logger.logStatus(items = shuttingDownStatus) { io }
        } else {
            logger.logStatus(items = unfinished.map { it.toString() }) { io }
        }
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

    override fun toString(): String {
        return "$os @ ${exec.pid}"
    }

    companion object {
        @Suppress("SpellCheckingInspection") object DockerPiImage :
            DockerImage("lukechilds", listOf("dockerpi"), tag = "vm")
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
fun OperatingSystemImage.boot(
    name: String?,
    logger: FixedWidthRenderingLogger,
    vararg programs: Program,
    headlineFormatter: Formatter = Formatter.PassThrough,
    decorationFormatter: Formatter = Formatter { it.ansi.cyan.done },
    autoLogin: Boolean = true,
    autoShutdown: Boolean = true,
    border: Border = SOLID,
): State = logger.logging(
    headlineFormatter("Running $shortName with ${programs.format()}"),
    decorationFormatter = decorationFormatter,
    border = border
) {
    val unfinished: MutableList<Program> = buildList {
        if (autoLogin) +loginProgram(credentials)
        addAll(programs)
        if (autoShutdown) +shutdownProgram()
    }.toMutableList()

    file.dockerPi(name ?: DockerContainer.from(file).name, this, run {
        var operatingSystemProcess: OperatingSystemProcess? = null
        var operatingSystemProcessor: OperatingSystemProcessor? = null
        {
            (operatingSystemProcess ?: OperatingSystemProcess(this@boot, this, this@logging).also { operatingSystemProcess = it }).let { osProcess ->
                (operatingSystemProcessor ?: stuckCheckingProcessor { io ->
                    when (io) {
                        is IO.Meta -> feedback(io.unformatted.trim())
                        is IO.Input -> logStatus(items = unfinished.map { it.toString() }) { io }
                        is IO.Output -> logStatus(items = unfinished.map { it.toString() }) { io }
                        is IO.Error -> negativeFeedback(io.unformatted.trim())
                    }
                    if ((io is IO.Output || io is IO.Error) && !unfinished.compute(this, io)) {
                        if (unfinished.isNotEmpty()) unfinished.removeFirst()

                        // if the OS was ready and the previous program "just" waited to confirm successful execution
                        // pass this IO also to the next program. Otherwise the execution might get stuck should more
                        // IO be emitted, like `bkahlert@bother-you-apYr:~$ [  OK  ] Started User Manager for UID 1000`
                        //                                 ready till here ↑ … now, no more, since line is no more matched
                        if (unfinished.isNotEmpty() && !unfinished.compute(this, io)) unfinished.removeFirst()
                    }
                }.also { operatingSystemProcessor = it }).let { osProcessor ->
                    osProcessor(osProcess, it)
                }
            }
        }
    }).waitFor()
}

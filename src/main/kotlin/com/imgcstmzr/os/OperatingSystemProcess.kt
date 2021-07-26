package com.imgcstmzr.os

import com.imgcstmzr.cli.PATCH_INNER_DECORATION_FORMATTER
import com.imgcstmzr.os.Program.Companion.compute
import com.imgcstmzr.os.Program.Companion.format
import koodies.builder.buildList
import koodies.docker.DockerContainer
import koodies.docker.DockerExec
import koodies.docker.DockerImage
import koodies.docker.dockerPi
import koodies.exec.Exec
import koodies.exec.IO
import koodies.exec.IO.Error
import koodies.exec.IO.Input
import koodies.exec.IO.Meta
import koodies.exec.IO.Output
import koodies.exec.Process.ExitState
import koodies.exec.Process.State
import koodies.exec.alive
import koodies.exec.input
import koodies.jvm.thread
import koodies.kaomoji.Kaomoji
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.ansiRemoved
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.trailingLineSeparatorRemoved
import koodies.text.Semantics.formattedAs
import koodies.text.joinToTruncatedString
import koodies.text.quoted
import koodies.text.truncateByColumns
import koodies.time.seconds
import koodies.tracing.CurrentSpan
import koodies.tracing.Key
import koodies.tracing.rendering.ColumnsLayout
import koodies.tracing.rendering.ColumnsLayout.Companion.columns
import koodies.tracing.rendering.Renderable
import koodies.tracing.rendering.RenderingAttributes
import koodies.tracing.rendering.Style
import koodies.tracing.rendering.Styles
import koodies.tracing.spanning
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
    open val span: CurrentSpan,
) : Exec by exec {

    /**
     * Forwards the [values] to the OS running process.
     */
    fun enter(vararg values: String, delay: Duration = 10.milli.seconds) {
        if (alive) {
            feedback("Entering ${values.joinToString { it.trailingLineSeparatorRemoved }.quoted}")
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
        span.log(LF + kaomoji.thinking(message.formattedAs.success) + LF)
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
        span.log(LF + kaomoji.thinking(message.formattedAs.error) + LF)
    }

    /**
     * Logs the current execution status given the [io] and [unfinished].
     */
    fun status(io: IO, unfinished: List<Program>) {
        if (shuttingDown) {
            span.ioEvent(io, emptyList())
        } else {
            span.ioEvent(io, unfinished)
        }
    }

    fun command(input: String) {
        enter(input, delay = Duration.ZERO)
    }

    fun isStuck(io: IO): Boolean {
        val stuck = os.deadEndPattern?.matches(io.ansiRemoved) == true
        if (stuck) {
            thread { negativeFeedback("The VM is stuck. Chances are the VM starts correctly with less load on this machine.") }
            stop()
            throw IllegalStateException(io.ansiRemoved)
        }
        return stuck
    }

    /**
     * Initiates the systems immediate shutdown.
     */
    fun shutdown() {
        enter(os.shutdownCommand.shellCommand)
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
 * Starts a [dockerPi] with [name] that boots the [this@run] and
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
    vararg programs: Program,
    nameFormatter: Formatter<CharSequence> = Formatter.ToCharSequence,
    decorationFormatter: Formatter<CharSequence> = PATCH_INNER_DECORATION_FORMATTER,
    autoLogin: Boolean = true,
    autoShutdown: Boolean = true,
    style: (ColumnsLayout, Int) -> Style = Styles.Solid,
): State {
    val unfinished: MutableList<Program> = buildList {
        if (autoLogin) +loginProgram(credentials)
        addAll(programs)
        if (autoShutdown) +shutdownProgram()
    }.toMutableList()

    return file.dockerPi(name ?: DockerContainer.from(file).name) { exec: DockerExec, block: ((IO) -> Unit) -> ExitState ->
        spanning(
            "Running ${shortName.formattedAs.input} with ${programs.format()}",
            nameFormatter = nameFormatter,
            decorationFormatter = decorationFormatter,
            layout = OS_BOOT_LAYOUT,
            style = style,
            block = {
                var operatingSystemProcess: OperatingSystemProcess? = null
                var operatingSystemProcessor: OperatingSystemProcessor? = null
                block {
                    val osProcess = operatingSystemProcess ?: OperatingSystemProcess(this@boot, exec, this).also { operatingSystemProcess = it }

                    val osProcessor = operatingSystemProcessor ?: stuckCheckingProcessor { io ->
                        when (io) {
                            is Meta -> feedback(io.ansiRemoved.trim())
                            is Input -> ioEvent(io, unfinished)
                            is Output -> ioEvent(io, unfinished)
                            is Error -> negativeFeedback(io.ansiRemoved.trim())
                        }
                        if ((io is Output || io is Error) && !unfinished.compute(this, io)) {
                            if (unfinished.isNotEmpty()) unfinished.removeFirst()

                            // if the OS was ready and the previous program "just" waited to confirm successful execution
                            // pass this IO also to the next program. Otherwise the execution might get stuck should more
                            // IO be emitted, like `user@bother-you-apYr:~$ [  OK  ] Started User Manager for UID 1000`
                            //                             ready till here ↑ … now, no longer, since line is no more matched
                            if (unfinished.isNotEmpty() && !unfinished.compute(this, io)) unfinished.removeFirst()
                        }
                    }.also { operatingSystemProcessor = it }

                    osProcessor(osProcess, it)
                }
            },
        )
    }.waitFor()
}

val STATUS: Key<List<String>, Collection<Program>> =
    Key.stringArrayKey("imgcstmzr.os.program.status") { programs ->
        programs.map { program -> program.toString().ansiRemoved }
    }

val OS_BOOT_LAYOUT = ColumnsLayout(
    RenderingAttributes.DESCRIPTION columns 120,
    STATUS columns 40,
    maxColumns = 160,
)

class RenderablePrograms(private val programs: Collection<Program>) : Collection<Program> by programs, Renderable by Renderable.of(programs, { columns, _ ->
    when {
        size == 0 -> "◼"
        columns != null -> {
            joinToTruncatedString(" ◀ ".formattedAs.meta, "◀◀ ".formattedAs.success).truncateByColumns(columns).toString()
        }
        else -> {
            joinToString(" ◀ ".formattedAs.meta, "◀◀ ".formattedAs.success)
        }
    }
}) {
    override fun isEmpty(): Boolean {
        return programs.isEmpty()
    }
}

fun CurrentSpan.ioEvent(io: IO, programs: Collection<Program>) =
    event("io", *io.attributes.toTypedArray(), STATUS to RenderablePrograms(programs))

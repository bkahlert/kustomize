package com.bkahlert.kustomize.os

import com.bkahlert.kommons.docker.DockerContainer
import com.bkahlert.kommons.docker.DockerExec
import com.bkahlert.kommons.docker.DockerImage
import com.bkahlert.kommons.docker.DockerRunCommandLine
import com.bkahlert.kommons.docker.DockerRunCommandLine.Options
import com.bkahlert.kommons.docker.MountOptions
import com.bkahlert.kommons.exec.CommandLine
import com.bkahlert.kommons.exec.IO
import com.bkahlert.kommons.exec.IO.Error
import com.bkahlert.kommons.exec.IO.Input
import com.bkahlert.kommons.exec.IO.Meta
import com.bkahlert.kommons.exec.IO.Output
import com.bkahlert.kommons.exec.Process.ExitState
import com.bkahlert.kommons.exec.Process.State
import com.bkahlert.kommons.kaomoji.Kaomoji
import com.bkahlert.kommons.text.ANSI.Formatter
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.text.Unicode.ESC
import com.bkahlert.kommons.tracing.Event
import com.bkahlert.kommons.tracing.SpanScope
import com.bkahlert.kommons.tracing.rendering.ColumnsLayout
import com.bkahlert.kommons.tracing.rendering.Style
import com.bkahlert.kommons.tracing.rendering.Styles
import com.bkahlert.kommons.tracing.runSpanning
import com.bkahlert.kustomize.cli.Layouts
import com.bkahlert.kustomize.cli.PATCH_INNER_DECORATION_FORMATTER

/**
 * A virtualized Raspberry Pi inside a Docker image.
 *
 * [lukechilds/dockerpi](https://hub.docker.com/r/lukechilds/dockerpi)
 */
@Suppress("SpellCheckingInspection")
object DockerPiImage : DockerImage("lukechilds", listOf("dockerpi"), tag = "vm")

/**
 * Boots this [OperatingSystem].
 */
fun OperatingSystemImage.boot(
    name: String? = null,
    nameFormatter: Formatter<CharSequence> = Formatter.ToCharSequence,
    decorationFormatter: Formatter<CharSequence> = PATCH_INNER_DECORATION_FORMATTER,
    style: (ColumnsLayout, Int) -> Style = Styles.Solid,
): State = DockerRunCommandLine(
    image = DockerPiImage,
    options = Options(
        name = DockerContainer.from(name ?: DockerContainer.from(file).name),
        mounts = MountOptions { this@boot.file mountAt "/sdcard/filesystem.img" }
    ),
    executable = CommandLine(""),
).exec.processing { _: DockerExec, block: ((IO) -> Unit) -> ExitState ->
    var stuck = false

    runSpanning(
        "Booting ${shortName.formattedAs.input}",
        nameFormatter = nameFormatter,
        decorationFormatter = decorationFormatter,
        layout = Layouts.DESCRIPTION,
        style = style,
        block = {
            block { io ->
                if (!stuck && io.ansiRemoved != "${ESC}M") {
                    stuck = this@boot.deadEndPattern?.matches(io.ansiRemoved) == true
                    if (stuck) {
                        negativeFeedback("The VM is stuck. Chances are the VM starts correctly with less load on this machine.")
                        throw IllegalStateException(io.ansiRemoved)
                    } else {
                        when (io) {
                            is Meta -> feedback(io.ansiRemoved.trim())
                            is Input -> event(io as Event)
                            is Output -> event(io as Event)
                            is Error -> negativeFeedback(io.ansiRemoved.trim())
                        }
                    }
                }
            }
        }
    )
}.waitFor()


/**
 * Prints [message] on the output without actually forwarding it
 * to the OS running process.
 */
private fun SpanScope.feedback(
    message: String,
    kaomoji: Kaomoji = Kaomoji.random(Kaomoji.Happy, Kaomoji.PeaceSign, Kaomoji.Smiling, Kaomoji.ThumbsUp, Kaomoji.Proud),
) {
    log(LF + kaomoji.thinking(message.formattedAs.success) + LF)
}

/**
 * Prints [message] on the output without actually forwarding it
 * to the OS running process.
 */
private fun SpanScope.negativeFeedback(
    message: String,
    kaomoji: Kaomoji = Kaomoji.random(Kaomoji.Crying, Kaomoji.Depressed, Kaomoji.Disappointed, Kaomoji.Sad, Kaomoji.Screaming).random(),
) {
    if (listOf(
            "WARNING: Image format was not specified for '/sdcard/filesystem.img' and probing guessed raw.",
            "Automatically detecting the format is dangerous for raw images, write operations on block 0 will be restricted.",
            "Specify the 'raw' format explicitly to remove the restrictions.",
        ).any { message.contains(it, ignoreCase = true) }
    ) return
    log(LF + kaomoji.thinking(message.formattedAs.error) + LF)
}

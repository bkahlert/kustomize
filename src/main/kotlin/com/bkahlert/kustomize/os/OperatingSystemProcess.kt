package com.bkahlert.kustomize.os

import com.bkahlert.kustomize.cli.Layouts
import com.bkahlert.kustomize.cli.PATCH_INNER_DECORATION_FORMATTER
import koodies.docker.DockerContainer
import koodies.docker.DockerExec
import koodies.docker.DockerImage
import koodies.docker.DockerRunCommandLine
import koodies.docker.DockerRunCommandLine.Options
import koodies.docker.MountOptions
import koodies.exec.CommandLine
import koodies.exec.IO
import koodies.exec.IO.Error
import koodies.exec.IO.Input
import koodies.exec.IO.Meta
import koodies.exec.IO.Output
import koodies.exec.Process.ExitState
import koodies.exec.Process.State
import koodies.kaomoji.Kaomoji
import koodies.text.ANSI.Formatter
import koodies.text.LineSeparators.LF
import koodies.text.Semantics.formattedAs
import koodies.text.Unicode.ESC
import koodies.tracing.CurrentSpan
import koodies.tracing.Event
import koodies.tracing.rendering.ColumnsLayout
import koodies.tracing.rendering.Style
import koodies.tracing.rendering.Styles
import koodies.tracing.spanning

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

    spanning(
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
private fun CurrentSpan.feedback(
    message: String,
    kaomoji: Kaomoji = Kaomoji.random(Kaomoji.Happy, Kaomoji.PeaceSign, Kaomoji.Smiling, Kaomoji.ThumbsUp, Kaomoji.Proud),
) {
    log(LF + kaomoji.thinking(message.formattedAs.success) + LF)
}

/**
 * Prints [message] on the output without actually forwarding it
 * to the OS running process.
 */
private fun CurrentSpan.negativeFeedback(
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

package com.imgcstmzr.cli

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import com.imgcstmzr.util.Disk
import com.imgcstmzr.util.flash
import koodies.text.Banner
import koodies.text.Semantics.formattedAs
import koodies.tracing.spanning
import java.nio.file.Path

// TODO test; actually completely untested since moved to separate command
class FlashCommand : NoOpCliktCommand(
    name = "flash",
    allowMultipleSubcommands = true,
    printHelpOnEmptyArgs = true,
    help = "Downloads and Customizes Raspberry Pi Images"
) {

    init {
        context {
            autoEnvvarPrefix = "IMGCSTMZR"
            helpFormatter = ColorHelpFormatter()
        }
    }

    private val image: Path by option("--image")
        .path(mustBeReadable = true, canBeDir = false)
        .required()

    private val disk: String? by option("--disk")

    override fun run() {

        spanning("Flashing $image", nameFormatter = { Banner.banner(it, prefix = "") }) {
            val flashed: Disk? = flash(image, disk)
            if (flashed != null) {
                log("Successfully flashed ${image.toUri().formattedAs.input} to ${flashed.formattedAs.input}.")
            } else {
                if (disk != null) {
                    log("Failed to flash ${image.toUri().formattedAs.input}. Try to set ${"disk".formattedAs.input} explicitly.")
                } else {
                    log("Failed to flash ${image.toUri().formattedAs.input} to ${disk.formattedAs.input}.")
                }
            }
            flashed
        }
    }
}

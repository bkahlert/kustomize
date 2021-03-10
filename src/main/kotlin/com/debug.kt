package com

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import com.imgcstmzr.cli.Options.os
import com.imgcstmzr.libguestfs.LibguestfsCommandLine
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.util.Downloader
import koodies.docker.DockerRunCommandLine
import koodies.io.path.Locations
import koodies.io.path.asString
import koodies.logging.BlockRenderingLogger
import koodies.logging.applyLogging
import koodies.terminal.ANSI
import koodies.terminal.colorize
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.moveTo
import kotlin.reflect.KClass

fun main() {
    DebugCommand().main(listOf(Locations.HomeDirectory.resolve(".imgcstmzr.test/guestfish").asString()))
}


class DebugCommand : NoOpCliktCommand(
    epilog = "((ε(*･ω･)_/${ANSI.termColors.colorize("ﾟ･.*･･｡☆")}",
    name = "imgcstmzr",
    allowMultipleSubcommands = false,
    printHelpOnEmptyArgs = true,
    help = "Downloads and Customizes Raspberry Pi Images"
) {

    private val os by option().os().default(OperatingSystems.RaspberryPiLite)

    private val path: Path by argument("dir", help = "where the image and script should be copied to")
        .path(mustExist = false,
            canBeDir = true,
            mustBeReadable = false)

    private val shared by findOrSetObject { mutableMapOf<KClass<*>, Any>() }

    override fun run() {
        BlockRenderingLogger("ImgCstmzr").applyLogging {
            logLine { "Getting OS copy" }
            val tmp = Downloader().download(os.downloadUrl, logger = this)
            logLine { "Downloaded to $tmp" }
            val imgFile = path.createDirectories().resolve(tmp.fileName)
            tmp.moveTo(imgFile, overwrite = true)
            logLine { "Moved to $imgFile" }
            val script = """
                    docker run --rm -v ${'$'}(PWD):/shared --entrypoint /usr/bin/guestfish -it bkahlert/libguestfs
                """.trimIndent()

            logLine { script }

            val z = DockerRunCommandLine {
                image by LibguestfsCommandLine.DOCKER_IMAGE
                options {
                    name { "guestfish" }
                    autoCleanup { off }
                    mounts {
                        path.resolve(path.fileName) mountAt "/images/disk.img"
                        path.resolve("shared") mountAt "/shared"
                    }
                }
                commandLine {
                    redirects { +"2>&1" } // needed since some commandrvf writes all output to stderr
                    arguments {
                        +"--rw"
                        +"--add /images/disk.img"
                        +"--mount /dev/sda2:/"
                        +"--mount /dev/sda1:/boot"
                    }
                }
            }
            logLine { z.toString() }
        }
    }
}

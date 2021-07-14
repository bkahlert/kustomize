package com

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import com.imgcstmzr.ImgCstmzr
import com.imgcstmzr.cli.Options.os
import com.imgcstmzr.libguestfs.LibguestfsImage
import com.imgcstmzr.os.OperatingSystems
import com.imgcstmzr.util.Downloader
import koodies.docker.DockerContainer
import koodies.docker.DockerRunCommandLine
import koodies.docker.DockerRunCommandLine.Options
import koodies.docker.MountOptions
import koodies.exec.CommandLine
import koodies.io.path.pathString
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.colorize
import koodies.tracing.spanning
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.moveTo
import kotlin.reflect.KClass

fun main() {
    DebugCommand().main(listOf(ImgCstmzr.HomeDirectory.resolve(".imgcstmzr.test/guestfish").pathString))
}


class DebugCommand : NoOpCliktCommand(
    epilog = "((ε(*･ω･)_/${"ﾟ･.*･･｡☆".ansi.colorize()}",
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
        spanning("ImgCstmzr") {
            log("Getting OS copy")
            val tmp = Downloader().download(os.downloadUrl)
            log("Downloaded to $tmp")
            val imgFile = path.createDirectories().resolve(tmp.fileName)
            tmp.moveTo(imgFile, overwrite = true)
            log("Moved to $imgFile")
            val script = """
                    docker run --rm -v ${'$'}(PWD):/shared --entrypoint /usr/bin/guestfish -it bkahlert/libguestfs
                """.trimIndent()

            log(script)

            val z = DockerRunCommandLine(
                image = LibguestfsImage,
                options = Options(
                    name = DockerContainer.from("guestfish"),
                    autoCleanup = false,
                    mounts = MountOptions {
                        path.resolve(path.fileName) mountAt "/images/disk.img"
                        path.resolve("shared") mountAt "/shared"
                    },
                ),
                executable = CommandLine(
                    "--rw",
                    "--add /images/disk.img",
                    "--mount /dev/sda2:/",
                    "--mount /dev/sda1:/boot",
                ),
            )
            log(z.toString())
        }
    }
}

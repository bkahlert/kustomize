package com.imgcstmzr.process

import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.cli.ColorHelpFormatter.Companion.tc
import com.imgcstmzr.process.OutputType.ERR
import com.imgcstmzr.util.fileNameWithExtension
import com.imgcstmzr.util.hereDoc
import com.imgcstmzr.util.quote
import com.imgcstmzr.util.replaceNonPrintableCharacters
import java.nio.file.Path

/**
 * API for easy usage of [guestfish](https://libguestfs.org/guestfish.1.html)—"the guest filesystem shell".
 *
 * Instances use [containerName] for the underlying Docker container (any existing container gets removed).
 * [img] will be mounted before running any command.
 */
class Guestfish(
    /**
     * Name to be used for the underlying Docker container. If a container with the same name exists, it will be stopped and removed.
     */
    private val containerName: String,

    /**
     * [Path] of the [img] this [Guestfish] mounts before running any command.
     */
    private val img: Path,

    private val dryRun: Boolean = false,
) {
    /**
     * Name of the directory used to exchange files between this host and the OS running inside Docker.
     */
    private val sharedDirectoryName: String = img.fileNameWithExtension(HOST_DIR_EXTENSION)

    /**
     * [Path] on this host containing the [img].
     */
    private val hostMountRoot: Path = img.parent

    /**
     * [Path] on this host mapped to the OS running inside Docker.
     */
    private val hostMountShared = hostMountRoot.resolve(sharedDirectoryName)

    /**
     * [Path] on the OS running inside Docker mapped to this host.
     */
    private val dockerMountShared = dockerMountRoot.resolve(sharedDirectoryName)

    /**
     * [Path] on the OS running inside Docker mapped to the [img].
     */
    private val dockerMountImg = dockerMountRoot.resolve(img.fileName)

    /**
     * Constructs a command that starts a Guestfish Docker container with the given [img] mounted and mapped.
     *
     * The command stops and removes any existing Guestfish Docker container with the same [containerName].
     */
    private fun imgMountingDockerCmd(vararg appArguments: String): String {
        val volumes = mapOf(
            img to dockerMountImg,
            hostPath() to dockerPath(),
        )

//        return dockerCmd(containerName, volumes, "--rw", "--add ${dockerMountImg.quote()}", "--mount /dev/sda1:/boot", "--inspector", *appArguments)
        return dockerCmd(containerName, volumes, "--rw", "--add ${dockerMountImg.quote()}", "--mount /dev/sda2:/", "--mount /dev/sda1:/boot", *appArguments)
    }

    /**
     * Constructs a command that starts a Guestfish Docker container that runs the given [commands] on [img].
     *
     * The command stops and removes any existing Guestfish Docker container with the same [containerName].
     */
    private fun commandApplyingDockerCmd(commands: List<String>): String = imgMountingDockerCmd(hereDoc(commands.plus("umount-all").plus("quit")))

    /**
     * Maps a [guestPath] (e.g. `/boot/config.txt`) which is valid inside of [img] to a corresponding path on this host (e.g. `~/.imgcstmzr/project.guestfish/boot/config.txt`).
     */
    fun hostPath(guestPath: Path? = null): Path =
        if (guestPath == null) hostMountShared
        else hostMountShared.resolve(guestPath.subpath(0, guestPath.nameCount))

    /**
     * Maps a [guestPath] (e.g. `/boot/config.txt`) which is valid inside of [img] to a corresponding path on the dockerized host (e.g. `/root/project.guestfish/boot/config.txt`).
     */
    fun dockerPath(guestPath: Path? = null): Path =
        if (guestPath == null) dockerMountShared
        else dockerMountShared.resolve(guestPath.subpath(0, guestPath.nameCount))

    /**
     * Creates the commands needed to copy the [guestPaths] to this host.
     */
    private fun copyOutCommands(guestPaths: List<Path>): List<String> =
        guestPaths.flatMap { sourcePath ->
            val destDir = dockerPath(sourcePath).parent
            listOf("!mkdir -p $destDir", "copy-out $sourcePath $destDir")
        }

    /**
     * Creates the commands needed to copy the [guestPaths] to this guest.
     */
    private fun copyInCommands(guestPaths: List<Path>): List<String> =
        guestPaths.flatMap { guestPath ->
            val sourcePath = dockerPath(guestPath)
            val destDir = guestPath.parent
            listOf("mkdir-p $destDir", "copy-in $sourcePath $destDir")
        }

    /**
     * Runs the given [commands] on a [Guestfish] instance with [img] mounted.
     */
    private fun run(commands: List<String>): Int {
        val cmd = commandApplyingDockerCmd(commands)
        echo(tc.yellow(cmd.replaceNonPrintableCharacters()))
        return runProcess(cmd) {
            if (it.type == ERR) echo(tc.red(it.unformatted))
        }.blockExitCode
    }

    /**
     * Copies the given [guestPaths] to this host.
     * @return path that contains the copied resources with the directory structure preserved
     */
    fun copyFromGuest(guestPaths: List<Path>): Path {
        echo("Reading ${guestPaths.size} resources from ${img.fileName}. This takes a moment...")

        val commands = copyOutCommands(guestPaths)
        check(run(commands) == 0) { "An error occurred while copying ${guestPaths.size} files from $img" }
        return hostPath().also { echo(tc.green("Files saved to $it.")) }
    }

    /**
     * Copies the given [guestPaths] residing in this hosts [hostMountShared] / this Docker's [dockerMountShared] directory to the guest.
     * @return path that contains the copies resources with the directory structure preserved
     */
    fun copyToGuest(guestPaths: List<Path>): Guestfish {
        echo("Writing ${guestPaths.size} resources to ${img.fileName}. This takes a moment...")

        val commands = copyInCommands(guestPaths)
        check(run(commands) == 0) { "An error occurred while copying ${guestPaths.size} files to $img" }
        echo(tc.green("Files saved to $img."))
        return this
    }

    companion object {
        private const val HOST_DIR_EXTENSION: String = "guestfish"
        private val dockerMountRoot = Path.of("/root")

        /**
         * Constructs a command that starts a Guestfish Docker container with the given [volumes] mapped.
         *
         * The command stops and removes any existing Guestfish Docker container with the same [containerName].
         */
        private fun dockerCmd(containerName: String, volumes: Map<Path, Path> = emptyMap(), vararg appArguments: String): String {
            val volumeArguments = volumes.map { "--volume \"${it.key}\":\"${it.value}\"" }.toTypedArray()
            val dockerRun = arrayOf(
                "docker", "run", "--name", "\"$containerName\"", "--rm", "-i", *volumeArguments, "curator/guestfish", *appArguments
            ).joinToString(" ")
            return "$(docker rm --force \"$containerName\" 1>/dev/null 2>&1); $dockerRun"
        }

        /**
         * Executes the given [commands] on a [Guestfish] instance with [img] mounted.
         */
        fun execute(containerName: String, volumes: Map<Path, Path>, vararg commands: String): Int {
            val cmd = dockerCmd(containerName, volumes, appArguments = arrayOf("--", hereDoc(commands.toList().plus("umount-all").plus("quit"))))
            echo(tc.yellow(cmd.replaceNonPrintableCharacters()))
            return runProcess(cmd) { if (it.type == ERR) echo(tc.red(it.unformatted)) }.blockExitCode
        }
    }
}


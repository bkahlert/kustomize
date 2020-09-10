package com.imgcstmzr.process

import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.asRootFor
import com.imgcstmzr.util.hereDoc
import com.imgcstmzr.util.quote
import com.imgcstmzr.util.splitLineBreaks
import com.imgcstmzr.util.withExtension
import java.nio.file.Path

/**
 * API for easy usage of [guestfish](https://libguestfs.org/guestfish.1.html)—"the guest filesystem shell".
 *
 * Instances use [containerName] for the underlying Docker container (any existing container gets removed).
 * [imgPathOnHost] will be mounted before running any command.
 */
class Guestfish(
    /**
     * [Path] of the [imgPathOnHost] this [Guestfish] mounts before running any command.
     */
    private val imgPathOnHost: Path,

    /**
     * Name to be used for the underlying Docker container. If a container with the same name exists, it will be stopped and removed.
     */
    private val containerName: String = imgPathOnHost.fileName.toString(),
) {

    /**
     * [Path] on this host mapped to the OS running inside Docker.
     */
    val guestRootOnHost: Path = guestRootOnHost(imgPathOnHost)

    /**
     * [Path] on the OS running inside Docker mapped to the [imgPathOnHost].
     */
    private val imgPathOnDocker = DOCKER_MOUNT_ROOT.resolve(imgPathOnHost.fileName)

    /**
     * Constructs a command that starts a Guestfish Docker container with the given [imgPathOnHost] mounted and mapped.
     *
     * The command stops and removes any existing Guestfish Docker container with the same [containerName].
     */
    private fun imgMountingDockerCmd(vararg appArguments: String): String {
        val volumes = mapOf(
            imgPathOnHost to imgPathOnDocker,
            guestRootOnHost to GUEST_ROOT_ON_DOCKER,
        )

        return dockerCmd(
            containerName,
            volumes,
            "--rw",
            "--add ${imgPathOnDocker.quote()}",
            "--mount /dev/sda2:/",
            "--mount /dev/sda1:/boot",
            *appArguments,
        )
    }

    /**
     * Constructs a command that starts a Guestfish Docker container that runs the given [commands] on [imgPathOnHost].
     *
     * The command stops and removes any existing Guestfish Docker container with the same [containerName].
     */
    private fun commandApplyingDockerCmd(commands: List<String>): String = imgMountingDockerCmd(hereDoc(commands.plus("umount-all").plus("quit")))

    /**
     * Runs the given [commands] on a [Guestfish] instance with [imgPathOnHost] mounted.
     */
    fun run(commands: List<String>): Int {
        echo("Running ${commands.size} commands inside ${imgPathOnHost.fileName}. This takes a moment...")
        val cmd = commandApplyingDockerCmd(commands)
        val exitCode = runProcess(cmd, processor = Output.LOGGING_PROCESSOR).blockExitCode
        check(exitCode == 0) { "An error while running the following command inside $imgPathOnHost:\n$cmd" }
        return exitCode
    }

    companion object {
        private const val IMAGE_NAME: String = "curator/guestfish"
        val DOCKER_MOUNT_ROOT: Path = Path.of("/root")
        val GUEST_MOUNT_ROOT: Path = Path.of("/")
        private const val SHARED_DIRECTORY_NAME: String = "guestfish.shared"

        /**
         * [Path] on the OS running inside Docker mapped to this host.
         */
        private val GUEST_ROOT_ON_DOCKER = DOCKER_MOUNT_ROOT.resolve(SHARED_DIRECTORY_NAME)

        /**
         * Given an [img] location returns the [Path] which is used to exchange data with [img] mounted using [Guestfish].
         */
        private fun guestRootOnHost(img: Path) = img.parent.resolve(SHARED_DIRECTORY_NAME)!!

        /**
         * Creates the commands needed to copy the [guestPaths] to this host.
         */
        fun copyOutCommands(guestPaths: List<Path>): List<String> =
            guestPaths.flatMap { sourcePath ->
                val sanitizedSourcePath = Path.of("/").asRootFor(sourcePath)
                val destDir = GUEST_ROOT_ON_DOCKER.asRootFor(sourcePath).parent
                listOf("!mkdir -p $destDir", "- copy-out $sanitizedSourcePath $destDir")
            }

        /**
         * Creates the commands needed to copy the [hostPaths] to this guest.
         */
        fun copyInCommands(hostPaths: List<Path>): List<String> {
            return hostPaths.flatMap { hostPath ->
                val relativeHostPath = if (hostPath.contains(SHARED_DIRECTORY_NAME)) {
                    hostPath.toList().dropWhile { it.toString() == SHARED_DIRECTORY_NAME }.drop(1).let { Paths.of(it) }
                } else {
                    hostPath
                }
                val sourcePath = GUEST_ROOT_ON_DOCKER.asRootFor(relativeHostPath)
                val destPath = GUEST_MOUNT_ROOT.asRootFor(relativeHostPath.parent)
                listOf("copy-in $sourcePath $destPath")
            }
        }

        /**
         * Creates the commands needed to change the password of [username] to [password].
         */
        fun changePasswordCommand(username: String, password: String, salt: String): List<String> {
            val shadowPath = Path.of("/etc/shadow")
            val bakExtension = "bak"
            val shadowBackup = shadowPath.withExtension(bakExtension)
            return """
            copy-out $shadowPath $DOCKER_MOUNT_ROOT
            ! perl -i.$bakExtension -pe 's|(?<=$username:)[^:]*|crypt("$password","\\\\\\\${'$'}6\\\\\\\${'$'}$salt\\\\\\\${'$'}")|e' $DOCKER_MOUNT_ROOT/shadow
            copy-in $DOCKER_MOUNT_ROOT/shadow $DOCKER_MOUNT_ROOT/shadow.$bakExtension /etc
        """.trimIndent().splitLineBreaks() + copyOutCommands(listOf(shadowPath, shadowBackup))
        }

        /**
         * Constructs a command that starts a Guestfish Docker container with the given [volumes] mapped.
         *
         * The command stops and removes any existing Guestfish Docker container with the same [containerName].
         *
         * TODO Use cmattoon/guestfish (/work instead of /root)
         */
        private fun dockerCmd(containerName: String, volumes: Map<Path, Path> = emptyMap(), vararg appArguments: String): String {
            val volumeArguments = volumes.map { "--volume \"${it.key}\":\"${it.value}\"" }.toTypedArray()
            val dockerRun = arrayOf(
                "docker", "run", "--name", "\"$containerName\"", "--rm", "-i", *volumeArguments, IMAGE_NAME, *appArguments
            ).joinToString(" ")
            return "docker rm --force \"$containerName\" &> /dev/null ; $dockerRun"
        }

        /**
         * Executes the given [commands] on a [Guestfish] instance with just [volumes] mounted.
         */
        fun execute(containerName: String, volumes: Map<Path, Path>, vararg commands: String): Int {
            val cmd = dockerCmd(containerName, volumes, appArguments = arrayOf("--", hereDoc(commands.toList().plus("umount-all").plus("quit"))))
//            val exitCode = runProcess(cmd, processor = Output.LOGGING_PROCESSOR).blockExitCode TODO
            val exitCode = runProcess(cmd, processor = Output.LOGGING_PROCESSOR).blockExitCode
            check(exitCode == 0) { "An error while running the following command inside $containerName:\n$cmd" }
            return exitCode
        }
    }
}


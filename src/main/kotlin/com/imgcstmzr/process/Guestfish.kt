package com.imgcstmzr.process

import com.bkahlert.koodies.string.match
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.terminal.ansi.Style.Companion.bold
import com.bkahlert.koodies.terminal.ascii.wrapWithBorder
import com.github.ajalt.clikt.output.TermUi
import com.imgcstmzr.process.Exec.Sync.execShellScript
import com.imgcstmzr.process.Output.Type.ERR
import com.imgcstmzr.process.Output.Type.META
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.runtime.OperatingSystems.Companion.Credentials
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.miniSegment
import com.imgcstmzr.runtime.log.segment
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.asRootFor
import com.imgcstmzr.util.exists
import com.imgcstmzr.util.quoted
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

    private val logger: BlockRenderingLogger<Any>,

    /**
     * Name to be used for the underlying Docker container. If a container with the same name exists, it will be stopped and removed.
     */
    private val containerName: String = imgPathOnHost.fileName.toString(),

    private val debug: Boolean = false,
) {
    fun withRandomSuffix(): Guestfish = Guestfish(imgPathOnHost, logger, imgPathOnHost.fileName.toString() + "-" + String.random(4))

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
            imgPathOnHost.parent.resolve(SHARED_DIRECTORY_NAME) to GUEST_ROOT_ON_DOCKER,
        )

        return dockerCmd(
            containerName,
            volumes,
            "--rw",
            "--add $imgPathOnDocker",
            "--mount /dev/sda2:/",
            "--mount /dev/sda1:/boot",
            *appArguments,
        )
    }

    /**
     * Constructs a command that starts a Guestfish Docker container that runs the given [guestfishOperation] on [imgPathOnHost].
     *
     * The command stops and removes any existing Guestfish Docker container with the same [containerName].
     */
    private fun commandApplyingDockerCmd(guestfishOperation: GuestfishOperation): String =
        imgMountingDockerCmd((guestfishOperation + shutdownOperation).asHereDoc())

    /**
     * Runs the given [guestfishOperation] on a [Guestfish] instance with [imgPathOnHost] mounted.
     */
    fun run(guestfishOperation: GuestfishOperation) {
        if (guestfishOperation.commandCount == 0) {
            logger.logLine(META typed "No commands specified to run inside ${imgPathOnHost.fileName}. Skipping.")
            return
        }

        val caption = "Running ${imgPathOnHost.fileName} ${guestfishOperation.summary} "
        val block: RenderingLogger<Unit>.() -> Unit = {
            val command = commandApplyingDockerCmd(guestfishOperation)
            require(imgPathOnHost.exists) { "imgcstmzr.img".wrapWithBorder(padding = 20, margin = 20) }
            val exitCode = execShellScript(workingDirectory = imgPathOnHost.parent,
                lines = arrayOf(command),
                outputProcessor = { output ->
                    if (debug || output.type == ERR) logLine(output)
                })
            check(exitCode == 0) {
                "An error while running the following command inside $imgPathOnHost:\n$command\n" +
                    "To debug you could try: docker exec -it ${imgPathOnHost.fileName} bash".bold()
            }
            guestfishOperation.commands.map { it.match("""! perl -i.{} -pe 's|(?<={}:)[^:]*|crypt("{}","\\\${'$'}6\\\${'$'}{}\\\${'$'}")|e' {}/shadow""") }
                .filter { it.size > 2 }
                .forEach {
                    val credentials = Credentials(it[1], it[2])
                    logLine(META typed "Password of user ${credentials.username.quoted} updated.")
                    OperatingSystems.credentials[imgPathOnHost] = credentials
                }
            0
        }
        if (debug) logger.segment(caption = caption, ansiCode = null, block = block)
        else logger.miniSegment(caption = caption, block = block)
    }

    fun copyOut(guestPathAsString: String): Path {
        val guestPath = Path.of(guestPathAsString)
        run(copyOutCommands(listOf(guestPath)))
        return guestRootOnHost.asRootFor(guestPath)
    }

    companion object {
        private const val IMAGE_NAME: String = "cmattoon/guestfish"//"curator/guestfish"
        val DOCKER_MOUNT_ROOT: Path = Path.of("/work")///root")
        val GUEST_MOUNT_ROOT: Path = Path.of("/")
        const val SHARED_DIRECTORY_NAME: String = "guestfish.shared"
        val shutdownOperation = GuestfishOperation("umount-all", "quit")

        /**
         * [Path] on the OS running inside Docker mapped to this host.
         */
        private val GUEST_ROOT_ON_DOCKER = DOCKER_MOUNT_ROOT.resolve(SHARED_DIRECTORY_NAME)

        /**
         * Given an [img] location returns the [Path] which is used to exchange data with [img] mounted using [Guestfish].
         */
        private fun guestRootOnHost(img: Path) = img.parent.resolve(SHARED_DIRECTORY_NAME)

        /**
         * Creates the commands needed to copy the [guestPaths] to this host.
         */
        fun copyOutCommands(guestPaths: List<Path>): GuestfishOperation =
            GuestfishOperation(guestPaths.flatMap { sourcePath ->
                val sanitizedSourcePath = Path.of("/").asRootFor(sourcePath)
                val destDir = GUEST_ROOT_ON_DOCKER.asRootFor(sourcePath).parent
                listOf("!mkdir -p $destDir", "- copy-out $sanitizedSourcePath $destDir")
            })

        /**
         * Creates the commands needed to copy the [hostPaths] to this guest.
         *
         * @see <a href="https://unix.stackexchange.com/questions/81240/manually-generate-password-for-etc-shadow">Manually generate password for /etc/shadow</a>
         */
        fun copyInCommands(hostPaths: List<Path>): GuestfishOperation =
            GuestfishOperation(hostPaths.flatMap { hostPath ->
                val relativeHostPath = if (hostPath.map { toString() }.contains(SHARED_DIRECTORY_NAME)) {
                    hostPath.toList().dropWhile { it.toString() == SHARED_DIRECTORY_NAME }.drop(1).let { Paths.of(it) }
                } else {
                    hostPath
                }
                val sourcePath = GUEST_ROOT_ON_DOCKER.asRootFor(relativeHostPath)
                val destPath = GUEST_MOUNT_ROOT.asRootFor(relativeHostPath.parent)
                listOf("copy-in $sourcePath $destPath")
            })

        /**
         * Creates the commands needed to change the password of [username] to [password].
         */
        fun changePasswordCommand(username: String, password: String, salt: String): GuestfishOperation {
            val shadowPath = Path.of("/etc/shadow")
            val bakExtension = "bak"
            val shadowBackup = shadowPath.withExtension(bakExtension)
            return GuestfishOperation("""
                    copy-out $shadowPath $DOCKER_MOUNT_ROOT
                    ! perl -i.$bakExtension -pe 's|(?<=$username:)[^:]*|crypt("$password","\\\${'$'}6\\\${'$'}$salt\\\${'$'}")|e' $DOCKER_MOUNT_ROOT/shadow
                    copy-in $DOCKER_MOUNT_ROOT/shadow $DOCKER_MOUNT_ROOT/shadow.$bakExtension /etc
                """.trimIndent().lines()) + copyOutCommands(listOf(shadowPath, shadowBackup))
        }

        /**
         * Constructs a command that starts a Guestfish Docker container with the given [volumes] mapped.
         *
         * The command stops and removes any existing Guestfish Docker container with the same [containerName].
         */
        private fun dockerCmd(containerName: String, volumes: Map<Path, Path> = emptyMap(), vararg appArguments: String): String {
            val volumeArguments = volumes.map { "--volume ${it.key}:${it.value}" }.toTypedArray()
            val dockerRun = arrayOf(
                "docker 2>&1", "run", "--name", "\"$containerName\"", "--rm", "-i", *volumeArguments, IMAGE_NAME, "\$CMD", *appArguments
            ).joinToString(" ")
            return "docker rm --force \"$containerName\" &> /dev/null ; $dockerRun"
        }

        /**
         * Executes the given [operation] on a [Guestfish] instance with just [volumes] mounted.
         */
        fun execute(containerName: String, volumes: Map<Path, Path>, operation: GuestfishOperation, debug: Boolean = false): Int {
            val cmd = dockerCmd(containerName, volumes, appArguments = arrayOf("--", (operation + shutdownOperation).asHereDoc()))
            val exitCode = runProcess(cmd, processor = { output ->
                if (debug || output.type == ERR) TermUi.echo(output)
            }).blockExitCode
            check(exitCode == 0) { "An error while running the following command inside $containerName:\n$cmd" }
            return exitCode
        }
    }
}


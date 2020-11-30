package com.imgcstmzr.guestfish

import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.LoggedProcess
import com.bkahlert.koodies.docker.DockerContainerName
import com.bkahlert.koodies.docker.DockerContainerName.Companion.toContainerName
import com.bkahlert.koodies.docker.DockerImage
import com.bkahlert.koodies.docker.DockerImageBuilder
import com.bkahlert.koodies.docker.DockerProcess
import com.bkahlert.koodies.docker.DockerRunCommandBuilder.Companion.buildRunCommand
import com.bkahlert.koodies.io.TarArchiver.tar
import com.bkahlert.koodies.kaomoji.Kaomojis
import com.bkahlert.koodies.nio.file.exists
import com.bkahlert.koodies.nio.file.withExtension
import com.bkahlert.koodies.string.match
import com.bkahlert.koodies.string.quoted
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.terminal.ascii.wrapWithBorder
import com.imgcstmzr.runtime.OperatingSystem.Credentials
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.fileLogger
import com.imgcstmzr.runtime.log.subLogger
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.asRootFor
import com.imgcstmzr.util.moveTo
import java.nio.file.Path

/**
 * API for easy usage of [guestfish](https://libguestfs.org/guestfish.1.html)—"the guest filesystem shell".
 *
 * Instances use [containerName] for the underlying Docker container (any existing container gets removed).
 * [imgPathOnHost] will be mounted before running any command.
 *
 * ## Developer Note
 * The following command starts Guestfish on a proper command line interactively:
 * ```bash
 * docker run --rm -v $(PWD):/work --entrypoint /usr/bin/guestfish -it cmattoon/guestfish
 * docker run --rm -v $(PWD):/work --entrypoint /usr/bin/guestfish -it cmattoon/guestfish -N /work/my.img=bootroot:vfat:ext4:6M:3M:mbr
 * ```
 *
 * For even more freedom the next command starts the bash:
 * ```bash
 * docker run --rm -v $(PWD):/work --entrypoint /bin/bash -it cmattoon/guestfish
 * ```
 */
class Guestfish(
    /**
     * [Path] of the [imgPathOnHost] this [Guestfish] mounts before running any command.
     */
    private val imgPathOnHost: OperatingSystemImage,

    private val logger: RenderingLogger<Any>,

    /**
     * Name to be used for the underlying Docker container. If a container with the same name exists, it will be stopped and removed.
     */
    private val containerName: DockerContainerName = imgPathOnHost.fileName.toContainerName(),

    private val debug: Boolean = false,
) {
    fun withRandomSuffix(): Guestfish = Guestfish(imgPathOnHost, logger, DockerContainerName(imgPathOnHost.fileName + "-" + String.random(4)))

    /**
     * [Path] on this host mapped to the OS running inside Docker.
     */
    val guestRootOnHost: Path = guestRootOnHost(imgPathOnHost)

    /**
     * [Path] on the OS running inside Docker mapped to the [imgPathOnHost].
     */
    private val imgPathOnDocker = DOCKER_MOUNT_ROOT.resolve(imgPathOnHost.fileName)

    /**
     * Runs the given [guestfishOperation] on a [Guestfish] instance with [imgPathOnHost] mounted.
     */
    fun run(guestfishOperation: GuestfishOperation) {
        if (guestfishOperation.commandCount == 0) {
            logger.logStatus { META typed "No commands specified to run inside ${imgPathOnHost.fileName}. Skipping." }
            return
        }

        val caption = "Running ${imgPathOnHost.fileName} ${guestfishOperation.summary} "
        val block: RenderingLogger<Unit>.() -> Unit = {
            require(imgPathOnHost.readable) { "Error running Guestfish: $imgPathOnHost can't be read.".wrapWithBorder() }

            val workingDirectory = imgPathOnHost.directory

            DockerProcess(
                command = IMAGE.buildRunCommand {
                    redirects { +"2>&1" } // needed since some commandrvf writes all output to stderr
                    options {
                        containerName { containerName }
                        autoCleanup { false }
                        volumes {
                            workingDirectory.resolve(imgPathOnHost.fileName) to imgPathOnDocker
                            workingDirectory.resolve(SHARED_DIRECTORY_NAME) to GUEST_ROOT_ON_DOCKER
                        }
                    }
                    args(listOf(
                        "--rw",
                        "--add $imgPathOnDocker",
                        "--mount /dev/sda2:/",
                        "--mount /dev/sda1:/boot",
                        (guestfishOperation + shutdownOperation).asHereDoc()))
                },
                workingDirectory = workingDirectory,
                ioProcessor = GuestfishIoProcessor(this, verbose = false),
            ).waitForSuccess()

            guestfishOperation.commands.map { it.match("""! perl -i.{} -pe 's|(?<={}:)[^:]*|crypt("{}","\\\${'$'}6\\\${'$'}{}\\\${'$'}")|e' {}/shadow""") }
                .filter { it.size > 2 }
                .forEach {
                    val credentials = Credentials(it[1], it[2])
                    logStatus { META typed "Password of user ${credentials.username.quoted} updated." }
                    imgPathOnHost.credentials = credentials
                }
        }
        if (debug) logger.subLogger(caption = caption, ansiCode = null, block = block)
        else logger.fileLogger(path = imgPathOnHost.newLogFilePath(), caption = caption, block = block)
    }

    fun copyOut(guestPathAsString: String): Path {
        val guestPath = Path.of(guestPathAsString)
        run(copyOutCommands(listOf(guestPath)))
        return guestRootOnHost.asRootFor(guestPath)
    }

    fun tarIn(): Unit = run(tarInCommands(imgPathOnHost))

    companion object {
        @Suppress("SpellCheckingInspection") val IMAGE: DockerImage = DockerImageBuilder.build { "cmattoon" / "guestfish" } //"curator/guestfish"
        val DOCKER_MOUNT_ROOT: Path = Path.of("/work")///root")
        private val GUEST_MOUNT_ROOT: Path = Path.of("/")
        const val SHARED_DIRECTORY_NAME: String = "guestfish.shared"
        private val shutdownOperation = GuestfishOperation("umount-all", "quit")

        /**
         * [Path] on the OS running inside Docker mapped to this host.
         */
        private val GUEST_ROOT_ON_DOCKER = DOCKER_MOUNT_ROOT.resolve(SHARED_DIRECTORY_NAME)

        /**
         * Given an [img] location returns the [Path] which is used to exchange data with [img] mounted using [Guestfish].
         */
        private fun guestRootOnHost(osImage: OperatingSystemImage) = osImage.directory.resolve(SHARED_DIRECTORY_NAME)

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
                listOf("- mkdir-p $destPath", "copy-in $sourcePath $destPath")
            })

        /**
         * Creates the commands needed to copy everything contained in the [SHARED_DIRECTORY_NAME]
         * using the same directory structure to the guest of [img].
         */
        fun tarInCommands(osImage: OperatingSystemImage): GuestfishOperation {
            val tarball = guestRootOnHost(osImage).tar().moveTo(guestRootOnHost(osImage).resolve("tarball.tar"))
            check(tarball.exists) { "Error creating tarball" }
            return GuestfishOperation(arrayOf(
                "-tar-in ${GUEST_ROOT_ON_DOCKER.resolve(tarball.fileName)} /",
                "! rm ${GUEST_ROOT_ON_DOCKER.resolve(tarball.fileName)}",
            ))
        }

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
         * Executes the given [commands] on a [Guestfish] instance with just [volumes] mounted.
         */
        fun execute(
            containerName: String,
            volumes: Map<Path, Path>,
            options: List<String> = emptyList(),
            commands: GuestfishOperation,
            workingDirectory: Path = Paths.WORKING_DIRECTORY,
            logger: RenderingLogger<*>,
        ): LoggedProcess = logger.subLogger("Running ${commands.commandCount} guestfish operations... ${Kaomojis.fishing()}") {
            DockerProcess(
                command = IMAGE.buildRunCommand {
                    redirects { +"2>&1" } // needed since some commandrvf writes all output to stderr
                    options {
                        name { containerName }
                        autoCleanup { false }
                        volumes { +volumes }
                    }
                    args(listOf(*options.toTypedArray(), "--", (commands + shutdownOperation).asHereDoc()))
                },
                workingDirectory = workingDirectory,
                ioProcessor = GuestfishIoProcessor(this, verbose = false),
            ).loggedProcess.get()
        }
    }
}


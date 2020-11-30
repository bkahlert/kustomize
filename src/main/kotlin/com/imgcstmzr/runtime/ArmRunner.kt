package com.imgcstmzr.runtime

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.IN
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.concurrent.startAsThread
import com.bkahlert.koodies.docker.DockerContainerName
import com.bkahlert.koodies.docker.DockerContainerName.Companion.toContainerName
import com.bkahlert.koodies.docker.DockerImage
import com.bkahlert.koodies.docker.DockerImageBuilder.Companion.build
import com.bkahlert.koodies.docker.DockerProcess
import com.bkahlert.koodies.docker.DockerRunCommandBuilder.Companion.buildRunCommand
import com.bkahlert.koodies.terminal.ANSI
import com.imgcstmzr.runtime.Program.Companion.compute
import com.imgcstmzr.runtime.Program.Companion.format
import com.imgcstmzr.runtime.log.MutedBlockRenderingLogger
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.subLogger
import java.nio.file.Path

/**
 * A tool to run an [ARM based](https://en.wikipedia.org/wiki/ARM_architecture)
 * [OperatingSystem] such as [OperatingSystems.RaspberryPiLite] provided
 * through an [OperatingSystemImage].
 *
 * Changes to the [OperatingSystemImage] are permanent. The manipulated
 * image file can be used to to flash a memory card and to run it
 * non-virtually on actual hardware.
 */
object ArmRunner {

    @Suppress("SpellCheckingInspection")
    private val DOCKER_IMAGE: DockerImage = build { "lukechilds" / "dockerpi" tag "vm" }

    /**
     * Starts a [DockerProcess] with [name] that boots the [osImage].
     * All [IO] is passed to the [ioProcessor] which has an instance
     * of [RunningOperatingSystem] as its receiver. The [RunningOperatingSystem]
     * also forwards all logging calls to [logger].
     */
    fun <T> run(
        name: DockerContainerName,
        osImage: OperatingSystemImage,
        logger: RenderingLogger<T>? = null,
        ioProcessor: (RunningOperatingSystem.(IO) -> Any)? = null,
    ): DockerProcess = run {
        lateinit var dockerProcess: DockerProcess
        DockerProcess(
            command = DOCKER_IMAGE.buildRunCommand {
                options {
                    containerName { name }
                    volumes { osImage.file to Path.of("/sdcard/filesystem.img") }
                }
            },
            ioProcessor = ioProcessor.let {
                val deadEndPattern = osImage.deadEndPattern
                var dying = false
                val runningOperatingSystem = object : RunningOperatingSystem(osImage.shutdownCommand) {
                    override val process: Process get() = dockerProcess
                    override val logger: RenderingLogger<T> = logger ?: MutedBlockRenderingLogger()
                }
                { io: IO ->
                    if (!dying) {
                        if (deadEndPattern?.matches(io.unformatted) == true) {
                            startAsThread {
                                dying = true
                                runningOperatingSystem.negativeFeedback("The VM is stuck. Chances are the VM starts correctly with less load on this machine.")
                            }
                            destroy()
                            throw IllegalStateException(io.unformatted)
                        }
                        it?.invoke(runningOperatingSystem, io)
                    }
                }
            },
        ).apply { dockerProcess = this }
    }

    /**
     * Starts a [DockerProcess] with [name] that boots the [osImage] and
     * runs all provided [programs].
     *
     * Before the [programs] the [osImage] will be booted and an [autoLogin] takes place.
     * After the execution of all [programs] finishes the [osImage] will
     * be shutdown.
     *
     * @return exit code `0` if no errors occurred.
     */
    fun run(
        name: DockerContainerName,
        osImage: OperatingSystemImage,
        logger: RenderingLogger<Any>,
        autoLogin: Boolean = true,
        vararg programs: Program,
    ): Int = logger.subLogger("Running ${osImage.shortName} with ${programs.format()}", ansiCode = ANSI.termColors.cyan) {
        val unfinished: MutableList<Program> = mutableListOf(
            *(if (autoLogin) arrayOf(osImage.loginProgram(osImage.credentials)/*.logging()*/) else emptyArray()),
            *programs,
            osImage.shutdownProgram(),/*.logging()*/
        )
        return@subLogger run(name, osImage, this) { output ->
            when (output.type) {
                META -> feedback(output.unformatted)
                IN -> logStatus(items = unfinished) { output }
                OUT -> logStatus(items = unfinished) { output }
                ERR -> feedback("Unfortunately an error occurred: ${output.formatted}")
            }
            if (!unfinished.compute(this, output)) unfinished.takeIf { it.isNotEmpty() }?.removeAt(0)
        }.waitFor()
    }

    /**
     * Starts a [DockerProcess] that boots the [osImage] and runs this [Program].
     * *(The program's name is used as the container name.)*
     *
     * Before this [Program] the [osImage] will be booted and a login takes place.
     * After the execution of this [Program] finishes the [osImage] will
     * be shutdown.
     *
     * @return exit code `0` if no errors occurred.
     */
    fun Program.runOn(
        osImage: OperatingSystemImage,
        logger: RenderingLogger<Any>,
    ): Int = run(
        name = name.toContainerName(),
        osImage = osImage,
        logger = logger,
        programs = arrayOf(this)
    )
}

package com.imgcstmzr.runtime

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.IN
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.concurrent.process.RunningProcess.Companion.nullRunningProcess
import com.bkahlert.koodies.concurrent.startAsThread
import com.bkahlert.koodies.docker.Docker
import com.bkahlert.koodies.docker.DockerProcess
import com.bkahlert.koodies.terminal.ANSI
import com.imgcstmzr.runtime.Program.Companion.compute
import com.imgcstmzr.runtime.Program.Companion.format
import com.imgcstmzr.runtime.log.MutedBlockRenderingLogger
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.RenderingLogger.Companion.subLogger
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
    private const val DOCKER_IMAGE = "lukechilds/dockerpi:vm"

    private val nullDockerProcess: DockerProcess = DockerProcess("NULL") { nullRunningProcess }

    /**
     * Starts a [DockerProcess] with [name] that boots the [osImage].
     * All [IO] is passed to the [outputProcessor] which has an instance
     * of [RunningOperatingSystem] as its receiver. The [RunningOperatingSystem]
     * also forwards all logging calls to [logger].
     */
    fun <T> run(
        name: String,
        osImage: OperatingSystemImage,
        logger: RenderingLogger<T>? = null,
        outputProcessor: (RunningOperatingSystem.(IO) -> Any)? = null,
    ): DockerProcess {
        var dockerProcess: DockerProcess = nullDockerProcess
        val runningOperatingSystem = object : RunningOperatingSystem() {
            override val process: Process get() = dockerProcess
            override val logger: RenderingLogger<T> = logger ?: MutedBlockRenderingLogger()
        }
        return Docker.run(
            outputProcessor = outputProcessor.let {
                var dying = false
                { io: IO ->
                    if (!dying) {
                        if (io.unformatted.matches(osImage.deadEndPattern)) {
                            startAsThread {
                                dying = true
                                runningOperatingSystem.negativeFeedback("The VM is stuck. Chances are the VM starts correctly with less load on this machine.")
                            }
                            dockerProcess.destroy()
                            throw IllegalStateException(io.unformatted)
                        }
                        it?.invoke(runningOperatingSystem, io)
                    }
                }
            },
            init = {
                run(
                    name = name,
                    volumes = mapOf(osImage.toAbsolutePath() to Path.of("/sdcard/filesystem.img")),
                    image = DOCKER_IMAGE,
                )
            }
        ).also { dockerProcess = it }
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
        name: String,
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
            0
        }.waitForExitCode(0).exitCode
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
        name = name,
        osImage = osImage,
        logger = logger,
        programs = arrayOf(this)
    )
}

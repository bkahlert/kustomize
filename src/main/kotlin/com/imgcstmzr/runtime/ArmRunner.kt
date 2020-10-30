package com.imgcstmzr.runtime

import com.bkahlert.koodies.docker.Docker
import com.bkahlert.koodies.docker.DockerProcess
import com.bkahlert.koodies.terminal.ANSI
import com.imgcstmzr.process.Output
import com.imgcstmzr.process.Output.Type.ERR
import com.imgcstmzr.process.Output.Type.IN
import com.imgcstmzr.process.Output.Type.META
import com.imgcstmzr.process.Output.Type.OUT
import com.imgcstmzr.process.RunningProcess
import com.imgcstmzr.runtime.Program.Companion.compute
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

    /**
     * Starts a [DockerProcess] with [name] that boots the [osImage].
     * All [Output] is passed to the [outputProcessor] which has an instance
     * of [RunningOperatingSystem] as its receiver. The [RunningOperatingSystem]
     * also forwards all logging calls to [logger].
     */
    fun <T> run(
        name: String,
        osImage: OperatingSystemImage,
        logger: RenderingLogger<T>? = null,
        outputProcessor: (RunningOperatingSystem.(Output) -> Any)? = null,
    ): DockerProcess {
        var runningProcess: RunningProcess = RunningProcess.nullRunningProcess
        val runningOperatingSystem = object : RunningOperatingSystem() {
            override val process: Process get() = runningProcess
            override val logger: RenderingLogger<T> = logger ?: MutedBlockRenderingLogger()
        }
        return Docker.run(
            name = name,
            volumes = listOf(osImage.toAbsolutePath() to Path.of("/sdcard/filesystem.img")).toMap(),
            image = DOCKER_IMAGE,
            args = emptyList(),
            outputProcessor = outputProcessor?.let {
                { output: Output ->
                    it(runningOperatingSystem, output)
                }
            }).also { runningProcess = it }
    }

    /**
     * Starts a [DockerProcess] with [name] that boots the [osImage] and
     * runs all provided [programs].
     *
     * Before the [programs] the [osImage] will be booted and a login takes place.
     * After the execution of all [programs] finished the [osImage] will
     * be shutdown.
     *
     * @return exit code `0` if no errors occurred.
     */
    fun run(
        name: String,
        osImage: OperatingSystemImage,
        logger: RenderingLogger<Any>,
        vararg programs: Program,
    ): Int = logger.subLogger("$osImage", ansiCode = ANSI.termColors.cyan) {
        val unfinished: MutableList<Program> = mutableListOf(
            osImage.loginProgram(OperatingSystems.Companion.Credentials(osImage.defaultUsername, osImage.defaultPassword)),/*.logging()*/
            *programs,
            osImage.shutdownProgram(),/*.logging()*/
        )
        return@subLogger run(name, osImage, this) { output ->
            when (output.type) {
                META -> feedback(output.unformatted)
                IN -> logStatus(items = unfinished) { output }
                OUT -> logStatus(items = unfinished) { output }
                ERR -> {
                    println("Err in ArmRunner occurred: $output")
                    //                    logException { // TODO
                    //                        RuntimeException(output.unformatted)
                    //                    }
                }
            }
            if (!unfinished.compute(this, output)) unfinished.takeIf { it.isNotEmpty() }?.removeAt(0)
            0
        }.waitForCompletion().exitCode
    }
}

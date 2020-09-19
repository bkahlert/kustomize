package com.imgcstmzr.runtime

import com.bkahlert.koodies.unit.Mega
import com.bkahlert.koodies.unit.Size
import com.bkahlert.koodies.unit.bytes
import com.bkahlert.koodies.unit.size
import com.imgcstmzr.process.CommandLineRunner
import com.imgcstmzr.process.Output
import com.imgcstmzr.process.RunningProcess
import com.imgcstmzr.process.alive
import com.imgcstmzr.process.enter
import com.imgcstmzr.process.input
import com.imgcstmzr.runtime.Program.Companion.calc
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.RenderingLogger
import java.io.File
import java.nio.file.Path

sealed class OperatingSystems : OperatingSystem {

    companion object {
        val supported: List<OperatingSystems> = listOf(RaspberryPiLite, DietPi)
    }

    /**
     * [Raspberry Pi OS Lite](https://www.raspberrypi.org/downloads/raspberry-pi-os/)
     */
    object RaspberryPiLite : OperatingSystems() {
        override val name: String = "Raspberry Pi OS Lite"
        override val downloadUrl: String = "https://downloads.raspberrypi.org/raspios_lite_armhf_latest"
        override val username: String = "pi"
        override val password: String = "raspberry"

        val loginPattern: Regex
            get() = Regex("(?<host>[\\w-_]+)(?<sep>\\s+)(?<const>login):(?<optWhitespace>\\s*)")
        val passwordPattern: Regex
            get() = Regex("Password:(?<optWhitespace>\\s*)")
        val readyPattern: Regex
            get() = Regex("(?<user>[\\w-_]+)@(?<host>[\\w-_]+):(?<path>[^#$]+?)[#$](?<optWhitespace>\\s*)")

        override fun increaseDiskSpace(size: Size, img: Path, runtime: Runtime): Int {
            var missing = size - img.size
            val tenMegaBytesArray = 10.Mega.bytes.toZeroFilledByteArray()
            if (missing > 0.bytes) {
                while (missing > 0.bytes) {
                    val write = if (missing < 10.Mega.bytes) missing.toZeroFilledByteArray() else tenMegaBytesArray
                    img.toFile().appendBytes(write)
                    missing -= write.size
                }
                return runtime.bootAndRun("Resizing", this, img, compileScript("expand-root", "raspi-config --expand-rootfs"))
            }
            return 0
        }

        override fun bootAndRun(
            scencario: String,
            img: Path,
            runtime: Runtime,
            processor: RunningOS.(Output) -> Unit,
        ): Int {
            val cmd: String = startCommand(runtime.name, img.toFile())
            val cmdRunner = CommandLineRunner(blocking = false)

            val startUp: MutableList<Program> = mutableListOf(login("pi", "raspberry"))

            val renderer = BlockRenderingLogger<Program>(scencario)
            val runningOS = RunningOS(renderer)
            val runningProcess: RunningProcess = cmdRunner.startProcessAndWaitForCompletion(
                directory = Path.of("/bin"),
                shellScript = "sh -c '$cmd'",
                outputRedirect = ProcessBuilder.Redirect.PIPE,
            ) { output ->
                runningOS.process = this
                if (startUp.isNotEmpty()) {
                    renderer.logLine(output, startUp)
                    if (!startUp.calc(runningOS, output)) startUp.removeAt(0)
                } else {
                    runningOS.processor(output)
                }
            }
            val exitCode = runningProcess.blockExitCode
            renderer.endLogging(scencario, exitCode)
            return exitCode
        }

        override fun compileSetupScript(name: String, commandBlocks: String): Array<Program> =
            Program.fromSetupScript(name = name, readyPattern, commandBlocks)

        override fun compileScript(name: String, vararg commands: String): Program =
            Program.fromScript(name, readyPattern, *commands)

        @Suppress("SpellCheckingInspection") private fun startCommand(name: String, img: File): String {
            val containerName = "$name-hot"
            val dockerRun = arrayOf(
                "docker",
                "run",
                "--name", "\"$containerName\"",
                "--rm",
                "-i",
                "--volume", "\"$img\":/sdcard/filesystem.img",
                "lukechilds/dockerpi:vm"
            ).joinToString(" ")
            return "docker rm --force \"$containerName\" &> /dev/null ; $dockerRun"
        }

        fun login(username: String, password: String): Program {
            return Program(
                "login",
                { output -> "waiting for login prompt" },
                "waiting for login prompt" to { output ->
                    if (output.matches(loginPattern)) {
                        input("$username\r")
                        "waiting for password prompt"
                    } else "waiting for login prompt"
                },
                "waiting for password prompt" to { output ->
                    if (output.matches(passwordPattern)) {
                        input("$password\r")
                        null
                    } else "waiting for password prompt"
                })
        }

        override fun toString(): String = name
    }

    /**
     * [DietPi](https://dietpi.com)
     */
    object DietPi : OperatingSystems() {
        override val name: String = "Diet Pi"
        override val downloadUrl: String = "https://dietpi.com/downloads/images/DietPi_RPi-ARMv6-Buster.7z"
        override val username: String = "root"
        override val password: String = "dietpi"

        override fun increaseDiskSpace(size: Size, img: Path, runtime: Runtime): Int {
            TODO("Not yet implemented")
        }

        override fun bootAndRun(scencario: String, img: Path, runtime: Runtime, processor: RunningOS.(Output) -> Unit): Int {
            TODO("Not yet implemented")
        }

        override fun compileSetupScript(name: String, commandBlocks: String): Array<Program> {
            TODO("Not yet implemented")
        }

        override fun compileScript(name: String, vararg commands: String): Program {
            TODO("Not yet implemented")
        }
    }
}


class RunningOS(
    val renderer: RenderingLogger<Program>,
    var process: Process? = null,
    val shutdownCommand: String = "sudo shutdown -h now",
) {
    /**
     * Forwards the [values] to the OS running process.
     */
    fun input(vararg values: String) {
        process.alive().input(*values)
    }

    /**
     * Logs the current execution status given the [output] and [unfinished].
     */
    fun status(output: Output, unfinished: List<Program>) {
        renderer.logLine(output, unfinished)
    }

    /**
     * Initiates the systems immediate shutdown.
     */
    fun shutdown() {
        process.alive().enter(shutdownCommand)
        shuttingDown = true
    }

    var shuttingDown: Boolean = false
}

/**
 * Representation of an operating system that can be customized.
 */
interface OperatingSystem {

    val name: String

    /**
     * URL that allows an image of this [OperatingSystem] to be downloaded.
     */
    val downloadUrl: String?

    val username: String
    val password: String

    fun increaseDiskSpace(
        size: Size,
        img: Path,
        runtime: Runtime,
    ): Int;

    fun bootAndRun(
        scencario: String,
        img: Path,
        runtime: Runtime,
        processor: RunningOS.(Output) -> Unit,
    ): Int

    /**
     * Compiles a special script with a [name] that consists itself of multiple scripts
     * in the form of labeled [commandBlocks].
     *
     * A command block starts with a header (e.g. `: label`) followed by a script.
     *
     * Example:
     * ```shell script
     * : say "Hello\nWorld""
     * echo "Hello"
     * echo "World"
     * ```
     */
    fun compileSetupScript(name: String, commandBlocks: String): Array<Program>

    /**
     * Compiles a script with a [name] consisting of [commands] to be executed.
     */
    fun compileScript(name: String, vararg commands: String): Program
}

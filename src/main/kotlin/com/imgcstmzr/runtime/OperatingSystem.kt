package com.imgcstmzr.runtime

import com.bkahlert.koodies.docker.toContainerName
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.terminal.ansi.Style.Companion.green
import com.bkahlert.koodies.terminal.ascii.Kaomojis
import com.bkahlert.koodies.unit.Mega
import com.bkahlert.koodies.unit.Size
import com.bkahlert.koodies.unit.bytes
import com.bkahlert.koodies.unit.size
import com.imgcstmzr.process.CommandLineRunner
import com.imgcstmzr.process.Output
import com.imgcstmzr.process.Output.Type.ERR
import com.imgcstmzr.process.Output.Type.META
import com.imgcstmzr.process.Output.Type.OUT
import com.imgcstmzr.process.RunningProcess
import com.imgcstmzr.process.alive
import com.imgcstmzr.process.enter
import com.imgcstmzr.process.input
import com.imgcstmzr.runtime.OperatingSystems.Companion.Credentials
import com.imgcstmzr.runtime.Program.Companion.compute
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.miniSegment
import com.imgcstmzr.runtime.log.segment
import java.io.File
import java.nio.file.Path
import kotlin.properties.Delegates.observable
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

// TODO rename to Booter
sealed class OperatingSystems : OperatingSystem {

    companion object {
        val supported: List<OperatingSystems> = listOf(RaspberryPiLite, DietPi)

        data class Credentials(val username: String, val password: String)

        val credentials: MutableMap<Path, Credentials> = mutableMapOf()
    }

    /**
     * [Raspberry Pi OS Lite](https://www.raspberrypi.org/downloads/raspberry-pi-os/)
     */
    object RaspberryPiLite : OperatingSystems() {
        override val name: String = "Raspberry Pi OS Lite"
        override val downloadUrl: String = "https://downloads.raspberrypi.org/raspios_lite_armhf_latest"
        override val defaultUsername: String = "pi"
        override val defaultPassword: String = "raspberry"

        override fun increaseDiskSpace(
            size: Size,
            img: Path,
            parentLogger: BlockRenderingLogger<Any>?,
        ): Any {
            return parentLogger.segment("Increasing Disk Space: ${img.size} ➜ $size", null) {
                var missing = size - img.size
                val bytesPerStep = 100.Mega.bytes
                val oneHundredMegaBytes = bytesPerStep.toZeroFilledByteArray()
                when {
                    missing < 0.bytes -> {
                        logStatus { ERR typed "Requested disk space is ${-missing} smaller than current size of ${img.fileName} (${img.size})." }
                        logStatus { ERR typed "Decreasing an image's disk space is currently not supported." }
                    }
                    missing == 0.bytes -> {
                        logStatus { OUT typed "${img.fileName} is has already ${img.size}" }
                    }
                    else -> {
                        miniSegment<Any, Size>("Progress:") {
                            logStatus { OUT typed img.size.toString() }
                            while (missing > 0.bytes) {
                                val write = if (missing < bytesPerStep) missing.toZeroFilledByteArray() else oneHundredMegaBytes
                                img.toFile().appendBytes(write)
                                missing -= write.size
                                logStatus { OUT typed "⥅ ${img.size}" }
                            }
                            img.size
                        }

                        logStatus { OUT typed "Image Disk Space Successfully increased to " + img.size.toString() + ". Booting OS to finalize..." }

                        val compileScript = compileScript("expand-root", "sudo raspi-config --expand-rootfs")
                        compileScript.bootRunStop("Resizing", this@RaspberryPiLite, img, this@segment)
                    }
                }
            }
        }

        override fun bootToUserSession(
            scenario: String,
            img: Path,
            parentLogger: BlockRenderingLogger<Any>?,
            processor: RunningOS.(Output) -> Any,
        ): Any {
            val cmd: String = startCommand(scenario, img.toFile())
            val cmdRunner = CommandLineRunner(blocking = false) // TODO delete

            val credentials = credentials[img] ?: Credentials(defaultUsername, defaultPassword)
            val startUp: MutableList<Program> = mutableListOf(loginProgram(credentials))

            return parentLogger.segment(scenario, null) {
                val runningOS = RunningOS(this@segment)
                val runningProcess: RunningProcess = cmdRunner.startProcessAndWaitForCompletion(
                    directory = Path.of("/bin"),
                    shellScript = "sh -c '$cmd'",
                    outputRedirect = ProcessBuilder.Redirect.PIPE,
                ) { output ->
                    runningOS.process = this
                    if (startUp.isNotEmpty()) {
                        // setup / logging in
                        logStatus(items = startUp) { output }
                        if (!startUp.compute(runningOS, output)) startUp.removeAt(0)
                    } else {
                        // running the actual programs by calling the passed processor
                        runningOS.processor(output)
                    }
                }
                runningProcess.blockExitCode
            }
        }

        @Suppress("SpellCheckingInspection")
        private fun startCommand(name: String, img: File): String {
            val containerName = name.toContainerName() + "-" + String.random(4)
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

        override fun toString(): String = name
    }

    /**
     * [DietPi](https://dietpi.com)
     */
    object DietPi : OperatingSystems() {
        override val name: String = "Diet Pi"
        override val downloadUrl: String = "https://dietpi.com/downloads/images/DietPi_RPi-ARMv6-Buster.7z"
        override val defaultUsername: String = "root"
        override val defaultPassword: String = "dietpi"

        override fun increaseDiskSpace(
            size: Size,
            img: Path,
            parentLogger: BlockRenderingLogger<Any>?,
        ): Any = RaspberryPiLite.increaseDiskSpace(size, img, parentLogger)

        override fun bootToUserSession(
            scenario: String,
            img: Path,
            parentLogger: BlockRenderingLogger<Any>?,
            processor: RunningOS.(Output) -> Any,
        ): Any = RaspberryPiLite.bootToUserSession(scenario, img, parentLogger, processor)
    }
}


class RunningOS(
    val renderer: BlockRenderingLogger<*>,
    var process: Process? = null,
    val shutdownCommand: String = "sudo shutdown -h now",
) {
    /**
     * Forwards the [values] to the OS running process.
     */
    fun input(vararg values: String) {
        process.alive().input(*values)
    }


    val shuttingDownStatus: List<HasStatus> = listOf(object : HasStatus {
        override fun status(): String = "shutting down"
    })

    /**
     * Logs the current execution status given the [output] and [unfinished].
     */
    fun status(output: Output, unfinished: List<Program>) {
        renderer.logStatus(items = if (shuttingDown) shuttingDownStatus else unfinished) { output }
    }

    @ExperimentalTime
    fun wait(duration: Duration) {
        Thread.sleep(duration.toLongMilliseconds())
    }

    fun command(input: String) {
        input("$input\r")
    }

    /**
     * Initiates the systems immediate shutdown.
     */
    fun shutdown() {
        process.alive().enter(shutdownCommand)
        shuttingDown = true
    }

    fun kill() {
        process?.destroy()
    }

    var shuttingDown: Boolean by observable(false) { property, oldValue, newValue ->
        renderer.logStatus { META typed "shutdown invoked ($oldValue -> $newValue)" }
    }
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

    val defaultUsername: String
    val defaultPassword: String

    val loginPattern: Regex
        get() = Regex("(?<host>[\\w-_]+)(?<sep>\\s+)(?<const>login):(?<optWhitespace>.*)")
    val passwordPattern: Regex
        get() = Regex("Password:(?<optWhitespace>\\s*)")
    val readyPattern: Regex
        get() = Regex("(?<user>[\\w-_]+)@(?<host>[\\w-_]+):(?<path>[^#$]+?)[#$](?<optWhitespace>\\s*)")

    fun increaseDiskSpace(
        size: Size,
        img: Path,
        parentLogger: BlockRenderingLogger<Any>?,
    ): Any

    /**
     * Boots the [OperatingSystem] on the [img] and performs the needed steps to get to the command prompt.
     * With that ready [processor] is called with every output.
     */
    fun bootToUserSession(
        scenario: String,
        img: Path,
        parentLogger: BlockRenderingLogger<Any>? = null,
        processor: RunningOS.(Output) -> Any,
    ): Any

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
    fun compileSetupScript(name: String, commandBlocks: String): Array<Program> =
        Program.fromSetupScript(name = name, readyPattern, commandBlocks)

    /**
     * Compiles a script with a [name] consisting of [commands] to be executed.
     */
    fun compileScript(name: String, vararg commands: String): Program =
        Program.fromScript(name, readyPattern, *commands)

    /**
     * Creates a program to log [Credentials.username] in using [Credentials.password].
     */
    fun loginProgram(credentials: Credentials): Program {
        fun RunningOS.enterUsername() {
            input("${credentials.username}\r")
        }

        fun RunningOS.enterPassword() {
            input("${credentials.password}\r")
        }
        return Program(
            "login", { output -> "1/4: waiting for prompt" },
            "1/4: waiting for prompt" to { output ->
                if (output.matches(loginPattern)) {
                    enterUsername()
                    "2/4: confirm username"
                } else "1/4: waiting for prompt"
            },
            "2/4: confirm username" to { output ->
                if (!output.matches(loginPattern)) {
                    "3/4: password..."
                } else "2/4: confirm username"
            },
            "3/4: password..." to { output ->
                if (output.matches(passwordPattern)) {
                    enterPassword()
                    "4/4 confirm password"
                } else "3/4: password..."
            },
            "4/4 confirm password" to { output ->
                if (!output.matches(passwordPattern)) {
                    "feedback"
                } else "4/4 confirm password"
            },
            "feedback" to { output ->
                val message = "Logged in successfully ${Kaomojis.Proud.random()}".green()
                input(": $message\r")
                println(message)
//                println("\n\n")
                if (output.contains("Login incorrect")) println("Login incorrect seen")
//                println(this.hi)
                null
            },
        )//.logging()
    }
//    fun loginProgram(credentials: Credentials): Program {
//        fun RunningOS.typeInCredentials(output: String, credentials: Credentials): String? =
//            when {
//                output.matches(loginPattern) -> {
//                    input("${credentials.username}\r")
//                    "2/2: password..."
//                }
//                output.matches(passwordPattern) -> {
//                    input("${credentials.password}\r")
//                    "debug"
//                }
//                else -> "1/2: waiting for prompt"
//            }
//
//        return Program(
//            "login", { output -> "1/2: waiting for prompt" },
//            "1/2: waiting for prompt" to { output ->
//                typeInCredentials(output, credentials)
//            },
//            "2/2: password..." to { output ->
//                typeInCredentials(output, credentials)
//            },
//            "debug" to { output ->
//                println("\n\n")
//                if (output.contains("Login incorrect")) println("Login incorrect seen")
////                println(this.hi)
//                null
//            },
//        )//.logging()
//    }


}

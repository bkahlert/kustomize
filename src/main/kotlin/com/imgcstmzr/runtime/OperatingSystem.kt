package com.imgcstmzr.runtime

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.process.RunningProcess
import com.bkahlert.koodies.concurrent.process.UserInput.enter
import com.bkahlert.koodies.docker.Docker.toContainerName
import com.bkahlert.koodies.string.LineSeparators.LF
import com.bkahlert.koodies.string.LineSeparators.withoutTrailingLineSeparator
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.terminal.ansi.AnsiColors.green
import com.bkahlert.koodies.terminal.ascii.Kaomojis
import com.bkahlert.koodies.terminal.ascii.Kaomojis.thinking
import com.bkahlert.koodies.time.Now
import com.imgcstmzr.process.CommandLineRunner
import com.imgcstmzr.runtime.OperatingSystems.Companion.Credentials
import com.imgcstmzr.runtime.Program.Companion.compute
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.segment
import com.imgcstmzr.util.quoted
import java.io.File
import java.nio.file.Path
import kotlin.properties.Delegates.observable
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds

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

        override fun bootToUserSession(
            scenario: String,
            img: Path,
            parentLogger: BlockRenderingLogger<Any>?,
            processor: RunningOperatingSystem.(IO) -> Any,
        ): Any {
            val cmd: String = startCommand(scenario, img.toFile())
            val cmdRunner = CommandLineRunner() // TODO delete

            val credentials = credentials[img] ?: Credentials(defaultUsername, defaultPassword)
            val startUp: MutableList<Program> = mutableListOf(loginProgram(credentials))

            return parentLogger.segment(scenario, null) {
                lateinit var runningProcess: RunningProcess
                val runningOS = object : RunningOperatingSystem() {
                    override val logger: RenderingLogger<*> = this@segment
                    override var process: Process = runningProcess
                }

                runningProcess = cmdRunner.startProcessAndWaitForCompletion(
                    directory = Path.of("/bin"),
                    shellScript = "sh -c '$cmd'",
//                    test = cmd
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
                runningProcess.waitForCompletion().exitCode
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
     * [Raspberry Pi OS Lite](https://www.raspberrypi.org/downloads/raspberry-pi-os/)
     */
    object RaspberryPi : OperatingSystems() {
        override val name: String = "Raspberry Pi OS"
        override val downloadUrl: String = "https://downloads.raspberrypi.org/raspios_armhf_latest"
        override val defaultUsername: String = "pi"
        override val defaultPassword: String = "raspberry"

        override fun bootToUserSession(
            scenario: String,
            img: Path,
            parentLogger: BlockRenderingLogger<Any>?,
            processor: RunningOperatingSystem.(IO) -> Any,
        ): Any {
            return RaspberryPiLite.bootToUserSession(scenario, img, parentLogger, processor)
        }
    }

    /**
     * [DietPi](https://dietpi.com)
     */
    object DietPi : OperatingSystems() {
        override val name: String = "Diet Pi"
        override val downloadUrl: String = "https://dietpi.com/downloads/images/DietPi_RPi-ARMv6-Buster.7z"
        override val defaultUsername: String = "root"
        override val defaultPassword: String = "dietpi"

        override fun bootToUserSession(
            scenario: String,
            img: Path,
            parentLogger: BlockRenderingLogger<Any>?,
            processor: RunningOperatingSystem.(IO) -> Any,
        ): Any = RaspberryPiLite.bootToUserSession(scenario, img, parentLogger, processor)
    }

    /**
     * [Tiny Core](http://tinycorelinux.net/ports.html)
     */
    object TinyCore : OperatingSystems() {
        override val name: String = "Tiny Core"
        override val downloadUrl: String = "http://tinycorelinux.net/12.x/armv6/releases/RPi/piCore-12.0.zip"
        override val defaultUsername: String = "tc"
        override val defaultPassword: String = "piCore"

        override fun bootToUserSession(
            scenario: String,
            img: Path,
            parentLogger: BlockRenderingLogger<Any>?,
            processor: RunningOperatingSystem.(IO) -> Any,
        ): Any = RaspberryPiLite.bootToUserSession(scenario, img, parentLogger, processor)
    }

    /**
     * [Arch Linux ARM](https://archlinuxarm.org/platforms/armv6/raspberry-pi)
     */
    object ArchLinuxArm : OperatingSystems() {
        override val name: String = "Arch Linux ARM"
        override val downloadUrl: String = "http://os.archlinuxarm.org/os/ArchLinuxARM-rpi-latest.tar.gz"
        override val defaultUsername: String = "alarm" // root
        override val defaultPassword: String = "alarm" // root

        override fun bootToUserSession(
            scenario: String,
            img: Path,
            parentLogger: BlockRenderingLogger<Any>?,
            processor: RunningOperatingSystem.(IO) -> Any,
        ): Any = RaspberryPiLite.bootToUserSession(scenario, img, parentLogger, processor)
    }

    /**
     * [RISC OS](https://www.riscosopen.org/content/downloads/raspberry-pi)
     *
     * Size: ~5MB
     */
    object RiscOs : OperatingSystems() {
        override val name: String = "RISC OS"
        override val downloadUrl: String = "https://www.riscosopen.org/zipfiles/platform/raspberry-pi/BCM2835.5.24.zip?1544451169"
        override val defaultUsername: String = ""
        override val defaultPassword: String = ""

        override fun bootToUserSession(
            scenario: String,
            img: Path,
            parentLogger: BlockRenderingLogger<Any>?,
            processor: RunningOperatingSystem.(IO) -> Any,
        ): Any = RaspberryPiLite.bootToUserSession(scenario, img, parentLogger, processor)
    }

    /**
     * [RISC OS Pico RC5](https://www.riscosopen.org/content/downloads/raspberry-pi)
     *
     * Size: ~2MB
     */
    object RiscOsPicoRc5 : OperatingSystems() {
        override val name: String = "RISC OS Pico RC5"
        override val downloadUrl: String = "https://www.riscosopen.org/zipfiles/platform/raspberry-pi/Pico.5.zip"
        override val defaultUsername: String = ""
        override val defaultPassword: String = ""

        override fun bootToUserSession(
            scenario: String,
            img: Path,
            parentLogger: BlockRenderingLogger<Any>?,
            processor: RunningOperatingSystem.(IO) -> Any,
        ): Any = RaspberryPiLite.bootToUserSession(scenario, img, parentLogger, processor)
    }

    /**
     * [RISC OS Pico RC5](https://cdimage.ubuntu.com/releases/20.10/release/ubuntu-20.10-preinstalled-server-armhf+raspi.img.xz)
     *
     * Size: ~700MB
     */
    object UbuntuServer : OperatingSystems() {
        override val name: String = "Ubuntu Server 20.10"
        override val downloadUrl: String = "https://www.riscosopen.org/zipfiles/platform/raspberry-pi/Pico.5.zip"
        override val defaultUsername: String = "ubuntu"
        override val defaultPassword: String = "ubuntu"

        override fun bootToUserSession(
            scenario: String,
            img: Path,
            parentLogger: BlockRenderingLogger<Any>?,
            processor: RunningOperatingSystem.(IO) -> Any,
        ): Any = RaspberryPiLite.bootToUserSession(scenario, img, parentLogger, processor)
    }

    /**
     * [WebThings Gateway](https://github.com/WebThingsIO/gateway/releases/download/0.12.0/gateway-0.12.0.img.zip)
     *
     * Size: ~860MB
     */
    object WebThingsGateway : OperatingSystems() {
        override val name: String = "WebThings Gateway"
        override val downloadUrl: String = "https://github.com/WebThingsIO/gateway/releases/download/0.12.0/gateway-0.12.0.img.zip"
        override val defaultUsername: String = "pi"
        override val defaultPassword: String = "raspberry"

        override fun bootToUserSession(
            scenario: String,
            img: Path,
            parentLogger: BlockRenderingLogger<Any>?,
            processor: RunningOperatingSystem.(IO) -> Any,
        ): Any = RaspberryPiLite.bootToUserSession(scenario, img, parentLogger, processor)
    }
}


abstract class RunningOperatingSystem(
    val shutdownCommand: String = "sudo shutdown -h now",
) {
    abstract val logger: RenderingLogger<*>
    abstract val process: Process

    /**
     * Forwards the [values] to the OS running process.
     */
    @OptIn(ExperimentalTime::class)
    fun enter(vararg values: String, delay: Duration = 10.milliseconds) {
        if (process.isAlive) {
            feedback("Entering ${values.joinToString { it.withoutTrailingLineSeparator }.quoted}")
            process.enter(*values, delay = delay)
        } else {
            feedback("Process $process is not alive.")
        }
    }

    /**
     * Prints [value] on the output without actually forwarding it
     * to the OS running process.
     */
    fun feedback(value: String) {
        logger.logLine { LF + Kaomojis.Proud.random().thinking(value.capitalize().green()) + LF }
    }


    val shuttingDownStatus: List<HasStatus> = listOf(object : HasStatus {
        override fun status(): String = "shutting down"
    })

    /**
     * Logs the current execution status given the [IO] and [unfinished].
     */
    fun status(IO: IO, unfinished: List<Program>) {
        logger.logStatus(items = if (shuttingDown) shuttingDownStatus else unfinished) { IO }
    }

    fun command(input: String) {
        enter(input)
    }

    /**
     * Initiates the systems immediate shutdown.
     */
    fun shutdown() {
        enter(shutdownCommand)
        shuttingDown = true
    }

    fun kill() {
        feedback("Kill invoked")
        process.destroyForcibly()
    }

    var shuttingDown: Boolean by observable(false) { _, oldValue, newValue ->
        feedback("Shutdown invoked")
    }

    override fun toString(): String =
        "RunningOperatingSystem(renderer=${logger.javaClass}, process=$process, shutdownCommand='$shutdownCommand', shuttingDownStatus=$shuttingDownStatus)"
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

    /**
     * Boots the [OperatingSystem] on the [img] and performs the needed steps to get to the command prompt.
     * With that ready [processor] is called with every output.
     */
    fun bootToUserSession(
        scenario: String,
        img: Path,
        parentLogger: BlockRenderingLogger<Any>? = null,
        processor: RunningOperatingSystem.(IO) -> Any,
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
    fun compileScript(name: String, vararg commands: String): Program {
        check(commands.isNotEmpty()) { "Script $name must not be empty." }
        return Program.fromScript(name, readyPattern, *commands)
    }

    /**
     * Creates a program to log [Credentials.username] in using [Credentials.password].
     */
    @OptIn(ExperimentalTime::class)
    fun loginProgram(credentials: Credentials): Program {
        var usernameLastEntered = 0L
        fun RunningOperatingSystem.enterUsername() {
            if (Now.passedSince(usernameLastEntered) > 10.seconds) {
                enter("${credentials.username}\r")
                usernameLastEntered = Now.millis
            }
        }

        var passwordLastEntered = 0L
        fun RunningOperatingSystem.enterPassword() {
            if (Now.passedSince(passwordLastEntered) > 10.seconds) {
                enter("${credentials.password}\r", delay = 500.milliseconds)
                passwordLastEntered = Now.millis
            }
        }

        var pwLineExpectationFailed = 0
        return Program(
            "login", { output -> "1/4: waiting for prompt" },
            "1/4: waiting for prompt" to { output ->
                when {
                    output.matches(loginPattern) -> {
                        enterUsername()
                        "2/4: confirm username"
                    }
                    else -> "1/4: waiting for prompt"
                }
            },
            "2/4: confirm username" to { output ->
                when {
                    !output.matches(loginPattern) -> {
                        "3/4: password..."
                    }
                    else -> "2/4: confirm username"
                }
            },
            "3/4: password..." to { output ->
                when {
                    output.contains("incorrect", ignoreCase = true) -> {
                        throw IncorrectPasswordException(credentials)
                    }
                    output.matches(passwordPattern) || pwLineExpectationFailed % 5 == 2 -> {
                        enterPassword()
                        "4/4 confirm password"
                    }
                    else -> {
                        pwLineExpectationFailed++
                        "3/4: password..."
                    }
                }
            },
            "4/4 confirm password" to { output ->
                when {
                    output.contains("incorrect", ignoreCase = true) -> {
                        throw IncorrectPasswordException(credentials)
                    }
                    listOf("'TAB'", "'ENTER'", "<Ok>").any { output.contains(it, ignoreCase = true) } -> {
                        enter("\t\t\t\t\t", delay = 500.milliseconds)
                        feedback("If something goes wrong, I hope it helps: PID is ${process.pid()}")
                        "4/4 confirm password"
                    }
                    output.matches(loginPattern) -> {
                        "1/4: waiting for prompt"
                    }
                    output.matches(passwordPattern) -> {
                        "3/4: password..."
                    }
                    output.matches(readyPattern) -> {
                        feedback("Logged in successfully")
                        null
                    }
                    else -> "4/4 confirm password"
                }
            },
        )
    }

    /**
     * Creates a program to shutdown immediately.
     */
    @OptIn(ExperimentalTime::class)
    fun shutdownProgram(): Program {
        var shutdownLastEntered = 0L
        fun RunningOperatingSystem.enterShutdown() {
            if (Now.passedSince(shutdownLastEntered) > 10.seconds) {
                enter("sudo shutdown -h now")
                shutdownLastEntered = Now.millis
            }
        }

        return Program(
            "shutdown",
            { output ->
                enterShutdown()
                "shutting down"
            },
            "shutting down" to { output ->
                when {
                    output.matches(readyPattern) -> {
                        enterShutdown()
                        "shutting down"
                    }
                    output.isNotBlank() -> {
                        "shutting down"
                    }
                    else -> null
                }
            },
        )
    }
}

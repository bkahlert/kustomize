package com.imgcstmzr.os

import com.imgcstmzr.logMeta
import koodies.exec.alive
import koodies.logging.RenderingLogger
import koodies.time.Now
import koodies.time.seconds
import koodies.unit.Size
import koodies.unit.milli
import kotlin.text.RegexOption.IGNORE_CASE

/**
 * Representation of an operating system that can be customized.
 */
interface OperatingSystem {

    /**
     * A set of [username] and [password] that can be used to log in to a user session.
     */
    data class Credentials(
        /**
         * The username to be used to log in to a user session.
         */
        val username: String,

        /**
         * The password to be used to log in to a user session.
         */
        val password: String,
    ) {
        companion object {
            private val EMPTY = Credentials("", "")

            /**
             * Factory to create [Credentials] from this string as the username and the specified [password].
             */
            infix fun String.withPassword(password: String): Credentials = Credentials(this, password)

            /**
             * Factory to create empty [Credentials].
             */
            val empty: Credentials = EMPTY
        }
    }

    companion object {
        val DEFAULT_LOGIN_PATTERN: Regex = Regex("(?<host>(?!Last\\b)[\\w-_]+)(?<sep>\\s+)(?<const>login):(?<optWhitespace>.*)", IGNORE_CASE)
        val DEFAULT_PASSWORD_PATTERN: Regex = Regex("Password:(?<optWhitespace>\\s*)", IGNORE_CASE)
        val DEFAULT_READY_PATTERN: Regex = Regex("(?<user>[\\w-_]+)@(?<host>[\\w-_]+):(?<path>[^#$]+?)[#$](?<optWhitespace>\\s*)", IGNORE_CASE)
        val DEFAULT_DEAD_END_PATTERN: Regex = Regex(".*in emergency mode.*", IGNORE_CASE)
        const val DEFAULT_SHUTDOWN_COMMAND: String = "sudo shutdown -h now"
    }

    /**
     * The full name of this [OperatingSystem].
     */
    val fullName: String

    /**
     * Technical name of this [OperatingSystem].
     */
    val name: String

    /**
     * URL that allows an image of this [OperatingSystem] to be downloaded.
     */
    val downloadUrl: String

    /**
     * The approximate [Size] of the actual image file.
     * (And consequently the minimum SD card capacity—plus customizations—needed
     * to store the final image and run it on the actual destined hardware.)
     */
    val approximateImageSize: Size

    /**
     * The [Credentials] to be used to log in to a user session if no others specified.
     */
    val defaultCredentials: Credentials

    /**
     * The [Regex] that matches a login prompt.
     */
    val loginPattern: Regex get() = DEFAULT_LOGIN_PATTERN

    /**
     * The [Regex] that matches a password prompt.
     */
    val passwordPattern: Regex get() = DEFAULT_PASSWORD_PATTERN

    /**
     * The [Regex] that matches a command prompt.
     */
    val readyPattern: Regex get() = DEFAULT_READY_PATTERN

    /**
     * The [Regex] that matches output which signifies that the [OperatingSystem] is stuck and
     * that there is no chance to recover without further steps.
     */
    val deadEndPattern: Regex? get() = DEFAULT_DEAD_END_PATTERN

    /**
     * The command allows to initiate a shutdown.
     */
    val shutdownCommand: String get() = DEFAULT_SHUTDOWN_COMMAND

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
    fun loginProgram(credentials: Credentials): Program {
        var usernameLastEntered = 0L
        fun OperatingSystemProcess.enterUsername() {
            if (Now.passedSince(usernameLastEntered) > 10.seconds) {
                val values = arrayOf(credentials.username)
                if (alive) enter(*values) else feedback("Process $this is not alive.")
                usernameLastEntered = Now.millis
            }
        }

        var passwordLastEntered = 0L
        fun OperatingSystemProcess.enterPassword() {
            if (Now.passedSince(passwordLastEntered) > 10.seconds) {
                enter(credentials.password, delay = 500.milli.seconds)
                passwordLastEntered = Now.millis
            }
        }

        var pwLineExpectationFailed = 0
        return Program(
            "login", { "1/4: waiting for prompt" },
            "1/4: waiting for prompt" to { output ->
                when {
                    // Raspbian GNU/Linux 10 raspberrypi ttyAMA0
                    // raspberrypi login: [  OK  ] Started Regenerate SSH host keys.
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
                        "3/4: password…"
                    }
                    else -> "2/4: confirm username"
                }
            },
            "3/4: password…" to { output ->
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
                        "3/4: password…"
                    }
                }
            },
            "4/4 confirm password" to { output ->
                when {
                    output.contains("incorrect", ignoreCase = true) -> {
                        throw IncorrectPasswordException(credentials)
                    }
                    listOf("'TAB'", "'ENTER'", "<Ok>").any { output.contains(it, ignoreCase = true) } -> {
                        enter("\t\t\t\t\t", delay = 500.milli.seconds)
                        feedback("If something goes wrong, I hope it helps: PID is $pid")
                        "4/4 confirm password"
                    }
                    output.matches(loginPattern) -> {
                        "1/4: waiting for prompt"
                    }
                    output.matches(passwordPattern) -> {
                        "3/4: password…"
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
    fun shutdownProgram(): Program {
        class IOWatchDog(private val logger: RenderingLogger, timedOut: RenderingLogger.() -> Any) : Watchdog(
            timeout = 5.seconds,
            timedOut = timedOut,
            logger = logger,
        ) {
            var lastIO: String? = null
            fun reset(io: String) {
                if (lastIO != io) {
                    super.reset()
                    lastIO = io
                } else logger.logMeta("Watchdog reset attempt ignored. Timing out in $remaining.")
            }
        }

        var deadMansSwitch: IOWatchDog? = null

        var shutdownLastEntered = 0L
        fun OperatingSystemProcess.enterShutdown() {
            if (Now.passedSince(shutdownLastEntered) > 10.seconds) {
                deadMansSwitch?.stop()
                deadMansSwitch = null
                enter(shutdownCommand)
                shutdownLastEntered = Now.millis
            }
        }

        return Program(
            "shutdown",
            {
                deadMansSwitch = IOWatchDog(logger) { enterShutdown() }
                "shutting down"
            },
            "shutting down" to { output ->
                deadMansSwitch?.reset(output)
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
        )//.logging()
    }
}

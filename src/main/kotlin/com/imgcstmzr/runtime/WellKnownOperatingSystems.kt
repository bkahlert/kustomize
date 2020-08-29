package com.imgcstmzr.runtime

import com.imgcstmzr.process.input
import java.io.File

/**
 * Pre-defined operating systems that can be customized.
 */
enum class WellKnownOperatingSystems : OS {

    /**
     * [Raspberry Pi OS Lite](https://www.raspberrypi.org/downloads/raspberry-pi-os/)
     */
    RASPBERRY_PI_OS_LITE {
        override val downloadUrl: String
            get() = "downloads.raspberrypi.org/raspios_lite_armhf_latest"
        override val loginPattern: Regex
            get() = Regex("(?<host>[\\w-_]+)(?<sep>\\s+)(?<const>login):(?<optWhitespace>\\s*)")
        val passwordPattern: Regex
            get() = Regex("Password:(?<optWhitespace>\\s*)")
        override val readyPattern: Regex
            get() = Regex("(?<user>[\\w-_]+)@(?<host>[\\w-_]+):(?<path>[^#$]+?)[#$](?<optWhitespace>\\s*)")

        @Suppress("SpellCheckingInspection")
        override fun startCommand(name: String, img: File): String {
            val dockerRun = arrayOf(
                "docker",
                "run",
                "--name", "\"$name\"",
                "--rm",
                "-i",
                "--volume", "\"$img\":/sdcard/filesystem.img",
                "lukechilds/dockerpi:vm"
            ).joinToString(" ")
            return "$dockerRun || docker rm --force \"$name\" && $dockerRun"
        }

        override fun login(username: String, password: String): Workflow {
            return Workflow(
                "login",
                { process, output -> "waiting for login prompt" },
                "waiting for login prompt" to { process, output ->
                    if (output.matches(loginPattern)) {
                        process.input("$username\r")
                        "waiting for password prompt"
                    } else "waiting for login prompt"
                },
                "waiting for password prompt" to { process, output ->
                    if (output.matches(passwordPattern)) {
                        process.input("$password\r")
                        null
                    } else "waiting for password prompt"
                })
        }

        override fun sequences(purpose: String, commandBlocks: String): Array<Workflow> {
            return commandBlocks.trimIndent().split("\\r\n:", "\r:", "\n:").map { block ->
                block.split("\r\n", "\r", "\n", limit = 2).let {
                    when (it.size) {
                        1 -> sequence(purpose, it[0])
                        else -> sequence(it[0], it[1])
                    }
                }
            }.toTypedArray()
        }

        private fun stateName(index: Int, commands: Array<out String>): String {
            val commandLine = commands[index]
            val command = commandLine.split(' ').first()
            return "${index + 1}/${commands.size}: $command"
        }

        override fun sequence(purpose: String, vararg commands: String): Workflow = Workflow(
            purpose.removePrefix(":").trim(),
            { process, output -> stateName(0, commands) },
            commands.mapIndexed { index, command ->
                val currentStateName: String = stateName(index, commands)
                val nextStateName: String? = if (index + 1 < commands.size) stateName(index + 1, commands) else null
                currentStateName to { process: Process, output: String ->
                    if (output.matches(readyPattern)) {
                        process.input("$command\r")
                        nextStateName
                    } else currentStateName
                }
            }
        )

        override fun sudoSequence(purpose: String, vararg commands: String): Workflow = sequence(purpose, "sudo -i", *commands, "exit")

        override fun shutdown(): Workflow =
            Workflow(
                "shutdown",
                { process, output -> process.input("sudo shutdown -h now\r"); null }
            )
    }
}

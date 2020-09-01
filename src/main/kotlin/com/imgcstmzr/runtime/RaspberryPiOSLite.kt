package com.imgcstmzr.runtime

import com.imgcstmzr.process.CommandLineRunner
import com.imgcstmzr.process.Output
import com.imgcstmzr.process.RunningProcess
import com.imgcstmzr.runtime.Program.Companion.calc
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import java.io.File
import java.lang.ProcessBuilder.Redirect.PIPE
import java.nio.file.Files
import java.nio.file.Path

/**
 * [Raspberry Pi OS Lite](https://www.raspberrypi.org/downloads/raspberry-pi-os/)
 */
class RaspberryPiOSLite : OS<Workflow> {
    override val downloadUrl: String
        get() = "downloads.raspberrypi.org/raspios_lite_armhf_latest"


    override fun increaseDiskSpace(size: Long, img: Path, runtime: Runtime<Workflow>): Int {
        var missing = size - Files.size(img)
        val tenMB = ByteArray(10 * 1024 * 1024)
        if (missing > 0) {
            while (missing > 0) {
                val write = if (missing < tenMB.size) ByteArray(missing.toInt()) else tenMB
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
        runtime: Runtime<Workflow>,
        processor: RunningOS<Workflow>.(Output) -> Unit,
    ): Int {
        val cmd: String = startCommand(runtime.name, img.toFile())
        val cmdRunner = CommandLineRunner(blocking = false)

        val startUp: MutableList<Workflow> = mutableListOf(login("pi", "raspberry"))

        val renderer = BlockRenderingLogger<Workflow>(scencario)
        val runningOS = WorkflowRunningOS(renderer)
        val runningProcess: RunningProcess = cmdRunner.startProcessAndWaitForCompletion(
            directory = Path.of("/bin"),
            shellScript = "sh -c '$cmd'",
            outputRedirect = PIPE,
        ) { output ->
            runningOS.process = this
            if (startUp.isNotEmpty()) {
                renderer.log(output, startUp)
                if (!startUp.calc(runningOS, output)) startUp.removeAt(0)
            } else {
                runningOS.processor(output)
            }
        }
        val exitCode = runningProcess.blockExitCode
        renderer.endLogging(scencario, exitCode)
        return exitCode
    }

    override fun compileSetupScript(name: String, commandBlocks: String): Array<Workflow> =
        Workflow.fromSetupScript(name, readyPattern, commandBlocks)

    override fun compileScript(name: String, vararg commands: String): Workflow =
        Workflow.fromScript(name, readyPattern, *commands)

    @Suppress("SpellCheckingInspection") private fun startCommand(name: String, img: File): String {
        val dockerName = "$name-hot"
        val dockerRun = arrayOf(
            "docker",
            "run",
            "--name", "\"$dockerName\"",
            "--rm",
            "-i",
            "--volume", "\"$img\":/sdcard/filesystem.img",
            "lukechilds/dockerpi:vm"
        ).joinToString(" ")
        return "$(docker rm --force \"$dockerName\" 1>/dev/null 2>&1); $dockerRun"
    }

    fun login(username: String, password: String): Workflow {
        return Workflow(
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

    companion object {
        val loginPattern: Regex
            get() = Regex("(?<host>[\\w-_]+)(?<sep>\\s+)(?<const>login):(?<optWhitespace>\\s*)")
        val passwordPattern: Regex
            get() = Regex("Password:(?<optWhitespace>\\s*)")
        val readyPattern: Regex
            get() = Regex("(?<user>[\\w-_]+)@(?<host>[\\w-_]+):(?<path>[^#$]+?)[#$](?<optWhitespace>\\s*)")
    }
}

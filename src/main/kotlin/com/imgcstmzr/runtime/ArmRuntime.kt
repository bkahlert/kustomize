package com.imgcstmzr.runtime

import com.github.ajalt.clikt.output.TermUi.echo
import com.github.ajalt.mordant.TermColors
import com.imgcstmzr.cli.ColorHelpFormatter.Companion.INSTANCE
import com.imgcstmzr.process.CommandLineRunner
import com.imgcstmzr.process.CommandLineRunner.Origin
import com.imgcstmzr.runtime.WellKnownOperatingSystems.RASPBERRY_PI_OS_LITE
import com.imgcstmzr.stripOffAnsi
import java.io.File
import java.lang.ProcessBuilder.Redirect.PIPE
import java.nio.file.Files
import java.time.Duration

/**
 * Runtime to execute a [img] [File] containing an [ARM](https://en.wikipedia.org/wiki/ARM_architecture) based [OS] like [RASPBERRY_PI_OS_LITE].
 */
class ArmRuntime(
    private val os: OS,
    private val name: String,
    val img: File,
    private val autoShutdown: Boolean = true,
) {

    /**
     * Boots the machine, runs all provided [Workflow] instances
     * and shuts down. Returns the machine's exit code.
     */
    fun bootAndRun(caption: String, vararg workflows: Workflow): Int {
        val watchdog = Watchdog(Duration.ofSeconds(15)) {
            echo(tc.red("The console seems to have halted... ◔̯◔"))
            echo(tc.cyan("To help debugging, you can open a separate console and connect using:"))
            echo(tc.dim(tc.cyan("(ᵔᴥᵔ)$") + (tc.cyan + tc.bold)(" docker attach bother-you")))
        }

        echoProlog(caption)
        val openWorkflows = mutableListOf(*workflows)
        if (autoShutdown) openWorkflows.add(os.shutdown())
        val cmd: String = os.startCommand(name, img)
        val cmdRunner = CommandLineRunner(blocking = false)
        val result = cmdRunner.startProcessAndWaitForCompletion(File("/bin").toPath(), "sh -c '$cmd'", outputRedirect = PIPE) { process, origin, str: String ->
            echoOutput(origin, str, openWorkflows)

            watchdog.reset()
            if (openWorkflows.firstOrNull()?.process(process, str) == false) {
                openWorkflows.removeAt(0)
            }
        }.get()
        echoEpilog(caption, result)
        watchdog.stop()
        return result ?: throw IllegalStateException("This should never happen")
    }

    private fun echoProlog(caption: String) {
        if (borderedOutput) echo(tc.bold("\n╭─────╴$caption\n│"))
    }

    private fun echoEpilog(caption: String, result: Int) {
        if (borderedOutput) echo(tc.bold("│\n╰─────╴$caption : ${INSTANCE.result(result)}\n"))
    }

    private fun echoOutput(origin: Origin, output: String, workflows: List<Workflow>) {
        if (output.stripOffAnsi().isBlank()) return
        val offset = if (autoShutdown) 1 else 0

        val fill = statusPadding(output)
        val prefix = if (borderedOutput) "│   " else ""
        val status = when (workflows.size) {
            0 -> noRunningWorkflowsIndicator
            1, 1 + offset, 2 + offset, 3 + offset -> workflows.joinToString(" ")
            else -> {
                val firstWorkflows = workflows.subList(0, 2).joinToString(" ")
                val hiddenWorkflows = Workflow("…", { _, _ -> "" }).toString()
                val lastWorkflows = workflows.subList(workflows.size - 1 - offset, workflows.size).joinToString(" ")
                listOf(firstWorkflows, hiddenWorkflows, lastWorkflows).joinToString(" ")
            }
        }
        echo(when (origin) {
            Origin.OUT -> {
                "$prefix$output$fill$status"
            }
            Origin.ERR -> prefix + (tc.red + tc.italic)(output)
            Origin.STATUS -> prefix + (tc.gray + tc.italic)(output)
            else -> (tc.magenta + tc.bold)(output.stripOffAnsi())
        })
    }

    companion object {
        private const val borderedOutput: Boolean = true
        private const val statusInformationColumn = 100
        private const val statusInformationMinimalPadding = 5
        private val statusPadding = { output: String ->
            " ".repeat((statusInformationColumn - output.stripOffAnsi().length).coerceAtLeast(statusInformationMinimalPadding))
        }


        private val tc = TermColors()
        private val noRunningWorkflowsIndicator = (tc.gray + tc.italic)("no workflow")

        private fun Collection<Workflow>.status(): String =
            if (this.isEmpty()) noRunningWorkflowsIndicator
            else this.joinToString(separator = " ") { "$it" }
    }

    /**
     * Increases the [img] itself and expands the actual partition(s) to make us of the newly acquired space.
     */
    fun increaseDiskSpace(size: Long): (Long) -> Int {
        return {
            var missing = size - Files.size(img.toPath())
            val tenMB = ByteArray(10 * 1024 * 1024)
            while (missing > 0) {
                val write = if (missing < tenMB.size) ByteArray(missing.toInt()) else tenMB
                img.appendBytes(write)
                missing -= write.size
            }
            bootAndRun("Resizing", os.login("pi", "raspberry"), os.sequence("raspi-config --expand-rootfs"))
        }
    }
}




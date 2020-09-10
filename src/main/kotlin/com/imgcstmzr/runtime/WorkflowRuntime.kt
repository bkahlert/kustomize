package com.imgcstmzr.runtime

import com.imgcstmzr.cli.ColorHelpFormatter.Companion.tc
import com.imgcstmzr.process.Output.Type.META
import com.imgcstmzr.runtime.Program.Companion.calc
import java.nio.file.Path
import java.time.Duration

class WorkflowRuntime(
    override val name: String,
) : Runtime<Workflow> {

    /**
     * Boots the machine using the given [OS] on [img],
     * runs all provided [programs] and finally shuts down the [OS].
     *
     * @return machine's exit code
     */
    override fun bootAndRun(scenario: String, os: OS<Workflow>, img: Path, vararg programs: Workflow): Int {
        val unfinishedPrograms: MutableList<Workflow> = mutableListOf(*programs)

        val watchdog = Watchdog(Duration.ofSeconds(15)) {
            logLine(META typed ("\n" + tc.red("\nThe console seems to have halted... ◔̯◔")), unfinishedPrograms)
            logLine(META typed ("\n" + tc.cyan("To help debugging, you can open a separate console and connect using:")), unfinishedPrograms)
            logLine(META typed (tc.dim(tc.cyan("$") + (tc.cyan + tc.bold)(" docker attach $name-*")) + "\n"), unfinishedPrograms)
        }

        return os.bootAndRun(scenario, img, this@WorkflowRuntime) { output ->
            if (!output.isBlank) {
                status(output, unfinishedPrograms)
                watchdog.reset()
            }

            if (!shuttingDown) {
                val programRunning: Boolean = unfinishedPrograms.calc(this, output)

                if (!programRunning) {
                    unfinishedPrograms.removeAt(0)
                }

                if (unfinishedPrograms.isEmpty()) {
                    shutdown()
                }
            }
        }.also { watchdog.stop() }
    }
}

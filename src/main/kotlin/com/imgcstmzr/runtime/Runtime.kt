package com.imgcstmzr.runtime

import com.imgcstmzr.cli.ColorHelpFormatter
import com.imgcstmzr.process.Output
import com.imgcstmzr.runtime.Program.Companion.calc
import java.nio.file.Path
import java.time.Duration

/**
 * Capable of running programs using an [OperatingSystem].
 */
class Runtime(
    /**
     * Name of this runtime.
     */
    val name: String,
) {

    /**
     * Boots the machine using the given [OperatingSystem] on [img],
     * runs all provided [programs] and finally shuts down the [OperatingSystem].
     *
     * @return machine's exit code
     */
    fun bootAndRun(scenario: String, os: OperatingSystem, img: Path, vararg programs: Program): Int {
        val unfinishedPrograms: MutableList<Program> = mutableListOf(*programs)

        val watchdog = Watchdog(Duration.ofSeconds(15)) {
            logLine(Output.Type.META typed ("\n" + ColorHelpFormatter.tc.red("\nThe console seems to have halted... ◔̯◔")), unfinishedPrograms)
            logLine(Output.Type.META typed ("\n" + ColorHelpFormatter.tc.cyan("To help debugging, you can open a separate console and connect using:")),
                unfinishedPrograms)
            logLine(Output.Type.META typed (ColorHelpFormatter.tc.dim(ColorHelpFormatter.tc.cyan("$") + (ColorHelpFormatter.tc.cyan + ColorHelpFormatter.tc.bold)(
                " docker attach $name-*")) + "\n"), unfinishedPrograms)
        }

        return os.bootAndRun(scenario, img, this) { output ->
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

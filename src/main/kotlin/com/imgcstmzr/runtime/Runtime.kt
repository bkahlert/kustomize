package com.imgcstmzr.runtime

import com.bkahlert.koodies.string.mapLines
import com.bkahlert.koodies.string.replaceNonPrintableCharacters
import com.bkahlert.koodies.terminal.ANSI.EscapeSequences.termColors
import com.bkahlert.koodies.terminal.ansi.Style.Companion.bold
import com.bkahlert.koodies.terminal.ansi.Style.Companion.cyan
import com.bkahlert.koodies.terminal.ascii.Kaomojis
import com.bkahlert.koodies.terminal.removeEscapeSequences
import com.imgcstmzr.process.Output
import com.imgcstmzr.process.Output.Type.ERR
import com.imgcstmzr.process.Output.Type.META
import com.imgcstmzr.runtime.Program.Companion.compute
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.segment
import java.nio.file.Path
import java.time.Duration

/**
 * Boots the machine using the given [OperatingSystem] on [img],
 * runs all programs and finally shuts down the [OperatingSystem].
 *
 * @return machine's exit code
 */
fun Program.bootRunStop(
    scenario: String,
    os: OperatingSystem,
    img: Path,
    parentLogger: BlockRenderingLogger<Unit>?,
): Unit =
    listOf(this).bootRunStop(scenario, os, img, parentLogger)


/**
 * Boots the machine using the given [OperatingSystem] on [img],
 * runs all programs and finally shuts down the [OperatingSystem].
 *
 * @return machine's exit code
 */
fun <P : Program> Collection<P>.bootRunStop(
    scenario: String,
    os: OperatingSystem,
    img: Path,
    parentLogger: BlockRenderingLogger<Unit>?,
) {
    val unfinishedPrograms: MutableList<Program> = this.toMutableList()
    var watchdog: Watchdog? = null
    return parentLogger.segment("Run ${unfinishedPrograms.size} programs on ${os.name}@${img.fileName}", null, additionalInterceptor = { originalMessage ->
        watchdog?.reset()
        originalMessage.removeEscapeSequences().takeIf { line ->
            line.toLowerCase().contains("failed") || line.toLowerCase().contains("error")
        }?.let { replace ->
            return@let replace.mapLines { line ->
                return@mapLines line.removeEscapeSequences().let {
                    it.substringBefore(" ") + " " + ERR.format(it.substringAfter(" "))
                }
            }
        } ?: originalMessage
    }) {
        val outputHistory = mutableListOf<Output>()

        watchdog = Watchdog(Duration.ofSeconds(45), repeating = true) {
            this@segment.logLineLambda { ERR typed ("\n" + termColors.red("\nThe console seems to have halted... ${Kaomojis.Dogs.random()}")) }
            this@segment.logLineLambda(listOf<HasStatus>(object : HasStatus {
                override fun status(): String = Kaomojis.Dogs.random() + " ... console seems to have halted." // TODO
            })) {
                META typed ("\nLast processed output was:\n" + outputHistory.joinToString("\n") {
                    it.unformatted.replaceNonPrintableCharacters()
                })
            }
            this@segment.logLineLambda(unfinishedPrograms) {
                META typed ("\n" + "To help debugging, you can open a separate console and connect using:".cyan())
            }
            this@segment.logLineLambda(unfinishedPrograms) {
                META typed ("$".cyan() + " docker attach ...".cyan().bold() + "\n")
            }
        }

        os.bootToUserSession(scenario, img, this) { output ->
            outputHistory.add(output) // TODO
            if (!output.isBlank) {
                status(output, unfinishedPrograms)
                watchdog?.reset()
            }

            if (!shuttingDown) {
                val programRunning: Boolean = unfinishedPrograms.compute(this, output)

                if (!programRunning) {
                    if (unfinishedPrograms.isNotEmpty()) unfinishedPrograms.removeAt(0)
                }

                if (unfinishedPrograms.isEmpty()) {
                    shutdown()
                }
            }
        }.also { watchdog?.stop() }
    }
}


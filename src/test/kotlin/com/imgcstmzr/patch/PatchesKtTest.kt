package com.imgcstmzr.patch

import com.bkahlert.koodies.nio.ClassPath
import com.bkahlert.koodies.nio.file.exists
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.test.strikt.hasSize
import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
import com.bkahlert.koodies.time.format
import com.bkahlert.koodies.time.parseableInstant
import com.bkahlert.koodies.unit.Giga
import com.bkahlert.koodies.unit.bytes
import com.imgcstmzr.patch.new.buildPatch
import com.imgcstmzr.process.Guestfish
import com.imgcstmzr.process.Guestfish.Companion.copyOutCommands
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystemImage.Companion.based
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.runtime.Program
import com.imgcstmzr.util.DockerRequired
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.copyToTempSiblingDirectory
import com.imgcstmzr.util.logging.CapturedOutput
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.logging.OutputCaptureExtension
import com.imgcstmzr.util.readAll
import com.imgcstmzr.util.touch
import com.imgcstmzr.util.writeLine
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.exists
import strikt.assertions.isEmpty
import strikt.assertions.isNotEmpty
import java.nio.file.Path
import java.time.Instant
import kotlin.time.ExperimentalTime

@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(OutputCaptureExtension::class)
class PatchesKtTest {

    @Nested
    @Isolated // flaky OutputCapture
    inner class ConsoleLoggingByDefault {
        @Test
        fun `should only log to console by default`(img: OperatingSystemImage, capturedOutput: CapturedOutput) {
            val nullPatch = buildPatch("No-Op Patch") {}
            nullPatch.patch(img)
            expectThat(capturedOutput.out.removeEscapeSequences()).isNotEmpty()
        }

        @Test
        fun `should log bordered by default`(img: OperatingSystemImage, capturedOutput: CapturedOutput) {
            val nullPatch = buildPatch("No-Op Patch") {}
            nullPatch.patch(img)
            expectThat(capturedOutput.out.trim().removeEscapeSequences()).matchesCurlyPattern("""
            Started: NO-OP PATCH
             IMG Operations: —
             File System Operations: —
             IMG Operations II: —
             Scripts: —
            Completed: ✔ returned 0
        """.trimIndent())
        }
    }

    @Nested
    @Isolated // flaky OutputCapture
    inner class NoSystemOut {
        @Test
        fun `should only log using specified logger`(img: OperatingSystemImage, logger: InMemoryLogger<Any>, capturedOutput: CapturedOutput) {
            val nullPatch = buildPatch("No-Op Patch") {}
            nullPatch.patch(img, logger)
            expectThat(logger.logged.removeEscapeSequences()).isNotEmpty()
            expectThat(capturedOutput.out.removeEscapeSequences()).isEmpty()
        }
    }

    @Test
    fun `should log not bordered if specified`(osImage: OperatingSystemImage, capturedOutput: CapturedOutput) {
        val logger = InMemoryLogger<Any>("not-bordered", false, -1, emptyList())
        val nullPatch = buildPatch("No-Op Patch") {}
        nullPatch.patch(osImage, logger)
        expectThat(logger.logged.removeEscapeSequences()).matchesCurlyPattern("""
        Started: not-bordered
         Started: NO-OP PATCH
          IMG Operations: —
          File System Operations: —
          IMG Operations II: —
          Scripts: —
         Completed: ✔ returned 0
        """.trimIndent())
    }

    @DockerRequired
    @Test
    fun `should prepare root directory then patch and copy everything back`(osImage: OperatingSystemImage, logger: InMemoryLogger<Any>) {
        val sshPatch = SshPatch()

        sshPatch.patch(osImage, logger)

        val guestfish = Guestfish(osImage based Path.of(osImage.path).copyToTempSiblingDirectory(), logger).withRandomSuffix()
        guestfish.run(copyOutCommands(listOf(Path.of("/boot/ssh"))))
        expectThat(guestfish.guestRootOnHost).get { resolve("boot/ssh") }.exists().get { }
    }

    @ExperimentalTime
    @DockerRequired
    @Test
    fun `should run each op type executing patch successfully`(@OS(RaspberryPiLite::class) osImage: OperatingSystemImage, logger: InMemoryLogger<Any>) {
        val timestamp = Instant.now()

        val patch = buildPatch("Try Everything Patch") {
            preFile {
                resize(2.Giga.bytes)
            }
            guestfish {
                changePassword("pi", "po")
            }
            files {
                edit("/root/.imgcstmzr.created", { path ->
                    require(path.readAll().parseableInstant<Any>())
                }) {
                    it.touch().writeLine(timestamp.format())
                }

                edit("/home/pi/demo.ansi", { path ->
                    require(path.exists)
                }) {
                    ClassPath("demo.ansi").copyTo(it)
                }
            }
            booted {
                run(osImage.compileScript("demo", "sudo cat /root/.imgcstmzr.created", "cat /home/pi/demo.ansi"))
            }
        }

        patch.patch(osImage, logger)

        expectThat(osImage)
            .hasSize(2.Giga.bytes)
            .booted(logger, Program("check",
                { "init" },
                "init" to { read ->
                    enter("sudo cat /root/.imgcstmzr.created")
                    "demo"
                },
                "validate" to {
                    if (it != timestamp.format()) "validate"
                    null
                }
            ))
    }
}

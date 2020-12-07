package com.imgcstmzr.patch

import com.bkahlert.koodies.nio.file.exists
import com.bkahlert.koodies.nio.file.readText
import com.bkahlert.koodies.nio.file.writeLine
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.test.junit.FiveMinutesTimeout
import com.bkahlert.koodies.test.junit.ThirtyMinutesTimeout
import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
import com.bkahlert.koodies.time.format
import com.bkahlert.koodies.time.parseableInstant
import com.bkahlert.koodies.unit.Giga
import com.bkahlert.koodies.unit.Size.Companion.bytes
import com.imgcstmzr.E2E
import com.imgcstmzr.guestfish.Guestfish
import com.imgcstmzr.guestfish.Guestfish.Companion.copyOutCommands
import com.imgcstmzr.libguestfs.libguestfs
import com.imgcstmzr.patch.new.buildPatch
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.runtime.Program
import com.imgcstmzr.runtime.size
import com.imgcstmzr.util.DockerRequiring
import com.imgcstmzr.util.MiscFixture
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.logging.CapturedOutput
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.logging.OutputCaptureExtension
import com.imgcstmzr.util.touch
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.exists
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEmpty
import java.nio.file.Path
import java.time.Instant

@Execution(CONCURRENT)
@ExtendWith(OutputCaptureExtension::class)
class PatchKtTest {

    @Nested
    @Isolated("flaky OutputCapture")
    inner class ConsoleLoggingByDefault {
        @Test
        fun `should only log to console by default`(osImage: OperatingSystemImage, capturedOutput: CapturedOutput) {
            val nullPatch = buildPatch("No-Op Patch") {}
            nullPatch.patch(osImage)
            expectThat(capturedOutput.out.removeEscapeSequences()).isNotEmpty()
        }

        @Test
        fun `should log bordered by default`(osImage: OperatingSystemImage, capturedOutput: CapturedOutput) {
            val nullPatch = buildPatch("No-Op Patch") {}
            nullPatch.patch(osImage)
            expectThat(capturedOutput.out.trim().removeEscapeSequences()).matchesCurlyPattern("""
            Started: NO-OP PATCH
             IMG Operations: —
             File System Operations: —
             IMG Operations II: —
             Scripts: —
            Completed: ✔
        """.trimIndent())
        }
    }

    @Nested
    @Isolated("flaky OutputCapture")
    inner class NoSystemOut {
        @Test
        fun `should only log using specified logger`(osImage: OperatingSystemImage, logger: InMemoryLogger, capturedOutput: CapturedOutput) {
            val nullPatch = buildPatch("No-Op Patch") {}
            nullPatch.patch(osImage, logger)
            expectThat(logger.logged.removeEscapeSequences()).isNotEmpty()
            expectThat(capturedOutput.out.removeEscapeSequences()).isEmpty()
        }
    }

    @Test
    fun `should log not bordered if specified`(osImage: OperatingSystemImage, capturedOutput: CapturedOutput) {
        val logger = InMemoryLogger("not-bordered", false, -1, emptyList())
        val nullPatch = buildPatch("No-Op Patch") {}
        nullPatch.patch(osImage, logger)
        expectThat(logger.logged.removeEscapeSequences()).matchesCurlyPattern("""
        Started: not-bordered
         Started: NO-OP PATCH
          IMG Operations: —
          File System Operations: —
          IMG Operations II: —
          Scripts: —
         Completed: ✔
        """.trimIndent())
    }

    @FiveMinutesTimeout @DockerRequiring @Test
    fun `should prepare root directory then patch and copy everything back`(osImage: OperatingSystemImage, logger: InMemoryLogger) {
        val sshPatch = SshEnablementPatch()

        sshPatch.patch(osImage, logger)

        val guestfish = Guestfish(osImage.duplicate(), logger).withRandomSuffix()
        guestfish.run(copyOutCommands(listOf(Path.of("/boot/ssh"))))
        expectThat(guestfish.guestRootOnHost).get { resolve("boot/ssh") }.exists().get { }
    }

    @ThirtyMinutesTimeout @E2E @Test
    fun `should run each op type executing patch successfully`(@OS(RaspberryPiLite) osImage: OperatingSystemImage, logger: InMemoryLogger) {
        val timestamp = Instant.now()

        val patch = buildPatch("Try Everything Patch") {
            preFile {
                resize(2.Giga.bytes)
            }
            libguestfs {
                // TODO TODO TODO test and guestfish and command line (format)
                hostname { "test-machine" }
            }
//            guestfish {
//                changePassword("pi", "po")
//            }
            files {
                edit("/root/.imgcstmzr.created", { path ->
                    require(path.readText().parseableInstant<Any>())
                }) {
                    it.touch().writeLine(timestamp.format())
                }

                edit("/home/pi/demo.ansi", { path ->
                    require(path.exists)
                }) {
                    MiscFixture.AnsiDocument.copyTo(it)
                }
            }
            booted {
                run(osImage.compileScript("demo", "sudo cat /root/.imgcstmzr.created", "cat /home/pi/demo.ansi"))
            }
        }

        patch.patch(osImage, logger)

        expectThat(osImage) {
            size.isEqualTo(2.Giga.bytes)
            booted(logger, Program("check",
                { "init" },
                "init" to {
                    enter("sudo cat /etc/hostname")
                    enter("sudo cat /root/.imgcstmzr.created")
                    "demo"
                },
                "validate" to {
                    if (it != timestamp.format()) "validate"
                    else null
                }
            ))
        }
    }
}

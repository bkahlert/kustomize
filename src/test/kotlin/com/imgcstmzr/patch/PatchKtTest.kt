package com.imgcstmzr.patch

import com.bkahlert.koodies.nio.file.exists
import com.bkahlert.koodies.nio.file.readText
import com.bkahlert.koodies.nio.file.toPath
import com.bkahlert.koodies.nio.file.writeLine
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.test.junit.ThirtyMinutesTimeout
import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
import com.bkahlert.koodies.time.format
import com.bkahlert.koodies.time.parseableInstant
import com.bkahlert.koodies.unit.Giga
import com.bkahlert.koodies.unit.Size.Companion.bytes
import com.imgcstmzr.E2E
import com.imgcstmzr.libguestfs.resolveOnDisk
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.runtime.Program
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.size
import com.imgcstmzr.util.MiscFixture
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.hasContent
import com.imgcstmzr.util.logging.CapturedOutput
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.logging.OutputCaptureExtension
import com.imgcstmzr.util.logging.expectThatLogged
import com.imgcstmzr.util.touch
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEmpty
import java.time.Instant

@Execution(CONCURRENT)
@ExtendWith(OutputCaptureExtension::class)
class PatchKtTest {

    @Nested
    @Isolated("flaky OutputCapture")
    inner class ConsoleLoggingByDefault {
        @Test
        fun `should only log to console by default`(osImage: OperatingSystemImage, capturedOutput: CapturedOutput) {
            val logger: RenderingLogger? = null
            with(buildPatch("No-Op Patch") {}) {
                patch(osImage, logger)
            }
            expectThat(capturedOutput.out.removeEscapeSequences()).isNotEmpty()
        }

        @Test
        fun `should log bordered by default`(osImage: OperatingSystemImage, capturedOutput: CapturedOutput) {
            val logger: RenderingLogger? = null
            with(buildPatch("No-Op Patch") {}) {
                patch(osImage, logger)
            }
            expectThat(capturedOutput.out.trim().removeEscapeSequences()).matchesCurlyPattern("""
            ╭─────╴No-Op Patch
            │   
            │   Disk Preparation: —
            │   Disk Customization: —
            │   Disk Operations: —
            │   File Operations: —
            │   OS Preparation: —
            │   OS Operations: —
            │
            ╰─────╴✔
        """.trimIndent())
        }
    }

    @Test
    fun `should log not bordered if specified`(osImage: OperatingSystemImage, capturedOutput: CapturedOutput) {
        with(InMemoryLogger("not-bordered", borderedOutput = false, statusInformationColumn = -1, outputStreams = emptyList())) {
            patch(osImage, buildPatch("No-Op Patch") {})

            expectThatLogged().matchesCurlyPattern("""
                Started: not-bordered
                 Started: No-Op Patch
                  Disk Preparation: —
                  Disk Customization: —
                  Disk Operations: —
                  File Operations: —
                  OS Preparation: —
                  OS Operations: —
                 Completed: ✔
        """.trimIndent())
        }
    }

    @ThirtyMinutesTimeout @E2E @Test
    fun InMemoryLogger.`should run each op type executing patch successfully`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        val timestamp = Instant.now()

        patch(osImage, buildPatch("Try Everything Patch") {
            prepareDisk {
                resize(2.Giga.bytes)
            }
            customizeDisk {
                hostname { "test-machine" }
            }
            guestfish {
                copyOut { it.resolveOnDisk("/etc/hostname") }
            }
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
            os {
                script("demo", "sudo cat /root/.imgcstmzr.created", "cat /home/pi/demo.ansi")
            }
        })

        expect {
            that(osImage) {
                getShared("/etc/hostname".toPath()).hasContent("test-machine\n")
                size.isEqualTo(2.Giga.bytes)
                booted(this@`should run each op type executing patch successfully`, Program("check",
                    { "init" },
                    "init" to {
                        enter("sudo cat /etc/hostname")
                        enter("sudo cat /root/.imgcstmzr.created")
                        "validate"
                    },
                    "validate" to {
                        if (it != timestamp.format()) "validate"
                        else null
                    }
                ))
            }
        }
    }
}


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
import com.imgcstmzr.util.logging.getExpectThatLogged
import com.imgcstmzr.util.touch
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.isEmpty
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
            with(logger) {
                with(buildPatch(osImage.operatingSystem, "No-Op Patch") {}) {
                    patch(osImage)
                }
            }
            expectThat(capturedOutput.out.removeEscapeSequences()).isNotEmpty()
        }

        @Test
        fun `should log bordered by default`(osImage: OperatingSystemImage, capturedOutput: CapturedOutput) {
            val logger: RenderingLogger? = null
            with(logger) {
                with(buildPatch(osImage.operatingSystem, "No-Op Patch") {}) {
                    patch(osImage)
                }
            }
            expectThat(capturedOutput.out.trim().removeEscapeSequences()).matchesCurlyPattern("""
            Started: NO-OP PATCH
             IMG Operations: —
             Customization Options: —
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
        fun InMemoryLogger.`should only log using specified logger`(osImage: OperatingSystemImage, capturedOutput: CapturedOutput) {
            with(buildPatch(osImage.operatingSystem, "No-Op Patch") {}) {
                patch(osImage)
            }
            getExpectThatLogged().isNotEmpty()
            expectThat(capturedOutput.out.removeEscapeSequences()).isEmpty()
        }
    }

    @Test
    fun `should log not bordered if specified`(osImage: OperatingSystemImage, capturedOutput: CapturedOutput) {
        with(InMemoryLogger("not-bordered", borderedOutput = false, statusInformationColumn = -1, outputStreams = emptyList())) {
            with(buildPatch(osImage.operatingSystem, "No-Op Patch") {}) {
                patch(osImage)
            }
            getExpectThatLogged().matchesCurlyPattern("""
        Started: not-bordered
         Started: NO-OP PATCH
          IMG Operations: —
          Customization Options: —
          File System Operations: —
          IMG Operations II: —
          Scripts: —
         Completed: ✔
        """.trimIndent())
        }
    }

    @ThirtyMinutesTimeout @E2E @Test
    fun InMemoryLogger.`should run each op type executing patch successfully`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        val timestamp = Instant.now()

        with(buildPatch(osImage.operatingSystem, "Try Everything Patch") {
            preFile {
                resize(2.Giga.bytes)
            }
            customize {
                hostname { "test-machine" }
            }
            guestfish {
                copyOut("/etc/hostname".toPath())
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
            booted {
                run(osImage.compileScript("demo", "sudo cat /root/.imgcstmzr.created", "cat /home/pi/demo.ansi"))
            }
        }) { patch(osImage) }

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


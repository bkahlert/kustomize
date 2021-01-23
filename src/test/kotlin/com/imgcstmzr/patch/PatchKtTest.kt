package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommand
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.runtime.Program
import com.imgcstmzr.runtime.size
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.MiscClassPathFixture
import com.imgcstmzr.test.OS
import com.imgcstmzr.test.ThirtyMinutesTimeout
import com.imgcstmzr.test.hasContent
import com.imgcstmzr.test.logging.CapturedOutput
import com.imgcstmzr.test.logging.OutputCaptureExtension
import com.imgcstmzr.test.logging.expectThatLogged
import com.imgcstmzr.test.matchesCurlyPattern
import koodies.io.path.touch
import koodies.io.path.writeLine
import koodies.logging.InMemoryLogger
import koodies.logging.RenderingLogger
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.test.copyTo
import koodies.time.format
import koodies.time.parseableInstant
import koodies.unit.Giga
import koodies.unit.bytes
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.Assertion
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEmpty
import java.time.Instant
import kotlin.io.path.exists
import kotlin.io.path.readText

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
            expectThat(capturedOutput.out.trim().removeEscapeSequences()).matchesCurlyPattern(
                """
            ╭─────╴No-Op Patch
            │   
            │   Disk Preparation: —
            │   Disk Customization: —
            │   Disk Operations: —
            │   File Operations: —
            │   OS Preparation: —
            │   OS Boot: —
            │   OS Operations: —
            │
            ╰─────╴➜️ []
        """.trimIndent()
            )
        }
    }

    @Test
    fun `should log not bordered if specified`(osImage: OperatingSystemImage, capturedOutput: CapturedOutput) {
        with(
            InMemoryLogger(
                "not-bordered",
                bordered = false,
                statusInformationColumn = -1,
                outputStreams = emptyList()
            )
        ) {
            patch(osImage, buildPatch("No-Op Patch") {})

            expectThatLogged().matchesCurlyPattern(
                """
                ▶ not-bordered
                · 
                · ╭─────╴No-Op Patch
                · │   
                · │   Disk Preparation: —
                · │   Disk Customization: —
                · │   Disk Operations: —
                · │   File Operations: —
                · │   OS Preparation: —
                · │   OS Boot: —
                · │   OS Operations: —
                · │
                · ╰─────╴➜️ []
                ·
        """.trimIndent()
            )
        }
    }

    @ThirtyMinutesTimeout
    @E2E
    @Test
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
                copyOut { DiskPath("/etc/hostname") }
            }
            files {
                edit(DiskPath("/root/.imgcstmzr.created"), { path ->
                    require(path.readText().parseableInstant<Any>())
                }) {
                    it.touch().writeLine(timestamp.format())
                }

                edit(DiskPath("/home/pi/demo.ansi"), { path ->
                    require(path.exists())
                }) {
                    MiscClassPathFixture.AnsiDocument.copyTo(it)
                }
            }
            os {
                script("demo", "sudo cat /root/.imgcstmzr.created", "cat /home/pi/demo.ansi")
            }
        })

        expect {
            that(osImage) {
                hostPath("/etc/hostname").hasContent("test-machine\n")
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


fun <T : Patch> Assertion.Builder<T>.matches(
    diskOperationsAssertion: Assertion.Builder<List<DiskOperation>>.() -> Unit = { hasSize(0) },
    customizationOptionsAssertion: Assertion.Builder<List<(OperatingSystemImage) -> VirtCustomizeCustomizationOption>>.() -> Unit = {
        hasSize(
            0
        )
    },
    guestfishCommandsAssertion: Assertion.Builder<List<(OperatingSystemImage) -> GuestfishCommand>>.() -> Unit = {
        hasSize(
            0
        )
    },
    fileSystemOperationsAssertion: Assertion.Builder<List<FileOperation>>.() -> Unit = { hasSize(0) },
    programsAssertion: Assertion.Builder<List<(OperatingSystemImage) -> Program>>.() -> Unit = { hasSize(0) },
) = compose("matches") { patch ->
    diskOperationsAssertion(get { diskPreparations })
    customizationOptionsAssertion(get { diskCustomizations })
    guestfishCommandsAssertion(get { diskOperations })
    fileSystemOperationsAssertion(get { fileOperations })
    programsAssertion(get { osOperations })
}.then { if (allPassed) pass() else fail() }


fun <T : Patch> Assertion.Builder<T>.customizations(
    osImage: OperatingSystemImage,
    block: Assertion.Builder<List<VirtCustomizeCustomizationOption>>.() -> Unit,
) = get("virt-customizations") { diskCustomizations.map { it(osImage) } }.block()

fun <T : Patch> Assertion.Builder<T>.getCustomizations(osImage: OperatingSystemImage, index: Int) =
    get("${index}th virt-customizations") { diskCustomizations[index].let { it(osImage) } }

fun <T : Patch> Assertion.Builder<T>.guestfishCommands(
    osImage: OperatingSystemImage,
    block: Assertion.Builder<List<GuestfishCommand>>.() -> Unit,
) = get("guestfish commands") { diskOperations.map { it(osImage) } }.block()

package com.imgcstmzr.patch

import com.bkahlert.koodies.nio.ClassPath
import com.bkahlert.koodies.terminal.removeEscapeSequences
import com.bkahlert.koodies.test.strikt.hasSize
import com.bkahlert.koodies.test.strikt.matches
import com.bkahlert.koodies.time.DateTimeFormatters.ISO8601_INSTANT
import com.bkahlert.koodies.time.format
import com.bkahlert.koodies.time.parseableInstant
import com.bkahlert.koodies.unit.Gibi
import com.bkahlert.koodies.unit.Giga
import com.bkahlert.koodies.unit.bytes
import com.imgcstmzr.patch.new.buildPatch
import com.imgcstmzr.process.Guestfish
import com.imgcstmzr.process.Guestfish.Companion.copyOutCommands
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.runtime.RunningOS
import com.imgcstmzr.util.DockerRequired
import com.imgcstmzr.util.FixtureResolverExtension
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.copyToTempSiblingDirectory
import com.imgcstmzr.util.exists
import com.imgcstmzr.util.logging.CapturedOutput
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.logging.OutputCaptureExtension
import com.imgcstmzr.util.readAll
import com.imgcstmzr.util.touch
import com.imgcstmzr.util.writeText
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.Assertion.Builder
import strikt.api.DescribeableBuilder
import strikt.api.expectThat
import strikt.assertions.exists
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEmpty
import java.nio.file.Path
import java.time.Instant
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(FixtureResolverExtension::class, OutputCaptureExtension::class)
internal class PatchesKtTest {

    @Nested
    @Isolated // flaky OutputCapture
    inner class ConsoleLoggingByDefault {
        @Test
        internal fun `should only log to console by default`(img: Path, capturedOutput: CapturedOutput) {
            val nullPatch = buildPatch("No-Op Patch") {}
            nullPatch.patch(img)
            expectThat(capturedOutput.out.removeEscapeSequences()).isNotEmpty()
        }

        @Test
        internal fun `should log bordered by default`(img: Path, capturedOutput: CapturedOutput) {
            val nullPatch = buildPatch("No-Op Patch") {}
            nullPatch.patch(img)
            expectThat(capturedOutput.out.trim().removeEscapeSequences()).matches("""
            Started: {}
             IMG Operations: —
             File System Operations: —
             Scripts: —
            Completed: {}
        """.trimIndent())
        }
    }

    @Test
    internal fun `should only log using specified logger`(img: Path, logger: InMemoryLogger<Any>, capturedOutput: CapturedOutput) {
        val nullPatch = buildPatch("No-Op Patch") {}
        nullPatch.patch(img, logger)
        expectThat(logger.logged.removeEscapeSequences()).isNotEmpty()
        expectThat(capturedOutput.out.removeEscapeSequences()).isEmpty()
    }

    @Test
    internal fun `should log not bordered if specified`(img: Path, capturedOutput: CapturedOutput) {
        val logger = InMemoryLogger<Any>("not-bordered", false, emptyList())
        val nullPatch = buildPatch("No-Op Patch") {}
        nullPatch.patch(img, logger)
        expectThat(logger.logged.removeEscapeSequences()).matches("""
        Started: not-bordered
         Started: NO-OP PATCH
          IMG Operations: —
          File System Operations: —
          Scripts: —
         Completed: ✔ returned ❬0⫻1❭
        """.trimIndent())
    }

    @DockerRequired
    @Test
    internal fun `should prepare root directory then patch and copy everything back`(img: Path, logger: InMemoryLogger<Any>) {
        val sshPatch = SshPatch()

        sshPatch.patch(img, logger)

        val guestfish = Guestfish(img.copyToTempSiblingDirectory(), logger).withRandomSuffix()
        guestfish.run(copyOutCommands(listOf(Path.of("/boot/ssh"))))
        expectThat(guestfish.guestRootOnHost).get { resolve("boot/ssh") }.exists().get { }
    }


    @ExperimentalTime
    @DockerRequired
    @Test
    internal fun `should run each op type executing patch successfully`(@OS(RaspberryPiLite::class) img: Path, logger: InMemoryLogger<Any>) {
        val now = Instant.now()
        val nowString = ISO8601_INSTANT.format<Instant>(now)

        val fullyFleshedPatch = buildPatch("Try Everything Patch") {
            img {
                resize(2.Giga.bytes)
            }
            guestfish {
                changePassword("pi", "po")
            }
            files {
                edit("/root/.imgcstmzr.created", { path ->
                    require(path.readAll().parseableInstant<Any>())
                }) {
                    it.touch().writeText(now.format())
                }

                edit("/home/pi/BKAHLERT.png", { path ->
                    require(path.exists)
                }) {
                    ClassPath("BKAHLERT.png").copyTo(it)
                }
            }
            booted {
                @Suppress("SpellCheckingInspection")
                program(
                    "printing .imgcstmzr.created",
                    { "init" },
                    "init" to { read ->
                        input("sudo cat /root/.imgcstmzr.created")
                        "chafa"
                    },
                    "chafa" to { read ->
                        if (RaspberryPiLite.readyPattern.matches(read)) {
                            input("sudo apt install -y -m chafa")
                            "chafa-install"
                        } else "chafa"
                    },
                    "chafa-install" to { read ->
                        if (RaspberryPiLite.readyPattern.matches(read)) "logo"
                        else "chafa-install"
                    },
                    "logo" to {
                        input("chafa /home/pi/BKAHLERT.png")
                        null
                    }
                )
            }
        }

        fullyFleshedPatch.patch(img, logger)

        expectThat(img)
            .hasSize(2.Gibi.bytes)
            .booted<RaspberryPiLite>(logger) {
                command("chafa /home/pi/BKAHLERT.png --duration 5");
                wait(5.seconds);
                { true }
            }
    }
}

inline fun <reified T : RunningOS> Builder<T>.command(input: String): DescribeableBuilder<String?> = get("running $input") {
    input(input)
    readLine()
}

fun Builder<String?>.outputs(expected: String): Unit {
    isEqualTo(expected)
}

interface RunningOSX

val <T : RunningOSX> Builder<T>.command: Builder<T>
    get() = get { this }

inline fun <reified T : OperatingSystem, U : CharSequence> Builder<Path>.booted(
    crossinline assertion: Builder<RunningOS>.() -> Unit,
): Builder<Path> {
    val os = T::class.objectInstance ?: error("Invalid OS")
    get("booted $os") {
        os.bootToUserSession("Booting to assert $this", this) { output ->
            get("logged in") { this@bootToUserSession }.apply(assertion)
        }
    }

    return this
}

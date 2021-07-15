package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.GuestAssertions
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystemProcess
import com.imgcstmzr.os.OperatingSystemProcess.Companion.DockerPiImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.os.Program
import com.imgcstmzr.os.ProgramState
import com.imgcstmzr.os.boot
import com.imgcstmzr.os.size
import com.imgcstmzr.patch.Patch.Companion.buildPatch
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.OS
import koodies.docker.DockerContainer
import koodies.docker.DockerRequiring
import koodies.exec.Process.State.Exited.Succeeded
import koodies.exec.output
import koodies.io.path.hasContent
import koodies.io.path.touch
import koodies.io.path.withDirectoriesCreated
import koodies.io.path.writeLine
import koodies.io.path.writeText
import koodies.io.toAsciiArt
import koodies.regex.groupValue
import koodies.shell.ScriptInit
import koodies.shell.ShellScript
import koodies.test.CapturedOutput
import koodies.test.FiveMinutesTimeout
import koodies.test.SixtyMinutesTimeout
import koodies.test.SvgFixture
import koodies.test.SystemIOExclusive
import koodies.text.LineSeparators.LF
import koodies.text.Semantics.formattedAs
import koodies.text.matchesCurlyPattern
import koodies.time.format
import koodies.time.parseableInstant
import koodies.unit.Giga
import koodies.unit.bytes
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isGreaterThanOrEqualTo
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.MULTILINE

@SystemIOExclusive
class PatchKtTest {

    private val nullPatch = buildPatch("No-Op Patch") {}

    @Test
    fun `should log not bordered if specified`(osImage: OperatingSystemImage, output: CapturedOutput) {
        nullPatch.patch(osImage)

        output.all.matchesCurlyPattern("""
            {{}}
            {}╭──╴No-Op Patch
            {}│   
            {}│   Disk Preparation: —
            {}│   Disk Customization: —
            {}│   Disk Operations: —
            {}│   File Operations: —
            {}│   OS Preparation: —
            {}│   OS Boot: —
            {}│   OS Operations: —
            {}│
            {}╰──╴✔︎
            """.trimIndent())
    }

    @FiveMinutesTimeout @DockerRequiring([DockerPiImage::class]) @Test
    fun `should empty exchange directory before file operations`(@OS(RaspberryPiLite) osImage: OperatingSystemImage, output: CapturedOutput) {
        osImage.hostPath(LinuxRoot / "home/pi/local.txt").withDirectoriesCreated().createFile().writeText("local")
        val patch = buildPatch("empty exchange") {
            files {
                edit(LinuxRoot / "home/pi/file.txt", { require(it.exists()) }) {
                    it.writeText("content")
                }
            }
        }

        patch.patch(osImage)

        expectThat(output.all) {
            contains("Copying in 1 file(s)")
            not { contains("Copying in 2 file(s)") }
        }
    }

    @SixtyMinutesTimeout @DockerRequiring([DockerPiImage::class]) @E2E @Test
    fun `should run each op type executing patch successfully`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        val timestamp = Instant.now()

        buildPatch("Try Everything Patch") {
            prepareDisk {
                resize(2.Giga.bytes)
            }
            customizeDisk {
                hostname { "test-machine" }
            }
            guestfish {
                copyOut { LinuxRoot.etc / "hostname" }
            }
            files {
                edit(LinuxRoot.root / ".imgcstmzr.created", {
                    require(it.readText().trim().parseableInstant<Any>())
                }) {
                    it.touch().writeLine(timestamp.format())
                }

                edit(LinuxRoot / "home/pi/demo.ansi", {
                    require(it.exists())
                }) {
                    it.writeText(SvgFixture.toAsciiArt())
                }
            }
            os {
                script("demo", "sudo cat /root/.imgcstmzr.created", "cat /home/pi/demo.ansi")
            }
        }.patch(osImage)

        expectThat(osImage) {
            hostPath("/etc/hostname").hasContent("test-machine$LF")
            size.isGreaterThanOrEqualTo(2.Giga.bytes)
            booted({ "init" },
                "init" to {
                    enter("sudo cat /etc/hostname")
                    enter("sudo cat /root/.imgcstmzr.created")
                    "validate"
                },
                "validate" to {
                    if (it != timestamp.format()) "validate"
                    else null
                }
            )
        }
    }
}


fun <T : Patch> Assertion.Builder<T>.matches(
    diskOperationsAssertion: Assertion.Builder<List<DiskOperation>>.() -> Unit = { hasSize(0) },
    customizationsAssertion: Assertion.Builder<List<(OperatingSystemImage) -> Customization>>.() -> Unit = {
        hasSize(
            0
        )
    },
    guestfishCommandsAssertion: Assertion.Builder<List<(OperatingSystemImage) -> GuestfishCommand>>.() -> Unit = {
        hasSize(
            0
        )
    },
    fileSystemOperationsAssertion: Assertion.Builder<List<(OperatingSystemImage) -> FileOperation>>.() -> Unit = { hasSize(0) },
    programsAssertion: Assertion.Builder<List<(OperatingSystemImage) -> Program>>.() -> Unit = { hasSize(0) },
) = compose("matches") {
    diskOperationsAssertion(get { diskPreparations })
    customizationsAssertion(get { diskCustomizations })
    guestfishCommandsAssertion(get { diskOperations })
    fileSystemOperationsAssertion(get { fileOperations })
    programsAssertion(get { osOperations })
}.then { if (allPassed) pass() else fail() }


fun <T : Patch> Assertion.Builder<T>.customizations(
    osImage: OperatingSystemImage,
    block: Assertion.Builder<List<Customization>>.() -> Unit,
) = get("virt-customizations") { diskCustomizations.map { it(osImage) } }.block()

fun <T : Patch> Assertion.Builder<T>.getCustomizations(osImage: OperatingSystemImage, index: Int) =
    get("${index}th virt-customizations") { diskCustomizations[index].let { it(osImage) } }

fun <T : Patch> Assertion.Builder<T>.guestfishCommands(
    osImage: OperatingSystemImage,
    block: Assertion.Builder<List<GuestfishCommand>>.() -> Unit,
) = get("guestfish commands") { diskOperations.map { it(osImage) } }.block()


/**
 * Returns callable that mounts `this` [OperatingSystemImage]
 * and runs the specified [GuestAssertions].
 */
inline val booted: Assertion.Builder<OperatingSystemImage>.(assertion: OperatingSystemProcess.(String) -> ((String) -> Boolean)?) -> Unit
    get() = { assertion: OperatingSystemProcess.(String) -> ((String) -> Boolean)? -> booted(assertion) }

inline fun Assertion.Builder<OperatingSystemImage>.booted(
    crossinline assertion: OperatingSystemProcess.(String) -> ((String) -> Boolean)?,
): Assertion.Builder<OperatingSystemImage> {
    get("booted ${this.get { operatingSystem }}") {

        val verificationStep: OperatingSystemProcess.(String) -> String? = { output: String ->
            val asserter: ((String) -> Boolean)? = this.assertion(output)
            if (asserter != null) {
                val successfullyTested = asserter(output)
                if (!successfullyTested) " …"
                else null
            } else {
                null
            }
        }

        val container = DockerContainer.from(file)
        kotlin.runCatching {
            boot(
                container.name, Program("test", { "testing" }, "testing" to verificationStep),
                decorationFormatter = { it.formattedAs.debug },
            )
        }.onFailure { container.remove(force = true) }.getOrThrow()
    }

    return this
}

/**
 * Runs the specified [script] in the currently running [OperatingSystemProcess]
 * and wraps the output (roughly, not exactly) with the given [outputMarker].
 *
 * Returns
 */
inline fun OperatingSystemProcess.script(
    outputMarker: String = "\u200B\uFEFF".repeat(5),
    crossinline script: ScriptInit,
): Sequence<String> {
    val snippet = ShellScript {
        embed(ShellScript {
            !"printf '$outputMarker'"
            script()
            !"printf '$outputMarker'"
        }, true)
    }.toString()
    command(snippet)
    return Sequence {
        val regex = Regex(Regex.escape(outputMarker) + "(?<captured>.*)" + Regex.escape(outputMarker), setOf(DOT_MATCHES_ALL, MULTILINE))
        regex.findAll(io.output.ansiRemoved).mapNotNull { it.groupValue("captured") }.iterator()
    }
}

/**
 * Assertions on the directory used to share files with the [OperatingSystemImage].
 */
fun Assertion.Builder<OperatingSystemImage>.hostPath(path: String): DescribeableBuilder<Path> =
    get("shared directory %s") { hostPath(LinuxRoot / path) }

fun Assertion.Builder<OperatingSystemImage>.booted(
    initialState: OperatingSystemProcess.() -> String?,
    vararg states: ProgramState,
): Assertion.Builder<OperatingSystemImage> =
    assert("booted ${this.get { operatingSystem }}") {
        when (val exitState = it.boot(DockerContainer.from("Assertion Boot of $this").name, Program("check", initialState, *states))) {
            is Succeeded -> pass()
            else -> fail(exitState)
        }
    }

inline fun <reified T : OperatingSystemProcess> Assertion.Builder<T>.command(input: String): DescribeableBuilder<String?> = get("running $input") {
    enter(input)
    readLine()
}

interface RunningOSX

val <T : RunningOSX> Assertion.Builder<T>.command: Assertion.Builder<T>
    get() = get { this }

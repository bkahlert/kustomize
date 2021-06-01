package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.GuestAssertions
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization
import com.imgcstmzr.os.DiskPath
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
import koodies.io.path.writeLine
import koodies.io.path.writeText
import koodies.io.toAsciiArt
import koodies.logging.BlockRenderingLogger
import koodies.logging.FixedWidthRenderingLogger
import koodies.logging.FixedWidthRenderingLogger.Border.DOTTED
import koodies.logging.InMemoryLogger
import koodies.logging.RenderingLogger
import koodies.logging.expectLogged
import koodies.regex.groupValue
import koodies.shell.ScriptInit
import koodies.shell.ShellScript
import koodies.test.SvgFixture
import koodies.test.ThirtyMinutesTimeout
import koodies.text.LineSeparators.LF
import koodies.text.Unicode
import koodies.text.matchesCurlyPattern
import koodies.text.repeat
import koodies.time.format
import koodies.time.parseableInstant
import koodies.unit.Giga
import koodies.unit.bytes
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isGreaterThanOrEqualTo
import java.time.Instant
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.MULTILINE

class PatchKtTest {

    private val nullPatch = buildPatch("No-Op Patch") {}

    @Test
    fun InMemoryLogger.`should log not bordered if specified`(osImage: OperatingSystemImage) {
        nullPatch.patch(osImage)

        expectLogged.matchesCurlyPattern("""
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

    @ThirtyMinutesTimeout @DockerRequiring([DockerPiImage::class]) @E2E @Test
    fun InMemoryLogger.`should run each op type executing patch successfully`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        val timestamp = Instant.now()

        buildPatch("Try Everything Patch") {
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
                edit(DiskPath("/root/.imgcstmzr.created"), {
                    require(it.readText().trim().parseableInstant<Any>())
                }) {
                    it.touch().writeLine(timestamp.format())
                }

//                edit(DiskPath("/home/pi/demo.ansi"), { path ->
//                    require(path.exists())
//                }) {
//                    MiscClassPathFixture.AnsiDocument.copyTo(it)
//                }

                edit(DiskPath("/home/pi/demo.ansi"), {
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
            booted(this@`should run each op type executing patch successfully`, { "init" },
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
) = compose("matches") { patch ->
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
 * Returns callable that mounts `this` [OperatingSystemImage] while logging with `this` [RenderingLogger]
 * and runs the specified [GuestAssertions].
 */
inline val FixedWidthRenderingLogger.booted: Assertion.Builder<OperatingSystemImage>.(assertion: OperatingSystemProcess.(String) -> ((String) -> Boolean)?) -> Unit
    get() = { assertion: OperatingSystemProcess.(String) -> ((String) -> Boolean)? -> booted(this@booted, assertion) }

inline fun Assertion.Builder<OperatingSystemImage>.booted(
    logger: FixedWidthRenderingLogger,
    crossinline assertion: OperatingSystemProcess.(String) -> ((String) -> Boolean)?,
): Assertion.Builder<OperatingSystemImage> {
    get("booted ${this.get { operatingSystem }}") {

        val verificationStep: OperatingSystemProcess.(String) -> String? = { output: String ->
            val asserter: ((String) -> Boolean)? = this.assertion(output)
            if (asserter != null) {
                val successfullyTested = asserter(output)
                if (!successfullyTested) " …"
                else null
            } else {
                null
            }
        }

        val container = DockerContainer.from(file)
        kotlin.runCatching {
            boot(container.name, logger, Program("test", { "testing" }, "testing" to verificationStep))
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
    outputMarker: String = Unicode.zeroWidthSpace.repeat(5),
    crossinline script: ScriptInit,
): Sequence<String> {
    val snippet = ShellScript {
        embed(ShellScript {
            !"printf '$outputMarker'"
            script()
            !"printf '$outputMarker'"
        })
    }.build()
    command(snippet)
    return Sequence {
        val regex = Regex(Regex.escape(outputMarker) + "(?<captured>.*)" + Regex.escape(outputMarker), setOf(DOT_MATCHES_ALL, MULTILINE))
        regex.findAll(io.output.ansiRemoved).mapNotNull { it.groupValue("captured") }.iterator()
    }
}

/**
 * Assertions on the directory used to share files with the [OperatingSystemImage].
 */
fun Assertion.Builder<OperatingSystemImage>.hostPath(path: String) =
    get("shared directory %s") { this.hostPath(DiskPath(path)) }

fun Assertion.Builder<OperatingSystemImage>.booted(
    logger: BlockRenderingLogger,
    initialState: OperatingSystemProcess.() -> String?,
    vararg states: ProgramState,
): Assertion.Builder<OperatingSystemImage> =
    assert("booted ${this.get { operatingSystem }}") {
        when (val exitState = it.boot(DockerContainer.from("Assertion Boot of $this").name, logger, Program("check", initialState, *states), border = DOTTED)) {
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

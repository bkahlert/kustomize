package com.imgcstmzr.patch

import com.imgcstmzr.expectRendered
import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization
import com.imgcstmzr.libguestfs.mounted
import com.imgcstmzr.os.DiskPath
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OS
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystemProcess
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.os.Program
import com.imgcstmzr.os.ProgramState
import com.imgcstmzr.os.boot
import com.imgcstmzr.os.size
import com.imgcstmzr.test.E2E
import koodies.docker.DockerContainer
import koodies.exec.Process.State.Exited.Succeeded
import koodies.exec.output
import koodies.io.path.hasContent
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.io.path.withDirectoriesCreated
import koodies.io.path.writeText
import koodies.io.toAsciiArt
import koodies.regex.groupValue
import koodies.shell.ScriptInit
import koodies.shell.ShellScript
import koodies.test.SvgFixture
import koodies.text.LineSeparators.LF
import koodies.text.Semantics.formattedAs
import koodies.text.ansiRemoved
import koodies.tracing.spanning
import koodies.unit.Gibi
import koodies.unit.bytes
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.java.exists
import strikt.java.resolve
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.MULTILINE

class SimplePhasedPatchTest {

    @E2E @Test
    fun `should render`(osImage: OperatingSystemImage) {
        val patch = SimplePhasedPatch(
            name = "patch",
            diskPreparations = emptyList(),
            diskCustomizations = listOf(
                Customization("command", "argument")
            ),
            diskOperations = listOf(
                GuestfishCommand("command1", "argument2"),
                GuestfishCommand("command2", "argument2"),
            ),
            fileOperations = listOf(
                FileOperation(LinuxRoot / "file1", {}, {}),
                FileOperation(LinuxRoot / "file2", {}, {}),
                FileOperation(LinuxRoot / "file3", {}, {}),
            ),
            osPreparations = listOf(
                { spanning("operation1") { log("event1") } },
                { spanning("operation2") { log("event2") } },
                { spanning("operation3") { log("event3") } },
                { spanning("operation4") { log("event4") } },
            ),
            osBoot = false,
            osOperations = listOf(
                osImage.compileScript("program1", "command1"),
                osImage.compileScript("program2", "command2"),
                osImage.compileScript("program3", "command3"),
                osImage.compileScript("program4", "command4"),
                osImage.compileScript("program5", "command5"),
            ),
        )

        patch.patch(osImage)

        expectRendered().ansiRemoved {
            contains("◼ Disk Preparations")
            contains("▶ Disk Customizations (1)")
            contains("▶ Disk Operations (5)")
            contains("▶ File Operations (3)")
            contains("▶ OS Preparations (4)")
            contains("◼ OS Boot")
            contains("▶ OS Operations (5)")
        }
    }

    @E2E @Test
    fun `should copy-in only relevant files`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        osImage.hostPath(LinuxRoot.home / "pi" / "local.txt").withDirectoriesCreated().createFile().writeText("local")

        val patch = PhasedPatch.build("test", osImage) {
            modifyFiles {
                edit(LinuxRoot.home / "pi" / "file.txt", { require(it.exists()) }) { it.writeText("content") }
            }
        }

        patch.patch(osImage)

        osImage.exchangeDirectory.listDirectoryEntriesRecursively()
        expectThat(osImage).mounted {
            path(LinuxRoot.home / "pi") {
                resolve("local.txt").not { exists() }
                resolve("file.txt").exists()
            }
        }
    }

    @E2E @Test
    fun `should run each op type executing patch successfully`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {

        val patch = PhasedPatch.build("All Phases Patch", osImage) {
            prepareDisk {
                resize(2.Gibi.bytes)
            }
            customizeDisk {
                hostname { "test-machine" }
            }
            modifyDisk {
                copyOut { LinuxRoot.etc.hostname }
            }
            modifyFiles {
                edit(LinuxRoot.home / "pi" / "demo.ansi", {
                    require(it.exists())
                }) {
                    it.writeText(SvgFixture.toAsciiArt() + LF)
                }
            }
            runPrograms {
                script("demo", "cat ${LinuxRoot.home / "pi" / "demo.ansi"}")
            }
        }

        patch.patch(osImage)

        expectThat(osImage) {
            hostPath(LinuxRoot.etc.hostname).hasContent("test-machine$LF")
            size.isGreaterThanOrEqualTo(2.Gibi.bytes)
            booted({ "init" },
                "init" to {
                    enter("sudo cat ${LinuxRoot.etc.hostname}")
                    "validate"
                },
                "validate" to {
                    if (it != "test-machine") "validate"
                    else null
                }
            )
        }
    }
}

fun <T : PhasedPatch> Assertion.Builder<T>.matches(
    diskPreparationsAssertion: Assertion.Builder<List<() -> Unit>>.() -> Unit = { hasSize(0) },
    diskCustomizationsAssertion: Assertion.Builder<List<Customization>>.() -> Unit = { hasSize(0) },
    diskOperationsAssertion: Assertion.Builder<List<GuestfishCommand>>.() -> Unit = { hasSize(0) },
    fileOperationsAssertion: Assertion.Builder<List<FileOperation>>.() -> Unit = { hasSize(0) },
    osPreparationsAssertion: Assertion.Builder<List<() -> Unit>>.() -> Unit = { hasSize(0) },
    osBootAssertion: Assertion.Builder<Boolean>.() -> Unit = { isFalse() },
    osOperationsAssertion: Assertion.Builder<List<Program>>.() -> Unit = { hasSize(0) },
) = compose("matches") {
    diskPreparationsAssertion(get { diskPreparations })
    diskCustomizationsAssertion(get { diskCustomizations })
    diskOperationsAssertion(get { diskOperations })
    fileOperationsAssertion(get { fileOperations })
    osPreparationsAssertion(get { osPreparations })
    osBootAssertion(get { osBoot })
    osOperationsAssertion(get { osOperations })
}.then { if (allPassed) pass() else fail() }


fun <T : PhasedPatch> Assertion.Builder<T>.diskCustomizations(
    block: Assertion.Builder<List<Customization>>.() -> Unit,
) = get("virt-customizations") { diskCustomizations }.block()

fun <T : PhasedPatch> Assertion.Builder<T>.diskOperations(
    block: Assertion.Builder<List<GuestfishCommand>>.() -> Unit,
) = get("guestfish commands") { diskOperations }.block()

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
fun Assertion.Builder<OperatingSystemImage>.hostPath(diskPath: DiskPath): DescribeableBuilder<Path> =
    get("shared directory %s") { hostPath(diskPath) }

fun Assertion.Builder<OperatingSystemImage>.booted(
    initialState: OperatingSystemProcess.() -> String?,
    vararg states: ProgramState,
): Assertion.Builder<OperatingSystemImage> =
    assert("booted ${this.get { operatingSystem }}") { osImage ->
        when (val exitState = osImage.boot(
            DockerContainer.from("Assertion Boot of $this").name,
            Program("check", initialState, *states),
            decorationFormatter = { it.formattedAs.debug }
        )) {
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
package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.expectRendered
import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine.GuestfishCommand
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization
import com.bkahlert.kustomize.libguestfs.mounted
import com.bkahlert.kustomize.os.DiskPath
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OS
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.OperatingSystems.RaspberryPiLite
import com.bkahlert.kustomize.os.size
import com.bkahlert.kustomize.test.E2E
import koodies.io.createParentDirectories
import koodies.io.path.hasContent
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.io.path.writeText
import koodies.io.toAsciiArt
import koodies.test.SvgFixture
import koodies.text.LineSeparators.LF
import koodies.text.ansiRemoved
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

class SimplePhasedPatchTest {

    @E2E @Test
    fun `should render`(osImage: OperatingSystemImage) {
        val patch = SimplePhasedPatch(
            name = "patch",
            diskPreparations = emptyList(),
            virtCustomizations = listOf(
                VirtCustomization("command", "argument")
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
            osBoot = false,
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
        osImage.hostPath(LinuxRoot.home / "pi" / "local.txt").createParentDirectories().createFile().writeText("local")

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
            disk {
                resize(2.Gibi.bytes)
            }
            virtCustomize {
                hostname { "test-machine" }
            }
            guestfish {
                copyOut { LinuxRoot.etc.hostname }
            }
            modifyFiles {
                edit(LinuxRoot.home / "pi" / "demo.ansi", {
                    require(it.exists())
                }) {
                    it.writeText(SvgFixture.toAsciiArt() + LF)
                }
            }
        }

        patch.patch(osImage)

        expectThat(osImage) {
            hostPath(LinuxRoot.etc.hostname).hasContent("test-machine$LF")
            size.isGreaterThanOrEqualTo(2.Gibi.bytes)
        }
    }
}

fun <T : PhasedPatch> Assertion.Builder<T>.matches(
    diskPreparationsAssertion: Assertion.Builder<List<() -> Unit>>.() -> Unit = { hasSize(0) },
    diskCustomizationsAssertion: Assertion.Builder<List<VirtCustomization>>.() -> Unit = { hasSize(0) },
    diskOperationsAssertion: Assertion.Builder<List<GuestfishCommand>>.() -> Unit = { hasSize(0) },
    fileOperationsAssertion: Assertion.Builder<List<FileOperation>>.() -> Unit = { hasSize(0) },
    osBootAssertion: Assertion.Builder<Boolean>.() -> Unit = { isFalse() },
) = compose("matches") {
    diskPreparationsAssertion(get { diskPreparations })
    diskCustomizationsAssertion(get { virtCustomizations })
    diskOperationsAssertion(get { diskOperations })
    fileOperationsAssertion(get { fileOperations })
    osBootAssertion(get { osBoot })
}.then { if (allPassed) pass() else fail() }


fun <T : PhasedPatch> Assertion.Builder<T>.diskCustomizations(
    block: Assertion.Builder<List<VirtCustomization>>.() -> Unit,
) = get("virt-customizations") { virtCustomizations }.block()

fun <T : PhasedPatch> Assertion.Builder<T>.diskOperations(
    block: Assertion.Builder<List<GuestfishCommand>>.() -> Unit,
) = get("guestfish commands") { diskOperations }.block()

/**
 * Assertions on the directory used to share files with the [OperatingSystemImage].
 */
fun Assertion.Builder<OperatingSystemImage>.hostPath(diskPath: DiskPath): DescribeableBuilder<Path> =
    get("shared directory %s") { hostPath(diskPath) }

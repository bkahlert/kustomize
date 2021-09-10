package com.bkahlert.kustomize.patch

import com.bkahlert.kommons.io.path.createParentDirectories
import com.bkahlert.kommons.io.path.hasContent
import com.bkahlert.kommons.io.path.writeText
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.ansiRemoved
import com.bkahlert.kommons.unit.Gibi
import com.bkahlert.kommons.unit.bytes
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

class SimplePhasedPatchTest {

    @E2E @Test
    fun `should render`(osImage: OperatingSystemImage) {
        val patch = SimplePhasedPatch(
            name = "patch",
            diskOperations = emptyList(),
            virtCustomizations = listOf(
                VirtCustomization("command", "argument")
            ),
            guestfishCommands = listOf(
                GuestfishCommand("command1", "argument2"),
                GuestfishCommand("command2", "argument2"),
            ),
            osBoot = false,
        )

        patch.patch(osImage)

        expectRendered().ansiRemoved {
            contains("◼ disk")
            contains("▶ virt-customize (1)")
            contains("▶ guestfish (2)")
            contains("◼ boot")
        }
    }

    @E2E @Test
    fun `should copy-in only relevant files`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        osImage.hostPath(LinuxRoot.home / "pi" / "local.txt").createParentDirectories().createFile().writeText("local")
        osImage.hostPath(LinuxRoot.home / "pi" / "shared.txt").createParentDirectories().createFile().writeText("shared")

        val patch = PhasedPatch.build("test", osImage) {
            guestfish {
                copyIn { LinuxRoot.home / "pi" / "shared.txt" }
            }
        }
        patch.patch(osImage)

        expectThat(osImage).mounted {
            path(LinuxRoot.home / "pi") {
                resolve("local.txt").not { exists() }
                resolve("shared.txt").exists()
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
        }

        patch.patch(osImage)

        expectThat(osImage) {
            hostPath(LinuxRoot.etc.hostname)
                .exists()
                .hasContent("test-machine$LF")
            size.isGreaterThanOrEqualTo(2.Gibi.bytes)
        }
    }
}

fun <T : PhasedPatch> Assertion.Builder<T>.matches(
    diskOperationsAssertion: Assertion.Builder<List<() -> Unit>>.() -> Unit = { hasSize(0) },
    virtCustomizationsAssertion: Assertion.Builder<List<VirtCustomization>>.() -> Unit = { hasSize(0) },
    guestfishCommandsAssertions: Assertion.Builder<List<GuestfishCommand>>.() -> Unit = { hasSize(0) },
    osBootAssertion: Assertion.Builder<Boolean>.() -> Unit = { isFalse() },
) = compose("matches") {
    diskOperationsAssertion(get { diskOperations })
    virtCustomizationsAssertion(get { virtCustomizations })
    guestfishCommandsAssertions(get { guestfishCommands })
    osBootAssertion(get { osBoot })
}.then { if (allPassed) pass() else fail() }


fun <T : PhasedPatch> Assertion.Builder<T>.virtCustomizations(
    block: Assertion.Builder<List<VirtCustomization>>.() -> Unit,
) = get("virt-customizations") { virtCustomizations }.block()

fun <T : PhasedPatch> Assertion.Builder<T>.guestfishCommands(
    block: Assertion.Builder<List<GuestfishCommand>>.() -> Unit,
) = get("guestfish commands") { guestfishCommands }.block()

/**
 * Assertions on the directory used to share files with the [OperatingSystemImage].
 */
fun Assertion.Builder<OperatingSystemImage>.hostPath(diskPath: DiskPath): DescribeableBuilder<Path> =
    get("shared directory %s") { hostPath(diskPath) }

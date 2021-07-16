package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.GuestfishCommandLine
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystemImage
import koodies.test.CapturedOutput
import koodies.test.SystemIOExclusive
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains

@SystemIOExclusive
class SimplePhasedPatchTest {

    @Test
    fun `should render`(osImage: OperatingSystemImage, output: CapturedOutput) {
        val patch = SimplePhasedPatch(
            name = "patch",
            diskPreparations = emptyList(),
            diskCustomizations = listOf {
                VirtCustomizeCommandLine.Customization("command", "argument")
            },
            diskOperations = listOf(
                { GuestfishCommandLine.GuestfishCommand("command1", "argument2") },
                { GuestfishCommandLine.GuestfishCommand("command2", "argument2") },
            ),
            fileOperations = listOf(
                FileOperation(LinuxRoot / "file1", {}, {}),
                FileOperation(LinuxRoot / "file2", {}, {}),
                FileOperation(LinuxRoot / "file3", {}, {}),
            ),
            osPreparations = listOf(
                {},
                {},
                {},
                {},
            ),
            osBoot = false,
            osOperations = listOf(
                { it.compileScript("program1", "command1") },
                { it.compileScript("program2", "command2") },
                { it.compileScript("program3", "command3") },
                { it.compileScript("program4", "command4") },
                { it.compileScript("program5", "command5") },
            ),
        )

        patch.patch(osImage)

        expectThat(output.all) {
            contains("◼ Disk Preparation")
            contains("▶ Disk Customization (1)")
            contains("▶ Disk Operations (5)")
            contains("▶ File Operations (4)")
            contains("OS Preparation (4) ✔︎")
            contains("◼ OS Boot")
            contains("◼ OS XXX")
        }
    }
}

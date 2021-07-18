package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.GuestfishCommandLine.GuestfishCommand.Composite.CopyOut
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.HostnameOption
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystemImage
import koodies.io.path.writeText
import koodies.junit.TestName
import koodies.test.hasElements
import koodies.unit.Gibi
import koodies.unit.bytes
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import kotlin.io.path.exists

class PatchKtTest {

    @Test
    fun `should build`(testName: TestName, osImage: OperatingSystemImage) {
        val patch = PhasedPatch.build("All Phases", osImage) {
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
                edit(LinuxRoot / "test.txt", {
                    require(it.exists())
                }) {
                    it.writeText(testName.toString())
                }
            }
            prepareOs {
                updatePassword("username", "new password")
            }
            bootOs = true
            runPrograms {
                script("name", "command1", "command2")
            }
        }
        expectThat(patch).matches(
            diskPreparationsAssertion = { hasSize(1) },
            diskCustomizationsAssertion = { hasElements({ isEqualTo(HostnameOption("test-machine")) }) },
            diskOperationsAssertion = { hasElements({ isA<CopyOut>() }) },
            fileOperationsAssertion = {
                hasElements({
                    get { file }.isEqualTo(LinuxRoot / "test.txt")
                })
            },
            osPreparationsAssertion = { hasSize(1) },
            osBootAssertion = { isTrue() },
            osOperationsAssertion = { hasSize(1) },
        )
    }
}

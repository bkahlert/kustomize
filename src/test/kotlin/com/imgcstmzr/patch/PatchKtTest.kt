package com.imgcstmzr.patch

import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystemImage
import koodies.builder.BooleanBuilder.OnOff.Context.on
import koodies.io.path.writeText
import koodies.junit.TestName
import koodies.unit.Gibi
import koodies.unit.bytes
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import kotlin.io.path.exists

class PatchKtTest {

    @Test
    fun `should build`(testName: TestName, osImage: OperatingSystemImage) {
        val patch = PhasedPatch.build("Try Everything Patch") {
            prepareDisk {
                resize(2.Gibi.bytes)
            }
            customizeDisk {
                hostname { "test-machine" }
            }
            modifyDisk {
                copyOut { LinuxRoot.etc / "hostname" }
            }
            modifyFiles {
                edit(LinuxRoot / "test.txt", {
                    require(it.exists())
                }) {
                    it.writeText(testName.toString())
                }
            }
            bootOs { on }
            runPrograms {
                script("name", "command1", "command2")
            }
        }
        expectThat(patch).matches(
            diskPreparationsAssertion = {},
            diskCustomizationsAssertion = {},
            diskOperationsAssertion = {},
            fileOperationsAssertion = {},
            osPreparationsAssertion = {},
            osBootAssertion = {},
            osOperationsAssertion = {},
        )
    }
}

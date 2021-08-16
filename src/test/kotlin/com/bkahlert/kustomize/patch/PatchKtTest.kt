package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine.GuestfishCommand.Composite.CopyOut
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.HostnameOption
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kommons.test.hasElements
import com.bkahlert.kommons.unit.Gibi
import com.bkahlert.kommons.unit.bytes
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

class PatchKtTest {

    @Test
    fun `should build`(osImage: OperatingSystemImage) {
        val patch = PhasedPatch.build("All Phases", osImage) {
            disk {
                resize(2.Gibi.bytes)
            }
            virtCustomize {
                hostname { "test-machine" }
            }
            guestfish {
                copyOut { LinuxRoot.etc.hostname }
            }
            bootOs = true
        }
        expectThat(patch).matches(
            diskOperationsAssertion = { hasSize(1) },
            virtCustomizationsAssertion = { hasElements({ isEqualTo(HostnameOption("test-machine")) }) },
            guestfishCommandsAssertions = { hasElements({ isA<CopyOut>() }) },
            osBootAssertion = { isTrue() },
        )
    }
}

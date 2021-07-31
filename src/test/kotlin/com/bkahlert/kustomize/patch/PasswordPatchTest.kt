package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.Customization
import com.bkahlert.kustomize.os.OperatingSystemImage
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.first
import strikt.assertions.isEqualTo

class PasswordPatchTest {

    @Test
    fun `should provide password change command`(osImage: OperatingSystemImage) {
        val passwordPatch = PasswordPatch("pi", "po")
        val expected = Customization.PasswordOption.byString("pi", "po")
        expectThat(passwordPatch(osImage)).matches(
            diskCustomizationsAssertion = { first().isEqualTo(expected) },
        )
    }
}

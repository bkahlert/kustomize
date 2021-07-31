package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization
import com.bkahlert.kustomize.os.OperatingSystemImage
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.first
import strikt.assertions.isEqualTo

class PasswordPatchTest {

    @Test
    fun `should provide password change command`(osImage: OperatingSystemImage) {
        val passwordPatch = PasswordPatch("pi", "po")
        val expected = VirtCustomization.PasswordOption.byString("pi", "po")
        expectThat(passwordPatch(osImage)).matches(
            virtCustomizationsAssertion = { first().isEqualTo(expected) },
        )
    }
}

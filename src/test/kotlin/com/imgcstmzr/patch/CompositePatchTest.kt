package com.imgcstmzr.patch

import com.imgcstmzr.os.OperatingSystemImage
import koodies.test.CapturedOutput
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains

class CompositePatchTest {

    @Test
    fun `should show all patch names`(osImage: OperatingSystemImage, output: CapturedOutput) {
        val patch = CompositePatch(
            PhasedPatch.build("Patch 1") {},
            PhasedPatch.build("Patch 2") {},
            PhasedPatch.build("Patch 3") {},
            PhasedPatch.build("Patch 4") {},
            PhasedPatch.build("Patch 5") {},
        )
        patch.patch(osImage)
        expectThat(output.all) {
            contains("╭──╴Patch 1")
            contains("│   Patch 2")
            contains("│   Patch 3")
            contains("│   Patch 4")
            contains("│   Patch 5")
        }
    }
}

package com.imgcstmzr.patch

import com.imgcstmzr.expectRendered
import com.imgcstmzr.os.OperatingSystemImage
import koodies.text.ansiRemoved
import org.junit.jupiter.api.Test
import strikt.assertions.contains

class CompositePatchTest {

    @Test
    fun `should show all patch names`(osImage: OperatingSystemImage) {
        val patch = CompositePatch(
            { PhasedPatch.build("Patch 1", it) {} },
            { PhasedPatch.build("Patch 2", it) {} },
            { PhasedPatch.build("Patch 3", it) {} },
            { PhasedPatch.build("Patch 4", it) {} },
            { PhasedPatch.build("Patch 5", it) {} },
        )

        osImage.patch(patch)
        
        expectRendered().ansiRemoved {
            contains("╭──╴Patch 1")
            contains("│   Patch 2")
            contains("│   Patch 3")
            contains("│   Patch 4")
            contains("│   Patch 5")
        }
    }
}

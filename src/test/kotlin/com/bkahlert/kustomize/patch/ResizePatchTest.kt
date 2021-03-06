package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.os.OS
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.OperatingSystems.RaspberryPiLite
import com.bkahlert.kustomize.test.E2E
import com.bkahlert.kommons.unit.Mebi
import com.bkahlert.kommons.unit.bytes
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isNotEqualTo

class ResizePatchTest {

    @E2E @Test
    fun `should increase size`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        val oldSize = osImage.size
        val newSize = osImage.size + 10.Mebi.bytes

        osImage.patch(ResizePatch(newSize))

        expectThat(osImage.size)
            .isGreaterThanOrEqualTo(newSize)
            .isNotEqualTo(oldSize)
    }
}

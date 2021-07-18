package com.imgcstmzr.patch

import com.imgcstmzr.os.OS
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.test.E2E
import koodies.unit.Mebi
import koodies.unit.bytes
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isNotEqualTo

class ImgResizePatchTest {

    @E2E @Test
    fun `should increase size`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        val oldSize = osImage.size
        val newSize = osImage.size + 10.Mebi.bytes

        osImage.patch(ImgResizePatch(newSize))

        expectThat(osImage.size)
            .isGreaterThanOrEqualTo(newSize)
            .isNotEqualTo(oldSize)
    }
}

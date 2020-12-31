package com.imgcstmzr.patch

import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.FifteenMinutesTimeout
import com.imgcstmzr.test.OS
import koodies.logging.InMemoryLogger
import koodies.unit.Mebi
import koodies.unit.bytes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

@Execution(CONCURRENT)
class ImgResizePatchTest {

    @FifteenMinutesTimeout @E2E @Test
    fun InMemoryLogger.`should increase size`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        val oldSize = osImage.size
        val newSize = osImage.size + 10.Mebi.bytes

        patch(osImage, ImgResizePatch(newSize))

        expectThat(osImage.size)
            .isEqualTo(newSize)
            .isNotEqualTo(oldSize)
    }
}

package com.imgcstmzr.patch

import com.bkahlert.koodies.test.junit.FifteenMinutesTimeout
import com.bkahlert.koodies.unit.Mebi
import com.bkahlert.koodies.unit.Size.Companion.bytes
import com.bkahlert.koodies.unit.Size.Companion.size
import com.imgcstmzr.E2E
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

@Execution(CONCURRENT)
class ImgResizePatchTest {

    @FifteenMinutesTimeout @E2E @Test
    fun `should increase size`(@OS(RaspberryPiLite) osImage: OperatingSystemImage, logger: InMemoryLogger<Any>) {
        val oldSize = osImage.size
        val newSize = osImage.size + 10.Mebi.bytes
        val patch = ImgResizePatch(newSize)

        patch.patch(osImage, logger)

        expectThat(osImage.size)
            .isEqualTo(newSize)
            .isNotEqualTo(oldSize)
    }
}

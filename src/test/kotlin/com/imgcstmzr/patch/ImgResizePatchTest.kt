package com.imgcstmzr.patch

import com.bkahlert.koodies.unit.Mebi
import com.bkahlert.koodies.unit.bytes
import com.bkahlert.koodies.unit.size
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.util.DockerRequired
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

@Execution(ExecutionMode.CONCURRENT)
class ImgResizePatchTest {

    @Test
    @DockerRequired
    fun `should increase size`(@OS(RaspberryPiLite::class) osImage: OperatingSystemImage, logger: InMemoryLogger<Any>) {
        val oldSize = osImage.size
        val newSize = osImage.size + 10.Mebi.bytes
        val patch = ImgResizePatch(newSize)

        patch.patch(osImage, logger)

        expectThat(osImage.size)
            .isEqualTo(newSize)
            .isNotEqualTo(oldSize)
    }
}

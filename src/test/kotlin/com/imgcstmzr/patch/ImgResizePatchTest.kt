package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.LibguestfsImage
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystemProcess.Companion.DockerPiImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.OS
import koodies.docker.DockerRequiring
import koodies.logging.InMemoryLogger
import koodies.test.FifteenMinutesTimeout
import koodies.unit.Mebi
import koodies.unit.bytes
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isNotEqualTo

class ImgResizePatchTest {

    @FifteenMinutesTimeout @DockerRequiring([LibguestfsImage::class, DockerPiImage::class]) @E2E @Test
    fun InMemoryLogger.`should increase size`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        val oldSize = osImage.size
        val newSize = osImage.size + 10.Mebi.bytes

        ImgResizePatch(newSize).patch(osImage)

        expectThat(osImage.size)
            .isGreaterThanOrEqualTo(newSize)
            .isNotEqualTo(oldSize)
    }
}

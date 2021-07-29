package com.bkahlert.kustomize.util

import com.bkahlert.kustomize.os.OperatingSystemImage
import koodies.test.ThirtyMinutesTimeout
import koodies.tracing.spanning
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class DiskTest {

    @Disabled
    @ThirtyMinutesTimeout @Test
    fun `should mount and unmount listed disks`(osImage: OperatingSystemImage) {
        val file = osImage.file
        val disk: String? = null

        spanning("Flashing ${file.toUri()}") {
            flash(file, disk)
        }
    }
}

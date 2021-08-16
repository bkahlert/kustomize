package com.bkahlert.kustomize.util

import com.bkahlert.kommons.test.ThirtyMinutesTimeout
import com.bkahlert.kommons.tracing.runSpanning
import com.bkahlert.kustomize.os.OperatingSystemImage
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class DiskTest {

    @Disabled
    @ThirtyMinutesTimeout @Test
    fun `should mount and unmount listed disks`(osImage: OperatingSystemImage) {
        val file = osImage.file
        val disk: String? = null

        runSpanning("Flashing ${file.toUri()}") {
            flash(file, disk)
        }
    }
}

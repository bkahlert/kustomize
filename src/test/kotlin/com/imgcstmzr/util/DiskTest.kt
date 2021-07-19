package com.imgcstmzr.util

import com.imgcstmzr.ImgCstmzr
import koodies.io.path.asPath
import koodies.test.ThirtyMinutesTimeout
import koodies.tracing.spanning
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class DiskTest {

    @Disabled
    @ThirtyMinutesTimeout @Test
    fun `should mount and unmount listed disks`() {
        val file = "${ImgCstmzr.HomeDirectory}/.imgcstmzr/bother-you/2021-01-03T21-28-04--csEt/2020-12-02-raspios-buster-armhf-lite.img".asPath()
        val disk: String? = null

        spanning("Flashing ${file.toUri()}") {
            flash(file, disk)
        }
    }
}

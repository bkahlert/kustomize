package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.TimeZoneOption
import com.imgcstmzr.os.OperatingSystemImage
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.single
import java.util.TimeZone

class TimeZonePatchTest {

    private val timeZone: TimeZone = TimeZone.getTimeZone("Europe/Berlin")

    @Test
    fun `should contain time zone customization`(osImage: OperatingSystemImage) {
        val patch = TimeZonePatch(timeZone).invoke(osImage)
        expectThat(patch).customizations {
            single().isA<TimeZoneOption>().get { timeZone == this@TimeZonePatchTest.timeZone }
        }
    }
}

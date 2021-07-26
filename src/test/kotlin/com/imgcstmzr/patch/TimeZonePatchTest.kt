package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.TimeZoneOption
import com.imgcstmzr.os.OperatingSystemImage
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.single
import java.util.TimeZone

class TimeZonePatchTest {

    private val berlinTimeZone: TimeZone = TimeZone.getTimeZone("Europe/Berlin")

    @Test
    fun `should contain time zone customization`(osImage: OperatingSystemImage) {
        val patch = TimeZonePatch(berlinTimeZone).invoke(osImage)
        expectThat(patch).diskCustomizations {
            single().isA<TimeZoneOption>().get { timeZone }.isEqualTo(berlinTimeZone)
        }
    }
}

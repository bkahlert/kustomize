package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.TimeZoneOption
import com.imgcstmzr.runtime.OperatingSystemImage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.single
import java.util.TimeZone

@Execution(CONCURRENT)
class TimeZonePatchTest {

    private val timeZone: TimeZone = TimeZone.getTimeZone("Europe/Berlin")

    @Test
    fun `should contain time zone customization`(osImage: OperatingSystemImage) {
        expectThat(TimeZonePatch(timeZone)).customizations(osImage) {
            single().isA<TimeZoneOption>().get { timeZone == this@TimeZonePatchTest.timeZone }
        }
    }
}

package com.imgcstmzr.patch

import com.imgcstmzr.runtime.OperatingSystemImage
import java.util.TimeZone

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * configures the timezone of the patched host.
 */
class TimeZonePatch(timeZone: TimeZone) : Patch by buildPatch("Set timezone to ${timeZone.displayName}", {
    customizeDisk { timeZone(timeZone) }
})

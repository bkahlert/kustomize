package com.imgcstmzr.patch

import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.patch.Patch.Companion.buildPatch
import java.util.TimeZone

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * configures the timezone of the patched host.
 */
class TimeZonePatch(timeZone: TimeZone) : Patch by buildPatch("Set Time Zone to ${timeZone.displayName}", {
    customizeDisk { timeZone { timeZone } }
})

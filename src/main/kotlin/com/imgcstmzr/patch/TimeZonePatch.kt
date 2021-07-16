package com.imgcstmzr.patch

import com.imgcstmzr.os.OperatingSystemImage
import java.util.TimeZone

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * configures the timezone of the patched host.
 */
class TimeZonePatch(timeZone: TimeZone) : PhasedPatch by PhasedPatch.build("Set Time Zone to ${timeZone.displayName}", {
    customizeDisk { timeZone { timeZone } }
})

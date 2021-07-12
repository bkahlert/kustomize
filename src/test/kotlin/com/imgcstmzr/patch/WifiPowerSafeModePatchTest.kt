package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.FirstBootOption
import com.imgcstmzr.libguestfs.file
import com.imgcstmzr.os.OperatingSystemImage
import koodies.content
import koodies.junit.UniqueId
import koodies.test.containsAtLeast
import koodies.test.expecting
import koodies.test.withTempDir
import org.junit.jupiter.api.Test
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.last

class WifiPowerSafeModePatchTest {

    @Test
    fun `should disable power-safe mode`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        expecting { WifiPowerSafeModePatch() } that {
            customizations(osImage) {
                last().isA<FirstBootOption>().file.content {
                    contains("/sbin/iw dev wlan0 set power_save off")
                    containsAtLeast("echo 0", 2)
                }
            }
        }
    }
}

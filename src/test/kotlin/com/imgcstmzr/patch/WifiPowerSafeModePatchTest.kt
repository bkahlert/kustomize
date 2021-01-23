package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.FirstBootOption
import com.imgcstmzr.libguestfs.virtcustomize.file
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.test.containsAtLeast
import com.imgcstmzr.test.content
import com.imgcstmzr.withTempDir
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.filterIsInstance
import strikt.assertions.hasSize
import strikt.assertions.single

@Execution(CONCURRENT)
class WifiPowerSafeModePatchTest {

    @Test
    fun `should disable power-safe mode`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        expectThat(WifiPowerSafeModePatch()) {
            customizations(osImage) {
                hasSize(1)
                filterIsInstance<FirstBootOption>().single().file.content {
                    contains("/sbin/iw dev wlan0 set power_save off")
                    containsAtLeast("echo 0", 2)
                }
            }
        }
    }
}

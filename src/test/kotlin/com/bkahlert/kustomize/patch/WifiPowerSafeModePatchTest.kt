package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.FirstBootOption
import com.bkahlert.kustomize.libguestfs.file
import com.bkahlert.kustomize.os.OperatingSystemImage
import koodies.io.path.pathString
import koodies.io.path.textContent
import koodies.junit.UniqueId
import koodies.test.containsAtLeast
import koodies.test.withTempDir
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.last

class WifiPowerSafeModePatchTest {

    @Test
    fun `should disable power-safe mode`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        val patch = WifiPowerSafeModePatch().invoke(osImage)

        expectThat(patch).diskCustomizations {
            last().isA<FirstBootOption>().file.textContent {
                not { contains(osImage.directory.pathString) }
                contains("/sbin/iw dev wlan0 set power_save off")
                containsAtLeast("echo 0", 2)
            }
        }
    }
}

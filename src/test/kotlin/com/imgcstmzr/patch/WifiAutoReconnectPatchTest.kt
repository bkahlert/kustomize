package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.AppendLineOption
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystemImage
import koodies.junit.UniqueId
import koodies.test.withTempDir
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.filterIsInstance
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.single

class WifiAutoReconnectPatchTest {

    @Test
    fun `should disable power-safe mode`(uniqueId: UniqueId, osImage: OperatingSystemImage) {
        val patch = WifiAutoReconnectPatch().invoke(osImage)
        withTempDir(uniqueId) {
            expectThat(patch) {
                customizations {
                    hasSize(1)
                    filterIsInstance<AppendLineOption>().single()
                        .get { file to line }
                        .isEqualTo(LinuxRoot.etc / "crontab" to "*/5 * * * *      root     printf 'Periodic internet connection check … ' && (nc -4z -w5 google.com 443 1>/dev/null 2>&1 || nc -4z -w5 amazon.com 443 1>/dev/null 2>&1) && echo 'ok.' || printf 'failed. Trying to re-connect … ' && sudo /sbin/ip --force link set wlan0 down && sudo /sbin/ip link set wlan0 up && /bin/sleep 10 && (nc -4z -w5 google.com 443 1>/dev/null 2>&1 || nc -4z -w5 amazon.com 443 1>/dev/null 2>&1) && echo 'internet connection re-established.' || echo 'failed.'")
                }
            }
        }
    }
}

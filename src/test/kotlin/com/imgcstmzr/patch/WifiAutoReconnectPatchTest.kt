package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.AppendLineOption
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.withTempDir
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.filterIsInstance
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.single

@Execution(CONCURRENT)
class WifiAutoReconnectPatchTest {

    @Test
    fun `should disable power-safe mode`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        expectThat(WifiAutoReconnectPatch()) {
            customizations(osImage) {
                hasSize(1)
                filterIsInstance<AppendLineOption>().single()
                    .get { file to line }
                    .isEqualTo(DiskPath("/etc/crontab") to "*/5 * * * *      root     printf 'Periodic internet connection check... ' && (nc -4z -w5 google.com 443 1>/dev/null 2>&1 || nc -4z -w5 amazon.com 443 1>/dev/null 2>&1) && echo 'ok.' || printf 'failed. Trying to re-connect... ' && sudo /sbin/ip --force link set wlan0 down && sudo /sbin/ip link set wlan0 up && /bin/sleep 10 && (nc -4z -w5 google.com 443 1>/dev/null 2>&1 || nc -4z -w5 amazon.com 443 1>/dev/null 2>&1) && echo 'internet connection re-established.' || echo 'failed.'")
            }
        }
    }
}

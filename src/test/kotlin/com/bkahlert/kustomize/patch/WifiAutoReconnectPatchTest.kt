package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine.GuestfishCommand.WriteAppendCommand
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kommons.junit.UniqueId
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.text.LineSeparators.LF
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.containsExactly
import strikt.assertions.hasSize

class WifiAutoReconnectPatchTest {

    @Test
    fun `should disable power-safe mode`(uniqueId: UniqueId, osImage: OperatingSystemImage) {
        val patch = WifiAutoReconnectPatch().invoke(osImage)
        withTempDir(uniqueId) {
            expectThat(patch) {
                guestfishCommands {
                    hasSize(1)
                    @Suppress("LongLine")
                    any {
                        containsExactly(-WriteAppendCommand(LinuxRoot.etc.crontab,
                            "*/5 * * * *      root     printf 'Periodic internet connection check … ' && (nc -4z -w5 google.com 443 1>/dev/null 2>&1 || nc -4z -w5 amazon.com 443 1>/dev/null 2>&1) && echo 'ok.' || printf 'failed. Trying to re-connect … ' && sudo /sbin/ip --force link set wlan0 down && sudo /sbin/ip link set wlan0 up && /bin/sleep 10 && (nc -4z -w5 google.com 443 1>/dev/null 2>&1 || nc -4z -w5 amazon.com 443 1>/dev/null 2>&1) && echo 'internet connection re-established.' || echo 'failed.'$LF"))
                    }
                }
            }
        }
    }
}

package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.os.DiskPath
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OperatingSystemImage

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * adds a cron job that attempts to connect to the specified [hosts]
 * on port `443`. If no connection to any host can be established
 * the wifi connection will be re-established.
 */
class WifiAutoReconnectPatch(private vararg val hosts: String = arrayOf("google.com", "amazon.com")) : (OperatingSystemImage) -> PhasedPatch {
    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build(
        NAME,
        osImage,
    ) {

        val printChecking = "printf 'Periodic internet connection check … '"
        val checkHosts = "(${hosts.map { netcat(it) }.or()})"
        val printOk = "echo 'ok.'"
        val printReconnecting = "printf 'failed. Trying to re-connect … '"
        val wifiDown = "sudo /sbin/ip --force link set wlan0 down"
        val wifiUp = "sudo /sbin/ip link set wlan0 up"
        val sleep10s = "/bin/sleep 10"
        val printReEstablished = "echo 'internet connection re-established.'"
        val printFailed = "echo 'failed.'"

        val checkConnection = printChecking and checkHosts and printOk
        val reEstablishConnection = printReconnecting and wifiDown and wifiUp and sleep10s and checkHosts and printReEstablished

        customizeDisk {
            appendLine {
                "*/5 * * * *      root     ${checkConnection or reEstablishConnection or printFailed}" to CRONTAB
            }
        }

        bootOs = true

    }

    companion object {
        fun netcat(host: String) = "nc -4z -w5 $host 443 1>/dev/null 2>&1"
        val CRONTAB: DiskPath = LinuxRoot.etc.crontab

        private val NAME = "Enable Wifi Auto-Reconnect"
        private fun Iterable<String>.and() = joinToString(" && ")
        private infix fun String.and(other: String) = listOf(this, other).and()
        private fun Iterable<String>.or() = joinToString(" || ")
        private infix fun String.or(other: String) = listOf(this, other).or()
    }
}

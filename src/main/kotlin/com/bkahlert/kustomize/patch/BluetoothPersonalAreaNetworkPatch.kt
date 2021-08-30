package com.bkahlert.kustomize.patch

import com.bkahlert.kommons.net.IPAddress
import com.bkahlert.kommons.net.IPSubnet
import com.bkahlert.kommons.net.div
import com.bkahlert.kommons.net.ip4Of
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OperatingSystemImage

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * configures Bluetooth to provide a Personal Area Network (PAN).
 *
 * The specified [dhcpRange] will be used to auto-configure attached devices
 * whereas the [deviceAddress] is the one used for this device. If not specified, [IPSubnet.firstUsableHost] is used.
 *
 * @see <a href="https://blog.fraggod.net/2015/03/28/bluetooth-pan-network-setup-with-bluez-5x.html">bt-pan Blog Entry</a>
 * @see <a href="https://github.com/mk-fg/fgtk/blob/master/bt-pan">bt-pan</a>
 * @see <a href="http://bluez.sourceforge.net/contrib/HOWTO-PAN">HowTo set up common PAN scenarios with BlueZ's integrated PAN support</a>
 */
class BluetoothPersonalAreaNetworkPatch(
    /**
     * The DHCP range to use to auto-configure devices making a connection
     * to the embedded machine.
     */
    val dhcpRange: IPSubnet<out IPAddress> = ip4Of("10.10.10.16") / 29, // usable: 10.10.10.17..10.10.10.22
    /**
     * The address of the embedded device.
     */
    val deviceAddress: IPAddress = ip4Of("10.10.10.20"),
) : (OperatingSystemImage) -> PhasedPatch {

    /**
     * usr/local/bin
     * wget https://raw.githubusercontent.com/emmercm/panr/master/panr && chmod +x panr
     * wget https://raw.githubusercontent.com/emmercm/panr/master/panr-acceptr && chmod +x panr-acceptr
     *
     */
    companion object {
        val DISABLE_SAP_CONF = LinuxRoot.etc.systemd.system.bluetooth_service_d / "01-disable-sap-plugin.conf"

        val BT_PAN = LinuxRoot.usr.local.sbin / "bt-pan"
        val BT_PAN_SCRIPT = LinuxRoot.usr.local.sbin / "bt-pan.service.sh"
        val BT_PAN_SERVICE = LinuxRoot.etc.systemd.system / "bt-pan.service"

        val BT_AGENT = LinuxRoot.usr.local.sbin / "blueagent5"
        val BT_AGENT_SERVICE = LinuxRoot.etc.systemd.system / "bt-agent.service"

        val DNSMASQ_PAN0 = LinuxRoot.etc.dnsmasq_d / "pan0"
    }

    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build(
        "Configure Bluetooth PAN with DHCP Address Range $dhcpRange",
        osImage,
    ) {
        virtCustomize {
            // SIM-Activation-Profile
            copyIn(DISABLE_SAP_CONF, """
                [Service]
                ExecStart=
                ExecStart=/usr/lib/bluetooth/bluetoothd --noplugin=sap
                
            """.trimIndent())
            chmods { "644" to DISABLE_SAP_CONF }

            firstBootInstall { listOf("bridge-utils", "bluez", "python-dbus", "python-gobject") }
            firstBoot("configure to stay discoverable forever") { "sed -i '/DiscoverableTimeout/s/^.*$/DiscoverableTimeout = 0/' ${LinuxRoot.etc.bluetooth.main_conf}" }
            firstBoot("configure to stay pairable forever") { "sed -i '/PairableTimeout/s/^.*$/PairableTimeout = 0/' ${LinuxRoot.etc.bluetooth.main_conf}" }

            copyIn(BT_PAN_SCRIPT, """
                #!/bin/sh
                modprobe bnep                           # load bnep module (Bluetooth Network Encapsulation Protocol)
                hciconfig hci0 lm master,accept         # act as Bluetooth master
                
                brctl addbr pan0                        # bridge to be able to use pan0 instead of bnepX
                brctl setfd pan0 0                      # disable "Listening and Learning States" to improve connection speed
                brctl stp pan0 off                      # disable "Spanning Tree Protocol" to improve connection speed
                #brctl addif pan0 en0                   # connect to infrastructure network
                ip addr add $deviceAddress/${dhcpRange.prefixLength} dev pan0      # set static IP of pan0 interface
                ip link set pan0 up
                #ip route add default via aaa.bbb.ccc.ddd dev pan0
                
                exec $BT_PAN --debug server pan0
                
            """.trimIndent())
            chmods { "755" to BT_PAN_SCRIPT }


            copyIn(BT_PAN_SERVICE, """
                [Unit]
                After=bluetooth.service
                PartOf=bluetooth.service

                [Service]
                ExecStart=$BT_PAN_SCRIPT

                [Install]
                WantedBy=bluetooth.target
                
            """.trimIndent())
            chmods { "644" to BT_PAN_SERVICE }
            firstBoot {
                """
                wget -O $BT_PAN https://github.com/mk-fg/fgtk/raw/master/bt-pan
                chmod 755 $BT_PAN
                systemctl enable bt-pan
            """.trimIndent()
            }


            copyIn(BT_AGENT_SERVICE, """
                [Unit]
                Description=Bluetooth Agent
                After=bt-pan.service
                Requires=bt-pan.service
        
                [Service]
                ExecStart=$BT_AGENT
        
                [Install]
                WantedBy=bluetooth.target
                
            """.trimIndent())
            chmods { "644" to BT_AGENT_SERVICE }
            firstBoot {
                """
                wget -O $BT_AGENT https://github.com/opustecnica/public/raw/master/raspberrypi/PAN/blueagent5.py
                chmod 755 $BT_AGENT
                systemctl enable bt-agent
            """.trimIndent()
            }


            firstBootInstall { listOf("dnsmasq") }
            copyIn(DNSMASQ_PAN0, """
                dhcp-authoritative
                dhcp-rapid-commit
                no-ping
                interface=pan0
                dhcp-range=${dhcpRange.firstUsableHost},${dhcpRange.lastUsableHost},1h
                # no gateway / routing
                dhcp-option=3
                #dhcp-option=option:dns-server,aaa.bbb.ccc.ddd
                #leasefile-ro
                
            """.trimIndent())
            firstBoot { "systemctl restart dnsmasq" }
        }
    }
}

package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.patch.Patch.Companion.buildPatch
import com.imgcstmzr.runtime.OperatingSystemImage
import koodies.net.IPAddress
import koodies.net.IPSubnet
import koodies.net.div
import koodies.net.ip4Of

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * configures the USB gadget to provide an ethernet connection
 * using a simple USB connection.
 *
 * The specified [dhcpRange] will be used to auto-configure attached devices
 * whereas the [deviceAddress] is the one used for this device. If not specified, [IpAddressRange.firstUsableHost] is used.
 *
 * This works for the Raspberry Pi Zero and 4.
 *
 * ```text
 * /sys/kernel/config/usb_gadget/
 * ╷
 * ├──╴g1/
 * │   ╷
 * │   ├──╴<device info: vid, pid, serial number, …>
 * │   │
 * │   ├──╴configs/
 * │   │   ╷
 * │   │   ╰──╴c1/
 * │   │       ╷
 * │   │       ├──╴<config info>
 * │   │       ├──╴func0
 * │   │       ╰──╴func1
 * │   │
 * │   ╰──╴functions/
 * │       ╷
 * │       ├──╴func0
 * │       ╰──╴func1
 * │
 * ╰──╴UDC
 * ```
 *
 * @see <a href="https://www.hardill.me.uk/wordpress/2019/11/02/pi4-usb-c-gadget/">Pi4 USB-C Gadget</a>
 * @see <a href="https://github.com/imgcstmzr/rpi-gadget-image-creator/blob/master/create-image">rpi-gadget-image-creator</a>
 */
class UsbEthernetGadgetPatch(
    /**
     * The DHCP range to use to auto-configure devices making a connection
     * to the embedded machine.
     */
    val dhcpRange: IPSubnet<out IPAddress> = DEFAULT_DHCP_RANGE,
    /**
     * The address of the embedded device.
     */
    val deviceAddress: IPAddress = dhcpRange.firstUsableHost,
    /**
     * Whether to configure the attached host as the embedded machines
     * default gateway (e.g. to provide Internet access).
     */
    val hostAsDefaultGateway: Boolean = false,
    /**
     * Whether to activate the serial console.
     */
    val enableSerialConsole: Boolean = false,
) : Patch by buildPatch("Activate USB gadget with DHCP address range $dhcpRange", {

    require(deviceAddress in dhcpRange.firstUsableHost..dhcpRange.lastUsableHost) { "$deviceAddress must be in range ${dhcpRange.usable}" }

    customizeDisk {

        firstBootInstall { listOf("dnsmasq") }

        /**
         * server=8.8.8.8
         * server=1.1.1.1
         * server=8.8.4.4
         * server=1.0.0.1
         * expand-hosts
         * domain=bother-you
         * address=/host/192.168.168.192
         * dhcp-range=192.168.168.168,192.168.168.189,24h
         * dhcp-option=option:dns-server,192.168.168.192
         */
        copyIn(USB0_DNSMASQD,
            """
                    dhcp-authoritative 
                    dhcp-rapid-commit
                    no-ping
                    interface=usb0 
                    dhcp-range=${dhcpRange.firstUsableHost},${dhcpRange.lastUsableHost},1h 
                    dhcp-option=3 # no gateway / routing
                    #dhcp-option=option:dns-server,192.168.168.192
                    ${if (hostAsDefaultGateway) "dhcp-script=$DHCP_SCRIPT" else ""}
                    leasefile-ro
                """.trimIndent())

        if (hostAsDefaultGateway) {
            copyIn(DHCP_SCRIPT,
                """
                        #!/bin/bash
                        op="${'$'}{1:-op}"
                        mac="${'$'}{2:-mac}"
                        ip="${'$'}{3:-ip}"
                        host="${'$'}{4}"
                        
                        if [[ ${'$'}op == "init" ]]; then
                            exit 0
                        fi
                        
                        if [[ ${'$'}op == "add" ]] || [[ ${'$'}op == "old" ]]; then
                            route add default gw ${'$'}ip usb0
                        fi
                        
                    """.trimIndent())
            chmods { "0755" to DHCP_SCRIPT }
        }

        /**
         * interface usb0
         * static ip_address=192.168.168.192/24
         * static routers=192.168.168.168
         * static domain_name_servers=192.168.168.168
         * metric 999
         * fallback usb0
         *
         * auto lo usb0
         * auth usb0
         * address 192.168.168.192
         * netmask 255.255.255.0
         * network 192.168.168.0
         * broadcast 192.168.168.255
         * gateway 192.168.168.168
         * metric 999
         * dns-nameservers 192.168.168.168
         */
        copyIn(USB0_NETWORK,
            """
                    auto usb0
                    allow-hotplug usb0
                    iface usb0 inet static
                      address $deviceAddress/${dhcpRange.prefixLength}
                """.trimIndent())

        copyIn(USB_GADGET,
            """
                    #!/bin/bash
    
                    cd /sys/kernel/config/usb_gadget/
                    mkdir -p display-pi
                    cd display-pi
                    echo 0x1d6b > idVendor # Linux Foundation
                    echo 0x0104 > idProduct # Multifunction Composite Gadget
                    echo 0x0100 > bcdDevice # v1.0.0
                    echo 0x0200 > bcdUSB # USB2
                    #echo 0xEF > bDeviceClass
                    #echo 0x02 > bDeviceSubClass
                    #echo 0x01 > bDeviceProtocol
                    mkdir -p strings/0x409
                    echo "fedcba9876543210" > strings/0x409/serialnumber
                    echo "Ben Hardill" > strings/0x409/manufacturer
                    echo "Display-Pi USB Device" > strings/0x409/product
                    mkdir -p configs/c.1/strings/0x409
                    echo "Config 1: ECM network" > configs/c.1/strings/0x409/configuration
                    echo 250 > configs/c.1/MaxPower
                    # Add functions here
                    # see gadget configurations below
                    # End functions
                    
                    mkdir -p functions/ecm.usb0
                    HOST="00:dc:c8:f7:75:15" # "HostPC"
                    SELF="00:dd:dc:eb:6d:a1" # "BadUSB"
                    echo ${'$'}HOST > functions/ecm.usb0/host_addr
                    echo ${'$'}SELF > functions/ecm.usb0/dev_addr
                    ln -s functions/ecm.usb0 configs/c.1/
                    
                    mkdir -p functions/acm.usb0
                    ln -s functions/acm.usb0 configs/c.1/
                    
                    #mkdir -p functions/mass_storage.usb0
                    #echo 0 > functions/mass_storage.usb0/stall
                    #echo 0 > functions/mass_storage.usb0/lun.0/cdrom
                    #echo 1 > functions/mass_storage.usb0/lun.0/ro
                    #echo 0 > functions/mass_storage.usb0/lun.0/nofua
                    #echo /opt/disk.img > functions/mass_storage.usb0/lun.0/file
                    #ln -s functions/mass_storage.usb0 configs/c.1/
                    
                    udevadm settle -t 5 || :
                    ls /sys/class/udc > UDC
                    
                    ifup usb0
                """.trimIndent())
        chmods { "0755" to USB_GADGET }

        copyIn(USBGADGET_SERVICE,
            """
                    [Unit]
                    Description=My USB gadget
                    After=network-online.target
                    Wants=network-online.target
                    #After=systemd-modules-load.service
                     
                    [Service]
                    Type=oneshot
                    RemainAfterExit=yes
                    ExecStart=$USB_GADGET
                     
                    [Install]
                    WantedBy=sysinit.target
                """.trimIndent())
        firstBoot("enable ${USBGADGET_SERVICE.fileName}") { !"systemctl enable ${USBGADGET_SERVICE.fileName}" }

        firstBoot("update ${CONFIG_TXT.fileName}") { !"echo dtoverlay=dwc2 >> $CONFIG_TXT" }
        firstBoot("update ${CMDLINE_TXT.fileName}") { !"sed -i 's/\$/ modules-load=dwc2/' $CMDLINE_TXT" }
        firstBoot("update ${MODULES.fileName}") { !"echo libcomposite >> $MODULES" }
        firstBoot("update ${DHCPCD_CONF.fileName}") { !"echo denyinterfaces usb0 >> $DHCPCD_CONF" }
        if (enableSerialConsole) firstBoot("enable getty@ttyGS0.service") { !"systemctl enable getty@ttyGS0.service" }
    }
}) {
    companion object {
        val USB0_DNSMASQD = DiskPath("/etc/dnsmasq.d/usb0")
        val DHCP_SCRIPT = DiskPath("/root/route.sh")
        val USB0_NETWORK: DiskPath = DiskPath("/etc/network/interfaces.d/usb0")
        val USB_GADGET = DiskPath("/usr/local/sbin/usb-gadget.sh")
        val USBGADGET_SERVICE = DiskPath("/lib/systemd/system/usbgadget.service")

        val CONFIG_TXT: DiskPath = DiskPath("/boot/config.txt")
        val CMDLINE_TXT: DiskPath = DiskPath("/boot/cmdline.txt")
        val MODULES: DiskPath = DiskPath("/etc/modules")
        val DHCPCD_CONF: DiskPath = DiskPath("/etc/dhcpcd.conf")

        val DEFAULT_DHCP_RANGE = ip4Of("10.55.0.1") / 29
    }
}

// TODO https://github.com/imgcstmzr/rpi-gadget-image-creator

/** https://www.hardill.me.uk/wordpress/2019/11/02/pi4-usb-c-gadget/
As I said in a earlier comment I don’t have an iPad so don’t know. I don’t expect this will work but you can do what I do with the Pi Zeros.

Add the following to /etc/dnsmasq.d/usb

dhcp-script=/root/route.sh
leasefile-ro

And then create a file called /root/route.sh with the following:

#!/bin/bash
op="${1:-op}"
mac="${2:-mac}"
ip="${3:-ip}"
host="${4}"

if [[ $op == "init" ]]; then
exit 0
fi

if [[ $op == "add" ]] || [[ $op == "old" ]]; then
route add default gw $ip usb0
fi

This will set the iPad to be the default route for the Pi. If the iPad is able to do NAT then it will work.
 */

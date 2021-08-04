package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.os.DiskDirectory.File
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.LinuxRoot.boot.cmdline_txt
import com.bkahlert.kustomize.os.LinuxRoot.boot.config_txt
import com.bkahlert.kustomize.os.LinuxRoot.etc.dhcpcd_conf
import com.bkahlert.kustomize.os.LinuxRoot.etc.modules
import com.bkahlert.kustomize.os.OperatingSystemImage
import koodies.net.IPAddress
import koodies.net.IPSubnet
import koodies.net.div
import koodies.net.ip4Of
import koodies.toBaseName

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * configures the USB gadget to provide an ethernet connection
 * using a simple USB connection.
 *
 * The specified [dhcpRange] will be used to auto-configure attached devices
 * whereas the [deviceAddress] is the one used for this device. If not specified, [IPSubnet.firstUsableHost] is used.
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
 * @see <a href="httpshttps://github.com/hardillb/rpi-gadget-image-creator">Raspberry Pi USB Gadget Image Builder</a>
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

    /**
     * Manufacturer of USB device.
     */
    val manufacturer: String,

    /**
     * Name of USB device.
     */
    val product: String,
) : (OperatingSystemImage) -> PhasedPatch {
    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build(
        "Configure USB Gadget with DHCP Address Range $dhcpRange",
        osImage,
    ) {

        require(deviceAddress in dhcpRange.firstUsableHost..dhcpRange.lastUsableHost) { "$deviceAddress must be in range ${dhcpRange.usable}" }

        virtCustomize {

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
            @Suppress("SpellCheckingInspection")
            copyIn(USB0_DNSMASQD, """
                dhcp-authoritative 
                dhcp-rapid-commit
                no-ping
                interface=usb0 
                dhcp-range=${dhcpRange.firstUsableHost},${dhcpRange.lastUsableHost},1h
                # no gateway / routing
                dhcp-option=3
                #dhcp-option=option:dns-server,192.168.168.192
                ${if (hostAsDefaultGateway) "dhcp-script=$DHCP_SCRIPT" else ""}
                leasefile-ro
            """.trimIndent())

            if (hostAsDefaultGateway) {
                copyIn(DHCP_SCRIPT, """
                    #!/bin/bash
                    op="${'$'}{1:-op}"
                    mac="${'$'}{2:-mac}"
                    ip="${'$'}{3:-ip}"
                    host="${'$'}{4:-}"
                    
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
            @Suppress("SpellCheckingInspection")
            copyIn(USB0_NETWORK, """
                auto usb0
                allow-hotplug usb0
                iface usb0 inet static
                  address $deviceAddress/${dhcpRange.prefixLength}
            """.trimIndent())

            val dirName = product.toBaseName()
            @Suppress("SpellCheckingInspection")
            copyIn(USB_GADGET, """
                #!/bin/bash
    
                cd /sys/kernel/config/usb_gadget/
                mkdir -p $dirName
                cd $dirName
                echo 0x1d6b > idVendor # Linux Foundation
                echo 0x0104 > idProduct # Multifunction Composite Gadget
                echo 0x0100 > bcdDevice # v1.0.0
                echo 0x0200 > bcdUSB # USB2
                #echo 0xEF > bDeviceClass
                #echo 0x02 > bDeviceSubClass
                #echo 0x01 > bDeviceProtocol
                mkdir -p strings/0x409
                echo "fedcba9876543210" > strings/0x409/serialnumber
                echo "$manufacturer" > strings/0x409/manufacturer
                echo "$product" > strings/0x409/product
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

            @Suppress("SpellCheckingInspection")
            copyIn(USBGADGET_SERVICE, """
                [Unit]
                Description=$product
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
            firstBoot("enable ${USBGADGET_SERVICE.fileName}") { "systemctl enable ${USBGADGET_SERVICE.fileName}" }

            firstBoot("update ${config_txt.fileName}") { "echo 'dtoverlay=dwc2' >> $config_txt" }
            firstBoot("update ${cmdline_txt.fileName}") { "sed -i 's/\$/ modules-load=dwc2/' $cmdline_txt" }
            @Suppress("SpellCheckingInspection")
            firstBoot("update ${modules.fileName}") { "echo 'libcomposite' >> $modules" }
            @Suppress("SpellCheckingInspection")
            firstBoot("update ${dhcpcd_conf.fileName}") { "echo 'denyinterfaces usb0' >> $dhcpcd_conf" }
            if (enableSerialConsole) firstBoot("enable serial-getty@ttyGS0.service") { "systemctl enable serial-getty@ttyGS0.service" }
        }
    }

    @Suppress("SpellCheckingInspection")
    companion object {

        /** dnsmasq configuration of the USB connection. */
        val USB0_DNSMASQD: File = LinuxRoot.etc.dnsmasq_d / "usb0"

        /** Script that is invoked on DHCP lease changes. */
        val DHCP_SCRIPT: File = LinuxRoot.root / "route.sh"

        /** USB network settings. */
        val USB0_NETWORK: File = LinuxRoot.etc.network.interfaces_d / "usb0"

        /** USB gadget setup script. */
        val USB_GADGET: File = LinuxRoot.usr.local.sbin / "usb-gadget.sh"

        /** USB gadget unit file. */
        val USBGADGET_SERVICE: File = LinuxRoot.etc.systemd.system / "usbgadget.service"

        /** [IPSubnet] to use by default. */
        val DEFAULT_DHCP_RANGE: IPSubnet<*> = ip4Of("10.55.0.1") / 29
    }
}

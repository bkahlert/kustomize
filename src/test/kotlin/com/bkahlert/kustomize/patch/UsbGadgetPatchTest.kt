package com.bkahlert.kustomize.patch

import com.bkahlert.kommons.io.path.textContent
import com.bkahlert.kommons.junit.UniqueId
import com.bkahlert.kommons.net.ip4Of
import com.bkahlert.kommons.net.ipOf
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.text.matchesCurlyPattern
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.ChmodOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.CopyInOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.FirstBootInstallOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.FirstBootOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.MkdirOption
import com.bkahlert.kustomize.libguestfs.file
import com.bkahlert.kustomize.os.LinuxRoot.boot.cmdline_txt
import com.bkahlert.kustomize.os.LinuxRoot.boot.config_txt
import com.bkahlert.kustomize.os.LinuxRoot.etc.dhcpcd_conf
import com.bkahlert.kustomize.os.LinuxRoot.etc.modules
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.patch.UsbGadgetPatch.Companion.DHCP_SCRIPT
import com.bkahlert.kustomize.patch.UsbGadgetPatch.Companion.USB0_DNSMASQD
import com.bkahlert.kustomize.patch.UsbGadgetPatch.Companion.USB0_NETWORK
import com.bkahlert.kustomize.patch.UsbGadgetPatch.Companion.USBGADGET_SERVICE
import com.bkahlert.kustomize.patch.UsbGadgetPatch.Companion.USB_GADGET
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.filterIsInstance
import strikt.assertions.isEqualTo

class UsbGadgetPatchTest {

    private val patch = UsbGadgetPatch(
        dhcpRange = (ip4Of("10.10.1.1")..ip4Of("10.10.1.20")).smallestCommonSubnet,
        deviceAddress = ipOf("10.10.1.10"),
        hostAsDefaultGateway = true,
        enableSerialConsole = true,
        manufacturer = "John Doe",
        product = "USB Test Device",
    )

    @Test
    fun `should have name`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        expectThat(patch(osImage).name).isEqualTo("Configure USB Gadget with DHCP Address Range 10.10.1.0/27")
    }

    @Test
    fun `should configure dnsmasq for usb0`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        expect {
            that(patch(osImage)).virtCustomizations {
                contains(
                    MkdirOption(USB0_DNSMASQD.parent),
                    CopyInOption(osImage.hostPath(USB0_DNSMASQD), USB0_DNSMASQD.parent),
                    FirstBootInstallOption("dnsmasq"),
                )
            }
            that(osImage.hostPath(USB0_DNSMASQD)) {
                @Suppress("SpellCheckingInspection")
                textContent.matchesCurlyPattern("""
                    dhcp-authoritative 
                    dhcp-rapid-commit
                    no-ping
                    interface=usb0 
                    dhcp-range=10.10.1.1,10.10.1.30,1h
                    # no gateway / routing
                    dhcp-option=3
                    #dhcp-option=option:dns-server,192.168.168.192
                    dhcp-script=$DHCP_SCRIPT
                    leasefile-ro
                    
                """.trimIndent())
            }
        }
    }

    @Test
    fun `should configure DHCP script`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        expect {
            that(patch(osImage)).virtCustomizations {
                contains(
                    MkdirOption(DHCP_SCRIPT.parent),
                    CopyInOption(osImage.hostPath(DHCP_SCRIPT), DHCP_SCRIPT.parent),
                    ChmodOption("0755", DHCP_SCRIPT),
                )
            }
            that(osImage.hostPath(DHCP_SCRIPT)) {
                @Suppress("SpellCheckingInspection")
                textContent.matchesCurlyPattern("""
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
            }
        }
    }

    @Test
    fun `should configure network interface for usb0`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        expect {
            that(patch(osImage)).virtCustomizations {
                contains(
                    MkdirOption(USB0_NETWORK.parent),
                    CopyInOption(osImage.hostPath(USB0_NETWORK), USB0_NETWORK.parent))
            }
            that(osImage.hostPath(USB0_NETWORK)) {
                @Suppress("SpellCheckingInspection")
                textContent.isEqualTo("""
                    auto usb0
                    allow-hotplug usb0
                    iface usb0 inet static
                      address 10.10.1.10/27
                    
                    
                """.trimIndent())
            }
        }
    }

    @Test
    fun `should configure USB gadget`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        expect {
            that(patch(osImage)).virtCustomizations {
                contains(
                    MkdirOption(USB_GADGET.parent),
                    CopyInOption(osImage.hostPath(USB_GADGET), USB_GADGET.parent),
                    ChmodOption("0755", USB_GADGET),
                )
            }
            that(osImage.hostPath(USB_GADGET)) {
                @Suppress("SpellCheckingInspection")
                textContent.isEqualTo("""
                    #!/bin/bash

                    cd /sys/kernel/config/usb_gadget/
                    mkdir -p USB-Test-Device
                    cd USB-Test-Device
                    echo 0x1d6b > idVendor # Linux Foundation
                    echo 0x0104 > idProduct # Multifunction Composite Gadget
                    echo 0x0100 > bcdDevice # v1.0.0
                    echo 0x0200 > bcdUSB # USB2
                    #echo 0xEF > bDeviceClass
                    #echo 0x02 > bDeviceSubClass
                    #echo 0x01 > bDeviceProtocol
                    mkdir -p strings/0x409
                    echo "fedcba9876543210" > strings/0x409/serialnumber
                    echo "John Doe" > strings/0x409/manufacturer
                    echo "USB Test Device" > strings/0x409/product
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
            }
        }
    }


    @Test
    fun `should configure USB gadget service`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        val patch = patch(osImage)
        expect {
            that(patch).virtCustomizations {
                contains(
                    MkdirOption(USBGADGET_SERVICE.parent),
                    CopyInOption(osImage.hostPath(USBGADGET_SERVICE), USBGADGET_SERVICE.parent),
                )
            }
            that(osImage.hostPath(USBGADGET_SERVICE)) {
                @Suppress("SpellCheckingInspection")
                textContent.isEqualTo("""
                    [Unit]
                    Description=USB Test Device
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
            }
            that(patch).virtCustomizations {
                filterIsInstance<FirstBootOption>().apply {
                    @Suppress("SpellCheckingInspection")
                    any { file.textContent.contains("systemctl enable usbgadget.service") }
                }
            }
        }
    }

    @Test
    fun `should update various files`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        expect {
            that(patch(osImage)).virtCustomizations {
                filterIsInstance<FirstBootOption>().apply {
                    any { file.textContent.contains("echo 'dtoverlay=dwc2' >> $config_txt") }
                    any { file.textContent.contains("sed -i 's/${'$'}/ modules-load=dwc2/' $cmdline_txt") }
                    @Suppress("SpellCheckingInspection")
                    any { file.textContent.contains("echo 'libcomposite' >> $modules") }
                    @Suppress("SpellCheckingInspection")
                    any { file.textContent.contains("echo 'denyinterfaces usb0' >> $dhcpcd_conf") }
                    any { file.textContent.contains("systemctl enable serial-getty@ttyGS0.service") }
                }
            }
        }
    }
}

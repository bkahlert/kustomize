package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.Libguestfs.Companion.hostPath
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.ChmodOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.CopyInOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.FirstBootOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.MkdirOption
import com.imgcstmzr.libguestfs.virtcustomize.file
import com.imgcstmzr.patch.UsbOnTheGoPatch.Companion.CMDLINE_TXT
import com.imgcstmzr.patch.UsbOnTheGoPatch.Companion.CONFIG_TXT
import com.imgcstmzr.patch.UsbOnTheGoPatch.Companion.DHCPCD_CONF
import com.imgcstmzr.patch.UsbOnTheGoPatch.Companion.MODULES
import com.imgcstmzr.patch.UsbOnTheGoPatch.Companion.USB0_DNSMASQD
import com.imgcstmzr.patch.UsbOnTheGoPatch.Companion.USB0_NETWORK
import com.imgcstmzr.patch.UsbOnTheGoPatch.Companion.USBGADGET_SERVICE
import com.imgcstmzr.patch.UsbOnTheGoPatch.Companion.USB_GADGET
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.test.content
import com.imgcstmzr.withTempDir
import koodies.net.ipOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.filterIsInstance
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class UsbOnTheGoPatchTest {

    private val usbOnTheGoPatch = UsbOnTheGoPatch(ipOf("192.168.168.161")..ipOf("192.168.168.174"), ipOf("192.168.168.168"), true)

    @Test
    fun `should have name`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        expectThat(usbOnTheGoPatch.name).isEqualTo("Activate USB gadget with DHCP address range 192.168.168.161..192.168.168.174")
    }

    @Test
    fun `should configure dnsmasq for usb0`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        expect {
            that(usbOnTheGoPatch).customizations(osImage) {
                contains(
                    MkdirOption(USB0_DNSMASQD.parent),
                    CopyInOption(osImage.hostPath(USB0_DNSMASQD), USB0_DNSMASQD.parent))
                filterIsInstance<FirstBootOption>().any {
                    file.content.contains("apt-get install -y dnsmasq")
                }
            }
            that(osImage.hostPath(USB0_DNSMASQD)) {
                content.isEqualTo("""
                    dhcp-authoritative 
                    dhcp-rapid-commit
                    no-ping
                    interface=usb0 
                    dhcp-range=192.168.168.161,192.168.168.174,255.255.255.240,1h 
                    dhcp-option=3 # no gateway / routing
                    #dhcp-option=option:dns-server,192.168.168.192
                    leasefile-ro
                    
                """.trimIndent())
            }
        }
    }

    @Test
    fun `should configure network interface for usb0`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        expect {
            that(usbOnTheGoPatch).customizations(osImage) {
                contains(
                    MkdirOption(USB0_NETWORK.parent),
                    CopyInOption(osImage.hostPath(USB0_NETWORK), USB0_NETWORK.parent))
            }
            that(osImage.hostPath(USB0_NETWORK)) {
                content.isEqualTo("""
                    auto usb0
                    allow-hotplug usb0
                    iface usb0 inet static
                      address 192.168.168.161
                      netmask 255.255.255.240
                    
                """.trimIndent())
            }
        }
    }

    @Test
    fun `should configure USB gadget`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        expect {
            that(usbOnTheGoPatch).customizations(osImage) {
                contains(
                    MkdirOption(USB_GADGET.parent),
                    CopyInOption(osImage.hostPath(USB_GADGET), USB_GADGET.parent),
                    ChmodOption("0755", USB_GADGET),
                )
            }
            that(osImage.hostPath(USB_GADGET)) {
                content.isEqualTo("""
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
            }
        }
    }


    @Test
    fun `should configure USB gadget service`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        expect {
            that(usbOnTheGoPatch).customizations(osImage) {
                contains(
                    MkdirOption(USBGADGET_SERVICE.parent),
                    CopyInOption(osImage.hostPath(USBGADGET_SERVICE), USBGADGET_SERVICE.parent),
                )
            }
            that(osImage.hostPath(USBGADGET_SERVICE)) {
                content.isEqualTo("""
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
            }
            that(usbOnTheGoPatch).customizations(osImage) {
                filterIsInstance<FirstBootOption>().apply {
                    any { file.content.contains("systemctl enable usbgadget.service") }
                }
            }
        }
    }

    @Test
    fun `should update various files`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        expect {
            that(usbOnTheGoPatch).customizations(osImage) {
                filterIsInstance<FirstBootOption>().apply {
                    any { file.content.contains("echo dtoverlay=dwc2 >> $CONFIG_TXT") }
                    any { file.content.contains("sed -i 's/${'$'}/ modules-load=dwc2/' $CMDLINE_TXT") }
                    any { file.content.contains("echo libcomposite >> $MODULES") }
                    any { file.content.contains("echo denyinterfaces usb0 >> $DHCPCD_CONF") }
                    any { file.content.contains("systemctl enable getty@ttyGS0.service") }
                }
            }
        }
    }
}

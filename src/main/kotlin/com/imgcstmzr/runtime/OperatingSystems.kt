package com.imgcstmzr.runtime

import com.imgcstmzr.runtime.OperatingSystem.Credentials
import com.imgcstmzr.runtime.OperatingSystem.Credentials.Companion.empty
import com.imgcstmzr.runtime.OperatingSystem.Credentials.Companion.withPassword
import com.imgcstmzr.util.GitHub
import koodies.unit.Giga
import koodies.unit.Mega
import koodies.unit.Size
import koodies.unit.bytes

/**
 * Technically supported, yet not necessarily fully compatible operating systems
 * that can be used for image customization.
 */
@Suppress("unused")
enum class OperatingSystems(
    override val fullName: String,
    override val downloadUrl: String,
    override val approximateImageSize: Size,
    override val defaultCredentials: Credentials,
) : OperatingSystem {

    /**
     * [belanaOS—Run Docker containers on embedded devices](https://www.balena.io/os/)
     */
    @Suppress("SpellCheckingInspection")
    BalenaOS(
        fullName = "balenaOS for Raspberry Pi (v1 and Zero)",
        downloadUrl = "https://api.balena-cloud.com/download?deviceType=raspberry-pi&version=2.54.2+rev1.dev&fileType=.zip",
        approximateImageSize = 950.Mega.bytes,
        defaultCredentials = empty,
    ),

    /**
     * [HypriotOS](https://www.raspberrypi.org/downloads/raspberry-pi-os/)
     */
    HypriotOs(
        fullName = "Hypriot OS",
        downloadUrl = GitHub.repo("hypriot/image-builder-rpi").latestTag
            .let { "https://github.com/hypriot/image-builder-rpi/releases/download/$it/hypriotos-rpi-$it.img.zip" },
        approximateImageSize = 420.Mega.bytes,
        defaultCredentials = "pirate" withPassword "hypriot",
    ),

    /**
     * [Raspberry Pi OS Lite](https://www.raspberrypi.org/downloads/raspberry-pi-os/)
     */
    RaspberryPiLite(
        fullName = "Raspberry Pi OS Lite",
        downloadUrl = "https://downloads.raspberrypi.org/raspios_lite_armhf_latest",
        approximateImageSize = 1.85.Giga.bytes,
        defaultCredentials = "pi" withPassword "raspberry",
    ),

    /**
     * [Raspberry Pi OS Lite](https://www.raspberrypi.org/downloads/raspberry-pi-os/)
     */
    RaspberryPi(
        fullName = "Raspberry Pi OS",
        downloadUrl = "https://downloads.raspberrypi.org/raspios_armhf_latest",
        approximateImageSize = 3.82.Giga.bytes,
        defaultCredentials = "pi" withPassword "raspberry",
    ),

    /**
     * [DietPi](https://dietpi.com)
     */
    @Suppress("SpellCheckingInspection")
    DietPi(
        fullName = "Diet Pi",
        downloadUrl = "https://dietpi.com/downloads/images/DietPi_RPi-ARMv6-Buster.7z",
        approximateImageSize = 1.06.Giga.bytes,
        defaultCredentials = "root" withPassword "dietpi",
    ),

    /**
     * [Tiny Core](http://tinycorelinux.net/ports.html)
     */
    TinyCore(
        fullName = "Tiny Core",
        downloadUrl = "http://tinycorelinux.net/12.x/armv6/releases/RPi/piCore-12.0.zip",
        approximateImageSize = 88.Mega.bytes,
        defaultCredentials = "tc" withPassword "piCore",
    ),

    /**
     * [Arch Linux ARM](https://archlinuxarm.org/platforms/armv6/raspberry-pi)
     */
    ArchLinuxArm(
        fullName = "Arch Linux ARM",
        downloadUrl = "http://os.archlinuxarm.org/os/ArchLinuxARM-rpi-latest.tar.gz",
        approximateImageSize = 1.2.Giga.bytes,
        defaultCredentials = "alarm" withPassword "alarm", // root, root
    ),

    /**
     * [RISC OS](https://www.riscosopen.org/content/downloads/raspberry-pi)
     */
    RiscOs(
        fullName = "RISC OS",
        downloadUrl = "https://www.riscosopen.org/zipfiles/platform/raspberry-pi/BCM2835.5.24.zip?1544451169",
        approximateImageSize = 5.2.Mega.bytes,
        defaultCredentials = empty,
    ),

    /**
     * [RISC OS Pico RC5](https://www.riscosopen.org/content/downloads/raspberry-pi)
     */
    RiscOsPicoRc5(
        fullName = "RISC OS Pico RC5",
        downloadUrl = "https://www.riscosopen.org/zipfiles/platform/raspberry-pi/BCM2835Dev.5.29.zip?1604815147",
        approximateImageSize = 2.Mega.bytes,
        defaultCredentials = empty,
    ),

    /**
     * [Ubuntu Server 20.10](https://cdimage.ubuntu.com/releases/20.10/release/ubuntu-20.10-preinstalled-server-armhf+raspi.img.xz)
     */
    UbuntuServer(
        fullName = "Ubuntu Server 20.10",
        downloadUrl = "https://cdimage.ubuntu.com/releases/20.10/release/ubuntu-20.10-preinstalled-server-armhf+raspi.img.xz",
        approximateImageSize = 3.03.Giga.bytes,
        defaultCredentials = "ubuntu" withPassword "ubuntu",
    ),

    /**
     * [WebThings Gateway](https://github.com/WebThingsIO/gateway/releases/download/0.12.0/gateway-0.12.0.img.zip)
     */
    WebThingsGateway(
        fullName = "WebThings Gateway for Raspberry Pi",
        downloadUrl = "https://github.com/WebThingsIO/gateway/releases/download/0.12.0/gateway-0.12.0.img.zip",
        approximateImageSize = 3.04.Giga.bytes,
        defaultCredentials = "pi" withPassword "raspberry",
    ),

    /**
     * ImgCstmzr Test OS
     *
     * Non-functional dynamically generated disk image for sole testing purposes.
     */
    ImgCstmzrTestOS(
        fullName = "ImgCstmzr Test OS",
        downloadUrl = "imgcstmzr://build?files=classpath:img/boot/cmdline.txt%3Eboot&files=classpath:img/boot/config.txt%3Eboot",
        approximateImageSize = 6.Mega.bytes,
        defaultCredentials = empty,
    );

    override fun toString(): String = fullName
}

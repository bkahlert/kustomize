package com.imgcstmzr.os

import com.imgcstmzr.os.OperatingSystem.Credentials
import com.imgcstmzr.os.OperatingSystem.Credentials.Companion.empty
import com.imgcstmzr.os.OperatingSystem.Credentials.Companion.withPassword
import com.imgcstmzr.util.GitHub
import koodies.unit.Giga
import koodies.unit.Mega
import koodies.unit.Size
import koodies.unit.bytes

/**
 * Technically supported, yet not necessarily fully compatible operating systems
 * that can be used for image customization.
 */
enum class OperatingSystems(
    override val fullName: String,
    downloadUrl: () -> String,
    override val approximateImageSize: Size,
    override val defaultCredentials: Credentials,
) : OperatingSystem {

    @Suppress("SpellCheckingInspection")
    BalenaOS(
        fullName = "balenaOS for Raspberry Pi (v1 and Zero)",
        downloadUrl = "https://api.balena-cloud.com/download?deviceType=raspberry-pi&version=2.54.2+rev1.dev&fileType=.zip",
        approximateImageSize = 950.Mega.bytes,
        defaultCredentials = empty,
    ),

    @Suppress("SpellCheckingInspection")
    HypriotOS(
        fullName = "Hypriot OS",
        downloadUrl = {
            val tag = GitHub.repo("hypriot/image-builder-rpi").latestTag
            "https://github.com/hypriot/image-builder-rpi/releases/download/$tag/hypriotos-rpi-$tag.img.zip"
        },
        approximateImageSize = 420.Mega.bytes,
        defaultCredentials = "pirate" withPassword "hypriot",
    ),

    RaspberryPiLite(
        fullName = "Raspberry Pi OS Lite",
        downloadUrl = "https://downloads.raspberrypi.org/raspios_lite_armhf_latest",
        approximateImageSize = 1.85.Giga.bytes,
        defaultCredentials = "pi" withPassword "raspberry",
    ),

    RaspberryPi(
        fullName = "Raspberry Pi OS",
        downloadUrl = "https://downloads.raspberrypi.org/raspios_armhf_latest",
        approximateImageSize = 3.82.Giga.bytes,
        defaultCredentials = "pi" withPassword "raspberry",
    ),

    DietPi(
        fullName = "Diet Pi",
        downloadUrl = "https://dietpi.com/downloads/images/DietPi_RPi-ARMv6-Buster.7z",
        approximateImageSize = 1.06.Giga.bytes,
        defaultCredentials = "root" withPassword @Suppress("SpellCheckingInspection") "dietpi",
    ),

    TinyCore(
        fullName = "Tiny Core",
        downloadUrl = "http://tinycorelinux.net/12.x/armv6/releases/RPi/piCore-12.0.zip",
        approximateImageSize = 88.Mega.bytes,
        defaultCredentials = "tc" withPassword "piCore",
    ),

    ArchLinuxArm(
        fullName = "Arch Linux ARM",
        downloadUrl = "http://os.archlinuxarm.org/os/ArchLinuxARM-rpi-latest.tar.gz",
        approximateImageSize = 1.2.Giga.bytes,
        defaultCredentials = "alarm" withPassword "alarm", // root, root
    ),

    RiscOs(
        fullName = "RISC OS",
        downloadUrl = "https://www.riscosopen.org/zipfiles/platform/raspberry-pi/BCM2835.5.24.zip?1544451169",
        approximateImageSize = 5.2.Mega.bytes,
        defaultCredentials = empty,
    ),

    RiscOsPicoRc5(
        fullName = "RISC OS Pico RC5",
        downloadUrl = "https://www.riscosopen.org/zipfiles/platform/raspberry-pi/BCM2835Dev.5.29.zip?1604815147",
        approximateImageSize = 2.Mega.bytes,
        defaultCredentials = empty,
    ),

    UbuntuServer(
        fullName = "Ubuntu Server 20.10",
        downloadUrl = "https://cdimage.ubuntu.com/releases/20.10/release/ubuntu-20.10-preinstalled-server-armhf+raspi.img.xz",
        approximateImageSize = 3.03.Giga.bytes,
        defaultCredentials = "ubuntu" withPassword "ubuntu",
    ),

    WebThingsGateway(
        fullName = "WebThings Gateway for Raspberry Pi",
        downloadUrl = "https://github.com/WebThingsIO/gateway/releases/download/0.12.0/gateway-0.12.0.img.zip",
        approximateImageSize = 3.04.Giga.bytes,
        defaultCredentials = "pi" withPassword "raspberry",
    ),

    RiscTestOS(
        fullName = "RISC OS Pico RC5 (test only)",
        downloadUrl = @Suppress("SpellCheckingInspection") "classpath:riscos.img",
        approximateImageSize = 2.Mega.bytes,
        defaultCredentials = empty,
    );

    constructor(fullName: String, downloadUrl: String, approximateImageSize: Size, defaultCredentials: Credentials) :
        this(fullName, { downloadUrl }, approximateImageSize, defaultCredentials)

    override val downloadUrl: String by lazy { downloadUrl() }

    override fun toString(): String = fullName
}

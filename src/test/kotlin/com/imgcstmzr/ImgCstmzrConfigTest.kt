package com.imgcstmzr

import com.imgcstmzr.libguestfs.guestfish.mounted
import com.imgcstmzr.patch.*
import com.imgcstmzr.runtime.OperatingSystem.Credentials.Companion.withPassword
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.test.*
import com.typesafe.config.ConfigFactory
import koodies.logging.InMemoryLogger
import koodies.shell.ShellScript
import koodies.unit.Giga
import koodies.unit.bytes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.*

@SystemProperties(
    SystemProperty("IMG_CSTMZR_USERNAME", "FiFi"),
    SystemProperty("IMG_CSTMZR_PASSWORD", "TRIXIbelle1234"),
    SystemProperty("IMG_CSTMZR_WPA_SUPPLICANT", "entry1\nentry2"),
    SystemProperty("INDIVIDUAL_KEY", "B{1:Βϊ𝌁\uD834\uDF57"),
)
@Execution(CONCURRENT)
class ImgCstmzrConfigTest {

    private fun loadImgCstmztn(): ImgCstmzrConfig =
        ImgCstmzrConfig.load(ConfigFactory.parseResources("sample.conf"))

    @Test
    fun `should deserialize`() {
        val imgCstmztn = loadImgCstmztn()

        expectThat(imgCstmztn).compose("has equal properties") {
            get { trace }.isTrue()
            get { name }.isEqualTo("Sample Project")
            get { os }.isEqualTo(OperatingSystems.RaspberryPiLite)
            get { timeZone ?: timezone }.isEqualTo("Europe/Berlin")
            get { hostname }.isEqualTo(ImgCstmzrConfig.Hostname("demo", true))
            get { wifi }.isEqualTo(ImgCstmzrConfig.Wifi("entry1\nentry2", true, false))
            get { imgSize }.isEqualTo(4.Giga.bytes)
            get { ssh?.enabled }.isTrue()
            get { ssh?.port }.isEqualTo(1234)
            @Suppress("SpellCheckingInspection")
            get { ssh?.authorizedKeys }.isNotNull().contains(
                listOf("""ssh-rsa MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKbic/EEoiSu09lYR1y5001NA1K63M/Jd+IV1b2YpoXJxWDrkzQ/3v/SE84/cSayWAy4LVEXUodrt1WkPZ/NjE8CAwEAAQ== "John Doe 2020-12-10 btw, Corona sucks"""")
            )
            get { ssh?.authorizedKeys }.isNotNull().none { containsAtLeast("ssh-", 2) }
            get { defaultUser }.isEqualTo(ImgCstmzrConfig.DefaultUser(null, "FiFi", "TRIXIbelle1234"))
            get { samba?.sanitizedHomeShare }.isEqualTo(true)
            get { samba?.sanitizedRootShare }.isEqualTo(RootShare.`read-write`)
            get { usbOtg }.isNotNull().isEqualTo("g_ether")
            get { usbOtgOptions }.isNotNull().contains("usb0-interfaces", "usb0-dhcp", "usb0-dnsmasq")
            get { tweaks?.aptRetries }.isEqualTo(10)
            get { files }.isNotNull().isEqualTo(listOf(
                ImgCstmzrConfig.FileOperation("\n        line 1\n        line 2\n        ", null, "/boot/file-of-lines.txt"),
                ImgCstmzrConfig.FileOperation(null, "BKAHLERT.png", "/home/FiFi/image.png"),
            )).any { get { sanitizedAppend }.isEqualTo("line 1\nline 2") }
            get { setup!![0].name }.isEqualTo("the basics")
            get { setup!![0] }.containsExactly(
                ShellScript(
                    name = "Configuring SSH port",
                    content = """sed -i 's/^\#Port 22${'$'}/Port 1234/g' /etc/ssh/sshd_config"""
                ),
                ShellScript(
                    name = "very-----------------long",
                    content = """"""
                ),
                ShellScript(
                    name = "middle",
                    content = """echo ''"""
                ),
                ShellScript(
                    name = "s",
                    content = """echo ''"""
                )
            )
            get { setup!![1].name }.isEqualTo("leisure")
            get { setup!![1] }.containsExactly(
                ShellScript(
                    name = "The Title",
                    content = """
                                # I rather explain things
                                echo "I'm writing a poem __〆(￣ー￣ )"
                                cat <<EOF >poem-for-you
                                Song of the Witches: “Double, double toil and trouble”
                                BY WILLIAM SHAKESPEARE
                                (from Macbeth)
                                Double, double toil and trouble;
                                Fire burn and caldron bubble.
                                Fillet of a fenny snake,
                                In the caldron boil and bake;
                                Eye of newt and toe of frog,
                                Wool of bat and tongue of dog,
                                Adder's fork and blind-worm's sting,
                                Lizard's leg and howlet's wing,
                                For a charm of powerful trouble,
                                Like a hell-broth boil and bubble.
                                Double, double toil and trouble;
                                Fire burn and caldron bubble.
                                Cool it with a baboon's blood,
                                Then the charm is firm and good.
                                EOF
                                
                            """.trimIndent()
                ),
            )
            get { firstBoot }.isNotNull().containsExactly(
                ShellScript(
                    name = "Finalizing A",
                    content = """echo "Type X to...">>${'$'}HOME/first-boot.txt"""
                ),
                ShellScript(
                    name = "Finalizing B",
                    content = "startx"
                )
            )
            get { flashDisk }.isEqualTo("auto")
        }.then { if (allPassed) pass() else fail() }
    }

    @Test
    fun `should create patch`() {
        val imgCstmztn = loadImgCstmztn()
        val patch = imgCstmztn.toPatches()
        expectThat(CompositePatch(patch)) {
            get { name }.contains("Increasing Disk Space: 4.00 GB").contains("Change Username")
            get { diskPreparations }.isNotEmpty()
            get { diskCustomizations }.isNotEmpty()
            get { diskOperations }.isNotEmpty()
            get { osPreparations }.isNotEmpty()
            get { osOperations }.isNotEmpty()
        }
    }

    @Test
    fun `should optimize patches`() {
        val imgCstmztn = loadImgCstmztn()
        val patches = imgCstmztn.toOptimizedPatches()
        expectThat(patches) {
            hasSize(6)
            get { get(0) }.isA<CompositePatch>().get { this.patches.map { it::class } }
                .contains(
                    TweaksPatch::class,
                    TimeZonePatch::class,
                    HostnamePatch::class,
                    ImgResizePatch::class,
                    UsernamePatch::class,
                    SshEnablementPatch::class,
                    WpaSupplicantPatch::class,
                )
            get { get(1) }.isA<CompositePatch>().get { this.patches.map { it::class } }
                .contains(
                    SambaPatch::class,
                    WifiAutoReconnectPatch::class,
                    WifiPowerSafeModePatch::class,
                )
            get { get(2) }.isA<CompositePatch>().get { this.patches.map { it::class } }
                .contains(
                    PasswordPatch::class,
                    SshAuthorizationPatch::class,
                    SshPortPatch::class,
                    UsbOnTheGoPatch::class,
                )
            get { get(3) }.isA<ShellScriptPatch>()
            get { get(4) }.isA<ShellScriptPatch>()
            get { get(5) }.isA<FirstBootPatch>()
        }
    }

    @ThirtyMinutesTimeout
    @E2E
    @Test
    fun InMemoryLogger.`should apply patches`(@OS(OperatingSystems.RaspberryPiLite) osImage: OperatingSystemImage) {
        val imgCstmztn = loadImgCstmztn()
        val patches = imgCstmztn.toOptimizedPatches()

        patches.forEach { patch ->
            patch(osImage, patch)
        }

        expect {
            that(osImage) {
                get { credentials }.isEqualTo("FiFi" withPassword "TRIXIbelle1234")
                mounted(this@`should apply patches`) {
                    path("/home/FiFi/first-boot.txt") { not { exists() } }
                }
                booted(this@`should apply patches`) {
                    command("echo '👏 🤓 👋'");
                    { true }
                }
            }
            that(logged).contains("__〆(￣ー￣ )")
        }
    }
}

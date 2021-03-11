package com.imgcstmzr

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.libguestfs.guestfish.mounted
import com.imgcstmzr.patch.CompositePatch
import com.imgcstmzr.patch.FirstBootPatch
import com.imgcstmzr.patch.HostnamePatch
import com.imgcstmzr.patch.ImgResizePatch
import com.imgcstmzr.patch.PasswordPatch
import com.imgcstmzr.patch.RootShare
import com.imgcstmzr.patch.SambaPatch
import com.imgcstmzr.patch.ShellScriptPatch
import com.imgcstmzr.patch.SshAuthorizationPatch
import com.imgcstmzr.patch.SshEnablementPatch
import com.imgcstmzr.patch.SshPortPatch
import com.imgcstmzr.patch.TimeZonePatch
import com.imgcstmzr.patch.TweaksPatch
import com.imgcstmzr.patch.UsbEthernetGadgetPatch
import com.imgcstmzr.patch.UsernamePatch
import com.imgcstmzr.patch.WifiAutoReconnectPatch
import com.imgcstmzr.patch.WifiPowerSafeModePatch
import com.imgcstmzr.patch.WpaSupplicantPatch
import com.imgcstmzr.patch.booted
import com.imgcstmzr.patch.patch
import com.imgcstmzr.runtime.OperatingSystem.Credentials.Companion.withPassword
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.OS
import com.imgcstmzr.test.Smoke
import com.imgcstmzr.test.SystemProperties
import com.imgcstmzr.test.SystemProperty
import com.imgcstmzr.test.ThirtyMinutesTimeout
import com.imgcstmzr.test.containsAtLeast
import com.imgcstmzr.test.exists
import com.typesafe.config.ConfigFactory
import koodies.io.path.Locations
import koodies.logging.InMemoryLogger
import koodies.net.div
import koodies.net.ip4Of
import koodies.shell.ShellScript
import koodies.unit.Giga
import koodies.unit.bytes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEmpty
import strikt.assertions.isTrue
import strikt.assertions.none
import java.util.TimeZone
import java.util.concurrent.TimeUnit

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

    @Timeout(20, unit = TimeUnit.MINUTES)
    @TestFactory
    fun `should deserialize`() = loadImgCstmztn().test {
        expect { trace }.isTrue()
        expect { name }.isEqualTo("Sample Project")
        expect { os }.isEqualTo(OperatingSystems.RaspberryPiLite)
        expect { timeZone }.isEqualTo(TimeZone.getTimeZone("Europe/Berlin"))
        expect { hostname }.isEqualTo(ImgCstmzrConfig.Hostname("demo", true))
        expect { wifi }.isEqualTo(ImgCstmzrConfig.Wifi("entry1\nentry2", true, false))
        expect { size }.isEqualTo(4.Giga.bytes)
        with { ssh }.notNull {
            expect { enabled }.isTrue()
            expect { port }.that {
                isEqualTo(1234)
                @Suppress("SpellCheckingInspection")
                expect { authorizedKeys }.that {
                    contains(
                        listOf("""ssh-rsa MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKbic/EEoiSu09lYR1y5001NA1K63M/Jd+IV1b2YpoXJxWDrkzQ/3v/SE84/cSayWAy4LVEXUodrt1WkPZ/NjE8CAwEAAQ== "John Doe 2020-12-10 btw, Corona sucks"""")
                    )
                    none { containsAtLeast("ssh-", 2) }
                }
            }
        }
        expect { defaultUser }.isEqualTo(ImgCstmzrConfig.DefaultUser(null, "FiFi", "TRIXIbelle1234"))
        expect { samba }.isEqualTo(ImgCstmzrConfig.Samba(true, RootShare.`read-write`))
        expect { usbGadgets }.containsExactly(ImgCstmzrConfig.UsbGadget.Ethernet(
            dhcpRange = ip4Of("192.168.168.160") / 28,
            deviceAddress = ip4Of("192.168.168.168"),
            hostAsDefaultGateway = true,
            enableSerialConsole = true,
        ))
        expect { tweaks?.aptRetries }.isEqualTo(10)
        expect { files }.that {
            isEqualTo(listOf(
                ImgCstmzrConfig.FileOperation("line 1\nline 2", null, DiskPath("/boot/file-of-lines.txt")),
                ImgCstmzrConfig.FileOperation(null, Locations.WorkingDirectory.resolve("src/test/resources/BKAHLERT.png"), DiskPath("/home/FiFi/image.png")),
            )).any { get { append } isEqualTo ("line 1\nline 2") }
        }
        expect { setup[0].name }.that { isEqualTo("the basics") }
        expect { setup[0] }.that {
            containsExactly(
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
        }
        expect { setup[1].name }.that { isEqualTo("leisure") }
        expect { setup[1] }.that {
            containsExactly(
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
        }
        expect { firstBoot }.that {
            containsExactly(
                ShellScript(
                    name = "Finalizing A",
                    content = """echo "Type X to …">>${'$'}HOME/first-boot.txt"""
                ),
                ShellScript(
                    name = "Finalizing B",
                    content = "startx"
                )
            )
        }
        expect { flashDisk }.that { isEqualTo("auto") }
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
                    UsbEthernetGadgetPatch::class,
                )
            get { get(3) }.isA<ShellScriptPatch>()
            get { get(4) }.isA<ShellScriptPatch>()
            get { get(5) }.isA<FirstBootPatch>()
        }
    }

    @ThirtyMinutesTimeout @E2E @Smoke @Test
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

package com.imgcstmzr.cli

import com.imgcstmzr.ImgCstmzr
import com.imgcstmzr.cli.CustomizationConfig.DefaultUser
import com.imgcstmzr.cli.CustomizationConfig.FileOperation
import com.imgcstmzr.cli.CustomizationConfig.Hostname
import com.imgcstmzr.cli.CustomizationConfig.Samba
import com.imgcstmzr.cli.CustomizationConfig.UsbGadget.Ethernet
import com.imgcstmzr.cli.CustomizationConfig.Wifi
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.patch.CompositePatch
import com.imgcstmzr.patch.FirstBootPatch
import com.imgcstmzr.patch.HostnamePatch
import com.imgcstmzr.patch.ImgResizePatch
import com.imgcstmzr.patch.PasswordPatch
import com.imgcstmzr.patch.RootShare.`read-write`
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
import com.typesafe.config.ConfigFactory
import koodies.net.div
import koodies.net.ip4Of
import koodies.shell.ShellScript
import koodies.test.SystemProperties
import koodies.test.SystemProperty
import koodies.test.containsAtLeast
import koodies.test.hasElements
import koodies.test.test
import koodies.text.LineSeparators.prefixLinesWith
import koodies.unit.Gibi
import koodies.unit.bytes
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEmpty
import strikt.assertions.isTrue
import strikt.assertions.none
import java.util.TimeZone
import kotlin.io.path.div

@Suppress("SpellCheckingInspection")
@SystemProperties(
    SystemProperty("IMG_CSTMZR_USERNAME", "john.doe"),
    SystemProperty("IMG_CSTMZR_PASSWORD", "Password1234"),
    SystemProperty("IMG_CSTMZR_WPA_SUPPLICANT", "entry1\nentry2"),
    SystemProperty("INDIVIDUAL_KEY", "B{1:Βϊ𝌁\uD834\uDF57"),
)
class CustomizationConfigTest {

    private fun loadMinimalConfig(config: String) =
        CustomizationConfig.load(ConfigFactory.parseString("""
                img {
                    name: .test
                    os: RISC OS Pico RC5 (test only)
                    ${config.prefixLinesWith("    ")}
                }
            """.trimIndent()))

    private fun loadImgCstmzrConfig(): CustomizationConfig =
        CustomizationConfig.load(ConfigFactory.parseResources("sample.conf"))

    @TestFactory
    fun `should deserialize`() = test(loadImgCstmzrConfig()) {
        expecting { trace } that { isTrue() }
        expecting { name } that { isEqualTo(".test") }
        expecting { os } that { isEqualTo(RaspberryPiLite) }
        expecting { timeZone } that { isEqualTo(TimeZone.getTimeZone("Europe/Berlin")) }
        expecting { hostname } that { isEqualTo(Hostname("demo", true)) }
        expecting { wifi } that { isEqualTo(Wifi("entry1\nentry2", autoReconnect = true, powerSafeMode = false)) }
        expecting { size } that { isEqualTo(4.Gibi.bytes) }
        with { ssh!! }.then {
            expecting { enabled } that { isTrue() }
            expecting { port } that { isEqualTo(1234) }
            @Suppress("SpellCheckingInspection")
            expecting { authorizedKeys } that {
                contains(
                    listOf("""ssh-rsa MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKbic/EEoiSu09lYR1y5001NA1K63M/Jd+IV1b2YpoXJxWDrkzQ/3v/SE84/cSayWAy4LVEXUodrt1WkPZ/NjE8CAwEAAQ== "John Doe 2020-12-10 btw, Corona sucks"""")
                )
                none { containsAtLeast("ssh-", 2) }
            }
        }
        expecting { defaultUser } that { isEqualTo(DefaultUser(null, "john.doe", "Password1234")) }
        expecting { samba } that { isEqualTo(Samba(true, `read-write`)) }
        expecting { usbGadgets } that {
            containsExactly(Ethernet(
                dhcpRange = ip4Of("192.168.168.160") / 28,
                deviceAddress = ip4Of("192.168.168.168"),
                hostAsDefaultGateway = true,
                enableSerialConsole = true,
            ))
        }
        expecting { tweaks?.aptRetries } that { isEqualTo(10) }
        expecting { files } that {
            isEqualTo(listOf(
                FileOperation("line 1\nline 2", null, LinuxRoot.boot / "file-of-lines.txt"),
                FileOperation(null, ImgCstmzr.WorkingDirectory / "src" / "test" / "resources" / "BKAHLERT.png", LinuxRoot.home / "john.doe" / "image.png"),
            )).any { get { append } isEqualTo ("line 1\nline 2") }
        }
        expecting { setup[0].name } that { isEqualTo("the basics") }
        expecting { setup[0] } that {
            containsExactly(
                ShellScript(
                    name = "Echoing setup",
                    content = "echo 'setup'"
                ),
            )
        }
        expecting { firstBoot } that {
            containsExactly(
                ShellScript(
                    name = "Finalizing",
                    content = "echo 'Type X to …'>>${'$'}HOME/first-boot.txt"
                ),
            )
        }
    }

    @Nested
    inner class HostnameConfig {

        @Test
        fun `should deserialize name`() {
            expectThat(loadMinimalConfig("""
                hostname {
                    name: "test"
                }
            """.trimIndent())) {
                get { hostname }.isEqualTo(Hostname("test", true))
            }
        }

        @Test
        fun `should deserialize no name`() {
            expectThat(loadMinimalConfig("")) {
                get { hostname }.isEqualTo(null)
            }
        }

        @Test
        fun `should deserialize random suffix`() {
            expectThat(loadMinimalConfig("""
                hostname {
                    name: "test"
                    random-suffix: true
                }
            """.trimIndent())) {
                get { hostname }.isEqualTo(Hostname("test", true))
            }
        }

        @Test
        fun `should deserialize no random suffix`() {
            expectThat(loadMinimalConfig("""
                hostname {
                    name: "test"
                    random-suffix: false
                }
            """.trimIndent())) {
                get { hostname }.isEqualTo(Hostname("test", false))
            }
        }
    }

    @Nested
    inner class FirstBootConfig {

        @Test
        fun `should deserialize scripts`() {
            expectThat(loadMinimalConfig("""
                first-boot = [
                    {
                      content: ""${'"'}
                          echo 'A'
                          echo 'B'
                          echo 'C'
                          ""${'"'}
                    },
                    {
                      name: "write and run scripts"
                      content: ""${'"'}
                
                          cat <<EOF >/root/script.sh
                          #!/bin/sh
                          echo '1'
                          echo '2'
                          echo '3'
                          EOF
                
                          chmod +x /root/script.sh
                          /root/script.sh
                          ""${'"'}
                    },
                  ]
            """.trimIndent())) {
                get { firstBoot }
                    .hasSize(2)
                    .containsExactly(
                        ShellScript("""
                            echo 'A'
                            echo 'B'
                            echo 'C'

                    """.trimIndent()),
                        ShellScript("write and run scripts", """
                            
                            
                            cat <<EOF >/root/script.sh
                            #!/bin/sh
                            echo '1'
                            echo '2'
                            echo '3'
                            EOF

                            chmod +x /root/script.sh
                            /root/script.sh

                    """.trimIndent()),
                    )
            }
        }

        @Test
        fun `should deserialize no scripts`() {
            expectThat(loadMinimalConfig("")) {
                get { firstBoot }.isEmpty()
            }
        }

        @Test
        fun `should deserialize empty scripts`() {
            expectThat(loadMinimalConfig("""
                first-boot = [ ]
            """.trimIndent())) {
                get { firstBoot }.isEmpty()
            }
        }
    }

    @Test
    fun `should create patch`(osImage: OperatingSystemImage) {
        val config = loadImgCstmzrConfig()
        val patch = config.toPatches()
        expectThat(CompositePatch(patch).invoke(osImage)) {
            get { name }.contains("Increase Disk Space to 4 GiB").contains("Change Username")
            get { diskPreparations }.isNotEmpty()
            get { diskCustomizations }.isNotEmpty()
            get { diskOperations }.isNotEmpty()
            get { osPreparations }.isNotEmpty()
            get { osOperations }.isNotEmpty()
        }
    }

    @Test
    fun `should optimize patches`() {
        val config = loadImgCstmzrConfig()
        val patches = config.toOptimizedPatches()
        expectThat(patches).hasElements(
            {
                isA<CompositePatch>().get { this.patches.map { it::class } }
                    .contains(
                        TweaksPatch::class,
                        TimeZonePatch::class,
                        HostnamePatch::class,
                        ImgResizePatch::class,
                        UsernamePatch::class,
                        SshEnablementPatch::class,
                        WpaSupplicantPatch::class,
                    )
            },
            {
                isA<CompositePatch>().get { this.patches.map { it::class } }
                    .contains(
                        SambaPatch::class,
                        WifiAutoReconnectPatch::class,
                        WifiPowerSafeModePatch::class,
                    )
            },
            {
                isA<CompositePatch>().get { this.patches.map { it::class } }
                    .contains(
                        PasswordPatch::class,
                        SshAuthorizationPatch::class,
                        SshPortPatch::class,
                        UsbEthernetGadgetPatch::class,
                    )
            },
            { isA<ShellScriptPatch>() },
            { isA<FirstBootPatch>() },
        )
    }
}

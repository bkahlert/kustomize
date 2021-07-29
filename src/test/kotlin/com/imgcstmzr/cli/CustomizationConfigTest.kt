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
import com.imgcstmzr.os.OperatingSystems.RiscTestOS
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
import koodies.io.path.asPath
import koodies.io.path.writeText
import koodies.junit.UniqueId
import koodies.net.div
import koodies.net.ip4Of
import koodies.shell.ShellScript
import koodies.test.SystemProperties
import koodies.test.SystemProperty
import koodies.test.containsAtLeast
import koodies.test.hasElements
import koodies.test.test
import koodies.test.withTempDir
import koodies.text.ansiRemoved
import koodies.unit.Gibi
import koodies.unit.bytes
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import strikt.assertions.message
import strikt.assertions.none
import java.nio.file.Path
import java.util.TimeZone
import kotlin.io.path.div

@Suppress("SpellCheckingInspection")
@SystemProperties(
    SystemProperty("SAMPLE_FULL_USERNAME", "john.doe"),
    SystemProperty("SAMPLE_FULL_PASSWORD", "Password1234"),
    SystemProperty("SAMPLE_FULL_WPA_SUPPLICANT", "entry1\nentry2"),
)
class CustomizationConfigTest {

    @Nested
    inner class OSConfig {

        private fun Path.loadConfig(os: String) =
            CustomizationConfig.load(resolve("minimal.conf").writeText("os: $os"))

        @Test
        fun `should throw blank value`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThrows<IllegalArgumentException> { (loadConfig("\"  \"")) }
                .message.isNotNull().ansiRemoved.isEqualTo("Missing configuration: os")
        }

        @Nested
        inner class FullName {

            @Test
            fun `should deserialize on exact match`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(loadConfig(RiscTestOS.fullName)).get { os }.isEqualTo(RiscTestOS)
            }

            @Test
            fun `should deserialize on unique match`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(loadConfig(RiscTestOS.fullName.dropLast(1))).get { os }.isEqualTo(RiscTestOS)
            }

            @Test
            fun `should throw on multiple matches`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThrows<IllegalArgumentException> { loadConfig("Raspberry Pi OS XXX") }
                    .message.isNotNull().ansiRemoved.isEqualTo("""
                        Raspberry Pi OS XXX is not supported. Did you mean any of the following? 
                        - RaspberryPiLite: Raspberry Pi OS Lite
                        - RaspberryPi: Raspberry Pi OS
                    """.trimIndent())
            }
        }

        @Nested
        inner class Name {

            @Test
            fun `should deserialize on exact match`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(loadConfig(RiscTestOS.name)).get { os }.isEqualTo(RiscTestOS)
            }

            @Test
            fun `should deserialize on unique match`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(loadConfig(RiscTestOS.name.dropLast(1))).get { os }.isEqualTo(RiscTestOS)
            }

            @Test
            fun `should throw on multiple matches`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThrows<IllegalArgumentException> { loadConfig(RaspberryPiLite.name.dropLast(2)) }
                    .message.isNotNull().ansiRemoved.isEqualTo("""
                        RaspberryPiLi is not supported. Did you mean any of the following? 
                        - RaspberryPiLite: Raspberry Pi OS Lite
                        - RaspberryPi: Raspberry Pi OS
                    """.trimIndent())
            }
        }
    }

    @Nested
    inner class MinimalConfig {

        private fun Path.loadMinimalConfig(config: String) =
            CustomizationConfig.load(resolve("minimal.conf").writeText("""
                    os: RISC OS Pico RC5 (test only)
                    $config
                """.trimIndent()))

        @Nested
        inner class HostnameConfig {

            @Test
            fun `should deserialize name`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(loadMinimalConfig("""
                    hostname {
                        name: "test"
                    }
                """.trimIndent())) {
                    get { hostname }.isEqualTo(Hostname("test", true))
                }
            }

            @Test
            fun `should deserialize no name`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(loadMinimalConfig("")) {
                    get { hostname }.isEqualTo(null)
                }
            }

            @Test
            fun `should deserialize random suffix`(uniqueId: UniqueId) = withTempDir(uniqueId) {
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
            fun `should deserialize no random suffix`(uniqueId: UniqueId) = withTempDir(uniqueId) {
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
            fun `should deserialize scripts`(uniqueId: UniqueId) = withTempDir(uniqueId) {
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
            fun `should deserialize no scripts`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(loadMinimalConfig("")) {
                    get { firstBoot }.isEmpty()
                }
            }

            @Test
            fun `should deserialize empty scripts`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(loadMinimalConfig("""
                    first-boot = [ ]
                """.trimIndent())) {
                    get { firstBoot }.isEmpty()
                }
            }
        }
    }

    @Nested
    inner class FullConfig {

        private fun loadFullConfig(): CustomizationConfig =
            CustomizationConfig.load("sample-full.conf".asPath())

        @TestFactory
        fun `should deserialize`() = test(loadFullConfig()) {
            expecting { trace } that { isTrue() }
            expecting { name } that { isEqualTo("sample-full") }
            expecting { os } that { isEqualTo(RaspberryPiLite) }
            expecting { timeZone } that { isEqualTo(TimeZone.getTimeZone("Europe/Berlin")) }
            expecting { hostname } that { isEqualTo(Hostname("sample-full", true)) }
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
                    FileOperation(null, ImgCstmzr.WorkingDirectory / "src" / "test" / "resources" / "sample.png", LinuxRoot.home / "john.doe" / "image.png"),
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
                        content = "echo '👏 🤓 👋'>>${'$'}HOME/first-boot.txt"
                    ),
                )
            }
        }

        @Test
        fun `should create patch`(osImage: OperatingSystemImage) {
            val config = loadFullConfig()
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
            val config = loadFullConfig()
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
}

package com.bkahlert.kustomize.cli

import com.bkahlert.kommons.io.path.asPath
import com.bkahlert.kommons.io.path.writeText
import com.bkahlert.kommons.junit.UniqueId
import com.bkahlert.kommons.net.div
import com.bkahlert.kommons.net.ip4Of
import com.bkahlert.kommons.shell.ShellScript
import com.bkahlert.kommons.test.SystemProperties
import com.bkahlert.kommons.test.SystemProperty
import com.bkahlert.kommons.test.hasElements
import com.bkahlert.kommons.test.test
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.text.ansiRemoved
import com.bkahlert.kommons.unit.Gibi
import com.bkahlert.kommons.unit.bytes
import com.bkahlert.kustomize.Kustomize
import com.bkahlert.kustomize.cli.CustomizationConfig.BluetoothProfile.PersonalAreaNetwork
import com.bkahlert.kustomize.cli.CustomizationConfig.DefaultUser
import com.bkahlert.kustomize.cli.CustomizationConfig.FileOperation
import com.bkahlert.kustomize.cli.CustomizationConfig.Hostname
import com.bkahlert.kustomize.cli.CustomizationConfig.Samba
import com.bkahlert.kustomize.cli.CustomizationConfig.UsbDevice.Gadget
import com.bkahlert.kustomize.cli.CustomizationConfig.Wifi
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.OperatingSystems.RaspberryPiLite
import com.bkahlert.kustomize.os.OperatingSystems.RiscTestOS
import com.bkahlert.kustomize.patch.AppendToFilesPatch
import com.bkahlert.kustomize.patch.CompositePatch
import com.bkahlert.kustomize.patch.CopyFilesPatch
import com.bkahlert.kustomize.patch.FirstBootPatch
import com.bkahlert.kustomize.patch.HostnamePatch
import com.bkahlert.kustomize.patch.PasswordPatch
import com.bkahlert.kustomize.patch.ResizePatch
import com.bkahlert.kustomize.patch.RootShare.`read-write`
import com.bkahlert.kustomize.patch.SambaPatch
import com.bkahlert.kustomize.patch.ShellScriptPatch
import com.bkahlert.kustomize.patch.SshAuthorizationPatch
import com.bkahlert.kustomize.patch.SshEnablementPatch
import com.bkahlert.kustomize.patch.SshPortPatch
import com.bkahlert.kustomize.patch.TimeZonePatch
import com.bkahlert.kustomize.patch.TweaksPatch
import com.bkahlert.kustomize.patch.UsbGadgetPatch
import com.bkahlert.kustomize.patch.UsernamePatch
import com.bkahlert.kustomize.patch.WifiAutoReconnectPatch
import com.bkahlert.kustomize.patch.WifiPowerSafeModePatch
import com.bkahlert.kustomize.patch.WpaSupplicantPatch
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import strikt.assertions.message
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
                    get { hostname?.name }.isEqualTo("test")
                }
            }

            @Test
            fun `should deserialize no name`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(loadMinimalConfig("")) {
                    get { hostname?.name }.isEqualTo(null)
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
                    get { hostname?.randomSuffix }.isEqualTo(true)
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
                    get { hostname?.randomSuffix }.isEqualTo(false)
                }
            }

            @Test
            fun `should deserialize pretty name`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(loadMinimalConfig("""
                    hostname {
                        name: "test"
                        pretty-name: "pretty name"
                    }
                """.trimIndent())) {
                    get { hostname?.prettyName }.isEqualTo("pretty name")
                }
            }

            @Test
            fun `should deserialize icon name`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(loadMinimalConfig("""
                    hostname {
                        name: "test"
                        icon-name: "computer-vm"
                    }
                """.trimIndent())) {
                    get { hostname?.iconName }.isEqualTo("computer-vm")
                }
            }

            @Test
            fun `should deserialize chassis`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(loadMinimalConfig("""
                    hostname {
                        name: "test"
                        chassis: "vm"
                    }
                """.trimIndent())) {
                    get { hostname?.chassis }.isEqualTo("vm")
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
            expecting { hostname } that { isEqualTo(Hostname("sample-full", true, "Pretty Name", "computer-vm", "vm")) }
            expecting { wifi } that { isEqualTo(Wifi("entry1\nentry2", autoReconnect = true, powerSafeMode = false)) }
            expecting { size } that { isEqualTo(2.Gibi.bytes) }
            with { ssh!! }.then {
                expecting { enabled } that { isTrue() }
                expecting { port } that { isEqualTo(1234) }
                @Suppress("SpellCheckingInspection")
                expecting { authorizedKeys } that {
                    containsExactlyInAnyOrder(
                        "ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBHs0pX2VqQlctO4TXDlkklFFdLKJ1R5c1rMpq84UxyIzNgdkiSjgckn9WvIwmynsybFuM4jjfOtJQnSnsr8k1Ug= \"John Doe\"",
                        "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAgQCGmJI38wiF3NGE3hfTdgxkrfb6WbiWw3hpUudhhrlIWApmvk08zTfeQbAxkF/hUgPd2zS3UsX+c1YOIH2Pja3JGzxRWdJIMB2S1uV3xMfqmUcfRQlQqPsJB8W9UpSbnkYgCsU//BX73G0fAIQArNcVTyAlsFFKQN87A9E8MhdKRQ== \"John Doe\"",
                    )
                }
            }
            expecting { defaultUser } that { isEqualTo(DefaultUser(null, "john.doe", "Password1234")) }
            expecting { samba } that { isEqualTo(Samba(true, `read-write`)) }
            expecting { usbDevices } that {
                containsExactly(Gadget(
                    dhcpRange = ip4Of("10.10.1.0") / 27,
                    deviceAddress = ip4Of("10.10.1.10"),
                    hostAsDefaultGateway = true,
                    enableSerialConsole = true,
                ))
            }
            expecting { bluetoothProfiles } that {
                containsExactly(PersonalAreaNetwork(
                    dhcpRange = ip4Of("10.10.2.0") / 27,
                    deviceAddress = ip4Of("10.10.2.10"),
                ))
            }
            expecting { tweaks?.aptRetries } that { isEqualTo(10) }
            expecting { files } that {
                isEqualTo(listOf(
                    FileOperation("line 1\nline 2", null, LinuxRoot.boot / "file-of-lines.txt"),
                    FileOperation(null,
                        Kustomize.work / "src" / "test" / "resources" / "sample.png",
                        LinuxRoot.home / "john.doe" / "image.png"),
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
                get { name }.contains("Increase Disk Space to 2 GiB").contains("Change Username")
                get { diskOperations }.isNotEmpty()
                get { virtCustomizations }.isNotEmpty()
                get { guestfishCommands }.isNotEmpty()
                get { osBoot }.isTrue()
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
                            ResizePatch::class,
                            UsernamePatch::class,
                            SshEnablementPatch::class,
                            WpaSupplicantPatch::class,
                        )
                },
                {
                    isA<CompositePatch>().get { this.patches.map { it::class } }
                        .contains(
                            CopyFilesPatch::class,
                            AppendToFilesPatch::class,
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
                            UsbGadgetPatch::class,
                            ShellScriptPatch::class,
                        )
                },
                { isA<FirstBootPatch>() },
            )
        }
    }
}

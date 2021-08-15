package com.bkahlert.kustomize.cli

import koodies.io.path.writeText
import koodies.io.randomFile
import koodies.junit.UniqueId
import koodies.test.withTempDir
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.get
import strikt.assertions.hasEntry
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotEqualTo
import java.nio.file.Path

class EnvTest {

    @Test
    fun `should read env file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val path: Path = randomFile("personal", ".env").apply { writeText("A=B\n CC= DD \n") }
        expectThat(Env(path))
            .hasEntry("A", "B")
            .hasEntry("CC", "DD")
    }

    @Test
    fun `should read multi-line variable`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val path: Path = randomFile("multi-line", ".env").apply {
            writeText("""
                FOO="foo"
                WPA_SUPPLICANT=""${'"'}ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev
                update_config=1
                country=DE
               
                network={
                  ssid="ssid1"
                  psk="psk1"
                  id_str="id_str1"
                }
               
                network={
                  ssid="ssid2"
                  psk="psk2"
                  id_str="id_str2"
                }
                ""${'"'}
                BAR=bar
            """.trimIndent())
        }
        expectThat(Env(path))
            .hasEntry("FOO", "foo")
            .hasEntry("WPA_SUPPLICANT", """
                ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev
                update_config=1
                country=DE

                network={
                  ssid="ssid1"
                  psk="psk1"
                  id_str="id_str1"
                }

                network={
                  ssid="ssid2"
                  psk="psk2"
                  id_str="id_str2"
                }
                
            """.trimIndent())
            .hasEntry("BAR", "bar")
    }

    @Test
    fun `should favor environment variables`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val path: Path = randomFile("personal", ".env").apply { writeText("JAVA_HOME=dummy\n") }
        expectThat(Env(path))["JAVA_HOME"].isNotEqualTo("dummy")
    }

    @Test
    fun `should still provide access to system env missing env file`() {
        expectThat(Env(Path.of("/nowhere"))).isNotEmpty()
    }
}

package com.imgcstmzr.patch

import koodies.shell.ShellScript
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.first
import strikt.assertions.isEqualTo

class AptTest {

    @Nested
    inner class Install {

        @Test
        fun `should add command`() {
            expectThat(ShellScript {
                apt install "dnsmasq"
            }.lines).first().isEqualTo("apt-get install -y dnsmasq")
        }
    }
}

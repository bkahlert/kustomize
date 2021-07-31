package com.bkahlert.kustomize.patch

import koodies.shell.ShellScript
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.first
import strikt.assertions.isEqualTo

class ShellScriptExtensionsKtTest {

    @Nested
    inner class Install {

        @Test
        fun `should add command`() {
            expectThat(ShellScript { aptGet install "dnsmasq" }).first().isEqualTo("'apt-get' '-qq' 'install' 'dnsmasq'")
        }
    }

    @Nested
    inner class List {

        @Test
        fun `should add command`() {
            expectThat(ShellScript { apt list "--installed" }).first().isEqualTo("'apt' 'list' '--installed'")
        }
    }
}

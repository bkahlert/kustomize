package com.bkahlert.kustomize.os.linux

import koodies.shell.ShellScript
import koodies.test.toStringIsEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat

class ServiceScriptTest {

    private val serviceScript = ServiceScript("test.sh", ShellScript())

    @Nested
    inner class DiskFile {

        @Test
        fun `should be located in etc`() {
            expectThat(serviceScript.diskFile).toStringIsEqualTo("/etc/systemd/scripts/test.sh")
        }
    }
}

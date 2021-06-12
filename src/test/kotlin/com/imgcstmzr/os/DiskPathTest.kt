package com.imgcstmzr.os

import koodies.test.expecting
import koodies.test.tests
import koodies.test.toStringIsEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class DiskPathTest {

    @Test
    fun `should have LinuxPaths`() {
        expecting { LinuxRoot } that { toStringIsEqualTo("/") }
    }

    @TestFactory
    fun `should have top level directories`() = tests {
        expecting { LinuxRoot.boot } that { toStringIsEqualTo("/boot") }
        expecting { LinuxRoot.etc } that { toStringIsEqualTo("/etc") }
        expecting { LinuxRoot.lib } that { toStringIsEqualTo("/lib") }
        expecting { LinuxRoot.home } that { toStringIsEqualTo("/home") }
        expecting { LinuxRoot.root } that { toStringIsEqualTo("/root") }
        expecting { LinuxRoot.usr } that { toStringIsEqualTo("/usr") }
    }

    @Test
    fun `should have nested directory`() {
        expecting { LinuxRoot.etc.systemd.system } that { toStringIsEqualTo("/etc/systemd/system") }
    }

    @Test
    fun `should have nested file`() {
        expecting { LinuxRoot.etc.passwd } that { toStringIsEqualTo("/etc/passwd") }
    }

    @Test
    fun `should resolve`() {
        expecting { LinuxRoot.etc.systemd / "test" } that { toStringIsEqualTo("/etc/systemd/test") }
    }
}

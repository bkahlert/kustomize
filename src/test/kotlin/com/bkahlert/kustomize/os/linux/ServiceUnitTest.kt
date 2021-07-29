package com.bkahlert.kustomize.os.linux

import koodies.test.testEach
import koodies.test.tests
import koodies.test.toStringIsEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class ServiceUnitTest {

    private val serviceUnit = ServiceUnit("test.service", """
        [Unit]
        Description=libguestfs firstboot service completion
        
        [Service]
        Type=oneshot
        ExecStart=echo 'test'
    """)

    @Nested
    inner class Text {

        @Test
        fun `should indent`() {
            expectThat(serviceUnit.text).isEqualTo("""
                [Unit]
                Description=libguestfs firstboot service completion
                
                [Service]
                Type=oneshot
                ExecStart=echo 'test'
            """.trimIndent())
        }
    }

    @Nested
    inner class DiskFile {

        @Test
        fun `should be located in etc`() {
            expectThat(serviceUnit.diskFile).toStringIsEqualTo("/etc/systemd/system/test.service")
        }
    }

    @Nested
    inner class GetSection {

        @TestFactory
        fun `should return section if exists`() = tests {
            expecting { serviceUnit["Unit"] } that { isEqualTo("Description=libguestfs firstboot service completion") }
            expecting { serviceUnit["Service"] } that { isEqualTo("Type=oneshot\nExecStart=echo 'test'") }
        }

        @TestFactory
        fun `should return null if section is missing`() = tests {
            expecting { serviceUnit["unit"] } that { isNull() }
            expecting { serviceUnit["Other"] } that { isNull() }
        }
    }

    @Nested
    inner class WantedBy {

        @TestFactory
        fun `should return no dependencies`() = testEach(
            ServiceUnit("test.service", """
                [Unit]
                Description=libguestfs firstboot service completion
                
                [Service]
                Type=oneshot
                ExecStart=echo 'test'
            """.trimIndent()),
            ServiceUnit("test.service", """
                [Unit]
                Description=libguestfs firstboot service completion
                
                [Service]
                Type=oneshot
                ExecStart=echo 'test'
                
                [Install]
            """.trimIndent()),
        ) { serviceUnit ->
            expecting { serviceUnit.wantedBy } that { isEmpty() }
        }

        @Test
        fun `should return one dependency`() {
            val serviceUnit = ServiceUnit("test.service", """
                [Unit]
                Description=libguestfs firstboot service completion
                
                [Service]
                Type=oneshot
                ExecStart=echo 'test'
                
                [Install]
                WantedBy=multi-user.target
            """.trimIndent())
            expectThat(serviceUnit.wantedBy).containsExactly("multi-user.target")
        }

        @Test
        fun `should return multiple dependencies`() {
            val serviceUnit = ServiceUnit("test.service", """
                [Unit]
                Description=libguestfs firstboot service completion
                
                [Service]
                Type=oneshot
                ExecStart=echo 'test'
                
                [Install]
                WantedBy=multi-user.target
                WantedBy=display-manager.service
            """.trimIndent())
            expectThat(serviceUnit.wantedBy).containsExactly("multi-user.target", "display-manager.service")
        }

        @Test
        fun `should only consider Install section`() {
            @Suppress("UnknownKeyInSection")
            val serviceUnit = ServiceUnit("test.service", """
                [Install]
                WantedBy=display-manager.service
                
                [Other]
                WantedBy=multi-user.target
            """.trimIndent())
            expectThat(serviceUnit.wantedBy).containsExactly("display-manager.service")
        }

        @Test
        fun `should handle whitespaces`() {
            @Suppress("UnknownKeyInSection")
            val serviceUnit = ServiceUnit("test.service", """
                [Install]
                WantedBy =  display-manager.service 
            """.trimIndent())
            expectThat(serviceUnit.wantedBy).containsExactly("display-manager.service")
        }
    }
}

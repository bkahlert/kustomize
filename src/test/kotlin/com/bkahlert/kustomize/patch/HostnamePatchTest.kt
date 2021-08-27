package com.bkahlert.kustomize.patch

import com.bkahlert.kommons.io.path.textContent
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.FirstBootOption
import com.bkahlert.kustomize.libguestfs.file
import com.bkahlert.kustomize.os.OperatingSystemImage
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.filterIsInstance
import strikt.assertions.first
import strikt.assertions.matches
import strikt.assertions.startsWith

class HostnamePatchTest {

    private val hostnameRegex = Regex("test-machine--[0-9a-zA-Z]{4}")

    @Test
    fun `should provide hostname changing command`(osImage: OperatingSystemImage) {
        val patch = HostnamePatch("test-machine", true).invoke(osImage)
        expectThat(patch).matches(virtCustomizationsAssertion = {
            first().get { get(1) }.matches(hostnameRegex).startsWith("test-machine")
            filterIsInstance<FirstBootOption>().any {
                file.textContent.contains("'hostnamectl' 'set-hostname' 'test-machine--")
            }
        })
    }

    @Test
    fun `should provide pretty-name changing command`(osImage: OperatingSystemImage) {
        val patch = HostnamePatch("test-machine", true, prettyName = "Test Machine").invoke(osImage)
        expectThat(patch).matches(virtCustomizationsAssertion = {
            filterIsInstance<FirstBootOption>().any {
                file.textContent.contains("'hostnamectl' 'set-hostname' '--pretty' 'Test Machine — ")
            }
        })
    }

    @Test
    fun `should provide icon-name changing command`(osImage: OperatingSystemImage) {
        val patch = HostnamePatch("test-machine", true, iconName = "computer-vm").invoke(osImage)
        expectThat(patch).matches(virtCustomizationsAssertion = {
            filterIsInstance<FirstBootOption>().any {
                file.textContent.contains("'hostnamectl' 'set-icon-name' 'computer-vm'")
            }
        })
    }

    @Test
    fun `should provide chassis changing command`(osImage: OperatingSystemImage) {
        val patch = HostnamePatch("test-machine", true, chassis = "vm").invoke(osImage)
        expectThat(patch).matches(virtCustomizationsAssertion = {
            filterIsInstance<FirstBootOption>().any {
                file.textContent.contains("'hostnamectl' 'set-chassis' 'vm'")
            }
        })
    }
}

package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.os.OperatingSystemImage
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.first
import strikt.assertions.matches

class HostnamePatchTest {

    private val hostnameRegex = Regex("test-machine--[0-9a-zA-Z]{4}")

    @Test
    fun `should provide hostname changing command`(osImage: OperatingSystemImage) {
        val patch = HostnamePatch("test-machine", true).invoke(osImage)
        expectThat(patch).matches(virtCustomizationsAssertion = {
            first().get { get(1) }.matches(hostnameRegex)
        })
    }
}

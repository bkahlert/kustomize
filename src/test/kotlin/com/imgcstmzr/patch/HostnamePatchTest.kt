package com.imgcstmzr.patch

import com.imgcstmzr.runtime.OperatingSystemImage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.first
import strikt.assertions.matches

@Execution(CONCURRENT)
class HostnamePatchTest {

    @Test
    fun `should provide hostname changing command`(osImage: OperatingSystemImage) {
        val patch = HostnamePatch("test-machine", true)
        expectThat(patch).matches(customizationOptionsAssertion = {
            first().get { invoke(osImage) }.get { get(1) }.matches(Regex("test-machine-[0-9a-zA-Z]{4}"))
        })
    }
}

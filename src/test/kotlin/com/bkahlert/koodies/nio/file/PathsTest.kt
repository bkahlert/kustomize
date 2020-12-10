package com.bkahlert.koodies.nio.file

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isTrue

@Execution(CONCURRENT)
class PathsTest {

    @EnabledOnOs(OS.LINUX, OS.MAC)
    @Test
    fun `should test cloneFile support positive`() {
        expectThat(Paths.clonefileSupport).isTrue()
    }
}

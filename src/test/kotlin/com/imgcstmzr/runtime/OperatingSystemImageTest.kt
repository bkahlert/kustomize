package com.imgcstmzr.runtime

import com.imgcstmzr.runtime.OperatingSystemImage.Companion.based
import com.imgcstmzr.util.Paths
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path

@Execution(CONCURRENT)
class OperatingSystemImageTest {

    @Test
    fun `should have correct absolute path`() {
        expectThat((OperatingSystemMock("abs-path-test") based Path.of("/foo/bar")).path).isEqualTo("/foo/bar")
    }

    @Test
    fun `should have correct relative path`() {
        expectThat((OperatingSystemMock("rel-path-test") based Path.of("foo/bar")).path).isEqualTo("foo/bar")
    }

    @Test
    fun `should have full name`() {
        expectThat((OperatingSystemMock("full-name-test") based Path.of("foo/bar")).fullName)
            .isEqualTo("ImgCstmzr Test OS at file://${Paths.WORKING_DIRECTORY}/foo/bar")
    }

    @Test
    fun `should have short name`() {
        expectThat((OperatingSystemMock("short-name-test") based Path.of("foo/bar")).shortName)
            .isEqualTo("ImgCstmzr Test OS at bar")
    }
}

package com.imgcstmzr.runtime

import com.bkahlert.koodies.docker.Docker
import com.bkahlert.koodies.nio.file.conditioned
import com.imgcstmzr.runtime.OperatingSystemImage.Companion.based
import com.imgcstmzr.util.DockerRequired
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import java.nio.file.Path

@Execution(CONCURRENT)
class OperatingSystemImageTest {

    @Test
    fun `should have correct absolute path`() {
        expectThat((OperatingSystemMock() based Path.of("/foo/bar")).path).isEqualTo("/foo/bar")
    }

    @Test
    fun `should have correct relative path`() {
        expectThat((OperatingSystemMock() based Path.of("foo/bar")).path).isEqualTo("foo/bar")
    }

    @Test
    fun `should have full name`() {
        expectThat((OperatingSystemMock() based Path.of("foo/bar")).fullName)
            .isEqualTo("ImgCstmzr Test OS at file://${Paths.WORKING_DIRECTORY}/foo/bar")
    }

    @Test
    fun `should have short name`() {
        expectThat((OperatingSystemMock() based Path.of("foo/bar")).shortName)
            .isEqualTo("ImgCstmzr Test OS at bar")
    }

    @DockerRequired
    @Test
    fun `should boot using extension function`(@OS(OperatingSystems.TinyCore::class) osImage: OperatingSystemImage, logger: InMemoryLogger<Any>) {

        val exitCode = osImage.boot(logger)

        expectThat(exitCode).isEqualTo(0)
        expectThat(logger.logged).contains("mounted filesystem with ordered data mode")
        expectThat(!Docker.exists(osImage.conditioned))
    }
}

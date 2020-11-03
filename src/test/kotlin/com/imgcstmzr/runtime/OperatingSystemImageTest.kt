package com.imgcstmzr.runtime

import com.bkahlert.koodies.docker.Docker
import com.bkahlert.koodies.nio.file.conditioned
import com.imgcstmzr.util.DockerRequired
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
internal class OperatingSystemImageTest {

    @DockerRequired
    @Test
    fun `should boot using extension function`(@OS(OperatingSystems.TinyCore::class) osImage: OperatingSystemImage, logger: InMemoryLogger<Any>) {

        val exitCode = osImage.boot(logger)

        expectThat(exitCode).isEqualTo(0)
        expectThat(logger.logged).contains("mounted filesystem with ordered data mode")
        expectThat(!Docker.exists(osImage.conditioned))
    }
}

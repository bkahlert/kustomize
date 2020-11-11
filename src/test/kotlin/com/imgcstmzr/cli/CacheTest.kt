package com.imgcstmzr.cli

import com.imgcstmzr.util.exists
import com.imgcstmzr.util.hasContent
import com.imgcstmzr.util.isDirectory
import com.imgcstmzr.util.isInside
import com.imgcstmzr.util.isWritable
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

@Execution(ExecutionMode.CONCURRENT)
class CacheTest {
    @Test
    fun `should instantiate in user home by default`() {
        val cache = Cache()

        expectThat(cache)
            .get { dir.toFile() }
            .exists()
            .isDirectory()
            .isWritable()
    }

    @Test
    fun `should instantiate in provided directory`() {
        val tempDir = createTempDir().toPath()

        val cache = Cache(tempDir)

        expectThat(cache.dir).isEqualTo(tempDir)
    }

    @Test
    fun `should provide a retrieved copy`(logger: InMemoryLogger<*>) {
        val cache = Cache(createTempDir().toPath())
        val path = prepareTestImgZip()

        val copy = cache.provideCopy("my-copy", logger = logger) {
            path
        }

        expectThat(copy)
            .hasContent("funny content")
            .isInside(cache.dir)
    }

    @Test
    fun `should only retrieve copy once`(logger: InMemoryLogger<*>) {
        val cache = Cache(createTempDir().toPath())
        val path = prepareTestImgZip()
        var providerCalls = 0
        val provider = {
            providerCalls++
            path
        }

        val copies = (0..2).map {
            cache.provideCopy("my-copy", false, logger, provider)
        }

        expectThat(providerCalls).isEqualTo(1)
        copies.forEach { copy ->
            expectThat(copy)
                .hasContent("funny content")
                .isInside(cache.dir)
        }
    }

    private fun prepareTestImgZip(): Path {
        val path = File.createTempFile("imgcstmzr", ".zip").toPath()
        val bytes = javaClass.getResource("/funny.img.zip").openStream().readAllBytes()
        Files.write(path, bytes)
        return path
    }
}

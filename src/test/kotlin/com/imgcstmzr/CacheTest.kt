package com.imgcstmzr

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.File
import java.nio.file.Files

@Execution(ExecutionMode.CONCURRENT)
internal class CacheTest {
    @Test
    internal fun `should instantiate in user home by default`() {
        val cache = Cache()

        expectThat(cache)
            .get { dir }
            .exists()
            .isDirectory()
            .isWritable()
    }

    @Test
    internal fun `should instantiate in provided directory`() {
        val tempDir = createTempDir()

        val cache = Cache(tempDir)

        expectThat(cache.dir).isEqualTo(tempDir)
    }

    @Test
    internal fun `should provide a retrieved copy`() {
        val cache = Cache(createTempDir())
        val file = File.createTempFile("imgcstmzr", ".zip")
        val bytes = javaClass.getResource("/funny.img.zip").openStream().readAllBytes()
        Files.write(file.toPath(), bytes)

        val copy = cache.provideCopy("my-copy") { file }

        expectThat(copy)
            .hasContent("funny content")
            .isInside(cache.dir)
    }

    @Test
    internal fun `should only retrieve copy once`() {
        val cache = Cache(createTempDir())
        val file = File.createTempFile("imgcstmzr", ".zip")
        val bytes = javaClass.getResource("/funny.img.zip").openStream().readAllBytes()
        Files.write(file.toPath(), bytes)
        var providerCalls = 0
        val provider = {
            providerCalls++
            file
        }

        val copies = (0..2).map {
            cache.provideCopy("my-copy", provider)
        }

        expectThat(providerCalls).isEqualTo(1)
        copies.forEach { copy ->
            expectThat(copy)
                .hasContent("funny content")
                .isInside(cache.dir)
        }
    }
}


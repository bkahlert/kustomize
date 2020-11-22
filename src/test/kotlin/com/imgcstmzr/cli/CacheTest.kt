package com.imgcstmzr.cli

import com.bkahlert.koodies.nio.file.isInside
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.spyable
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.MiscFixture.FunnyImgZip
import com.imgcstmzr.util.exists
import com.imgcstmzr.util.hasContent
import com.imgcstmzr.util.isDirectory
import com.imgcstmzr.util.isWritable
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class CacheTest {

    private val tempDir = tempDir().deleteOnExit()

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
        val tempDir = tempDir.tempDir()

        val cache = Cache(tempDir)

        expectThat(cache.dir).isEqualTo(tempDir)
    }

    @Test
    fun `should provide a retrieved copy`(logger: InMemoryLogger<*>) {
        val cache = Cache(tempDir.tempDir())

        val copy = cache.provideCopy("my-copy", logger = logger) {
            FunnyImgZip.copyToTemp()
        }

        expectThat(copy)
            .hasContent("funny content")
            .isInside(cache.dir)
    }

    @Test
    fun `should only retrieve copy once`(logger: InMemoryLogger<*>) {
        val cache = Cache(tempDir.tempDir())
        var providerCalls = 0
        val provider by spyable(FunnyImgZip::copyToTemp) { providerCalls++ }

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
}

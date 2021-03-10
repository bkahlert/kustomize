package com.imgcstmzr.cli

import com.imgcstmzr.test.MiscClassPathFixture.FunnyImgZip
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.test.exists
import com.imgcstmzr.test.hasContent
import com.imgcstmzr.test.isDirectory
import com.imgcstmzr.withTempDir
import koodies.io.path.isInside
import koodies.io.path.randomDirectory
import koodies.logging.InMemoryLogger
import koodies.test.Fixtures.copyToDirectory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

@Execution(CONCURRENT)
class CacheTest {

    @Test
    fun `should instantiate in user home by default`() {
        val cache = Cache()

        expectThat(cache.dir) {
            exists()
            isDirectory()
        }
    }

    @Test
    fun `should instantiate in provided directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val tempDir = randomDirectory()

        val cache = Cache(tempDir)

        expectThat(cache.dir).isEqualTo(tempDir)
    }

    @Test
    fun InMemoryLogger.`should provide a retrieved copy`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val cache = Cache(randomDirectory())

        val copy = with(cache) {
            provideCopy("my-copy") {
                FunnyImgZip.copyToDirectory(this@withTempDir)
            }
        }

        expectThat(copy)
            .hasContent("test.img content\n")
            .get { isInside(cache.dir) }.isTrue()
    }

    @Test
    fun InMemoryLogger.`should only retrieve a copy once`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val cache = Cache(randomDirectory())
        var providerCalls = 0
        val provider = {
            providerCalls++
            FunnyImgZip.copyToDirectory(this)
        }

        val copies = (0..2).map {
            with(cache) {
                provideCopy("my-copy", false, provider)
            }
        }

        expectThat(providerCalls).isEqualTo(1)
        copies.forEach { copy ->
            expectThat(copy)
                .hasContent("test.img content\n")
                .get { isInside(cache.dir) }.isTrue()
        }
    }
}

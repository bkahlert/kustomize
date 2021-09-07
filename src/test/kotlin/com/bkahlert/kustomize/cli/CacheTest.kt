package com.bkahlert.kustomize.cli

import com.bkahlert.kommons.io.path.age
import com.bkahlert.kommons.io.path.asPath
import com.bkahlert.kommons.io.path.deleteRecursively
import com.bkahlert.kommons.io.path.forEachDirectoryEntryRecursively
import com.bkahlert.kommons.io.path.hasContent
import com.bkahlert.kommons.io.path.isInside
import com.bkahlert.kommons.io.path.listDirectoryEntriesRecursively
import com.bkahlert.kommons.io.path.randomDirectory
import com.bkahlert.kommons.junit.UniqueId
import com.bkahlert.kommons.runtime.ClassPathFile
import com.bkahlert.kommons.test.expectCatching
import com.bkahlert.kommons.test.single
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.time.days
import com.bkahlert.kustomize.Kustomize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.Assertion.Builder
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isSuccess
import strikt.assertions.isTrue
import strikt.java.exists
import strikt.java.fileName
import strikt.java.resolve
import java.nio.file.Path

class CacheTest {

    @Test
    fun `should instantiate in provided directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val cache = Cache(this)
        expectThat(cache.dir).isEqualTo(normalize())
    }

    @Nested
    inner class WithRelativePath {

        private val relativePath = ".cache.test".asPath()

        @Test
        fun `should instantiate relative to working directory`() {
            val cache = Cache(relativePath)
            expectThat(cache.dir).isEqualTo(Kustomize.work.resolve(relativePath))
        }

        @AfterEach
        fun cleanup() {
            Kustomize.work.resolve(relativePath).deleteRecursively()
        }
    }

    @Test
    fun `should provide a retrieved copy`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val cache = Cache(randomDirectory())

        val copy = cache.provideCopy("project") {
            ClassPathFile("riscos.img.zip").copyToTemp()
        }

        expectThat(copy)
            .hasContent(ClassPathFile("riscos.img").data)
            .get { isInside(cache.dir) }.isTrue()
    }

    @Test
    fun `should re-use existing copy`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val cache = Cache(randomDirectory()).apply {
            provideCopy("project") { ClassPathFile("riscos.img.zip").copyToTemp() }
        }

        expectCatching {
            cache.provideCopy("project") { throw IllegalStateException("existing copy ignored") }
        } that {
            isSuccess()
        }
    }

    @Test
    fun `should re-retrieve copy if too old`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val cacheDir = randomDirectory()
        val cache = Cache(cacheDir).apply {
            provideCopy("project") {
                ClassPathFile("riscos.img.zip").copyToTemp()
            }
        }
        cacheDir.forEachDirectoryEntryRecursively { it.age = 31.days }

        var retrieved = false
        cache.provideCopy("project") {
            ClassPathFile("riscos.img.zip").copyToTemp().also { retrieved = true }
        }

        expectThat(retrieved).isTrue()
    }
}

fun Builder<Path>.containsImage(name: String): Builder<Path> =
    exists() and {
        resolve("raw").exists() and {
            get { listDirectoryEntriesRecursively() }
                .hasSize(1)
                .single { fileName.isEqualTo(name.asPath()) }
        }
    }

package com.bkahlert.kustomize.cli

import koodies.io.ClassPathFile
import koodies.io.path.asPath
import koodies.io.path.deleteRecursively
import koodies.io.path.hasContent
import koodies.io.path.isInside
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.io.randomDirectory
import koodies.junit.UniqueId
import koodies.test.single
import koodies.test.withTempDir
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.Assertion.Builder
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
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
            expectThat(cache.dir).isEqualTo(com.bkahlert.kustomize.Kustomize.WorkingDirectory.resolve(relativePath))
        }

        @AfterEach
        fun cleanup() {
            com.bkahlert.kustomize.Kustomize.WorkingDirectory.resolve(relativePath).deleteRecursively()
        }
    }

    @Test
    fun `should provide a retrieved copy`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val cache = Cache(randomDirectory())

        val copy = cache.provideCopy("my-copy") {
            ClassPathFile("riscos.img.zip").copyToTemp()
        }

        expectThat(copy)
            .hasContent(ClassPathFile("riscos.img").data)
            .get { isInside(cache.dir) }.isTrue()
    }

    @Test
    fun `should only retrieve a copy once`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val cache = Cache(randomDirectory())
        var providerCalls = 0
        val provider = {
            providerCalls++
            ClassPathFile("riscos.img.zip").copyToDirectory(this)
        }

        val copies = (0..2).map {
            with(cache) {
                provideCopy("my-copy", false, provider)
            }
        }

        expectThat(providerCalls).isEqualTo(1)
        copies.forEach { copy ->
            expectThat(copy)
                .hasContent(ClassPathFile("riscos.img").data)
                .get { isInside(cache.dir) }.isTrue()
        }
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

package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.number.size
import com.bkahlert.koodies.test.FixturePath61C285F09D95930D0AE298B00AF09F918B0A.fixtureContent
import com.bkahlert.koodies.test.FixturePath61C285F09D95930D0AE298B00AF09F918B0A.fixtureFileName
import com.bkahlert.koodies.unit.Size.Companion.bytes
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.readAllBytes
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThan
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.ReadOnlyFileSystemException

@Execution(CONCURRENT)
class ClassPathTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should map root with no provided path`() {
        expectThat(classPath("") {
            listRecursively().any { it.fileName.toString().endsWith(".class") }
        }).isTrue()
    }

    @Test
    fun `should map resource on matching path`() {
        expectThat(classPath(fixtureFileName) { readAllBytes() }).isEqualTo(fixtureContent)
    }

    @Test
    fun `should map resource on non-matching path`() {
        expectThat(classPath("invalid.file") { this }).isNull()
    }

    @TestFactory
    fun `should support different notations`() = listOf(
        fixtureFileName,
        "/$fixtureFileName",
        "classpath:$fixtureFileName",
        "classpath:/$fixtureFileName",
        "ClassPath:$fixtureFileName",
        "ClassPath:/$fixtureFileName",
    ).map { dynamicTest(it) { expectThat(classPath(it) { readAllBytes() }).isEqualTo(fixtureContent) } }

    @Test
    fun `should map read-only root`() {
        expectThat(classPath("") { this::class.qualifiedName }).isNotNull().contains("ReadOnly")
    }

    @Test
    fun `should map read-only resource`() {
        expectThat(classPath(fixtureFileName) { this::class.qualifiedName }).isNotNull().contains("ReadOnly")
    }

    @Test
    fun `should list read-only resources`() {
        expectThat(classPath("") { list().mapNotNull { it::class.qualifiedName }.toList() }).isNotNull().all { contains("ReadOnly") }
    }

    @TestFactory
    fun `should throw on write access`() = mapOf<String, (Path) -> Unit>(
        "outputStream" to { Files.newBufferedWriter(it) },
        "move" to { Files.move(it, tempDir.tempPath()) },
        "delete" to { Files.delete(it) },
    ).map { (name, operation) ->
        dynamicTest(name) {
            classPath("try.it") {
                expectThat(exists).isTrue()
                expectCatching { operation(this@classPath) }.isFailure().isA<ReadOnlyFileSystemException>()
                expectThat(exists).isTrue()
                expectThat(readAllBytes()).isEqualTo(fixtureContent)
            }
        }
    }

    @Nested
    inner class UsingDelegatesProperty {
        @Test
        fun `should copy class path directory`() {
            val fixtures by classPath("img")
            classPath("img") {
                fixtures.copyToDirectory(tempDir.tempDir("copy1")) to copyToDirectory(tempDir.tempDir("copy2"))
            }?.also { (copy1, copy2) ->
                expectThat(copy1) {
                    size.isGreaterThan(0.bytes)
                    isCopyOf(fixtures)
                    isCopyOf(copy2)
                }
            }
        }
    }
}


package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.test.FixturePath61C285F09D95930D0AE298B00AF09F918B0A.fixtureContent
import com.bkahlert.koodies.test.FixturePath61C285F09D95930D0AE298B00AF09F918B0A.fixtureFileName
import com.imgcstmzr.util.readAllBytes
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.filter
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThan
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isTrue
import strikt.assertions.size
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.ReadOnlyFileSystemException

@Execution(CONCURRENT)
class ClassPathsTest {

    @Test
    fun `should map root with no provided path`() {
        expectThat(classPaths("") {
            listRecursively().any { it.fileName.toString().endsWith(".class") }
        }).filter { true }.size.isGreaterThan(2)
    }

    @Test
    fun `should map resource on matching path`() {
        expectThat(classPaths(fixtureFileName) { readAllBytes() }).hasSize(2).all { isEqualTo(fixtureContent) }
    }

    @Test
    fun `should map resource on non-matching path`() {
        expectThat(classPaths("invalid.file") { this }).isEmpty()
    }

    @TestFactory
    fun `should support different notations`() = listOf(
        fixtureFileName,
        "/$fixtureFileName",
        "classpath:$fixtureFileName",
        "classpath:/$fixtureFileName",
        "ClassPath:$fixtureFileName",
        "ClassPath:/$fixtureFileName",
    ).map { dynamicTest(it) { expectThat(classPaths(it) { readAllBytes() }).hasSize(2).all { isEqualTo(fixtureContent) } } }

    @Test
    fun `should map read-only root`() {
        expectThat(classPaths("") { this::class.qualifiedName!! }).filter { it.contains("ReadOnly") }.size.isGreaterThan(2)
    }

    @Test
    fun `should map read-only resource`() {
        expectThat(classPaths(fixtureFileName) { this::class.qualifiedName!! }).filter { it.contains("ReadOnly") }.size.isGreaterThanOrEqualTo(2)
    }

    @Test
    fun `should list read-only resources`() {
        expectThat(classPaths("") {
            list().mapNotNull { it::class.qualifiedName!! }.toList()
        }).filter { it.all { pathType -> pathType.contains("ReadOnly") } }.size.isGreaterThanOrEqualTo(2)
    }

    @TestFactory
    fun `should throw on write access`() = mapOf<String, (Path) -> Unit>(
        "outputStream" to { Files.newBufferedWriter(it) },
        "move" to { Files.move(it, tempPath()) },
        "delete" to { Files.delete(it) },
    ).map { (name, operation) ->
        dynamicTest(name) {
            classPaths("try.it") {
                expectThat(exists).isTrue()
                expectCatching { operation(this@classPaths) }.isFailure().isA<ReadOnlyFileSystemException>()
                expectThat(exists).isTrue()
                expectThat(readAllBytes()).isEqualTo(fixtureContent)
            }
        }
    }
}


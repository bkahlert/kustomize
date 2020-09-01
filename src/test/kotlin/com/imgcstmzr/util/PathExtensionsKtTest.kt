package com.imgcstmzr.util

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import java.io.File
import java.io.IOException
import java.nio.file.Path

@Execution(ExecutionMode.CONCURRENT)
@Suppress("RedundantInnerClassModifier")
internal class PathExtensionsKtTest {

    @Nested
    inner class FileNameWithExtension {

        @TestFactory
        @DisplayName("should generate filename.test for")
        internal fun `should generate filename_test for`() = listOf(
            "filename", "filename.test",
            "my/path/filename", "my/path/filename.test",
            "/my/path/filename", "/my/path/filename.test",
        ).flatMap { path ->
            listOf(
                dynamicTest("$path") {
                    val actual = Path.of(path).fileNameWithExtension("test")
                    expectThat(actual).isEqualTo("filename.test")
                }
            )
        }
    }

    @Nested
    inner class Wrap {

        @TestFactory
        @DisplayName("should wrap")
        internal fun `should generate filename_test for`() = listOf(
            "filename" to "@@filename@@",
            "filename.test" to "@@filename.test@@",
            "my/path/filename" to "@@my/path/filename@@",
            "my/path/filename.test" to "@@my/path/filename.test@@",
            "/my/path/filename" to "@@/my/path/filename@@",
            "/my/path/filename.test" to "@@/my/path/filename.test@@",
        ).flatMap { (path, expected) ->
            listOf(
                dynamicTest("$path -> $expected") {
                    val actual = Path.of(path).wrap("@@")
                    expectThat(actual).isEqualTo(expected)
                }
            )
        }
    }

    @Nested
    inner class Quote {

        @TestFactory
        @DisplayName("should quote")
        internal fun `should generate filename_test for`() = listOf(
            "filename" to "\"filename\"",
            "filename.test" to "\"filename.test\"",
            "my/path/filename" to "\"my/path/filename\"",
            "my/path/filename.test" to "\"my/path/filename.test\"",
            "/my/path/filename" to "\"/my/path/filename\"",
            "/my/path/filename.test" to "\"/my/path/filename.test\"",
        ).flatMap { (path, expected) ->
            listOf(
                dynamicTest("$path -> $expected") {
                    val actual = Path.of(path).quote()
                    expectThat(actual).isEqualTo(expected)
                }
            )
        }
    }

    @Nested
    inner class ReadAllBytes {

        val tempFilePrefix = PathExtensionsKtTest::class.simpleName!!
        val tempFile = File.createTempFile(tempFilePrefix, ".txt").also { it.writeBytes(ByteArray(10)); it.deleteOnExit() }

        @TestFactory
        internal fun `should read bytes`() = mapOf(
            tempFile.toPath() to 10,
            ClassPath.of("classpath:funny.img.zip") to 540,
            ClassPath.of("classpath:/cmdline.txt") to 169,
        ).flatMap { (path, expectedSize) ->
            listOf(
                dynamicTest("$expectedSize <- $path") {
                    val actual = path.readAllBytes()
                    expectThat(actual).get { size }.isEqualTo(expectedSize)
                }
            )
        }

        @Test
        internal fun `should throw on missing file`() {
            val deletedFile = File.createTempFile(tempFilePrefix, ".txt").also { it.delete() }.toPath()
            expectCatching { deletedFile.readAllBytes() }.isFailure().isA<IOException>()
        }
    }

    @Nested
    inner class Copy {

        val tempFilePrefix = PathExtensionsKtTest::class.simpleName!!
        val tempFile = File.createTempFile(tempFilePrefix, ".txt").also { it.writeBytes(ByteArray(10)); it.deleteOnExit() }.toPath()

        @Test
        internal fun `should copy file if destination not exists`() {
            val deletedFile = File.createTempFile(tempFilePrefix, ".txt").also { it.delete() }.toPath()
            tempFile.copy(deletedFile)
            expectThat(deletedFile).get { readAllBytes().size }.isEqualTo(10)
        }

        @Test
        internal fun `should throw on missing file`() {
            val deletedFile = File.createTempFile(tempFilePrefix, ".txt").also { it.delete() }.toPath()
            expectCatching { deletedFile.copy(tempFile) }.isFailure().isA<IOException>()
        }
    }
}

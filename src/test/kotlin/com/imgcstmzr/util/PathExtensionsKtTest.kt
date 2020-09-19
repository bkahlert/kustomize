package com.imgcstmzr.util

import com.bkahlert.koodies.nio.ClassPath
import com.bkahlert.koodies.string.random
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.exists
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isRegularFile
import strikt.assertions.isTrue
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.time.Instant
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@Execution(ExecutionMode.CONCURRENT)
@Suppress("RedundantInnerClassModifier")
internal class PathExtensionsKtTest {

    @Nested
    inner class Exists {

        @Test
        internal fun `should return true if path exists`() {
            val path = File.createTempFile("what", "ever").toPath()
            expectThat(path.exists).isTrue()
        }


        @Test
        internal fun `should return true if classpath exists`() {
            val path = ClassPath.of("config.txt")
            expectThat(path.exists).isTrue()
        }


        @Test
        internal fun `should return false if file is missing exists`() {
            val path = File.createTempFile("what", "ever").toPath()
            path.toFile().delete()
            expectThat(path.exists).isFalse()
        }


        @Test
        internal fun `should return false if classpath is missing`() {
            val path = ClassPath.of("missing.txt")
            expectThat(path.exists).isFalse()
        }
    }

    @Nested
    inner class FileName {

        @TestFactory
        internal fun `basename of`() = listOf(
            "filename", "filename.test",
            "my/path/filename", "my/path/filename.test",
            "/my/path/filename", "/my/path/filename.test",
        ).flatMap { path ->
            listOf(
                dynamicTest("$path should be filename") {
                    expectThat(Path.of(path).baseName).isEqualTo("filename")
                }
            )
        }

        @TestFactory
        internal fun `extension of`() = listOf(
            "filename" to null,
            "filename." to "",
            "filename.test" to "test",
            "my/path/filename" to null,
            "my/path/filename." to "",
            "my/path/filename.test" to "test",
            "/my/path/filename" to null,
            "/my/path/filename." to "",
            "/my/path/filename.test" to "test",
        ).flatMap { (path, expectedExtension) ->
            listOf(
                dynamicTest("$path should be $expectedExtension") {
                    expectThat(Path.of(path).extension).isEqualTo(expectedExtension)
                }
            )
        }

        @TestFactory
        internal fun `replaced extension of`() = listOf(
            "filename", "filename.test",
            "my/path/filename", "my/path/filename.test",
            "/my/path/filename", "/my/path/filename.test",
        ).flatMap { path ->
            listOf(
                dynamicTest("$path should be filename.test") {
                    expectThat(Path.of(path).fileNameWithExtension("test")).isEqualTo("filename.test")
                }
            )
        }
    }

    @Nested
    inner class Creation {
        @ExperimentalTime
        @Nested
        inner class Touch {
            val somewhereInThePast = Instant.parse("1984-01-01T00:00:00Z").toEpochMilli()

            @Test
            internal fun `should update modification timestamp`() {
                expectThat(createTempFile().toPath().touch()).lastModified(1.seconds)
            }

            @Test
            internal fun `should throw if file is directory`() {
                expectThat(createTempFile().also { it.delete() }.toPath().mkdirs().touch()).lastModified(1.seconds)
            }

            @Test
            internal fun `should create file if missing`() {
                val path = createTempFile().also { it.delete() }.toPath()
                path.touch()
                expectThat(path).exists()
            }

            @Test
            internal fun `should leave file unchanged if exists`() {
                val path = createTempFile().also { it.writeText("Test") }.toPath()
                path.touch()
                expectThat(path).hasContent("Test")
            }
        }
    }

    @Nested
    inner class InputStreaming {

        val testFile: Path = ClassPath.of("classpath:/cmdline.txt").copyToTempFile()
        val expected = "console=serial0,115200 console=tty1 root=PARTUUID=907af7d0-02 rootfstype=ext4 elevator=deadline " +
            "fsck.repair=yes rootwait quiet init=/usr/lib/raspi-config/init_resize.sh\n"

        @TestFactory
        internal fun `should read bytes`(): List<DynamicTest> {
            return listOf(
                ClassPath.of("classpath:/cmdline.txt"),
                testFile,
            ).flatMap { path: Path ->
                listOf(
                    dynamicTest("should stream ${path.quote()}") {
                        expectThat(path.resourceAsStream()).isNotNull().get { String(readAllBytes()) }.isEqualTo(expected)
                    },
                    dynamicTest("should stream buffered ${path.quote()}") {
                        expectThat(path.resourceAsBufferedStream()).isNotNull().get { String(readAllBytes()) }.isEqualTo(expected)
                    },
                    dynamicTest("should read buffered ${path.quote()}") {
                        expectThat(path.resourceAsBufferedReader()).isNotNull().get { readText() }.isEqualTo(expected)
                    },
                )
            }
        }

        @TestFactory
        internal fun `should return null when streaming missing resource`() = listOf(
            ClassPath.of("classpath:/missing.txt"),
            ClassPath.of("/Users/missing/missing.missing"),
        ).flatMap { path: Path ->
            listOf(
                dynamicTest("as stream") {
                    expectThat(path.resourceAsStream()).isNull()
                },

                dynamicTest("as buffered stream") {
                    expectThat(path.resourceAsBufferedStream()).isNull()
                },

                dynamicTest("as buffered reader") {
                    expectThat(path.resourceAsBufferedReader()).isNull()
                },
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
    inner class ReadAll {

        @TestFactory
        internal fun `should read complete string`() {
            val expected = """
                console=serial0,115200 console=tty1 root=PARTUUID=907af7d0-02 rootfstype=ext4 elevator=deadline fsck.repair=yes rootwait quiet init=/usr/lib/raspi-config/init_resize.sh

            """.trimIndent()
            expectThat(ClassPath.of("cmdline.txt").readAll()).isEqualTo(expected)
        }

        @Test
        internal fun `should throw on missing file`() {
            expectCatching { ClassPath.of("deleted").readAllBytes() }.isFailure().isA<IOException>()
        }
    }

    @Nested
    inner class Copy {

        val tempFilePrefix = PathExtensionsKtTest::class.simpleName!!
        val tempFile = File.createTempFile(tempFilePrefix, ".txt").also { it.writeBytes(ByteArray(10)); it.deleteOnExit() }.toPath()

        @Test
        internal fun `should copy file if destination not exists`() {
            val deletedFile = File.createTempFile(tempFilePrefix, ".txt").also { it.delete() }.toPath()
            tempFile.copyTo(deletedFile)
            expectThat(deletedFile).get { readAllBytes().size }.isEqualTo(10)
        }

        @Test
        internal fun `should throw on missing file`() {
            val deletedFile = File.createTempFile(tempFilePrefix, ".txt").also { it.delete() }.toPath()
            expectCatching { deletedFile.copyTo(tempFile) }.isFailure().isA<IOException>()
        }

        @TestFactory
        internal fun `should create temporary copy for`() = listOf(
            ClassPath.of("cmdline.txt") to Regex(".*/cmdline.*\\.txt"),
            ClassPath.of("cmdline") to Regex(".*/cmdline.*\\.tmp"),
        ).map { (path, matcher) ->
            dynamicContainer("$path",
                listOf(
                    dynamicTest("with matching name") {
                        val tempCopy = path.copyToTempFile()
                        expectThat(tempCopy).absolutePathMatches(matcher)
                    },
                    dynamicTest("with correct content") {
                        val tempCopy = path.copyToTempFile()
                        expectThat(tempCopy).hasEqualContent(ClassPath.of("cmdline.txt"))
                    },
                ))
        }
    }

    @Nested
    inner class Temp {

        val prefix = String.random(4)

        @Test
        internal fun `should create empty temp file`() {
            val path = File.createTempFile(prefix, ".txt").also { it.writeText("Test") }.toPath()
            expectThat(path.asEmptyTempFile())
                .isRegularFile()
                .hasContent("")
                .isInside(path.parent)
        }

        @Test
        internal fun `should create empty temp file in isolated directory`() {
            val path = File.createTempFile(prefix, ".txt").also { it.writeText("Test") }.toPath()
            val temp = path.asEmptyTempFile(isolated = true)
            expectThat(temp)
                .isRegularFile()
                .hasContent("")
                .isInside(temp.parent)
                .and { temp.delete() }
                .get { parent }
                .isEmptyDirectory()
        }

        @Test
        internal fun `should create temp copy`() {
            val path = File.createTempFile(prefix, ".txt").also { it.writeText("Test") }.toPath()
            val tempCopy = path.copyToTempFile()
            expectThat(tempCopy)
                .isRegularFile()
                .hasContent("Test")
                .isInside(path.parent)
        }

        @Test
        internal fun `should create temp copy in isolated directory`() {
            val path = File.createTempFile(prefix, ".txt").also { it.writeText("Test") }.toPath()
            val tempCopy = path.copyToTempFile(isolated = true)
            expectThat(tempCopy)
                .isRegularFile()
                .hasContent("Test")
                .isInside(path.parent)
                .and { tempCopy.delete() }
                .get { parent }
                .isEmptyDirectory()
        }

        @Test
        internal fun `should create temp sibling`() {
            val path = File.createTempFile(prefix, ".txt").also { it.writeText("Test") }.toPath()
            val tempSiblingCopy = path.copyToTempSiblingDirectory()
            expectThat(tempSiblingCopy)
                .isRegularFile()
                .hasContent("Test")
                .isInside(path.parent.parent)
                .and { tempSiblingCopy.delete() }
                .get { parent }
                .isEmptyDirectory()
        }
    }
}

inline fun <reified T : Number> T.times(function: (Int) -> Unit) {
    (0..this.toInt()).forEach { i -> function(i) }
}

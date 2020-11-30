package com.imgcstmzr.util

import com.bkahlert.koodies.nio.file.appendLine
import com.bkahlert.koodies.nio.file.appendText
import com.bkahlert.koodies.nio.file.classPath
import com.bkahlert.koodies.nio.file.mkdirs
import com.bkahlert.koodies.nio.file.readBytes
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.nio.file.tempPath
import com.bkahlert.koodies.nio.file.writeBytes
import com.bkahlert.koodies.nio.file.writeText
import com.bkahlert.koodies.number.hasSize
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.unit.Size.Companion.bytes
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.exists
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.io.IOException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.ReadOnlyFileSystemException
import kotlin.time.seconds

@Execution(CONCURRENT)
class PathExtensionsKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Nested
    inner class Executables {

        @Test
        fun `should assert isExecutable`() {
            val path = tempDir.tempFile()
            expectThat(path.isExecutable).isFalse()

            path.makeExecutable()
            expectThat(path.isExecutable).isTrue()
        }
    }

    @Nested
    inner class FileName {


    }

    @Nested
    inner class Creation {
        @Nested
        inner class Touch {
            @Test
            fun `should update modification timestamp`() {
                expectThat(tempDir.tempFile()).lastModified(1.seconds)
            }

            @Test
            fun `should throw if file is directory`() {
                expectThat(tempDir.tempPath().mkdirs().touch()).lastModified(1.seconds)
            }

            @Test
            fun `should create file if missing`() {
                val path = tempDir.tempPath().touch()
                expectThat(path).exists()
            }

            @Test
            fun `should leave file unchanged if exists`() {
                val path = tempDir.tempFile().writeText("Test")
                path.touch()
                expectThat(path).hasContent("Test")
            }

            @Test
            fun `should append text to file`() {
                val path = tempDir.tempFile()
                path.appendText("text 1")
                path.appendText("text 2")
                expectThat(path).hasContent("text 1text 2")
            }

            @Test
            fun `should append line to file`() {
                val path = tempDir.tempFile()
                path.appendLine("line 1")
                path.appendLine("line 2")
                expectThat(path).hasContent("line 1\nline 2\n")
            }
        }
    }

    @Nested
    inner class Wrap {

        @TestFactory
        @DisplayName("should wrap")
        fun `should generate filename_test for`() = listOf(
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
    inner class Move {

        private val tempFilePrefix = PathExtensionsKtTest::class.simpleName!!
        fun tempFile() = tempDir.tempFile(tempFilePrefix, ".txt").writeBytes(ByteArray(10)).deleteOnExit()

        @Test
        fun `should copy file if destination not exists`() {
            val deletedFile = tempDir.tempPath(tempFilePrefix, ".txt")
            val tempFile = tempFile()
            tempFile.moveTo(deletedFile)
            expectThat(deletedFile).get { readBytes().size }.isEqualTo(10)
            expectThat(tempFile).not { exists() }
        }

        @Test
        fun `should throw on missing file`() {
            val deletedFile = tempDir.tempPath(tempFilePrefix, ".txt")
            expectCatching { deletedFile.moveTo(tempFile()) }.isFailure().isA<IOException>()
        }
    }

    @Nested
    inner class Rename {

        private val tempFilePrefix = PathExtensionsKtTest::class.simpleName!!
        val tempFile = tempDir.tempFile(tempFilePrefix, ".txt").writeBytes(ByteArray(10)).deleteOnExit()

        @Test
        fun `should rename file if destination not exists`() {
            val renamedFilename = String.random()
            val renamedFile = tempFile.renameTo("$renamedFilename.txt")
            expectThat(tempFile).not { exists() }
            expectThat(renamedFile).exists().get { readBytes().size }.isEqualTo(10)
        }

        @Test
        fun `should throw on missing file`() {
            val deletedFile = tempDir.tempPath(tempFilePrefix, ".txt")
            expectCatching { deletedFile.renameTo("filename") }.isFailure().isA<NoSuchFileException>()
        }

        @Test
        fun `should throw on classpath file`() {
            expectCatching { classPath("try.it") { renameTo("filename") } }.isFailure().isA<ReadOnlyFileSystemException>()
        }
    }

    @Nested
    inner class Cleanup {

        private val tempFilePrefix = PathExtensionsKtTest::class.simpleName!!
        private fun getTempFile(additionalSuffix: String) = tempDir.tempFile(tempFilePrefix, ".txt$additionalSuffix").writeBytes(ByteArray(10))

        @Suppress("LongLine")
        @Test
        fun `should rename file if trailing dirt`() {
            val path = getTempFile("?x")
            val cleanedFile = path.cleanUp("?")
            expectThat(path).not { exists() }
            expectThat(cleanedFile)
                .exists()
                .hasSize(10.bytes)
                .get { "$fileName" }.not { contains("?") }
        }

        @Test
        fun `should not rename file without trailing dirt`() {
            val file = getTempFile("?x")
            val cleanedFile = file.cleanUp("#")
            expectThat(cleanedFile).exists().isEqualTo(file)
        }
    }
}

inline operator fun <reified T : Number> T.times(function: (Int) -> Unit) {
    (0 until toInt()).forEach { i -> function(i) }
}

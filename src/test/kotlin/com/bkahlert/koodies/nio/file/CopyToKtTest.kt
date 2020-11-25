package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.io.PathFixtures.directoryWithTwoFiles
import com.bkahlert.koodies.io.TarArchiver.tar
import com.bkahlert.koodies.time.minus
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.containsAllFiles
import com.imgcstmzr.util.hasContent
import com.imgcstmzr.util.hasSameFiles
import com.imgcstmzr.util.isDirectory
import com.imgcstmzr.util.isFile
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expect
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.time.days
import kotlin.time.seconds

@Execution(CONCURRENT)
class CopyToKtTest {

    private val tempDir = tempDir().deleteOnExit()

    private val testFile = tempDir.tempFile(extension = ".txt").writeText("test file").apply { lastModified -= 7.days }
    private val testDir = tempDir.directoryWithTwoFiles().apply { listRecursively().forEach { it.lastModified -= 7.days } }


    @Test
    fun `should throw on missing src`() {
        val src = tempDir.tempPath(extension = ".txt")
        expectCatching { src.copyTo(testFile) }.isFailure().isA<NoSuchFileException>()
    }

    @Nested
    inner class CopyFile {

        @Test
        fun `should copy file if destination not exists`() {
            val dest = tempDir.tempPath(extension = ".txt")
            expectThat(testFile.copyTo(dest))
                .hasContent("test file")
                .isEqualTo(dest)
        }

        @Test
        fun `should create missing directories on missing destination parent`() {
            val dest = tempDir.tempPath("missing").tempPath()
            expectThat(testFile.copyTo(dest))
                .hasContent("test file")
                .isEqualTo(dest)
        }

        @Test
        fun `should throw on existing file destination`() {
            val dest = tempDir.tempFile(extension = ".txt").writeText("old")
            expect {
                catching { testFile.copyTo(dest) }.isFailure().isA<FileAlreadyExistsException>()
                that(dest).hasContent("old")
            }
        }

        @Test
        fun `should throw on existing directory destination`() {
            val dest = tempDir.tempDir().apply { tempFile().writeText("old") }
            expect {
                catching { testFile.copyTo(dest) }.isFailure().isA<FileAlreadyExistsException>()
                that(dest.list().single()).hasContent("old")
            }
        }

        @Test
        fun `should override existing file destination`() {
            val dest = tempDir.tempFile(extension = ".txt").writeText("old")
            expectThat(testFile.copyTo(dest, overwrite = true))
                .hasContent("test file")
                .isEqualTo(dest)
        }

        @Test
        fun `should override existing directory destination`() {
            val dest = tempDir.tempDir().apply { tempFile().writeText("old") }
            expectThat(testFile.copyTo(dest, overwrite = true))
                .hasContent("test file")
                .isEqualTo(dest)
        }

        @Test
        fun `should not copy attributes by default`() {
            val dest = tempDir.tempPath(extension = ".txt")
            expectThat(testFile.copyTo(dest))
                .age.isLessThan(10.seconds)
        }

        @Test
        fun `should copy attributes if specified`() {
            val dest = tempDir.tempPath(extension = ".txt")
            expectThat(testFile.copyTo(dest, preserve = true))
                .age.isGreaterThan(6.9.days).isLessThan(7.1.days)
        }
    }

    @Nested
    inner class CopyDirectory {

        @Test
        fun `should copy file if destination not exists`() {
            val dest = tempDir.tempPath(extension = ".txt")
            expectThat(testDir.copyTo(dest))
                .isCopyOf(testDir)
                .isEqualTo(dest)
        }

        @Test
        fun `should create missing directories on missing destination parent`() {
            val dest = tempDir.tempPath().tempPath()
            expectThat(testDir.copyTo(dest))
                .isCopyOf(testDir)
                .isEqualTo(dest)
        }

        @Test
        fun `should throw on existing file destination`() {
            val dest = tempDir.tempFile(extension = ".txt").writeText("old")
            expect {
                catching { testDir.copyTo(dest) }.isFailure().isA<FileAlreadyExistsException>()
                that(dest).hasContent("old")
            }
        }

        @Test
        fun `should merge on existing directory destination`() {
            val dest = tempDir.tempDir().apply { tempFile().writeText("old") }
            val alreadyExistingFile = dest.list().single()
            expectThat(testDir.copyTo(dest))
                .containsAllFiles(testDir)
                .and { get { resolve(alreadyExistingFile.fileName) }.hasContent("old") }
                .isEqualTo(dest)
        }

        @Test
        fun `should override existing file destination`() {
            val dest = tempDir.tempFile(extension = ".txt").writeText("old")
            expectThat(testDir.copyTo(dest, overwrite = true))
                .isCopyOf(testDir)
                .isEqualTo(dest)
        }

        @Test
        fun `should merge on overwriting non-empty directory destination`() {
            val dest = tempDir.tempDir().apply { tempFile().writeText("old") }
            val alreadyExistingFile = dest.list().single()
            expectThat(testDir.copyTo(dest, overwrite = true))
                .containsAllFiles(testDir)
                .and { get { resolve(alreadyExistingFile.fileName) }.hasContent("old") }
                .isEqualTo(dest)
        }

        @Test
        fun `should not copy attributes by default`() {
            val dest = tempDir.tempPath(extension = ".txt")
            expectThat(testDir.copyTo(dest)) {
                get { listRecursively().toList() }.all {
                    age.isLessThan(10.seconds)
                }
            }
        }

        @Test
        fun `should copy attributes if specified for all but not-empty directories`() {
            val dest = tempDir.tempPath(extension = ".txt")
            expectThat(testDir.copyTo(dest, preserve = true)) {
                get { listRecursively().filter { !it.isDirectory || it.isEmpty }.toList() }.all {
                    age.isGreaterThan(6.9.days).isLessThan(7.1.days)
                }
            }
        }
    }
}


fun Assertion.Builder<Path>.createsEqualTar(other: Path) =
    assert("is copy of $other") { self ->
        val selfTar = self.tar(tempFile()).deleteOnExit()
        val otherTar = other.tar(tempFile()).deleteOnExit()

        val selfBytes = selfTar.readBytes()
        val otherBytes = otherTar.readBytes()
        if (selfBytes.contentEquals(otherBytes)) pass()
        else fail("The resulting tarballs do not match. Expected size ${selfBytes.size} but was ${otherBytes.size}")
    }

fun Assertion.Builder<Path>.isCopyOf(other: Path) =
    assert("is copy of $other") { self ->
        if (self.isFile && !other.isFile) fail("$self is a file and can only be compared to another file")
        else if (self.isDirectory && !other.isDirectory) fail("$self is a directory and can only be compared to another directory")
        else if (self.isDirectory) {
            kotlin.runCatching {
                expectThat(self).hasSameFiles(other)
            }.exceptionOrNull()?.let { fail("Directories contained different files.") } ?: pass()
        } else {
            val selfBytes = self.readBytes()
            val otherBytes = other.readBytes()
            if (selfBytes.contentEquals(otherBytes)) pass()
            else fail("The resulting tarballs do not match. Expected size ${selfBytes.size} but was ${otherBytes.size}")
        }
    }

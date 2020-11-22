package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.io.PathFixtures.directoryWithTwoFiles
import com.bkahlert.koodies.time.minus
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.containsAllFiles
import com.imgcstmzr.util.hasContent
import com.imgcstmzr.util.isDirectory
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
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
import kotlin.time.days
import kotlin.time.seconds

@Execution(CONCURRENT)
class CopyToDirectoryKtTest {

    private val tempDir = tempDir().deleteOnExit()

    private val testFile = tempDir.tempFile(extension = ".txt").writeText("test file").apply { lastModified -= 7.days }
    private val testDir = tempDir.directoryWithTwoFiles().apply { listRecursively().forEach { it.lastModified -= 7.days } }


    @Test
    fun `should throw on missing src`() {
        val src = tempDir.tempPath(extension = ".txt")
        expectCatching { src.copyToDirectory(testFile) }.isFailure().isA<NoSuchFileException>()
    }

    @Nested
    inner class CopyFile {

        @Test
        fun `should copy file if destination not exists`() {
            val dest = tempDir.tempDir().resolve(testFile.fileName)
            expectThat(testFile.copyToDirectory(dest.parent))
                .hasContent("test file")
                .isEqualTo(dest)
        }

        @Test
        fun `should create missing directories on missing destination parent`() {
            val dest = tempDir.tempPath("missing").resolve(testFile.fileName)
            expectThat(testFile.copyToDirectory(dest.parent))
                .hasContent("test file")
                .isEqualTo(dest)
        }

        @Test
        fun `should throw on existing file destination`() {
            val dest = tempDir.tempDir().resolve(testFile.fileName).writeText("old")
            expect {
                catching { testFile.copyToDirectory(dest.parent) }.isFailure().isA<FileAlreadyExistsException>()
                that(dest).hasContent("old")
            }
        }

        @Test
        fun `should throw on existing directory destination`() {
            val dest = tempDir.tempDir().resolve(testFile.fileName).mkdirs().apply { tempFile().writeText("old") }
            expect {
                catching { testFile.copyToDirectory(dest.parent) }.isFailure().isA<FileAlreadyExistsException>()
                that(dest.list().single()).hasContent("old")
            }
        }

        @Test
        fun `should override existing file destination`() {
            val dest = tempDir.tempDir().resolve(testFile.fileName).writeText("old")
            expectThat(testFile.copyToDirectory(dest.parent, overwrite = true))
                .hasContent("test file")
                .isEqualTo(dest)
        }

        @Test
        fun `should override existing directory destination`() {
            val dest = tempDir.tempDir().resolve(testFile.fileName).mkdirs().apply { tempFile().writeText("old") }
            expectThat(testFile.copyToDirectory(dest.parent, overwrite = true))
                .hasContent("test file")
                .isEqualTo(dest)
        }

        @Test
        fun `should not copy attributes by default`() {
            val dest = tempDir.tempDir().resolve(testFile.fileName)
            expectThat(testFile.copyToDirectory(dest.parent))
                .age.isLessThan(10.seconds)
        }

        @Test
        fun `should copy attributes if specified`() {
            val dest = tempDir.tempDir().resolve(testFile.fileName)
            expectThat(testFile.copyToDirectory(dest.parent, preserve = true))
                .age.isGreaterThan(6.9.days).isLessThan(7.1.days)
        }
    }

    @Nested
    inner class CopyDirectory {

        @Test
        fun `should copy file if destination not exists`() {
            val dest = tempDir.tempDir().resolve(testDir.fileName)
            expectThat(testDir.copyToDirectory(dest.parent))
                .isCopyOf(testDir)
                .isEqualTo(dest)
        }

        @Test
        fun `should create missing directories on missing destination parent`() {
            val dest = tempDir.tempPath().resolve(testDir.fileName)
            expectThat(testDir.copyToDirectory(dest.parent))
                .isCopyOf(testDir)
                .isEqualTo(dest)
        }

        @Test
        fun `should throw on existing file destination`() {
            val dest = tempDir.tempDir().resolve(testDir.fileName).writeText("old")
            expect {
                catching { testDir.copyToDirectory(dest.parent) }.isFailure().isA<FileAlreadyExistsException>()
                that(dest).hasContent("old")
            }
        }

        @Test
        fun `should merge on existing directory destination`() {
            val dest = tempDir.tempDir().resolve(testDir.fileName).apply { tempFile().writeText("old") }
            val alreadyExistingFile = dest.list().single()
            expectThat(testDir.copyToDirectory(dest.parent))
                .containsAllFiles(testDir)
                .and { get { resolve(alreadyExistingFile.fileName) }.hasContent("old") }
                .isEqualTo(dest)
        }

        @Test
        fun `should override existing file destination`() {
            val dest = tempDir.tempDir().resolve(testDir.fileName).writeText("old")
            expectThat(testDir.copyToDirectory(dest.parent, overwrite = true))
                .isCopyOf(testDir)
                .isEqualTo(dest)
        }

        @Test
        fun `should merge on overwriting non-empty directory destination`() {
            val dest = tempDir.tempDir().resolve(testDir.fileName).apply { tempFile().writeText("old") }
            val alreadyExistingFile = dest.list().single()
            expectThat(testDir.copyToDirectory(dest.parent, overwrite = true))
                .containsAllFiles(testDir)
                .and { get { resolve(alreadyExistingFile.fileName) }.hasContent("old") }
                .isEqualTo(dest)
        }

        @Test
        fun `should not copy attributes by default`() {
            val dest = tempDir.tempDir().resolve(tempDir.fileName)
            expectThat(testDir.copyToDirectory(dest.parent)) {
                get { listRecursively().toList() }.all {
                    age.isLessThan(10.seconds)
                }
            }
        }

        @Test
        fun `should copy attributes if specified for all but not-empty directories`() {
            val dest = tempDir.tempDir().resolve(tempDir.fileName)
            expectThat(testDir.copyToDirectory(dest.parent, preserve = true)) {
                get { listRecursively().filter { !it.isDirectory || it.isEmpty }.toList() }.all {
                    age.isGreaterThan(6.9.days).isLessThan(7.1.days)
                }
            }
        }
    }
}

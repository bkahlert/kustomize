package com.imgcstmzr.runtime

import com.bkahlert.koodies.nio.file.delete
import com.bkahlert.koodies.nio.file.isDuplicateOf
import com.bkahlert.koodies.nio.file.readLines
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.nio.file.writeLine
import com.imgcstmzr.runtime.OperatingSystemImage.Companion.based
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.isWritable
import com.imgcstmzr.util.touch
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path

@Execution(CONCURRENT)
class OperatingSystemImageTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should have correct absolute path`() {
        expectThat((OperatingSystemMock("abs-path-test") based Path.of("/foo/bar"))).serializedPathIsEqualTo("/foo/bar")
    }

    @Test
    fun `should have correct relative path`() {
        expectThat((OperatingSystemMock("rel-path-test") based Path.of("foo/bar"))).serializedPathIsEqualTo("foo/bar")
    }

    @Test
    fun `should have full name`() {
        expectThat((OperatingSystemMock("full-name-test") based Path.of("foo/bar")).fullName)
            .isEqualTo("ImgCstmzr Test OS at file://${Paths.WORKING_DIRECTORY}/foo/bar")
    }

    @Test
    fun `should have short name`() {
        expectThat((OperatingSystemMock("short-name-test") based Path.of("foo/bar")).shortName)
            .isEqualTo("ImgCstmzr Test OS at bar")
    }

    @Test
    fun `should duplicate`() {
        val osImage = OperatingSystemImage(OperatingSystemMock(::`should duplicate`.name), path = tempDir.tempDir().tempFile(extension = ".img"))
        expectThat(osImage.duplicate()).path.isDuplicateOf(osImage.file)
    }

    @Nested
    inner class LogFileLocation {
        @Test
        fun `should return in same directory`() {
            val osImage = OperatingSystemImage(OperatingSystemMock(::`should return in same directory`.name), path = tempDir.tempDir().resolve("test.img"))
            expectThat(osImage.newLogFilePath()).canWriteAndRead().isEqualTo(osImage.directory.resolve("test.00.log"))
        }

        @Test
        fun `should increase index`() {
            val osImage = OperatingSystemImage(OperatingSystemMock(::`should increase index`.name), path = tempDir.tempDir().resolve("test.img"))
            expect {
                (0 until 10).forEach { i ->
                    that(osImage.newLogFilePath()).canWriteAndRead().isEqualTo(osImage.directory.resolve("test.0$i.log"))
                }
            }
        }

        @Test
        fun `should just count existing files`() {
            val osImage = OperatingSystemImage(OperatingSystemMock(::`should return in same directory`.name), path = tempDir.tempDir().resolve("test.img"))
            expectThat(osImage.newLogFilePath()).canWriteAndRead().isEqualTo(osImage.directory.resolve("test.00.log"))
            expectThat(osImage.newLogFilePath()).canWriteAndRead().isEqualTo(osImage.directory.resolve("test.01.log"))
            osImage.directory.resolve("test.01.log").delete()
            expectThat(osImage.newLogFilePath()).canWriteAndRead().isEqualTo(osImage.directory.resolve("test.01.log"))
        }
    }
}

val Assertion.Builder<OperatingSystemImage>.size get() = get("size") { size }
val Assertion.Builder<OperatingSystemImage>.path get() = get("path") { file }
fun Assertion.Builder<OperatingSystemImage>.serializedPathIsEqualTo(path: String) =
    assert("path equals %s") {
        val actual = it.file.serialized
        if (actual == path) pass()
        else fail("is $actual")
    }

fun <T : Path> Assertion.Builder<T>.canWriteAndRead() =
    assert("can write and read %s") {
        it.touch()
        if (!it.isWritable) fail("is not writable")
        it.writeLine("write assertion")
        val lines = it.readLines()
        if (lines.first() != "write assertion") fail("content was $lines instead of \"write assertion\"")
        pass()
    }

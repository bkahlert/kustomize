package com.imgcstmzr.runtime

import com.imgcstmzr.runtime.OperatingSystemImage.Companion.based
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.test.isDuplicateOf
import com.imgcstmzr.withTempDir
import koodies.io.path.Locations
import koodies.io.path.asString
import koodies.io.path.delete
import koodies.io.path.randomDirectory
import koodies.io.path.randomFile
import koodies.io.path.touch
import koodies.io.path.writeLine
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path
import kotlin.io.path.isWritable
import kotlin.io.path.readLines

@Execution(CONCURRENT)
class OperatingSystemImageTest {

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
            .isEqualTo("ImgCstmzr Test OS at file://${Locations.WorkingDirectory}/foo/bar")
    }

    @Test
    fun `should have short name`() {
        expectThat((OperatingSystemMock("short-name-test") based Path.of("foo/bar")).shortName)
            .isEqualTo("ImgCstmzr Test OS at bar")
    }

    @Test
    fun `should duplicate`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val osImage = OperatingSystemImage(OperatingSystemMock(uniqueId.simple), path = randomDirectory().randomFile(extension = ".img"))
        expectThat(osImage.duplicate()).path.isDuplicateOf(osImage.file)
    }

    @Nested
    inner class LogFileLocation {
        @Test
        fun `should return in same directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val osImage = OperatingSystemImage(OperatingSystemMock(uniqueId.simple), path = randomDirectory().resolve("test.img"))
            expectThat(osImage.newLogFilePath()).canWriteAndRead().isEqualTo(osImage.directory.resolve("test.00.log"))
        }

        @Test
        fun `should increase index`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val osImage = OperatingSystemImage(OperatingSystemMock(uniqueId.simple), path = randomDirectory().resolve("test.img"))
            expect {
                (0 until 10).forEach { i ->
                    that(osImage.newLogFilePath()).canWriteAndRead().isEqualTo(osImage.directory.resolve("test.0$i.log"))
                }
            }
        }

        @Test
        fun `should just count existing files`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val osImage = OperatingSystemImage(OperatingSystemMock(uniqueId.simple), path = randomDirectory().resolve("test.img"))
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
        val actual = it.file.asString()
        if (actual == path) pass()
        else fail("is $actual")
    }

fun <T : Path> Assertion.Builder<T>.canWriteAndRead() =
    assert("can write and read %s") {
        it.touch()
        if (!it.isWritable()) fail("is not writable")
        it.writeLine("write assertion")
        val lines = it.readLines()
        if (lines.first() != "write assertion") fail("content was $lines instead of \"write assertion\"")
        pass()
    }

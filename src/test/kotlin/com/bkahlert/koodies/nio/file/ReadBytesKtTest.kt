package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.FixtureLog.deleteOnExit
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import java.io.IOException
import java.nio.file.Path

@Execution(CONCURRENT)
class ReadBytesKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @TestFactory
    fun `should read bytes`() = mapOf(
        tempDir.tempFile(extension = ".txt").writeBytes(ByteArray(10)) to 10,
        classPath("classpath:funny.img.zip") { this }!! to 540,
        classPath("classpath:/cmdline.txt") { this }!! to 169,
    ).flatMap { (path: Path, expectedSize) ->
        listOf(
            DynamicTest.dynamicTest("$expectedSize <- $path") {
                val actual = path.readBytes()
                expectThat(actual).get { size }.isEqualTo(expectedSize)
            }
        )
    }

    @Test
    fun `should throw on missing file`() {
        val deletedFile = tempDir.tempPath(extension = ".txt")
        expectCatching { deletedFile.readBytes() }.isFailure().isA<IOException>()
    }
}

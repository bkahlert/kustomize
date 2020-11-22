package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.FixtureLog.deleteOnExit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.nio.file.NoSuchFileException

@Execution(CONCURRENT)
class ExistsKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should return true if path exists`() {
        val path = tempDir.tempFile("what", "ever")
        expectThat(path.exists).isTrue()
    }

    @Test
    fun `should return true if classpath exists`() {
        val path by classPath("config.txt")
        expectThat(path.exists).isTrue()
    }


    @Test
    fun `should return false if file is missing`() {
        val path = tempDir.tempFile("what", "ever")
        path.toFile().delete()
        expectThat(path.exists).isFalse()
    }

    @Test
    fun `should throw if classpath is missing`() {
        val path by classPath("missing.txt")
        expectCatching { path.exists }.isFailure().isA<NoSuchFileException>()
    }
}

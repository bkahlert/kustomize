package com.imgcstmzr.util

import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.exists
import java.nio.file.Path

@Isolated
@Execution(SAME_THREAD)
class FixtureLogTest {

    @BeforeEach
    private fun reset(logger: InMemoryLogger<String>) {
        FixtureLog.apply { logger.delete() }
    }

    @Test
    fun `should not exist on creation`() {
        expectThat(FixtureLog.location).not { exists() }
    }

    @Test
    fun `should add to log using invoke`() {
        val imgFixture = Paths.tempFile("sample", ".img")
        FixtureLog.invoke(imgFixture)
        expectThat(FixtureLog.paths()).containsExactly(imgFixture.toAbsolutePath())
    }

    @Test
    fun `should add to log using deleteOnExit`() {
        val imgFixture = Paths.tempFile("sample", ".img").deleteOnExit()
        expectThat(FixtureLog.paths()).containsExactly(imgFixture.toAbsolutePath())
    }

    @Test
    fun `should add parent directory if has same basename`() {
        val imgFixture = Paths.tempFile("sample", ".img").deleteOnExit()
        val dirFileFixture: Path = Paths.tempFile("dir", extension = "").also { it.delete() }.mkdirs().let {
            it.resolve(it.fileName.fileNameWithExtension("txt")).deleteOnExit().writeText("$it")
        }
        expectThat(FixtureLog.paths()).containsExactly(imgFixture, dirFileFixture.parent)
    }

    @Test
    internal fun `should delete read-only files`(logger: InMemoryLogger<String>) {
        val file = Paths.tempFile().deleteOnExit()
        file.toFile().setReadOnly()
        FixtureLog.apply { logger.delete() }
        expectThat(file).not { exists() }
    }
}

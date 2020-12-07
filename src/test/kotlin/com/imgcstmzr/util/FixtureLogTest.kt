package com.imgcstmzr.util

import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.nio.file.withExtension
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.exists
import java.nio.file.Path

@Isolated
@Execution(SAME_THREAD)
class FixtureLogTest {

    private val tempDir = tempDir().deleteOnExit()

    @BeforeEach
    private fun reset(logger: InMemoryLogger) {
        FixtureLog.apply { logger.delete() }
    }

    @Test
    fun `should not exist on creation`() {
        expectThat(FixtureLog.location).not { exists() }
    }

    @Test
    fun `should add to log using invoke`() {
        val imgFixture = tempDir.tempFile("sample", ".img")
        FixtureLog.invoke(imgFixture)
        expectThat(FixtureLog.paths()).containsExactly(imgFixture.toAbsolutePath())
    }

    @Test
    fun `should add to log using deleteOnExit`() {
        val imgFixture = tempDir.tempFile("sample", ".img").deleteOnExit()
        expectThat(FixtureLog.paths()).containsExactly(imgFixture.toAbsolutePath())
    }

    @Test
    fun `should add parent directory if has same basename`() {
        val fixture: Path = tempDir.tempDir().run {
            resolve(fileName).withExtension("img").touch()
        }.deleteOnExit()
        expectThat(FixtureLog.paths()).not { contains(fixture) }.contains(fixture.parent)
    }

    @Test
    fun `should not add parent directory if have different basename`() {
        val fixture: Path = tempDir.tempDir().run {
            resolve("${fileName}different").withExtension("img").touch()
        }.deleteOnExit()
        expectThat(FixtureLog.paths()).contains(fixture).not { contains(fixture.parent) }
    }

    @Test
    fun `should delete read-only files`(logger: InMemoryLogger) {
        val file = tempDir.tempFile().deleteOnExit()
        file.toFile().setReadOnly()
        FixtureLog.apply { logger.delete() }
        expectThat(file).not { exists() }
    }
}

package com.imgcstmzr.util

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.containsExactly
import strikt.assertions.exists
import java.nio.file.Path

@Execution(CONCURRENT)
class FixtureLogTest {
    val fixtureLog = FixtureLog(Paths.TEMP.resolve("fixture.log"))

    @Test
    internal fun `should not exist on creation`() {
        val log = FixtureLog(Paths.tempFile("fixture", ".log").also { fixtureLog(it) })
        expectThat(log.location).not { exists() }
    }

    @Test
    internal fun `should add to log`() {
        val log = FixtureLog(Paths.tempFile("fixture", ".log").also { fixtureLog(it) })
        val imgFixture = Paths.tempFile("sample", ".img").also { fixtureLog(it) }
        log(imgFixture)
        expectThat(log.paths()).containsExactly(imgFixture.toAbsolutePath())
    }

    @Test
    internal fun `should add parent directory if has same basename`() {
        val log = FixtureLog(Paths.tempFile("fixture", ".log").also { fixtureLog(it) })
        val imgFixture = Paths.tempFile("sample", ".img").also { fixtureLog(it) }
        val dirFileFixture: Path = Paths.tempFile("dir", extension = "").also { it.delete() }.mkdirs().let {
            it.resolve(it.fileName.fileNameWithExtension("txt")).also { fixtureLog(it) }.also { it.writeText(it.toString()) }
        }

        log(imgFixture)
        log(dirFileFixture)
        expectThat(log.paths()).containsExactly(imgFixture, dirFileFixture.parent)
    }

    @Test
    internal fun `should cleanup on start`() {
        val fixtureLogLocation = Paths.tempFile("fixture", ".log").also { fixtureLog(it) }

        val log = FixtureLog(fixtureLogLocation)
        val imgFixture = Paths.tempFile("sample", ".img").also { fixtureLog(it) }
        val dirFileFixture: Path = Paths.tempFile("dir", extension = "").also { it.delete() }.mkdirs().let {
            it.resolve(it.fileName.fileNameWithExtension("txt")).also { fixtureLog(it) }.also { it.writeText(it.toString()) }
        }
        log(imgFixture)
        log(dirFileFixture)

        val newLog = FixtureLog(fixtureLogLocation)
        expectThat(newLog.location).not { exists() }
        expectThat(listOf(imgFixture, dirFileFixture.parent)).all { not { exists() } }
    }

    @AfterEach
    internal fun tearDown() {
        FixtureLog(Paths.TEMP.resolve(fixtureLog.location))
    }
}

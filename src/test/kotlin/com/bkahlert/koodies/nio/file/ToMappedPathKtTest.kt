package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.string.Unicode
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.test.FixturePath61C285F09D95930D0AE298B00AF09F918B0A.fixtureContent
import com.bkahlert.koodies.test.FixturePath61C285F09D95930D0AE298B00AF09F918B0A.fixtureFileName
import com.imgcstmzr.patch.isEqualTo
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.readAll
import com.imgcstmzr.util.readAllBytes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class ToMappedPathKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should map regular path`() {
        val randomContent = String.random()
        val uri = tempDir.tempFile().writeText(randomContent).deleteOnExit().toUri()
        val readContent = uri.toMappedPath { it.readAll() }
        expectThat(readContent).isEqualTo(randomContent)
    }

    @Test
    fun `should map stdlib class path`() {
        val url = Regex::class.java.getResource("Regex.class")
        val siblingFileNames = url.toMappedPath { it.readAll() }
        expectThat(siblingFileNames)
            .contains("Matcher")
            .contains("MatchResult")
            .contains("getRange")
            .contains(" ")
            .contains("")
            .contains(Unicode.startOfHeading.toString())
    }

    @Test
    fun `should map own class path`() {
        val uri = Thread.currentThread().contextClassLoader.getResource(fixtureFileName)?.toURI()
        val bytes: ByteArray? = uri?.toMappedPath { it.readAllBytes() }
        expectThat(bytes).isEqualTo(fixtureContent)
    }
}

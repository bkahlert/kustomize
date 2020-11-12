package com.bkahlert.koodies.concurrent

import com.bkahlert.koodies.nio.file.sameFile
import com.bkahlert.koodies.time.Now
import com.imgcstmzr.util.writeText
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.exists
import java.nio.file.Path

@Isolated
@Execution(CONCURRENT)
class CleanUpOnShutdownKtTest {

    private val name = "koodies-cleanup-does-not-work.txt"
    private val markerFile: Path = sameFile(name)

    @BeforeAll
    fun setUp() {
        cleanUpOnShutdown(markerFile)
    }

    @Test
    fun `should clean up on shutdown`() {
        expectThat(markerFile).not { exists() }
    }

    @AfterAll
    fun tearDown() {
        markerFile.writeText("""
            This file was created $Now.
            It used to be cleaned up by the com.bkahlert.koodies library
            the moment the application in question shut down.
            
            The application was started by ${System.getProperty("sun.java.command")}.
        """.trimIndent())
    }
}

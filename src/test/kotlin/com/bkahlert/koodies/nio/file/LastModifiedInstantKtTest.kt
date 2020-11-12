package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.time.Now
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import kotlin.time.minutes

@Execution(CONCURRENT)
class LastModifiedInstantKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should read last modified`() {
        expectThat(tempDir.tempFile().lastModifiedInstant)
            .isLessThan(Now.plus(1.minutes))
            .isGreaterThan(Now.minus(1.minutes))
    }

    @Test
    fun `should write last modified`() {
        val file = tempDir.tempFile()
        file.lastModifiedInstant = Now.minus(20.minutes)
        expectThat(file.lastModifiedInstant)
            .isLessThan(Now.plus(21.minutes))
            .isGreaterThan(Now.minus(21.minutes))
    }
}


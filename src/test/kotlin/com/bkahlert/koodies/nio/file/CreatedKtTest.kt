package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.time.Now
import com.bkahlert.koodies.time.toFileTime
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import kotlin.time.minutes

@Execution(CONCURRENT)
class CreatedKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should read created`() {
        expectThat(tempDir.tempFile().created.toInstant())
            .isLessThan(Now.plus(1.minutes))
            .isGreaterThan(Now.minus(1.minutes))
    }

    @Test
    fun `should write created`() {
        val file = tempDir.tempFile()
        file.created = Now.minus(20.minutes).toFileTime()
        expectThat(file.created)
            .isLessThan(Now.plus(21.minutes).toFileTime())
            .isGreaterThan(Now.minus(21.minutes).toFileTime())
    }
}


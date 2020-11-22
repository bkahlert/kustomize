package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.time.Now
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import java.nio.file.attribute.FileTime
import kotlin.time.minutes

@Execution(CONCURRENT)
class LastAccessedKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should read last accessed`() {
        expectThat(tempDir.tempFile().lastAccessed.toInstant())
            .isLessThan(Now.plus(1.minutes))
            .isGreaterThan(Now.minus(1.minutes))
    }

    @Test
    fun `should write last accessed`() {
        val file = tempDir.tempFile()
        file.lastAccessed = FileTime.from(Now.minus(20.minutes))
        expectThat(file.lastAccessed.toInstant())
            .isLessThan(Now.plus(21.minutes))
            .isGreaterThan(Now.minus(21.minutes))
    }
}

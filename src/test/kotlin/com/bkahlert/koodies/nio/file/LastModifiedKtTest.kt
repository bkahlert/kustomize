package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.time.Now
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.Paths
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import java.nio.file.attribute.FileTime
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

@Execution(CONCURRENT)
internal class LastModifiedKtTest {
    @OptIn(ExperimentalTime::class)
    @Test
    internal fun `should read last modified`() {
        expectThat(Paths.tempFile().deleteOnExit().lastModified.toInstant())
            .isLessThan(Now.plus(1.minutes))
            .isGreaterThan(Now.minus(1.minutes))
    }

    @OptIn(ExperimentalTime::class)
    @Test
    internal fun `should write last modified`() {
        val file = Paths.tempFile().deleteOnExit()
        file.lastModified = FileTime.from(Now.minus(20.minutes))
        expectThat(file.lastModified.toInstant())
            .isLessThan(Now.plus(21.minutes))
            .isGreaterThan(Now.minus(21.minutes))
    }
}

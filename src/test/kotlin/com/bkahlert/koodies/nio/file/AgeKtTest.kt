package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.Paths
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.minutes

@Execution(CONCURRENT)
class AgeKtTest {
    @OptIn(ExperimentalTime::class)
    @Test
    fun `should read age`() {
        expectThat(Paths.tempFile().deleteOnExit().age)
            .isLessThan(100.milliseconds)
            .isGreaterThan(Duration.ZERO)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should save age`() {
        val file = Paths.tempFile().deleteOnExit()
        file.age = 20.minutes
        expectThat(file.age)
            .isLessThan(20.minutes + 100.milliseconds)
            .isGreaterThan(20.minutes)
    }
}



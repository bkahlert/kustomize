package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.FixtureLog.deleteOnExit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isLessThan
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.minutes
import kotlin.time.seconds

@Execution(CONCURRENT)
class AgeKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should read age`() {
        expectThat(tempDir.tempFile().deleteOnExit()).age
            .isLessThan(1.seconds)
            .isGreaterThanOrEqualTo(Duration.ZERO)
    }

    @Test
    fun `should save age`() {
        val file = tempDir.tempFile().deleteOnExit()
        file.age = 20.minutes
        expectThat(file).age
            .isLessThan(20.minutes + 1.seconds)
            .isGreaterThanOrEqualTo(20.minutes)
    }
}

val Assertion.Builder<out Path>.age get() = get { age }

fun <T : Path> Assertion.Builder<T>.hasAge(age: Duration) =
    assert("is $age old") {
        val actualAge = it.age
        when (actualAge == age) {
            true -> pass()
            else -> fail("is actually $actualAge old")
        }
    }

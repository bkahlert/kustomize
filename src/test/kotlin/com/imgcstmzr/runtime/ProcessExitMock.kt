package com.imgcstmzr.runtime

import com.bkahlert.koodies.time.busyWait
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

@OptIn(ExperimentalTime::class)
data class ProcessExitMock(val exitCode: Int, val delay: Duration) {
    operator fun invoke(): Int {
        delay.busyWait()
        return exitCode
    }

    operator fun invoke(timeout: Duration): Boolean {
        delay.busyWait()
        return timeout > delay
    }

    companion object {
        fun immediateSuccess() = ProcessExitMock(0, Duration.ZERO)
        fun immediateExit(exitCode: Int) = ProcessExitMock(exitCode, Duration.ZERO)
        fun computing() = delayedExit(0, 1.milliseconds)
        fun delayedExit(exitCode: Int, delay: Duration) = ProcessExitMock(exitCode, delay)
        fun deadLock() = ProcessExitMock(-1, Duration.INFINITE)
    }
}

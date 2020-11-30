package com.imgcstmzr.runtime

import com.bkahlert.koodies.time.busyWait
import kotlin.time.Duration
import kotlin.time.milliseconds

data class ProcessExitMock(val exitValue: Int, val delay: Duration) {
    operator fun invoke(): Int {
        delay.busyWait()
        return exitValue
    }

    operator fun invoke(timeout: Duration): Boolean {
        delay.busyWait()
        return timeout > delay
    }

    companion object {
        fun immediateSuccess() = ProcessExitMock(0, Duration.ZERO)
        fun immediateExit(exitValue: Int) = ProcessExitMock(exitValue, Duration.ZERO)
        fun computing() = delayedExit(0, 1.milliseconds)
        fun delayedExit(exitValue: Int, delay: Duration) = ProcessExitMock(exitValue, delay)
        fun deadLock() = ProcessExitMock(-1, Duration.INFINITE)
    }
}

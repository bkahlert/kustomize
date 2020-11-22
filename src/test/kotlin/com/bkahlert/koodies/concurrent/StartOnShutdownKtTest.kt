package com.bkahlert.koodies.concurrent

import com.bkahlert.koodies.nio.file.delete
import com.bkahlert.koodies.nio.file.sameFile
import com.imgcstmzr.util.touch
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.exists

@Execution(CONCURRENT)
class StartOnShutdownKtTest {

    val marker = sameFile("com.bkahlert.koodies.start-on-shutdown.failed")

    @Test
    fun `should register hook`() {
        expectThat(marker) {
            not { exists() }
            get { marker.touch() }.exists()
        }
        startOnShutdown {
            marker.delete(false)
        }
    }
}

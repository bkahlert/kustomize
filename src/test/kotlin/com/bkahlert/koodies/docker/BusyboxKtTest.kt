package com.bkahlert.koodies.docker

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.process.process
import com.bkahlert.koodies.test.junit.JUnit
import com.bkahlert.koodies.test.junit.uniqueId
import com.imgcstmzr.util.DockerRequiring
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse

@Execution(CONCURRENT)
class BusyboxKtTest {

    @DockerRequiring @Test
    fun `should start busybox`() {
        val processed = mutableListOf<IO>()
        val dockerProcess: DockerProcess = Docker.busybox(JUnit.uniqueId, "echo busybox").execute().process { io ->
            processed.add(io)
        }

        expect {
            that(dockerProcess.waitFor()).isEqualTo(0)
            that(dockerProcess.alive).isFalse()
            that(processed).containsExactly(IO.Type.OUT typed "busybox")
        }
    }
}

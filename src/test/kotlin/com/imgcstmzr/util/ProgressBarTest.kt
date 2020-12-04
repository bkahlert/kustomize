package com.imgcstmzr.util

import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.concurrent.process.Processes.startShellScript
import com.bkahlert.koodies.test.junit.Slow
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.Isolated
import java.nio.file.Path

@Isolated
@Execution(CONCURRENT)
class ProgressBarTest {

    @Slow
    @Test
    fun `should show progress bar`(logger: InMemoryLogger<*>) {
        startShellScript(
            workingDirectory = Path.of("").toAbsolutePath().resolve("src/main/resources"),
            processor = { if (it.type == OUT) println(System.out) }) { !"progressbar.sh" }
        (0..100).forEach {
            logger.logLine { "\u0013" + "X".repeat(it % 10) }
            Thread.sleep(50)
        }
    }
}


fun main() {
}

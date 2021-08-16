package com.bkahlert.kommons.docker

import com.bkahlert.kommons.docker.DockerRunCommandLine.Options
import com.bkahlert.kommons.docker.TestImages.Ubuntu
import com.bkahlert.kommons.exec.CommandLine
import com.bkahlert.kommons.exec.IO
import com.bkahlert.kommons.exec.Processor
import com.bkahlert.kommons.exec.Processors.spanningProcessor
import com.bkahlert.kommons.exec.alive
import com.bkahlert.kommons.junit.UniqueId
import com.bkahlert.kommons.time.poll
import com.bkahlert.kommons.time.seconds
import com.bkahlert.kommons.unit.milli
import org.junit.jupiter.api.fail
import java.nio.file.Path
import kotlin.time.Duration

fun Path.createExec(
    uniqueId: UniqueId,
    command: String,
    vararg args: String,
    processor: Processor<DockerExec> = spanningProcessor(),
): DockerExec =
    CommandLine(command, *args)
        .dockerized(Ubuntu, Options(name = DockerContainer.from("$uniqueId")))
        .exec.processing(workingDirectory = this, processor = processor)

private fun DockerExec.waitForCondition(
    errorMessage: String,
    atMost: Duration = 4.seconds,
    test: DockerExec.() -> Boolean,
) {
    poll {
        test()
    }.every(100.milli.seconds).forAtMost(atMost) {
        fail(errorMessage)
    }
}

private fun DockerExec.waitForOutputOrFail(
    errorMessage: String,
    stillRunningErrorMessage: String = errorMessage,
    test: List<IO>.() -> Boolean,
) {
    poll {
        io.toList().test()
    }.every(100.milli.seconds).forAtMost(8.seconds) {
        if (alive) fail(stillRunningErrorMessage)
        fail(errorMessage)
    }
}

package koodies.docker

import koodies.docker.DockerRunCommandLine.Options
import koodies.docker.TestImages.Ubuntu
import koodies.exec.CommandLine
import koodies.exec.IO
import koodies.exec.Processor
import koodies.exec.Processors.spanningProcessor
import koodies.exec.alive
import koodies.junit.UniqueId
import koodies.time.poll
import koodies.time.seconds
import koodies.unit.milli
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

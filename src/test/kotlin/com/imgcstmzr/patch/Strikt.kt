package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.resolveOnHost
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystemProcess
import com.imgcstmzr.runtime.Program
import com.imgcstmzr.runtime.execute
import koodies.docker.Docker
import koodies.docker.DockerContainerName.Companion.toContainerName
import koodies.docker.DockerContainerName.Companion.toUniqueContainerName
import koodies.logging.BlockRenderingLogger
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.assertions.isEqualTo
import java.nio.file.Path

inline fun Assertion.Builder<OperatingSystemImage>.booted(
    logger: BlockRenderingLogger,
    crossinline assertion: OperatingSystemProcess.(String) -> ((String) -> Boolean)?,
): Assertion.Builder<OperatingSystemImage> {
    get("booted ${this.get { operatingSystem }}") {

        val verificationStep: OperatingSystemProcess.(String) -> String? = { output: String ->
            val asserter: ((String) -> Boolean)? = this.assertion(output)
            if (asserter != null) {
                val successfullyTested = asserter(output)
                if (!successfullyTested) "..."
                else null
            } else {
                null
            }
        }

        val containerName = file.toUniqueContainerName().sanitized
        kotlin.runCatching {
            execute(
                containerName,
                logger,
                true,
                true,
                Program("test", { "testing" }, "testing" to verificationStep),//.logging(),
            )
        }.onFailure { Docker.remove(containerName, forcibly = true) }.getOrThrow()
    }

    return this
}

/**
 * Assertions on the directory used to share files with the [OperatingSystemImage].
 */
fun Assertion.Builder<OperatingSystemImage>.getShared(path: Path) =
    get("shared directory %s") { resolveOnHost(path) }

fun Assertion.Builder<OperatingSystemImage>.booted(
    logger: BlockRenderingLogger,
    program: Program,
): Assertion.Builder<OperatingSystemImage> =
    compose("booted ${this.get { operatingSystem }}") {
        get {
            execute("Assertion Boot of $this".toContainerName().sanitized, logger, true, true, program)
        }.isEqualTo(0)
    } then {
        if (allPassed) pass() else fail("Program ${program.name} did not return successfully")
    }


inline fun <reified T : OperatingSystemProcess> Assertion.Builder<T>.command(input: String): DescribeableBuilder<String?> = get("running $input") {
    enter(input)
    readLine()
}

interface RunningOSX

val <T : RunningOSX> Assertion.Builder<T>.command: Assertion.Builder<T>
    get() = get { this }

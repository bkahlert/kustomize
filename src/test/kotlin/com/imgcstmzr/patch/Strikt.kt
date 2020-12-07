package com.imgcstmzr.patch

import com.bkahlert.koodies.docker.DockerContainerName.Companion.toContainerName
import com.bkahlert.koodies.docker.DockerContainerName.Companion.toUniqueContainerName
import com.imgcstmzr.guestfish.Guestfish
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystemProcess
import com.imgcstmzr.runtime.Program
import com.imgcstmzr.runtime.execute
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.util.asRootFor
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.assertions.isEqualTo
import java.nio.file.Path

fun Assertion.Builder<Guestfish>.path(guestPath: String): Assertion.Builder<Path> = path(Path.of(guestPath))

fun Assertion.Builder<Guestfish>.path(guestPath: Path): Assertion.Builder<Path> = get("running $guestPath") {
    run(Guestfish.copyOutCommands(listOf(guestPath)))

    val root = guestRootOnHost
    root.asRootFor(guestPath)
}

inline fun Assertion.Builder<OperatingSystemImage>.mounted(
    logger: BlockRenderingLogger<Any>,
    crossinline assertion: Assertion.Builder<Guestfish>.() -> Unit,
): Assertion.Builder<OperatingSystemImage> {
    get("mounted") {
        get { Guestfish(this, logger) }.apply(assertion)
    }
    return this
}

inline fun Assertion.Builder<OperatingSystemImage>.booted(
    logger: BlockRenderingLogger<Any>,
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

        execute(
            file.toUniqueContainerName().sanitized,
            logger,
            true,
            Program("test", { "testing" }, "testing" to verificationStep),//.logging(),
        )
    }

    return this
}

fun Assertion.Builder<OperatingSystemImage>.booted(
    logger: BlockRenderingLogger<Any>,
    program: Program,
): Assertion.Builder<OperatingSystemImage> =
    compose("booted ${this.get { operatingSystem }}") {
        get {
            execute("Assertion Boot of $this".toContainerName().sanitized, logger, true, program)
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

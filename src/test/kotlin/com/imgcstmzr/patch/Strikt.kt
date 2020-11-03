package com.imgcstmzr.patch

import com.imgcstmzr.process.Guestfish
import com.imgcstmzr.runtime.ArmRunner
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.Program
import com.imgcstmzr.runtime.RunningOperatingSystem
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.util.asRootFor
import com.imgcstmzr.util.quoted
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
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

fun Assertion.Builder<String>.isEqualTo(expected: String) =
    assert("is equal to ${expected.quoted}") {
        val actual = it
        when (actual == expected) {
            true -> pass()
            else -> fail()
        }
    }

inline fun <reified T : OperatingSystem> Assertion.Builder<OperatingSystemImage>.booted(
    logger: BlockRenderingLogger<Any>,
    crossinline assertion: RunningOperatingSystem.(String) -> ((String) -> Boolean)?,
): Assertion.Builder<OperatingSystemImage> {
    val os = T::class.objectInstance ?: error("Invalid OS")
    get("booted ${this.get { operatingSystem }}") {

        val verificationStep: RunningOperatingSystem.(String) -> String? = { output: String ->
            val asserter: ((String) -> Boolean)? = this.assertion(output)
            if (asserter != null) {
                val successfullyTested = asserter(output)
                if (!successfullyTested) "..."
                else null
            } else {
                null
            }
        }

        ArmRunner.run(
            name = "Assertion Boot of $this",
            osImage = this,
            logger = logger,
            programs = arrayOf(
                Program("test", verificationStep, "testing" to verificationStep),//.logging(),
            ))
    }

    return this
}


inline fun <reified T : RunningOperatingSystem> Assertion.Builder<T>.command(input: String): DescribeableBuilder<String?> = get("running $input") {
    enter(input)
    readLine()
}

interface RunningOSX

val <T : RunningOSX> Assertion.Builder<T>.command: Assertion.Builder<T>
    get() = get { this }

package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.libguestfs.Libguestfs.Companion.hostPath
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystemProcess
import com.imgcstmzr.runtime.Program
import com.imgcstmzr.runtime.execute
import koodies.concurrent.output
import koodies.docker.Docker
import koodies.docker.DockerContainerName.Companion.toContainerName
import koodies.docker.DockerContainerName.Companion.toUniqueContainerName
import koodies.logging.BlockRenderingLogger
import koodies.regex.groupValue
import koodies.shell.ShellScript
import koodies.text.Unicode
import koodies.text.repeat
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.assertions.isEqualTo
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.MULTILINE

inline fun Assertion.Builder<OperatingSystemImage>.booted(
    logger: BlockRenderingLogger,
    crossinline assertion: OperatingSystemProcess.(String) -> ((String) -> Boolean)?,
): Assertion.Builder<OperatingSystemImage> {
    get("booted ${this.get { operatingSystem }}") {

        val verificationStep: OperatingSystemProcess.(String) -> String? = { output: String ->
            val asserter: ((String) -> Boolean)? = this.assertion(output)
            if (asserter != null) {
                val successfullyTested = asserter(output)
                if (!successfullyTested) " …"
                else null
            } else {
                null
            }
        }

        val containerName = file.toUniqueContainerName().sanitized
        kotlin.runCatching {
            execute(
                name = containerName,
                logger = logger,
                autoLogin = true,
                autoShutdown = true,
                bordered = true,
                Program("test", { "testing" }, "testing" to verificationStep)//.logging(),
            )
        }.onFailure { Docker.remove(containerName, forcibly = true) }.getOrThrow()
    }

    return this
}

/**
 * Runs the specified [shellScript] in the currently running [OperatingSystemProcess]
 * and wraps the output (roughly, not exactly) with the given [outputMarker].
 *
 * Returns
 */
inline fun OperatingSystemProcess.script(
    outputMarker: String = Unicode.zeroWidthSpace.repeat(5),
    crossinline shellScript: ShellScript.() -> Unit,
): Sequence<String> {
    val snippet = ShellScript {
        embed(ShellScript {
            !"printf '$outputMarker'"
            shellScript()
            !"printf '$outputMarker'"
        })
    }.build()
    command(snippet)
    return Sequence {
        val regex = Regex(Regex.escape(outputMarker) + "(?<captured>.*)" + Regex.escape(outputMarker), setOf(DOT_MATCHES_ALL, MULTILINE))
        regex.findAll(output()).mapNotNull { it.groupValue("captured") }.iterator()
    }
}

/**
 * Assertions on the directory used to share files with the [OperatingSystemImage].
 */
fun Assertion.Builder<OperatingSystemImage>.hostPath(path: String) =
    get("shared directory %s") { this.hostPath(DiskPath(path)) }

fun Assertion.Builder<OperatingSystemImage>.booted(
    logger: BlockRenderingLogger,
    program: Program,
): Assertion.Builder<OperatingSystemImage> =
    compose("booted ${this.get { operatingSystem }}") {
        get {
            execute("Assertion Boot of $this".toContainerName().sanitized, logger, true, true, false, program)
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

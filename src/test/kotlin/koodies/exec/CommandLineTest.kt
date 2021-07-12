package koodies.exec

import koodies.io.Locations
import koodies.time.seconds
import koodies.time.sleep
import koodies.unit.milli
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.assertions.isEqualTo


val <T : CharSequence> Assertion.Builder<T>.continuationsRemoved: DescribeableBuilder<String>
    get() = get("continuation removed %s") { replace("\\s+\\\\.".toRegex(RegexOption.DOT_MATCHES_ALL), " ") }

val Assertion.Builder<CommandLine>.evaluated: Assertion.Builder<Exec>
    get() = get("evaluated %s") {
        toExec(false, emptyMap(), Locations.Temp, null)
            .process(ProcessingMode { sync }, Processors.spanningProcessor())
    }

fun Assertion.Builder<CommandLine>.evaluated(block: Assertion.Builder<Exec>.() -> Unit) =
    evaluated.block()

val Assertion.Builder<Exec>.output
    get() = get("output of type IO.Output %s") { io.output.ansiRemoved }

val <P : Exec> Assertion.Builder<P>.exitCodeOrNull
    get() = get("exit value %s") { exitCodeOrNull }

fun Assertion.Builder<CommandLine>.evaluatesTo(expectedOutput: String) {
    with(evaluated) {
        io.output.ansiRemoved.isEqualTo(expectedOutput)
        50.milli.seconds.sleep()
    }
}

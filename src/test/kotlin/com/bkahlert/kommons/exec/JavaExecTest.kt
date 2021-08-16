package com.bkahlert.kommons.exec

import com.bkahlert.kommons.exception.rootCause
import com.bkahlert.kommons.exec.IO.Error
import com.bkahlert.kommons.exec.IO.Output
import com.bkahlert.kommons.exec.Process.ExitState
import com.bkahlert.kommons.exec.Process.State.Excepted
import com.bkahlert.kommons.exec.Process.State.Exited.Failed
import com.bkahlert.kommons.exec.Process.State.Running
import com.bkahlert.kommons.time.Now
import com.bkahlert.kommons.time.poll
import com.bkahlert.kommons.time.seconds
import com.bkahlert.kommons.unit.milli
import strikt.api.Assertion.Builder
import strikt.api.DescribeableBuilder
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isNotNull
import java.nio.file.Path
import java.time.Instant
import kotlin.time.Duration

val <T : Exec> Builder<T>.alive: Builder<T>
    get() = assert("is alive") { if (it.alive) pass() else fail("is not alive: ${it.io}") }

val <T : Exec> Builder<T>.workingDirectory: Builder<Path?>
    get() = get("working directory") { workingDirectory }

val <T : Exec> Builder<T>.commandLine: Builder<CommandLine>
    get() = get("command line") { commandLine }

val <T : Exec> Builder<T>.log: DescribeableBuilder<List<IO>> get() = get("log %s") { io.toList() }

private fun Builder<Exec>.completesWithIO() = log.logs(Output typed "test out", Error typed "test err")

val <T : Exec> Builder<T>.io: DescribeableBuilder<List<IO>>
    get() = get("logged IO") { io.toList() }

val Builder<List<IO>>.output: DescribeableBuilder<List<Output>>
    get() = get("out") { filterIsInstance<Output>() }

val Builder<List<IO>>.error: DescribeableBuilder<List<Error>>
    get() = get("err") { filterIsInstance<Error>() }

val Builder<out List<IO>>.ansiRemoved: DescribeableBuilder<String>
    get() = get("ANSI escape codes removed") { IOSequence(this).ansiRemoved }

val Builder<out List<IO>>.ansiKept: DescribeableBuilder<String>
    get() = get("ANSI escape codes kept") { IOSequence(this).ansiKept }

@JvmName("failureContainsDump")
fun <T : Failed> Builder<T>.containsDump(vararg containedStrings: String = emptyArray()) =
    with({ dump }) { isNotNull().and { containsDump(*containedStrings) } }

@JvmName("fatalContainsDump")
fun Builder<Excepted>.containsDump(vararg containedStrings: String = emptyArray()) =
    with({ dump }) { containsDump(*containedStrings) }

fun Builder<String>.containsDump(vararg containedStrings: String = arrayOf(".sh")) {
    compose("contains dump") {
        contains("dump has been written")
        containedStrings.forEach { contains(it) }
        contains(".log")
        contains(".ansi-removed.log")
    }.then { if (allPassed) pass() else fail() }
}


fun Builder<List<IO>>.logs(vararg io: IO) = logs(io.toList())
fun Builder<List<IO>>.logs(io: Collection<IO>) = logsWithin(io = io)
fun Builder<List<IO>>.logs(predicate: List<IO>.() -> Boolean) = logsWithin(predicate = predicate)

fun Builder<List<IO>>.logsWithin(timeFrame: Duration = 5.seconds, io: Collection<IO>) =
    assert("logs $io within $timeFrame") { ioLog ->
        when (poll {
            ioLog.toList().containsAll(io)
        }.every(100.milli.seconds).forAtMost(5.seconds)) {
            true -> pass()
            else -> fail("logged ${ioLog.toList()} instead")
        }
    }

fun Builder<List<IO>>.logsWithin(timeFrame: Duration = 5.seconds, predicate: List<IO>.() -> Boolean) =
    assert("logs within $timeFrame") { ioLog ->
        when (poll {
            ioLog.toList().predicate()
        }.every(100.milli.seconds).forAtMost(5.seconds)) {
            true -> pass()
            else -> fail("did not log within $timeFrame")
        }
    }

inline val <reified T : Process> Builder<T>.stopped: Builder<T>
    get() = get("with stop() called") { stop() }.isA()

inline val <reified T : Process> Builder<T>.killed: Builder<T>
    get() = get("with kill() called") { kill() }.isA()

inline fun <reified T : Process.State> Builder<out Process>.hasState(
    crossinline statusAssertion: Builder<T>.() -> Unit,
): Builder<out Process> =
    compose("state") {
        get { state }.isA<T>().statusAssertion()
    }.then { if (allPassed) pass() else fail() }

inline fun <reified T : Process.State> Builder<out Process>.hasState(
): Builder<out Process> =
    compose("state") {
        get { state }.isA<T>()
    }.then { if (allPassed) pass() else fail() }


inline val Builder<out Process.State>.start
    get(): Builder<Instant> = get("start") { start }
inline val Builder<out Process.State>.status
    get(): Builder<String> = get("status") { status }

inline val Builder<Running>.runningPid
    get(): Builder<Long> = get("pid") { pid }

inline val Builder<out ExitState>.end
    get(): Builder<Instant> = get("end") { end }
inline val Builder<out ExitState>.runtime
    get(): Builder<Duration> = get("runtime") { runtime }

inline val <T : ExitState> Builder<T>.pid
    get(): Builder<Long> = get("pid") { pid }
inline val <T : ExitState> Builder<T>.exitCode
    get(): Builder<Int> = get("exit code") { exitCode }
inline val <T : ExitState> Builder<T>.io
    get(): Builder<List<IO>> = get("io") { io.toList() }


inline val Builder<out Failed>.dump: Builder<String?>
    get(): Builder<String?> = get("dump") { dump }

inline val Builder<out Excepted>.exception: Builder<Throwable?>
    get(): Builder<Throwable?> = get("exception") { exception }

inline val Builder<out Excepted>.rootCause: Builder<Throwable?>
    get(): Builder<Throwable?> = get("root cause") { exception?.rootCause }


fun Builder<out ExitState>.io() =
    get("IO with kept ANSI escape codes") { io.ansiKept }


val Builder<Instant>.timePassed
    get() = get("time passed since now") { Now.passedSince(toEpochMilli()) }

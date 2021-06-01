package koodies.exec

import koodies.exec.Process.ExitState
import koodies.exec.Process.State.Exited.Failed
import koodies.exec.Process.State.Exited.Succeeded
import koodies.exec.Process.State.Running
import koodies.logging.InMemoryLogger
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.mapLines
import koodies.text.toStringMatchesCurlyPattern
import koodies.text.withoutPrefix
import strikt.api.Assertion.Builder
import strikt.assertions.isA


inline fun <reified T : Exec> Builder<T>.starts(): Builder<T> =
    assert("has started") {
        kotlin.runCatching { it.pid }.fold({
            if (it != 0L) pass()
            else fail(it, "non-ß PID expected")
        }, {
            fail(cause = it)
        })
    }

inline val <reified T : Exec> Builder<T>.exited: Builder<ExitState> get() = get("exited") { onExit.get() }.isA()
inline fun <reified T : Exec> Builder<T>.logsIO(curlyPattern: String): Builder<String> = exited.io().toStringMatchesCurlyPattern(curlyPattern)

@JvmName("logsIOInMemoryLogger")
inline fun <reified T : InMemoryLogger> Builder<T>.logsIO(ignorePrefix: String, curlyPattern: String, dropFirst: Int = 2, dropLast: Int = 1): Builder<String> =
    get { toString(null, false, dropFirst).lines().dropLast(dropLast).joinToString(LF) }.logsIO(ignorePrefix, curlyPattern)

@JvmName("logsIOString")
fun Builder<String>.logsIO(ignorePrefix: String, curlyPattern: String): Builder<String> =
    get { mapLines { it.withoutPrefix(ignorePrefix) } }.toStringMatchesCurlyPattern(curlyPattern)


inline fun <reified T : Exec> Builder<T>.succeeds(): Builder<Succeeded> = exited.isA()
inline fun <reified T : Exec> Builder<T>.logsSuccessfulIO(): Builder<String> = logsIO(successfulIO)

@JvmName("logsSuccessfulIOInMemoryLogger")
inline fun <reified T : InMemoryLogger> Builder<T>.logsSuccessfulIO(ignorePrefix: String = "· ", dropFirst: Int = 2): Builder<String> =
    logsIO(ignorePrefix, successfulIO, dropFirst)

@JvmName("logsSuccessfulIOString")
private fun Builder<String>.logsSuccessfulIO(ignorePrefix: String = "· "): Builder<String> =
    logsIO(ignorePrefix, "{{}}$LF$successfulIO$LF{{}}")

val successfulIO = """
    Executing printenv TEST_PROP
    TEST_VALUE
    Process {} terminated successfully at {}
""".trimIndent()

inline fun <reified T : Exec> Builder<T>.fails(): Builder<Failed> =
    exited.isA<Failed>().assert("unsuccessfully with non-zero exit code") {
        val actual = it.exitCode
        when (actual != 0) {
            true -> pass()
            else -> fail("completed successfully")
        }
    }

inline fun <reified T : Exec> Builder<T>.logsFailedIO(): Builder<String> = logsIO(failedIO) and { containsDump(containedStrings = emptyArray()) }

@JvmName("logsFailedIOInMemoryLogger")
inline fun <reified T : InMemoryLogger> Builder<T>.logsFailedIO(ignorePrefix: String = "· ", dropFirst: Int = 2): Builder<String> =
    logsIO(ignorePrefix, failedIO, dropFirst) and { containsDump(containedStrings = emptyArray()) }

@JvmName("logsFailedIOString")
private fun Builder<String>.logsFailedIO(ignorePrefix: String = "· "): Builder<String> =
    logsIO(ignorePrefix, "{{}}$LF$failedIO$LF{{}}") and { containsDump(containedStrings = emptyArray()) }

val failedIO = """
    Executing printenv TEST_PROP
    Process {} terminated with exit code 1
    {{}}
""".trimIndent()

inline val Builder<out Process>.state get() = get("exit state") { state }
inline fun <reified T : Exec> Builder<T>.runsSynchronously(): Builder<ExitState> = state.isA()
inline fun <reified T : Exec> Builder<T>.runsAsynchronously(): Builder<Running> = state.isA()


inline val <reified T : Process> Builder<T>.joined: Builder<T>
    get() = get("joined using waitFor") { also { waitFor() } }

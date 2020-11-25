package com.bkahlert.koodies.concurrent.process

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.endsWith
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.size
import strikt.assertions.startsWith

@Execution(CONCURRENT)
class LoggedProcessTest {

    private val loggedProcess = createLoggedProcess(0)

    @Test
    fun `should provide extend process`() {
        expectThat(loggedProcess) {
            get { pid() }.isGreaterThan(0)
            get { exitValue() }.isEqualTo(0)
        }
    }

    @Test
    fun `should provide log as string`() {
        expectThat(loggedProcess) {
            get { toString() }.startsWith("Executing ").endsWith("terminated successfully.")
        }
    }

    @Test
    fun `should provide log as components`() {
        val (io, meta, input, output, error) = loggedProcess

        expectThat(meta.lines()).size.isGreaterThan(0)
        expectThat(input.lines()).size.isGreaterThanOrEqualTo(0)
        expectThat(output.lines()).size.isGreaterThan(0)
        expectThat(error.lines()).size.isGreaterThan(0)
        expectThat(io).contains(meta.lines() + output.lines() + error.lines())
    }

    @Test
    fun `should provide log as properties`() {
        val (io, meta, input, output, error) = loggedProcess

        expectThat(loggedProcess) {
            get { all }.isEqualTo(io)
            get { meta }.isEqualTo(meta)
            get { input }.isEqualTo(input)
            get { output }.isEqualTo(output)
            get { error }.isEqualTo(error)
        }
    }

    private fun IO.nonEmptyLines() = lines().filter { isBlank }
}

fun createLoggedProcess(exitCode: Int): LoggedProcess = createCompletingLoggingProcess(exitCode).onExit().get() as LoggedProcess

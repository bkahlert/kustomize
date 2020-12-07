package com.imgcstmzr.guestfish

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.concurrent.process.ManagedProcess
import com.bkahlert.koodies.string.LineSeparators.lines
import com.bkahlert.koodies.test.junit.test
import com.imgcstmzr.util.MiscFixture
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThanOrEqualTo
import strikt.assertions.none

@Execution(CONCURRENT)
class GuestfishIoProcessorTest {
    private val guestfishIO = MiscFixture.BootingGuestfish.text

    @Nested
    inner class Verbosity {
        @Test
        fun `should log all on active verbose option`(logger: InMemoryLogger<ManagedProcess>) {
            val guestfishIoProcessor = GuestfishIoProcessor(logger, verbose = true)
            guestfishIO.lines().map { OUT typed it }.sendTo(guestfishIoProcessor)
            expectThat(logger.logged.lines()) {
                get { size }.isGreaterThan(950).isLessThanOrEqualTo(1050)
                get { take(20).joinToString("\n") }.contains("libguestfs: create: flags = 0, handle = 0x1b19580, program = guestfish")
            }
        }

        @Test
        fun `should not log boot sequence on inactive verbose option`(logger: InMemoryLogger<ManagedProcess>) {
            val guestfishIoProcessor = GuestfishIoProcessor(logger, verbose = false)
            guestfishIO.lines().map { OUT typed it }.sendTo(guestfishIoProcessor)
            expectThat(logger.logged.lines()) {
                get { size }.isGreaterThan(80).isLessThanOrEqualTo(120)
                get { take(10).joinToString("\n") }.contains("guestfsd: main_loop: new request, len 0x3c")
            }
        }

        @Test
        fun `should only log errors on inactive verbose option`(logger: InMemoryLogger<ManagedProcess>) {
            val guestfishIoProcessor = GuestfishIoProcessor(logger, verbose = false)
            IO.Type.values().map { type -> type typed "$type" }.sendTo(guestfishIoProcessor)
            expectThat(logger).get { logged }
                .contains("ERR").and {
                    not {
                        contains("META")
                        contains("IN")
                        contains("OUT")
                    }
                }
        }

        @Nested
        inner class SkippingLines {

            @TestFactory
            fun `should meta log skipped lines depending on verbosity`(logger: InMemoryLogger<ManagedProcess>) =
                listOf(true, false).test { verbose ->
                    val guestfishIoProcessor = GuestfishIoProcessor(logger, verbose = verbose)
                    guestfishIO.lines().map { IO.Type.META typed it }.sendTo(guestfishIoProcessor)
                    expectThat(logger.logged.lines()) {
                        if (verbose) none { contains("lines skipped") }
                        else any { contains("lines skipped") }
                    }
                }
        }
    }

    private fun List<IO>.sendTo(guestfishIoProcessor: (ManagedProcess, IO) -> Unit) {
        forEach { io -> guestfishIoProcessor.invoke(null as ManagedProcess, io) }
    }
}


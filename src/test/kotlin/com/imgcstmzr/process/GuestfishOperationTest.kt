package com.imgcstmzr.process

import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
class GuestfishOperationTest {
    @Test
    fun `should provide summary`() {
        expectThat(GuestfishOperation(arrayOf(
            "!ls -lisa",
            "!mkdir -p /work",
            "!mkdir -p /work/guestfish.shared/boot",
            "- copy-out /boot/cmdline.txt /work/guestfish.shared/boot",
            "!mkdir -p /work/guestfish.shared/non",
            "- copy-out /non/existing.txt /work/guestfish.shared/non",
        )).summary).matchesCurlyPattern("◀◀ ls…lisa  ◀ mkdir…work  ◀ …  ◀ copy…boot")
    }
}

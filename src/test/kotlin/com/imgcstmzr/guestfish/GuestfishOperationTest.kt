package com.imgcstmzr.guestfish

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
            "!mkdir -p /shared",
            "!mkdir -p /shared/guestfish.shared/boot",
            "- copy-out /boot/cmdline.txt /shared/guestfish.shared/boot",
            "!mkdir -p /shared/guestfish.shared/non",
            "- copy-out /non/existing.txt /shared/guestfish.shared/non",
        )).summary).matchesCurlyPattern("◀◀ ls…lisa  ◀ mkdir…shared  ◀ …  ◀ copy…boot")
    }
}

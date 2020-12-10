package com.imgcstmzr.patch

import com.bkahlert.koodies.nio.file.toPath
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption
import com.imgcstmzr.util.FixtureResolverExtension
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.matches
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.exists
import strikt.assertions.first
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class SshEnablementPatchTest {

    @Test
    fun `should create ssh file`(logger: InMemoryLogger) {
        val root = FixtureResolverExtension.prepareSharedDirectory().also { expectThat(it).get { resolve("boot/ssh") }.not { exists() } }
        val sshPatch = SshEnablementPatch()
        expectThat(sshPatch).matches(customizationOptionsAssertion = {
            first().isEqualTo(VirtCustomizeCustomizationOption.TouchOption("/boot/ssh".toPath()))
        })
    }
}

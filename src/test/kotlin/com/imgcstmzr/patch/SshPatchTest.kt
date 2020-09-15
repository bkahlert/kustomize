package com.imgcstmzr.patch

import com.imgcstmzr.util.FixtureExtension
import com.imgcstmzr.util.exists
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.exists

@Execution(ExecutionMode.CONCURRENT)
@Suppress("RedundantInnerClassModifier")
internal class SshPatchTest {

    @Test
    internal fun `should create ssh file`() {
        val root = FixtureExtension.prepareSharedDirectory().also { expectThat(it).get { resolve("boot/ssh") }.not { exists() } }
        val sshPatch = SshPatch()

        sshPatch(root)

        expectThat(root).get { resolve("boot/ssh").exists }
    }
}

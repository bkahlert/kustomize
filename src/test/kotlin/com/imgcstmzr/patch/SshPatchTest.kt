package com.imgcstmzr.patch

import com.imgcstmzr.util.FixtureResolverExtension
import com.imgcstmzr.util.exists
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.matches
import com.imgcstmzr.util.single
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.exists

@Execution(ExecutionMode.CONCURRENT)
internal class SshPatchTest {

    @Test
    internal fun `should create ssh file`(logger: InMemoryLogger<Any>) {
        val root = FixtureResolverExtension.prepareSharedDirectory().also { expectThat(it).get { resolve("boot/ssh") }.not { exists() } }
        val sshPatch = SshPatch()
        expectThat(sshPatch).matches(fileSystemOperationsAssertion = {
            single {
                assert("creates ssh file in boot directory") { op: PathOperation ->
                    op(root, logger)
                    expectThat(root).get { resolve("boot/ssh").exists }
                }
            }
        })
    }
}

package com.imgcstmzr.patch

import com.bkahlert.koodies.nio.file.delete
import com.bkahlert.koodies.nio.file.listRecursively
import com.bkahlert.koodies.nio.file.writeText
import com.bkahlert.koodies.test.junit.FifteenMinutesTimeout
import com.imgcstmzr.E2E
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.util.FixtureResolverExtension.Companion.prepareSharedDirectory
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.asRootFor
import com.imgcstmzr.util.containsContent
import com.imgcstmzr.util.hasContent
import com.imgcstmzr.util.isFile
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.matches
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isSuccess
import strikt.assertions.none

@Execution(CONCURRENT)
class UsernamePatchTest {

    @Test
    fun `should not do anything but patch 6 password files`() {
        expectThat(UsernamePatch("pi", "ella")).matches(fileSystemOperationsAssertion = { hasSize(6) })
    }

    @Test
    fun `should clean all files`(logger: InMemoryLogger) {
        val root = prepareSharedDirectory()
        val patch = UsernamePatch("pi", "ella")

        patch.fileSystemOperations.onEach { op ->
            op(root.asRootFor(op.target), logger)
        }

        expectThat(root.listRecursively().filter {
            it.isFile && it.parent.fileName.toString() == "etc"
        }.toList()) {
            all { containsContent("ella:") }
        }
    }

    @Test
    fun `should finish if files are missing`(logger: InMemoryLogger) {
        val root = prepareSharedDirectory().apply {
            resolve("etc").delete(true)
        }
        val patch = UsernamePatch("pi", "ella")
        expectCatching {
            patch.fileSystemOperations.onEach { op ->
                op(root.asRootFor(op.target), logger)
            }
        }.isSuccess()
    }

    @Test
    fun `should not touch other files`(logger: InMemoryLogger) {
        val root = prepareSharedDirectory().apply { resolve("dont-touch-me").writeText("pi\npi\n") }
        val patch = UsernamePatch("pi", "ella")

        patch.fileSystemOperations.onEach { op ->
            op(root.asRootFor(op.target), logger)
        }

        expectThat(root.resolve("dont-touch-me")).hasContent("pi\npi\n")
    }

    @Test
    fun `should not pull a single word apart`(logger: InMemoryLogger) {
        val root = prepareSharedDirectory()
        val patch = UsernamePatch("pi", "ella")

        patch.fileSystemOperations.onEach { op ->
            op(root.asRootFor(op.target), logger)
        }

        @Suppress("SpellCheckingInspection")
        expectThat(root.listRecursively().filter {
            it.isFile && it.parent.fileName.toString() == "etc"
        }.toList()) {
            all { containsContent("dietpi:") }
            none { containsContent("dietella:") }
        }
    }

    @FifteenMinutesTimeout @E2E @Test
    fun `should log in with updated username`(@OS(RaspberryPiLite) osImage: OperatingSystemImage, logger: InMemoryLogger) {
        val newUsername = "ella".also { check(it != osImage.defaultCredentials.username) { "$it is already the default username." } }
        val patch = UsernamePatch(osImage.defaultCredentials.username, newUsername)

        patch.patch(osImage, logger)

        expectThat(osImage.credentials).isEqualTo(OperatingSystem.Credentials(newUsername, osImage.defaultCredentials.password))
        expectThat(osImage).booted(logger) {
            command("echo 'Hi $newUsername 👋'");
            { true }
        }
    }
}

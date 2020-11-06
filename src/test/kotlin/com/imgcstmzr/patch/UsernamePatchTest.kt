package com.imgcstmzr.patch

import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.util.DockerRequired
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.asRootFor
import com.imgcstmzr.util.containsContent
import com.imgcstmzr.util.delete
import com.imgcstmzr.util.hasContent
import com.imgcstmzr.util.isFile
import com.imgcstmzr.util.listFilesRecursively
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.matches
import com.imgcstmzr.util.mkdirs
import com.imgcstmzr.util.writeText
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isSuccess
import strikt.assertions.none
import java.nio.file.Path

@Execution(ExecutionMode.CONCURRENT)
class UsernamePatchTest {

    @Test
    fun `should not do anything but patch 6 password files`() {
        expectThat(UsernamePatch("pi", "ella")).matches(fileSystemOperationsAssertion = { hasSize(6) })
    }

    @Test
    fun `should clean all files`(logger: InMemoryLogger<Any>) {
        val root = prepare()
        val patch = UsernamePatch("pi", "ella")

        patch.fileSystemOperations.onEach { op ->
            op(root.asRootFor(op.target), logger)
        }

        expectThat(root.listFilesRecursively(Path::isFile)) {
            all { containsContent("ella:") }
        }
    }

    @Test
    fun `should finish if files are missing`(logger: InMemoryLogger<Any>) {
        val root = prepare().also { it.resolve("etc").delete() }
        val patch = UsernamePatch("pi", "ella")
        expectCatching {
            patch.fileSystemOperations.onEach { op ->
                op(root.asRootFor(op.target), logger)
            }
        }.isSuccess()
    }

    @Test
    fun `should not touch other files`(logger: InMemoryLogger<Any>) {
        val root = prepare().also { it.resolve("dont-touch-me").writeText("pi\npi\n") }
        val patch = UsernamePatch("pi", "ella")

        patch.fileSystemOperations.onEach { op ->
            op(root.asRootFor(op.target), logger)
        }

        expectThat(root.resolve("dont-touch-me")).hasContent("pi\npi\n")
    }

    @Test
    fun `should not pull a single word apart`(logger: InMemoryLogger<Any>) {
        val root = prepare()
        val patch = UsernamePatch("pi", "ella")

        patch.fileSystemOperations.onEach { op ->
            val target = root.asRootFor(op.target)
            op(root.asRootFor(op.target), logger)
        }

        expectThat(root.listFilesRecursively(Path::isFile)) {
            all { containsContent("dietpi:") }
            none { containsContent("dietella:") }
        }
    }

    @DockerRequired
    @Test
    fun `should update log in was updated username`(@OS(RaspberryPiLite::class) osImage: OperatingSystemImage, logger: InMemoryLogger<Any>) {
        val newUsername = "ella".also { check(it != osImage.defaultUsername) { "$it is already the default username." } }
        val patch = UsernamePatch(osImage.defaultUsername, newUsername)

        patch.patch(osImage, logger)

        expectThat(osImage.credentials).isEqualTo(OperatingSystems.Companion.Credentials(newUsername, osImage.defaultPassword))
        expectThat(osImage).booted(logger) {
            command("echo 'Hi $newUsername 👋'");
            { true }
        }
    }
}

@Suppress("SpellCheckingInspection")
private fun prepare(): Path = createTempDir("username-patch").toPath().also {
    listOf("passwd", "shadow").onEach { file ->
        it.resolve("etc").mkdirs().resolve(file).writeText("""
        root:2ed1Qf2siSjhEu82LZEsIh1CmRFpPjMp469cyHWENm/9b7pLkG.0atJwpVn5aQio3pFpmA0odPs4N4D5wYeP//:18421:0:99999:7:::
        daemon:*:18409:0:99999:7:::
        bin:*:18409:0:99999:7:::
        pi:*:18409:0:99999:7:::
        systemd-resolve:*:18409:0:99999:7:::
        _apt:*:18409:0:99999:7:::
        messagebus:*:18409:0:99999:7:::
        _rpc:*:18409:0:99999:7:::
        statd:*:18409:0:99999:7:::
        systemd-coredump:!!:18421::::::
        dietpi:dietpi:18421:0:99999:7:::
    """.trimIndent())
    }
    listOf("group", "gshadow").onEach { file ->
        it.resolve("etc").mkdirs().resolve(file).writeText("""
        generalpi:!!:pi:dietpi,other
        generaldietpi:!!:dietpi:pi,other
        generalother:!!:other:pi,dietpi
    """.trimIndent())
    }
    listOf("subuid", "subgid").onEach { file ->
        it.resolve("etc").mkdirs().resolve(file).writeText("""
        pi:165537:65537
        dietpi:165536:65536
        other:165535:65535
    """.trimIndent())
    }
}

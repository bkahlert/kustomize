package com.imgcstmzr.patch

import com.imgcstmzr.util.FixtureExtension
import com.imgcstmzr.util.containsContent
import com.imgcstmzr.util.delete
import com.imgcstmzr.util.hasContent
import com.imgcstmzr.util.isFile
import com.imgcstmzr.util.listFilesRecursively
import com.imgcstmzr.util.mkdirs
import com.imgcstmzr.util.writeText
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.isSuccess
import strikt.assertions.none
import java.nio.file.Path

@Execution(ExecutionMode.CONCURRENT)
@Suppress("RedundantInnerClassModifier")
@ExtendWith(FixtureExtension::class)
internal class UsernamePatchTest {

    @Test
    internal fun `should clean all files`() {
        val root = prepare()
        val patch = UsernamePatch("pi", "ella")

        patch(root)

        expectThat(root.listFilesRecursively(Path::isFile)) {
            all { containsContent("ella:") }
        }
    }

    @Test
    internal fun `should finish if files are missing`() {
        val root = prepare().also { it.resolve("etc").delete() }
        val patch = UsernamePatch("pi", "ella")

        expectCatching { patch(root) }.isSuccess()
    }

    @Test
    internal fun `should not touch other files`() {
        val root = prepare().also { it.resolve("dont-touch-me").writeText("pi\npi\n") }
        val patch = UsernamePatch("pi", "ella")

        patch(root)

        expectThat(root.resolve("dont-touch-me")).hasContent("pi\npi\n")
    }

    @Test
    internal fun `should not pull a single word apart`() {
        val root = prepare()
        val patch = UsernamePatch("pi", "ella")

        patch(root)

        expectThat(root.listFilesRecursively(Path::isFile)) {
            all { containsContent("dietpi:") }
            none { containsContent("dietella:") }
        }
    }
}

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

package com.bkahlert.koodies.io

import com.bkahlert.koodies.io.TarArchiver.tar
import com.bkahlert.koodies.io.TarArchiver.untar
import com.bkahlert.koodies.nio.ClassPath
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.bkahlert.koodies.unit.size
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.copyTo
import com.imgcstmzr.util.delete
import com.imgcstmzr.util.hasSameFiles
import com.imgcstmzr.util.mkdirs
import com.imgcstmzr.util.removeExtension
import com.imgcstmzr.util.renameTo
import com.imgcstmzr.util.touch
import com.imgcstmzr.util.writeText
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThan

@Execution(CONCURRENT)
internal class TarArchiverTest {
    @ConcurrentTestFactory
    internal fun `should throw on missing source`() = listOf(
        { Paths.tempDir().also { it.delete() }.tar() },
        { Paths.tempFile(extension = ".tar").also { it.delete() }.untar() },
    ).map { call ->
        DynamicTest.dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @ConcurrentTestFactory
    internal fun `should throw on non-empty destination`() = listOf(
        { Paths.tempDir().also { it.removeExtension("tar").touch().writeText("content") }.tar() },
        { Paths.tempFile(extension = ".tar").also { it.copyTo(it.removeExtension("tar").mkdirs().resolve(it.fileName)) }.untar() },
    ).map { call ->
        DynamicTest.dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @Test
    internal fun `should tar and untar`() {
        val dir = Paths.tempDir()
            .also { ClassPath("example.html").copyTo(it.resolve("example.html")) }
            .also { ClassPath("config.txt").copyTo(it.resolve("sub-dir/config.txt")) }

        val archivedDir = dir.tar()
        expectThat(archivedDir.size).isGreaterThan(dir.size)

        val renamedDir = dir.renameTo("${dir.fileName}-renamed")

        val unarchivedDir = archivedDir.untar()
        expectThat(unarchivedDir).hasSameFiles(renamedDir)
    }
}

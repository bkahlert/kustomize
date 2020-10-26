package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.nio.ClassPath
import com.imgcstmzr.util.Paths
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isFailure
import java.nio.file.NotDirectoryException

@Execution(CONCURRENT)
internal class ListKtTest {
    @Test
    internal fun `should only list entries in current directory`() {
        val dir = Paths.tempDir()
            .also { ClassPath("example.html").copyTo(it.resolve("example.html")) }
            .also { ClassPath("config.txt").copyTo(it.resolve("sub-dir/config.txt")) }
        expectThat(dir.list().toList()).containsExactly(dir.resolve("sub-dir"), dir.resolve("example.html"))
    }

    @Test
    internal fun `should throw on listing file`() {
        expectCatching { Paths.tempFile().list() }.isFailure().isA<NotDirectoryException>()
    }
}

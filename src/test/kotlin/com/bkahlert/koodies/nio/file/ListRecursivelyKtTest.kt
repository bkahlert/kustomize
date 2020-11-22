package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.io.PathFixtures.directoryWithTwoFiles
import com.imgcstmzr.util.FixtureLog.deleteOnExit
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
class ListRecursivelyKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should list all entries recursively`() {
        val dir = tempDir.directoryWithTwoFiles()
        expectThat(dir.listRecursively().toList()).containsExactly(
            dir.resolve("sub-dir"),
            dir.resolve("sub-dir/config.txt"),
            dir.resolve("example.html"))
    }

    @Test
    fun `should throw on listing file`() {
        expectCatching { tempDir.tempFile().listRecursively() }.isFailure().isA<NotDirectoryException>()
    }
}


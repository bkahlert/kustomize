package com.imgcstmzr.util

import com.imgcstmzr.test.Slow
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.withTempDir
import koodies.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.size
import kotlin.io.path.exists

@Execution(CONCURRENT)
class GitHubTest {

    @Test
    fun InMemoryLogger.`should resolve latest tag`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(GitHub.repo("bkahlert/koodies").latestTag).isEqualTo("v.1.2.3")
    }

    @Slow @Test
    fun InMemoryLogger.`should download release`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val file = GitHub.repo("bkahlert/koodies").downloadRelease("v.1.2.3")
        expectThat(file) {
            exists()
            size.isEqualTo(2_216_941L)
        }
    }
}


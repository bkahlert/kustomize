package com.imgcstmzr.util

import com.imgcstmzr.test.Slow
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.withTempDir
import koodies.io.path.getSize
import koodies.logging.InMemoryLogger
import koodies.unit.Mega
import koodies.unit.bytes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isGreaterThan
import strikt.assertions.matchesIgnoringCase
import kotlin.io.path.exists

@Execution(CONCURRENT)
class GitHubTest {

    @Test
    fun InMemoryLogger.`should resolve latest tag`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(GitHub.repo("bkahlert/koodies").latestTag).matchesIgnoringCase(Regex("v\\.?(?:\\d+)(?:\\.\\d+)*"))
    }

    @Slow @Test
    fun InMemoryLogger.`should download release`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val release = GitHub.repo("bkahlert/koodies").latestTag
        val file = GitHub.repo("bkahlert/koodies").downloadRelease(release)
        expectThat(file) {
            exists()
            get { getSize() }.isGreaterThan(1.Mega.bytes)
        }
    }
}


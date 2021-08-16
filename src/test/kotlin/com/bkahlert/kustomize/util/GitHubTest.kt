package com.bkahlert.kustomize.util

import com.bkahlert.kommons.io.path.getSize
import com.bkahlert.kommons.junit.UniqueId
import com.bkahlert.kommons.test.Slow
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.unit.Mega
import com.bkahlert.kommons.unit.bytes
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isGreaterThan
import strikt.assertions.matchesIgnoringCase
import kotlin.io.path.exists

class GitHubTest {

    @Slow @Test
    fun `should resolve latest tag`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(GitHub.repo("bkahlert/kommons").latestTag).matchesIgnoringCase(Regex("v\\.?\\d+(?:\\.\\d+)*"))
    }

    @Slow @Test
    fun `should download release`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val release = GitHub.repo("bkahlert/kommons").latestTag
        val file = GitHub.repo("bkahlert/kommons").downloadRelease(release)
        expectThat(file) {
            exists()
            get { getSize() }.isGreaterThan(1.Mega.bytes)
        }
    }
}

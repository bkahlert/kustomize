package com.bkahlert.kustomize.util

import koodies.io.path.getSize
import koodies.junit.UniqueId
import koodies.test.Slow
import koodies.test.withTempDir
import koodies.unit.Mega
import koodies.unit.bytes
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isGreaterThan
import strikt.assertions.matchesIgnoringCase
import kotlin.io.path.exists

class GitHubTest {

    @Slow @Test
    fun `should resolve latest tag`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(GitHub.repo("bkahlert/koodies").latestTag).matchesIgnoringCase(Regex("v\\.?\\d+(?:\\.\\d+)*"))
    }

    @Slow @Test
    fun `should download release`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val release = GitHub.repo("bkahlert/koodies").latestTag
        val file = GitHub.repo("bkahlert/koodies").downloadRelease(release)
        expectThat(file) {
            exists()
            get { getSize() }.isGreaterThan(1.Mega.bytes)
        }
    }
}

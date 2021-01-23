package com.imgcstmzr.util

import koodies.concurrent.output
import koodies.ext.concurrent.script
import koodies.ext.docker.docker
import koodies.io.path.Locations
import koodies.io.path.randomDirectory
import koodies.logging.RenderingLogger
import koodies.unit.size
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries

class LoggingGitHub private constructor(private val logger: RenderingLogger?) {

    fun repo(repo: String) = Repository(repo)

    inner class Repository(val repo: String) {
        val latestTag: String get() = script(logger) { !"curl --silent \"https://api.github.com/repos/$repo/releases/latest\" | jq -r .tag_name" }.output()

        fun downloadRelease(release: String = "latest", downloadDirectory: Path = Locations.Temp) =
            downloadDirectory.randomDirectory().run {
                docker({ "zero88" / "ghrd" }, {
                    name { "download-$release-$repo" }
                    autoCleanup { true }
                    mounts { this@run mountAt "/app" }
                }, "--regex", repo)
                listDirectoryEntries().sortedBy { it.size }.first()
            }
    }

    companion object {
        operator fun invoke(logger: RenderingLogger?) = LoggingGitHub(logger)
    }
}

val RenderingLogger.GitHub get() = LoggingGitHub(this)
val GitHub get() = LoggingGitHub(null)

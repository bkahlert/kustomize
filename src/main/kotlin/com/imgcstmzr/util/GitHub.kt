package com.imgcstmzr.util

import com.imgcstmzr.Locations
import koodies.docker.curlJq
import koodies.docker.docker
import koodies.exec.output
import koodies.io.path.getSize
import koodies.io.path.randomDirectory
import koodies.logging.RenderingLogger
import koodies.takeIfDebugging
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries

class LoggingGitHub private constructor(private val logger: RenderingLogger?) {

    fun repo(repo: String) = Repository(repo)

    inner class Repository(val repo: String) {
        val latestTag: String
            get() {
                val url = "https://api.github.com/repos/$repo/releases/latest"
                return Locations.Temp.curlJq(null) { "curl '$url' 2>/dev/null | jq -r '.tag_name'" }.io.output.ansiRemoved
            }

        fun downloadRelease(release: String = "latest", downloadDirectory: Path = Locations.Temp): Path =
            downloadDirectory.randomDirectory().let { randomSubDir ->
                randomSubDir.docker({ "zero88" / "ghrd" tag "1.1.2" },
                    null,
                    "--source",
                    "zip",
                    "--release",
                    release,
                    "--regex",
                    repo,
                    logger = logger.takeIfDebugging()
                )
                randomSubDir.listDirectoryEntries().maxByOrNull { it.getSize() }!!
            }
    }

    companion object {
        operator fun invoke(logger: RenderingLogger?) = LoggingGitHub(logger)
    }
}

val RenderingLogger.GitHub get() = LoggingGitHub(this)
val GitHub get() = LoggingGitHub(null)

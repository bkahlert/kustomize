package com.imgcstmzr.util

import koodies.builder.BooleanBuilder.YesNo.Context.yes
import koodies.concurrent.output
import koodies.concurrent.script
import koodies.docker.Docker.run
import koodies.docker.asContainerPath
import koodies.io.path.Locations
import koodies.io.path.getSize
import koodies.io.path.randomDirectory
import koodies.logging.RenderingLogger
import koodies.logging.onlyIfDebugging
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries

class LoggingGitHub private constructor(private val logger: RenderingLogger?) {

    fun repo(repo: String) = Repository(repo)

    inner class Repository(val repo: String) {
        val latestTag: String
            get() = script(logger.onlyIfDebugging()) { !"curl \"https://api.github.com/repos/$repo/releases/latest\" | jq -r .tag_name" }.output()

        fun downloadRelease(release: String = "latest", downloadDirectory: Path = Locations.Temp) =
            downloadDirectory.randomDirectory().let { randomSubDir ->
                val tmp = "/tmp".asContainerPath()
                with(logger.onlyIfDebugging()) {
                    run {
                        image { "zero88" / "ghrd" tag "1.1.2" }
                        options {
                            name { "download-$release-$repo" }
                            autoCleanup { yes }
                            mounts { randomSubDir mountAt tmp }
                            workingDirectory { tmp }
                        }
                        commandLine {
                            arguments {
                                +"--source" + "zip"
                                +"--release" + release
                                +"--regex" + repo
                            }
                        }
                    }
                }
                randomSubDir.listDirectoryEntries().maxByOrNull { it.getSize() }!!
            }
    }

    companion object {
        operator fun invoke(logger: RenderingLogger?) = LoggingGitHub(logger)
    }
}

val RenderingLogger.GitHub get() = LoggingGitHub(this)
val GitHub get() = LoggingGitHub(null)

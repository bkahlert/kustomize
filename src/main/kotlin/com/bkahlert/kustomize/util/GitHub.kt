package com.bkahlert.kustomize.util

import koodies.docker.curlJq
import koodies.docker.docker
import koodies.exec.output
import koodies.io.path.getSize
import koodies.io.randomDirectory
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries

object GitHub {

    fun repo(repo: String) = Repository(repo)

    class Repository(val repo: String) {
        val latestTag: String
            get() {
                val url = "https://api.github.com/repos/$repo/releases/latest"
                return com.bkahlert.kustomize.Kustomize.Temp.curlJq("github.com latest tag") { "curl '$url' 2>/dev/null | jq -r '.tag_name'" }.io.output.ansiRemoved
            }

        fun downloadRelease(release: String = "latest", downloadDirectory: Path = com.bkahlert.kustomize.Kustomize.Temp): Path =
            downloadDirectory.randomDirectory().let { randomSubDir ->
                randomSubDir.docker(
                    { "zero88" / "ghrd" tag "1.1.2" },
                    null,
                    "--source",
                    "zip",
                    "--release",
                    release,
                    "--regex",
                    repo,
                )
                randomSubDir.listDirectoryEntries().maxByOrNull { it.getSize() }!!
            }
    }
}

package com.bkahlert.kustomize.util

import com.bkahlert.kommons.docker.download
import com.bkahlert.kommons.io.path.copyToDirectory
import com.bkahlert.kommons.io.useRequiredClassPath
import com.bkahlert.kommons.shell.ShellScript
import com.bkahlert.kustomize.Kustomize
import com.bkahlert.kustomize.patch.chown
import java.net.URI
import java.nio.file.Path

class Downloader(private val downloadDirectory: Path = Kustomize.download) {

    private fun String.schemeOrThrow(): String = try {
        URI.create(this).scheme
    } catch (e: Throwable) {
        throw IllegalArgumentException("Invalid URI", e)
    }

    /**
     * Downloads the specified [url].
     *
     * Optionally a [filename] can be provided which is used for logging at places where the [url] would otherwise have been used.
     */
    fun download(url: String, filename: String? = null): Path {
        val scheme = url.schemeOrThrow()
        return if (scheme == "classpath") {
            useRequiredClassPath(url) {
                it.copyToDirectory(downloadDirectory)
            }
        } else {
            ShellScript("ls -lisa .").exec.logging(downloadDirectory)
            downloadDirectory.download(url, filename).also { chown(it) }
        }
    }
}

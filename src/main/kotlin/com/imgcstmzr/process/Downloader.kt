package com.imgcstmzr.process

import com.bkahlert.koodies.concurrent.process.Processes.startShellScript
import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.util.checkSingleFile
import com.imgcstmzr.util.cleanUp
import java.nio.file.Files
import java.nio.file.Path

object Downloader {
    fun OperatingSystem.download(): Path = download(downloadUrl ?: throw IllegalArgumentException("$this does not provide a download URL"), filename = name)

    fun download(url: String, retries: Int = 10, filename: String? = null): Path {
        val temp = Files.createTempDirectory("imgcstmzr")
        echo("Downloading ${filename ?: url} to $temp...", trailingNewline = false)

        startShellScript {
            !"for i in ${(1..retries).joinToString(" ")} ; do"
            command(
                "wget",
                "-q --show-progress --progress=bar:force",
                "--tries=10",
                "--timeout=300",
                "--waitretry=5",
                "--retry-connrefused",
                "--compression=auto",
                "--content-disposition",
                "--continue",
                "--ignore-case",
                "--adjust-extension",
                "--directory-prefix=$temp",
                url,
                "&& break",
            )
            !"done"
        }.waitForCompletion()

        return temp.checkSingleFile { "Failed to download $url." }.cleanUp("?").also { echo(" Completed.") }
    }
}

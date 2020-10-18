package com.imgcstmzr.process

import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.process.Exec.Sync.execShellScript
import com.imgcstmzr.runtime.OperatingSystem
import java.nio.file.Files
import java.nio.file.Path

object Downloader {
    fun OperatingSystem.download(): Path = download(downloadUrl ?: throw IllegalArgumentException("$this does not provide a download URL"), filename = name)

    fun download(url: String, retries: Int = 10, filename: String? = null): Path {
        val temp = Files.createTempDirectory("imgcstmzr")
        echo("Downloading ${filename ?: url} to $temp...", trailingNewline = false)

        execShellScript {
            line("for i in", (1..retries).joinToString(" "), "; do")
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
            line("done")
        }

        val file = temp.toFile()?.listFiles()?.firstOrNull()?.toPath() ?: throw IllegalStateException("Failed to download $url.")
        echo(" Completed.")
        return file
    }
}


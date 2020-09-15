package com.imgcstmzr.process

import com.imgcstmzr.process.Exec.Sync.execShellScript
import com.imgcstmzr.runtime.OperatingSystem
import java.nio.file.Files
import java.nio.file.Path

object Downloader {
    fun OperatingSystem.download(): Path = download(downloadUrl ?: throw IllegalArgumentException("$this does not provide a download URL"))

    fun download(url: String, retries: Int = 10): Path {
        val temp = Files.createTempDirectory("imgcstmzr")

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

        return temp.toFile()?.listFiles()?.firstOrNull()?.toPath() ?: throw IllegalStateException("Failed to download $url.")
    }
}


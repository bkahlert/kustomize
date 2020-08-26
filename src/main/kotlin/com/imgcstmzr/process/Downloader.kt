package com.imgcstmzr.process

import java.io.File
import java.nio.file.Files

class Downloader {
    fun download(url: String, retries: Int = 10): File {
        val temp = Files.createTempDirectory("imgcstmzr")

        runProcess(
            "for i in [1..$retries]; do ",
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
            "--directory-prefix=\"$temp\"",
            "\"$url\"",
            "&& break; done;"
        )

        return temp.toFile()?.listFiles()?.first()
            ?: throw IllegalStateException("An unknown error occurred while downloading. $temp was supposed to contain the downloaded file but was empty.")
    }
}

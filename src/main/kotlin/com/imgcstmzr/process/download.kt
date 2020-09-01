package com.imgcstmzr.process

import com.github.ajalt.clikt.output.TermUi.echo
import java.nio.file.Files
import java.nio.file.Path

fun download(url: String, retries: Int = 10): Path {
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
        "&& break; done;",
        "processor",
    ) { echo(it) }

    return temp.toFile()?.listFiles()?.first()?.toPath()
        ?: throw IllegalStateException("An unknown error occurred while downloading. $temp was supposed to contain the downloaded file but was empty.")
}

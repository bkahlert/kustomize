package com.imgcstmzr.process

import com.github.ajalt.clikt.output.TermUi.echo
import java.nio.file.Files
import java.nio.file.Path

fun unarchive(archive: Path): Path {
    val temp = Files.createTempDirectory("imgcstmzr")
    runProcess("tar", "-xvf", "\"$archive\"", "-C", "\"$temp\"") { echo(it) }

    return temp.toFile()?.listFiles()?.first()?.toPath()
        ?: throw IllegalStateException("An unknown error occurred while unarchiving. $temp was supposed to contain the unarchived file but was empty.")
}

package com.imgcstmzr.process

import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.util.size
import java.nio.file.Files
import java.nio.file.Path

object Unarchiver {
    fun unarchive(archive: Path): Path {
        val temp = Files.createTempDirectory("imgcstmzr")
        runProcess("tar", "-xvf", "\"$archive\"", "-C", "\"$temp\"") { echo(it) }.waitFor()

        return temp.toFile()
            ?.listFiles()
            ?.map { it.toPath() }
            ?.maxByOrNull { it.size }
            ?: throw IllegalStateException("An unknown error occurred while unarchiving. $temp was supposed to contain the unarchived file but was empty.")
    }
}

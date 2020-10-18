package com.imgcstmzr.process

import com.bkahlert.koodies.unit.size
import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.process.Exec.Sync.execShellScript
import java.nio.file.Files
import java.nio.file.Path

object Unarchiver {
    fun unarchive(archive: Path): Path {
        val temp = Files.createTempDirectory("imgcstmzr")
        echo("Unarchiving ${archive.fileName} (${archive.size})...", trailingNewline = false)

        execShellScript { command("tar", "-xvf", "$archive", "-C", "$temp") }

        val file = (temp.toFile()
            ?.listFiles()
            ?.map { it.toPath() }
            ?.maxByOrNull { it.size }
            ?: throw IllegalStateException("An unknown error occurred while unarchiving. $temp was supposed to contain the unarchived file but was empty."))
        return file.also { echo(" Completed (${it.size}).") }
    }
}

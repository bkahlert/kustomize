package com.imgcstmzr.process

import com.bkahlert.koodies.unit.size
import com.imgcstmzr.process.Exec.Sync.execShellScript
import java.nio.file.Files
import java.nio.file.Path

object Unarchiver {
    fun unarchive(archive: Path): Path {
        val temp = Files.createTempDirectory("imgcstmzr")

        execShellScript { command("tar", "-xvf", "$archive", "-C", "$temp") }

        return temp.toFile()
            ?.listFiles()
            ?.map { it.toPath() }
            ?.maxByOrNull { it.size }
            ?: throw IllegalStateException("An unknown error occurred while unarchiving. $temp was supposed to contain the unarchived file but was empty.")
    }
}

package com.imgcstmzr.process

import java.io.File
import java.nio.file.Files

class Unarchiver {
    fun unarchive(archive: File): File {
        val temp = Files.createTempDirectory("imgcstmzr")
        runProcess("tar", "-xvf", "\"$archive\"", "-C", "\"$temp\"")

        return temp.toFile()?.listFiles()?.first()
            ?: throw IllegalStateException("An unknown error occurred while unarchiving. $temp was supposed to contain the unarchived file but was empty.")
    }
}

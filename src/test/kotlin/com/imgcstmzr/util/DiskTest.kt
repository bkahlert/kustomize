package com.imgcstmzr.util

import com.imgcstmzr.ImgCstmzrConfig
import com.imgcstmzr.test.FifteenMinutesTimeout
import koodies.io.path.toPath
import koodies.logging.InMemoryLogger
import koodies.logging.logging
import koodies.terminal.AnsiColors.cyan
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT

@Execution(CONCURRENT)
class DiskTest {

    @Disabled
    @FifteenMinutesTimeout @Test
    fun InMemoryLogger.`should mount and unmount listed disks`() {
        val file = "/Users/bkahlert/.imgcstmzr.test/test/RaspberryPiLite/test/2020-08-20-raspios-buster-armhf-lite.img".toPath()
        val flashDrive: String? = "auto"

        logging("Flashing ${file.toUri()}") {
            flashDrive?.let { disk ->
                flash(file, disk.takeUnless { it.equals("auto", ignoreCase = true) })
            } ?: also {
                logLine {
                    """
                        
                    To make use of your image, you have the following options:
                    a) Check you actually inserted an SD card. 
                       Also an once ejected SD card needs to be re-inserted to get recognized.
                    b) Set ${ImgCstmzrConfig::flashDisk.name.cyan()} to ${"auto".cyan()} for flashing to any available physical removable disk.
                    c) Set ${ImgCstmzrConfig::flashDisk.name.cyan()} explicitly to any available physical removable disk (e.g. ${"disk2".cyan()}) if auto fails.
                    d) ${"Flash manually".cyan()} using the tool of your choice.
                       ${file.toUri()}
                """.trimIndent()
                }
            }
        }
    }
}

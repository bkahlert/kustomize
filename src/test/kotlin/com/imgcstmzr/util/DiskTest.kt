package com.imgcstmzr.util

import com.imgcstmzr.ImgCstmzr
import com.imgcstmzr.ImgCstmzrConfig
import koodies.io.path.asPath
import koodies.test.SixtyMinutesTimeout
import koodies.text.ANSI.Text.Companion.ansi
import koodies.tracing.spanning
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class DiskTest {

    @Disabled
    @SixtyMinutesTimeout @Test
    fun `should mount and unmount listed disks`() {
        val file = "${ImgCstmzr.HomeDirectory}/.imgcstmzr/bother-you/2021-01-03T21-28-04--csEt/2020-12-02-raspios-buster-armhf-lite.img".asPath()
        val flashDrive: String? = "auto"

        spanning("Flashing ${file.toUri()}") {
            flashDrive?.let { disk ->
                flash(file, disk.takeUnless { it.equals("auto", ignoreCase = true) })
            } ?: also {
                log(
                    """
                        
                    To make use of your image, you have the following options:
                    a) Check you actually inserted an SD card. 
                       Also an once ejected SD card needs to be re-inserted to get recognized.
                    b) Set ${ImgCstmzrConfig::flashDisk.name.ansi.cyan} to ${"auto".ansi.cyan} for flashing to any available physical removable disk.
                    c) Set ${ImgCstmzrConfig::flashDisk.name.ansi.cyan} explicitly to any available physical removable disk (e.g. ${"disk2".ansi.cyan}) if auto fails.
                    d) ${"Flash manually".ansi.cyan} using the tool of your choice.
                       ${file.toUri()}
                """.trimIndent()
                )
            }
        }
    }
}

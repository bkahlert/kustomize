package com.imgcstmzr.util

import com.bkahlert.koodies.test.Fixture

object ImgFixture : Fixture("img") {
    object Boot : SubFixture("boot") {
        object CmdlineTxt : SubFixture("cmdline.txt")
        object ConfigTxt : SubFixture("config.txt")
    }

    object Home : SubFixture("home") {
        object User : SubFixture("user") {
            object ExampleHtml : SubFixture("example.html")
        }
    }
}

package com.imgcstmzr.util

import com.bkahlert.koodies.test.Fixture

object ImgFixture : Fixture("img") {
    object Boot : SubFixture("boot") {
        object CmdlineTxt : SubFixture("cmdline.txt")
        object ConfigTxt : SubFixture("config.txt")
    }

    object Etc : SubFixture("etc") {
        object Passwd : SubFixture("passwd")
        object Group : SubFixture("group")
        object Shadow : SubFixture("shadow")
        object Gshadow : SubFixture("gshadow")
        object Subuid : SubFixture("subuid")
        object Subgid : SubFixture("subgid")
    }

    object Home : SubFixture("home") {
        object User : SubFixture("user") {
            object ExampleHtml : SubFixture("example.html")
        }
    }
}

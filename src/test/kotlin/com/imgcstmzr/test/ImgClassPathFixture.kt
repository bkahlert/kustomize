package com.imgcstmzr.test

import koodies.io.ClassPathDirectory

object ImgClassPathFixture : ClassPathDirectory("img") {
    object Boot : Dir("boot") {
        object CmdlineTxt : File("cmdline.txt")
        object ConfigTxt : File("config.txt")
    }

    object Etc : Dir("etc") {
        object Passwd : File("passwd")
        object Group : File("group")
        object Shadow : File("shadow")
        object Gshadow : File("gshadow")
        object Subuid : File("subuid")
        object Subgid : File("subgid")
    }

    object Home : Dir("home") {
        object User : Dir("user") {
            object ExampleHtml : File("example.html")
        }
    }
}

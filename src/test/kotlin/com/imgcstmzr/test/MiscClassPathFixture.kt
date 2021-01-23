package com.imgcstmzr.test

import koodies.test.ClassPathDirectoryFixture

object MiscClassPathFixture : ClassPathDirectoryFixture("") {
    object BootingRaspberry : File("raspberry.boot")
    object BootingGuestfish : File("guestfish.boot")
    object FunnyImgZip : File("funny.img.zip")
    object AnsiDocument : File("demo.ansi")

    object JourneyToTheWest : File("Journey to the West - Introduction.txt")
    object MacBeth : File("Macbeth - Chapter I.txt")
}

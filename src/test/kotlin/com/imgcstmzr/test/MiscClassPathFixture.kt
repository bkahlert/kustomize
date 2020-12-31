package com.imgcstmzr.test

import koodies.test.ClassPathFixture

object MiscClassPathFixture : ClassPathFixture("") {
    object BootingRaspberry : SubFixture("raspberry.boot")
    object BootingGuestfish : SubFixture("guestfish.boot")
    object FunnyImgZip : SubFixture("funny.img.zip")
    object AnsiDocument : SubFixture("demo.ansi")

    object JourneyToTheWest : SubFixture("Journey to the West - Introduction.txt")
    object MacBeth : SubFixture("Macbeth - Chapter I.txt")
}

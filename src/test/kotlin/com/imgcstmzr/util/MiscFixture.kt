package com.imgcstmzr.util

import com.bkahlert.koodies.test.Fixture

object MiscFixture : Fixture("") {
    object BootingRaspberry : SubFixture("raspberry.boot")
    object BootingGuestfish : SubFixture("guestfish.boot")
    object FunnyImgZip : SubFixture("funny.img.zip")
    object AnsiDocument : SubFixture("demo.ansi")

    object JourneyToTheWest : SubFixture("Journey to the West - Introduction.txt")
    object MacBeth : SubFixture("Macbeth - Chapter I.txt")
}

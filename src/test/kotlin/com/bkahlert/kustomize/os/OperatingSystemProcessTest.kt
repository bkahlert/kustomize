package com.bkahlert.kustomize.os

import com.bkahlert.kustomize.expectRendered
import com.bkahlert.kustomize.test.E2E
import koodies.exec.Process.State.Exited.Failed
import koodies.exec.containsDump
import koodies.test.Smoke
import koodies.text.ansiRemoved
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA

class OperatingSystemProcessTest {

    @E2E @Smoke @Test
    fun `should terminate if obviously stuck`(@OS(OperatingSystems.TinyCore) osImage: OperatingSystemImage) {
        val corruptingString = "Booting Linux"
        val corruptedOsImage = OperatingSystemImage(object : OperatingSystem by osImage.operatingSystem {
            override val deadEndPattern: Regex get() = ".*$corruptingString.*".toRegex()
        }, osImage.file)

        expectThat(corruptedOsImage.boot(name = null))
            .isA<Failed>()
            .containsDump()

        expectRendered().ansiRemoved {
            contains("Booting QEMU machine")
            contains("The VM is stuck.")
        }
    }
}

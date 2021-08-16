package com.bkahlert.kustomize.cli

import com.bkahlert.kommons.io.InMemoryTextFile
import com.bkahlert.kommons.test.Fixture

/**
 * A `.conf` file with the minimal settings.
 */
object MinimalConfFixture : Fixture<String>, InMemoryTextFile(
    "minimal.conf", """
        name = minimal
        os = RISC OS Pico RC5 (test only)
    """.trimIndent()) {
    override val contents: String get() = text
}

package com.bkahlert.koodies.string

import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.cli.ColorHelpFormatter.Companion.INSTANCE
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.doesNotContain
import strikt.assertions.hasLength
import strikt.assertions.hasSize
import strikt.assertions.isEmpty


@Execution(ExecutionMode.CONCURRENT)
internal class RandomKtTest {

    @Test
    internal fun `should have 16 length by default`() {
        expectThat(String.random()).hasLength(16)
    }

    @Test
    internal fun `should have allow variable length`() {
        expectThat(String.random(7)).hasLength(7)
    }

    @Test
    internal fun `should only contain boring characters`() {
        val boringCharacters = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        expectThat(String.random(10000)).get { filter { !boringCharacters.contains(it) } }.isEmpty()
    }

    @Test
    internal fun `should not easily produce the same string`() {
        val calculated = mutableListOf<String>()
        (0 until 10000).onEach {
            calculated += String.random(8).also {
                echo(INSTANCE.wizard() + " " + it)
                expectThat(calculated).doesNotContain(it)
            }
        }
        expectThat(calculated).hasSize(10000)
    }
}

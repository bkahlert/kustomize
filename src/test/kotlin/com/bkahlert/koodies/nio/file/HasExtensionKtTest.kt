package com.bkahlert.koodies.nio.file

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFailure
import java.nio.file.Path

@Execution(CONCURRENT)
class HasExtensionKtTest {

    private val path = Paths["path/file.something.ext"]

    @Test
    fun `should throw on empty extensions`() {
        expectCatching { path.addExtension() }.isFailure().isA<IllegalArgumentException>()
    }

    @Test
    fun `should match extension with ignoring case and leading period`() {
        expectThat(path)
            .hasExtension(".EXT")
            .hasExtension(".ext")
            .hasExtension("EXT")
            .hasExtension("ext")
    }

    @Test
    fun `should match multiple extensions`() {
        expectThat(path)
            .hasExtension("something", ".EXT")
            .hasExtension(".something", "ext")
            .hasExtension("something.EXT")
            .hasExtension(".something.ext")
    }

    @Test
    fun `should not match non extension`() {
        expectThat(path)
            .not { hasExtension("something") }
            .not { hasExtension(".something") }
    }

    @Test
    fun `should match extension without ignoring case and leading period`() {
        expectThat(path)
            .not { hasExtension(".EXT", ignoreCase = false) }
            .hasExtension(".ext", ignoreCase = false)
            .not { hasExtension("EXT", ignoreCase = false) }
            .hasExtension("ext", ignoreCase = false)
    }
}

fun <T : Path> Assertion.Builder<T>.hasExtension(
    vararg extensions: String,
    ignoreCase: Boolean = true,
) =
    assert("has extension(s) ${extensions.joinToString(", ")}") {
        when (it.hasExtension(
            extensions = extensions,
            ignoreCase = ignoreCase,
        )) {
            true -> pass()
            else -> fail("has extension ${it.extensionOrNull}")
        }
    }

package com.bkahlert.koodies.nio.file

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectThat
import java.nio.file.Path

@Execution(CONCURRENT)
class IsClassPathKtTest {

    @Nested
    inner class CharSequenceBased {
        @Test
        fun `should return true on class path`() {
            expectThat("classpath:path/file").isClassPath()
        }


        @Test
        fun `should return false on regular path`() {
            expectThat("path/file").not { isClassPath() }
        }


        @Test
        fun `should return false on illegal path`() {
            expectThat("!!!").not { isClassPath() }
        }
    }

    @Nested
    inner class PathBased {

        @Test
        fun `should return false on regular path`() {
            expectThat("path/file".toPath()).not { isClassPath() }
        }


        @Test
        fun `should return false on illegal path`() {
            expectThat("!!!".toPath()).not { isClassPath() }
        }
    }
}

@JvmName("charSequenceBasedIsClassPath")
fun <T : CharSequence> Assertion.Builder<T>.isClassPath() =
    assert("is class path") { value ->
        when (value.startsWith("classpath" + ":")) {
            true -> pass()
            else -> fail("is no class path")
        }
    }

fun <T : Path> Assertion.Builder<T>.isClassPath() =
    assert("is class path") { value ->
        when (value is WrappedPath) {
            true -> pass()
            else -> fail("is no class path")
        }
    }

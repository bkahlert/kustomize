package com.bkahlert.koodies.nio.file

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectThat
import java.nio.file.Path

@Execution(CONCURRENT)
class IsInsideKtTest {

    @Test
    fun `should return true if inside path`() {
        expectThat(Path.of("/dir1/foo/bar/./../baz/../../dir2/file")).isInside(Path.of("/dir1/dir2"))
    }

    @Test
    fun `should return false if not inside path`() {
        expectThat(Path.of("/dir1/./dir2/../file")).not { isInside(Path.of("/dir1/dir2")) }
    }
}

fun Assertion.Builder<Path>.isInside(path: Path) =
    assert("is inside $path") {
        when (it.isInside(path)) {
            true -> pass()
            else -> fail("${it.normalize().toAbsolutePath()} is not inside ${path.normalize().toAbsolutePath()}")
        }
    }

package koodies.io.file

import koodies.io.path.pathString
import strikt.api.Assertion
import java.nio.file.Path

fun <T : Path> Assertion.Builder<T>.pathStringIsEqualTo(path: String) =
    assert("equals $path") {
        val actual = it.pathString
        if (actual == path) pass()
        else fail()
    }

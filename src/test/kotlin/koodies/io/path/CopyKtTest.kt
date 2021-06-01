package koodies.io.path

import koodies.io.compress.TarArchiver.tar
import koodies.io.file.isSiblingOf
import koodies.io.tempFile
import koodies.test.hasSameFileName
import strikt.api.Assertion
import strikt.api.expectThat
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes


fun <T : Path> Assertion.Builder<T>.createsEqualTar(other: Path) =
    assert("is copy of $other") { self ->
        val selfTar = self.tar(tempFile()).deleteOnExit()
        val otherTar = other.tar(tempFile()).deleteOnExit()

        val selfBytes = selfTar.readBytes()
        val otherBytes = otherTar.readBytes()
        if (selfBytes.contentEquals(otherBytes)) pass()
        else fail("The resulting tarballs do not match. Expected size ${selfBytes.size} but was ${otherBytes.size}")
    }

fun <T : Path> Assertion.Builder<T>.isCopyOf(other: Path) =
    assert("is copy of $other") { self ->
        if (self.isRegularFile() && !other.isRegularFile()) fail("$self is a file and can only be compared to another file")
        else if (self.isDirectory() && !other.isDirectory()) fail("$self is a directory and can only be compared to another directory")
        else if (self.isDirectory()) {
            kotlin.runCatching {
                expectThat(self).hasSameFiles(other)
            }.exceptionOrNull()?.let { fail("Directories contained different files.") } ?: pass()
        } else {
            val selfBytes = self.readBytes()
            val otherBytes = other.readBytes()
            if (selfBytes.contentEquals(otherBytes)) pass()
            else fail("The resulting tarballs do not match. Expected size ${selfBytes.size} but was ${otherBytes.size}")
        }
    }

@Suppress("unused")
fun <T : Path> Assertion.Builder<T>.isDuplicateOf(expected: Path, order: Int = 1) {
    isCopyOf(expected)
    hasSameFileName(expected)
    isSiblingOf(expected, order)
}

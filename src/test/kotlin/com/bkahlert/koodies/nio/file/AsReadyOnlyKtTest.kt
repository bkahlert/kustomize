package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.hasContent
import com.imgcstmzr.util.isExecutable
import com.imgcstmzr.util.isReadable
import com.imgcstmzr.util.isWritable
import com.imgcstmzr.util.moveTo
import com.imgcstmzr.util.renameTo
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.ReadOnlyFileSystemException

@Execution(CONCURRENT)
class AsReadyOnlyKtTest {

    @TestFactory
    fun `should allow`() = listOf(
        allowedFileOperation(
            "readable",
            { isReadable },
            { Files.isReadable(this) },
        ) { isTrue() },

        allowedFileOperation(
            "writable",
            { isWritable },
            { Files.isWritable(this) },
        ) { isFalse() },

        allowedFileOperation(
            "executable",
            { isExecutable },
            { Files.isExecutable(this) },
        ) { },

        allowedFileOperation(
            "exists",
            { exists },
            { Files.exists(this) },
        ) { isTrue() },

        allowedFileOperation(
            "copy",
            { copyTo(sameFile("$fileName").deleteOnExit()) },
            { sameFile("$fileName").deleteOnExit().also { Files.copy(this, it) } },
        ) { get { sameFile("$fileName") }.hasContent("line #1\nline #2\n") },

        allowedFileOperation(
            "buffered reading",
            { bufferedInputStream().bufferedReader().readText() },
            { Files.newBufferedReader(this).readText() },
        ) { isEqualTo("line #1\nline #2\n") },
    )

    @TestFactory
    fun `should disallow`() = listOf(

        disallowedFileOperation(
            "move",
            { renameTo("does not matter") },
            { moveTo(tempDir.tempPath()) },
            { Files.move(this, tempDir.tempPath()) },
        ) { isA<ReadOnlyFileSystemException>() },

        disallowedFileOperation(
            "any type of output stream",
            { outputStream() },
            { bufferedOutputStream() },
            { Files.newBufferedWriter(this) },
        ) { isA<ReadOnlyFileSystemException>() },

        disallowedFileOperation(
            "delete",
            { delete(false) },
            { Files.delete(this) },
        ) { isA<ReadOnlyFileSystemException>() },
    )
}

private val tempDir = tempDir().deleteOnExit()

internal inline fun <reified T> allowedFileOperation(
    name: String,
    vararg variants: Path.() -> T,
    crossinline validator: Assertion.Builder<T>.() -> Unit,
): DynamicContainer {
    return dynamicContainer("call to $name", variants.map { variant ->
        dynamicTest("$variant") {
            val tempFile = tempDir.tempFile().writeText("line #1\nline #2\n").asReadOnly()
            expectThat(variant(tempFile)).validator()
        }
    })
}

internal inline fun disallowedFileOperation(
    name: String,
    vararg variants: Path.() -> Unit,
    crossinline validator: Assertion.Builder<Throwable>.() -> Unit,
): DynamicContainer {
    return dynamicContainer("call to $name", variants.map { variant ->
        dynamicTest("$variant") {
            val tempFile = tempDir.tempFile().writeText("line #1\nline #2\n").asReadOnly()
            expectCatching { variant(tempFile) }.isFailure().validator()
        }
    })
}

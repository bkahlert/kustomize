package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.nio.ClassPath
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.containsContent
import com.imgcstmzr.util.delete
import com.imgcstmzr.util.writeText
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isAbsolute
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import java.io.File
import java.nio.file.Path

@Execution(CONCURRENT)
class ConditionedKtTest {

    val tmpDir: Path = File.createTempFile("file", ".txt").toPath().also { it.delete() }.parent

    @Test
    fun `should return normalized`() {
        val path = createTempFile("file", ".txt").toPath().deleteOnExit()
        val other = path.resolve(".").resolve("sub").resolve("..").resolve(".")
        expectThat(path.conditioned).isEqualTo("${path.toAbsolutePath().normalize()}")
        expectThat(other.conditioned).isEqualTo("$path")
    }


    @Test
    fun `should return absolute`() {
        val workDir = Paths.WORKING_DIRECTORY
        val relativize = workDir.relativize(workDir.resolve("file.txt").deleteOnExit())
        println(relativize)
        val conditioned = relativize.also { it.writeText("was me") }.conditioned
        expectThat(Path.of(conditioned)).isAbsolute().containsContent("was me")
    }

    @Test
    fun `should not rely on toString`() {
        val path = object : Path by File.createTempFile("what", "ever").toPath() {
            override fun toString(): String = "Resource@${toFile()}"
        }
        expectThat(path.conditioned).contains("what").contains("ever").not { contains("Resource") }
    }

    @Test
    fun `should return true if classpath exists`() {
        expectCatching { ClassPath.of("config.txt").conditioned }.isFailure().isA<NotImplementedError>()
    }


    @Test
    fun `should return normally on missing files`() {
        val path = Path.of("what/ever")
        expectThat(path.conditioned).contains("what/ever")
    }

    @Test
    fun `should return false if classpath is missing`() {
        expectCatching { ClassPath.of("config.txt").conditioned }.isFailure().isA<NotImplementedError>()
    }
}

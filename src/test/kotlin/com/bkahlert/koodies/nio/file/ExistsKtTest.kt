package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.nio.ClassPath
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.io.File
import java.nio.file.Path

@Execution(CONCURRENT)
class ExistsKtTest {
    @Test
    fun `should return true if path exists`() {
        val path = File.createTempFile("what", "ever").toPath()
        expectThat(path.exists).isTrue()
    }

    @Test
    fun `should not rely on toString`() {
        val path = object : Path by File.createTempFile("what", "ever").toPath() {
            override fun toString(): String = "Resource@${toFile()}"
        }
        expectThat(path.exists).isTrue()
    }

    @Test
    fun `should return true if classpath exists`() {
        val path = ClassPath.of("config.txt")
        expectThat(path.exists).isTrue()
    }


    @Test
    fun `should return false if file is missing`() {
        val path = File.createTempFile("what", "ever").toPath()
        path.toFile().delete()
        expectThat(path.exists).isFalse()
    }


    @Test
    fun `should return false if classpath is missing`() {
        val path = ClassPath.of("missing.txt")
        expectThat(path.exists).isFalse()
    }
}

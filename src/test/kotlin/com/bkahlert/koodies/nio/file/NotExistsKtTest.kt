package com.bkahlert.koodies.nio.file//import static org.assertj.core.api.Assertions.assertThat;
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
class NotExistsKtTest {
    @Test
    fun `should return false if path exists`() {
        val path = File.createTempFile("what", "ever").toPath()
        expectThat(path.notExists).isFalse()
    }

    @Test
    fun `should not rely on toString`() {
        val path = object : Path by File.createTempFile("what", "ever").toPath() {
            override fun toString(): String = "Resource@${toFile()}"
        }
        expectThat(path.notExists).isFalse()
    }

    @Test
    fun `should return false if classpath exists`() {
        val path = ClassPath.of("config.txt")
        expectThat(path.notExists).isFalse()
    }


    @Test
    fun `should return true if file is missing`() {
        val path = File.createTempFile("what", "ever").toPath()
        path.toFile().delete()
        expectThat(path.notExists).isTrue()
    }

    @Test
    fun `should return true if classpath is missing`() {
        val path = ClassPath.of("missing.txt")
        expectThat(path.notExists).isTrue()
    }
}


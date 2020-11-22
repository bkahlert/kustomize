package com.bkahlert.koodies.nio.file//import static org.assertj.core.api.Assertions.assertThat;
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@Execution(CONCURRENT)
class NotExistsKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should return false if path exists`() {
        val path = tempDir.tempFile("what", "ever")
        expectThat(path.notExists).isFalse()
    }

    @Test
    fun `should return false if classpath exists`() {
        val path by classPath("config.txt")
        expectThat(path.notExists).isFalse()
    }
    
    @Test
    fun `should return true if file is missing`() {
        val path = tempDir.tempFile("what", "ever")
        path.toFile().delete()
        expectThat(path.notExists).isTrue()
    }
}


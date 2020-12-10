package com.bkahlert.koodies.nio.file//import static org.assertj.core.api.Assertions.assertThat;
import com.bkahlert.koodies.concurrent.cleanUpOnShutdown
import com.bkahlert.koodies.string.withRandomSuffix
import com.imgcstmzr.util.hasContent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.exists
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import java.nio.file.FileAlreadyExistsException

@Execution(CONCURRENT)
class CloneCopyKtTest {

    @EnabledOnOs(OS.LINUX, OS.MAC)
    @Test
    fun `should clone file`() {
        val file = kotlin.io.path.createTempFile().writeText("cloneFile test").cleanUpOnShutdown()
        val clone = file.resolveSibling("cloned".withRandomSuffix())
        expectThat(file.cloneTo(clone)).exists().hasContent("cloneFile test")
    }

    @EnabledOnOs(OS.LINUX, OS.MAC)
    @Test
    fun `should return target`() {
        val file = kotlin.io.path.createTempFile().writeText("cloneFile test").cleanUpOnShutdown()
        val clone = file.resolveSibling("cloned".withRandomSuffix())
        expectThat(file.cloneTo(clone)).isEqualTo(clone).not { isEqualTo(file) }
    }

    @EnabledOnOs(OS.LINUX, OS.MAC)
    @Test
    fun `should fail on existing target`() {

        val file = kotlin.io.path.createTempFile().writeText("cloneFile test").cleanUpOnShutdown()
        val clone = file.resolveSibling("cloned".withRandomSuffix()).writeText("already there")
        expect {
            catching { file.cloneTo(clone) }.isFailure().isA<FileAlreadyExistsException>()
            that(clone).exists().hasContent("already there")
        }
    }
}

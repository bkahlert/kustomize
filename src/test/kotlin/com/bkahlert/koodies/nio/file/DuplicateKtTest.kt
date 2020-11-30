package com.bkahlert.koodies.nio.file//import static org.assertj.core.api.Assertions.assertThat;
import com.bkahlert.koodies.io.PathFixtures.directoryWithTwoFiles
import com.bkahlert.koodies.io.PathFixtures.singleFile
import com.bkahlert.koodies.test.strikt.hasSameFileName
import com.imgcstmzr.util.renameTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectThat
import java.nio.file.Path

@Execution(CONCURRENT)
class DuplicateKtTest {

    private val tempDir = tempDir()

    @Test
    fun `should duplicate file`() {
        val file = tempDir.resolve("file-dir").singleFile().renameTo("file.ext")
        val copy = file.duplicate()
        expectThat(copy) {
            isCopyOf(file)
            hasSameFileName(file)
            isSiblingOf(file)
        }
    }

    @Test
    fun `should duplicate directory`() {
        val dir = tempDir.resolve("dir-dir").directoryWithTwoFiles().renameTo("dir")
        val copy = dir.duplicate()
        expectThat(copy) {
            isCopyOf(dir)
            hasSameFileName(dir)
            isSiblingOf(dir)
        }
    }
}

fun <T : Path> Assertion.Builder<T>.isDuplicateOf(expected: Path, order: Int = 1) {
    isCopyOf(expected)
    hasSameFileName(expected)
    isSiblingOf(expected)
}

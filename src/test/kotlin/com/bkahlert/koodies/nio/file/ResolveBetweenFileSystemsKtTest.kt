package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.FixtureLog.deleteOnExit
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path


@Execution(CONCURRENT)
class ResolveBetweenFileSystemsKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Nested
    inner class WithSameFileSystem {

        @Test
        fun `should return relative jar path resolved against jar path`() {
            tempDir.tempJarFileSystem().use { jarFileSystem ->
                val receiverJarPath: Path = jarFileSystem.rootDirectories.first().tempDir().tempDir()
                val relativeJarPath: Path = receiverJarPath.parent.relativize(receiverJarPath)
                expectThat(receiverJarPath.resolveBetweenFileSystems(relativeJarPath))
                    .isEqualTo(receiverJarPath.resolve(receiverJarPath.last()))
            }
        }

        @Test
        fun `should return relative file path resolved against file path`() {
            val receiverFilePath = tempDir.tempDir().tempDir()
            val relativeFilePath: Path = receiverFilePath.parent.relativize(receiverFilePath)
            expectThat(receiverFilePath.resolveBetweenFileSystems(relativeFilePath))
                .isEqualTo(receiverFilePath.resolve(receiverFilePath.last()))
        }
    }

    @Nested
    inner class WithAbsoluteOtherPath {

        @Test
        fun `should return absolute jar path resolved against jar path`() {
            tempDir.tempJarFileSystem().use { jarFileSystem ->
                val receiverJarPath: Path = jarFileSystem.rootDirectories.first().tempDir().tempFile()
                val absoluteJarPath: Path = jarFileSystem.rootDirectories.first()
                expectThat(receiverJarPath.resolveBetweenFileSystems(absoluteJarPath)).isEqualTo(absoluteJarPath)
            }
        }

        @Test
        fun `should return absolute jar path resolved against file path`() {
            val receiverFilePath: Path = tempDir.tempDir().tempFile()
            tempDir.tempJarFileSystem().use { jarFileSystem ->
                val absoluteJarPath: Path = jarFileSystem.rootDirectories.first()
                expectThat(receiverFilePath.resolveBetweenFileSystems(absoluteJarPath)).isEqualTo(absoluteJarPath)
            }
        }

        @Test
        fun `should return absolute file path resolved against jar path`() {
            val otherFileAbsPath: Path = tempDir.tempDir()
            tempDir.tempJarFileSystem().use { jarFileSystem ->
                val receiverJarPath: Path = jarFileSystem.rootDirectories.first().tempDir().tempFile()
                expectThat(receiverJarPath.resolveBetweenFileSystems(otherFileAbsPath)).isEqualTo(otherFileAbsPath)
            }
        }

        @Test
        fun `should return absolute file path resolved against file path`() {
            val receiverFilePath = tempDir.tempDir().tempFile()
            val otherFileAbsPath: Path = tempDir.tempDir()
            expectThat(receiverFilePath.resolveBetweenFileSystems(otherFileAbsPath)).isEqualTo(otherFileAbsPath)
        }
    }


    @Nested
    inner class WithRelativeOtherPath {

        @Test
        fun `should return file path on relative jar path resolved against file path`() {
            val receiverFilePath: Path = tempDir.tempDir().tempFile()
            tempDir.tempJarFileSystem().use { jarFileSystem ->
                val relativeJarPath: Path = jarFileSystem.rootDirectories.first().tempDir().tempFile()
                    .let { absPath -> absPath.parent.relativize(absPath) }
                expectThat(receiverFilePath.resolveBetweenFileSystems(relativeJarPath))
                    .isEqualTo(receiverFilePath.resolve(relativeJarPath.first().toString()))
            }
        }

        @Test
        fun `should return jar path on relative file path resolved against jar path`() {
            val relativeFilePath: Path = tempDir.tempDir().tempFile()
                .let { absPath -> absPath.parent.relativize(absPath) }
            tempDir.tempJarFileSystem().use { jarFileSystem ->
                val receiverJarPath: Path = jarFileSystem.rootDirectories.first().tempDir().tempFile()
                expectThat(receiverJarPath.resolveBetweenFileSystems(relativeFilePath))
                    .isEqualTo(receiverJarPath.resolve(relativeFilePath.first().toString()))
            }
        }
    }
}

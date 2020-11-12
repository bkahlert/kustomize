package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.nio.ClassPath
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.delete
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.exists
import strikt.assertions.isA
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class ToPathKtTest {

    @Test
    fun `should return regular path`() {
        val path = createTempFile("file", ".txt").toPath().deleteOnExit()
        expectThat("$path".toPath())
            .isEqualTo(path)
            .not { isA<ClassPath>() }
    }


    @Test
    fun `should return class path`() {
        val path = ClassPath("config.txt")
        expectThat("$path".toPath())
            .isEqualTo(path)
            .isA<ClassPath>()
    }

    @Test
    fun `should not check existence`() {
        val path = createTempFile("file", ".txt").toPath().apply { delete() }
        expectThat("$path".toPath())
            .isEqualTo(path)
            .not { exists() }
    }
}

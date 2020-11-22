package com.bkahlert.koodies.nio.file

import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path
import java.nio.file.Paths

@Execution(CONCURRENT)
class SerializedKtTest {

    @TestFactory
    fun `serialized property`() = listOf(
        "/absolute/path",
        "file.ext",
        "dir",
        "..",
        ".",
        "",
    ).map { path ->
        dynamicContainer("\"$path\"", listOf(
            dynamicContainer("as generic path", Paths.get(path).let { genericPath ->
                listOf(
                    dynamicTest("should have identical serialization") {
                        expectThat(genericPath.serialized).isEqualTo(path)
                    },
                )
            }),
            dynamicContainer("as path with overwritten toString", (object : Path by Paths.get(path) {
                override fun toString(): String = "custom toString"
            }).let { customPath ->
                listOf(
                    dynamicTest("should have custom toString") {
                        expectThat("$customPath").isEqualTo("custom toString")
                    },
                    dynamicTest("should have identical serialization") {
                        expectThat(customPath.serialized).isEqualTo(path)
                    },
                    dynamicTest("should have get equal path from serialized path") {
                        expectThat(Paths.get(customPath.serialized)).isEqualTo(Paths.get(path))
                    },
                )
            }),
        ))
    }
}

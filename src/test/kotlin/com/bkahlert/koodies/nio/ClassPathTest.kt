package com.bkahlert.koodies.nio

import com.bkahlert.koodies.nio.ClassPath.Companion.SCHEMA
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.imgcstmzr.util.delete
import com.imgcstmzr.util.quoted
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.exists
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import java.io.File
import java.io.IOException
import java.nio.file.Path

@Execution(ExecutionMode.CONCURRENT)
class ClassPathTest {

    data class Spec(val exists: Boolean, val resource: String, val size: Int?)

    @ConcurrentTestFactory
    fun `fulfills specification`() = listOf(
        "cmdline.txt" to Spec(true, "cmdline.txt", 169),
        "/cmdline.txt" to Spec(true, "cmdline.txt", 169),
        "i-dont-exist" to Spec(false, "i-dont-exist", null),
    ).map { (path, spec) ->
        val classPath = ClassPath.of(path)
        val validity: String = if (spec.exists) "exist" else "not exist"
        val not = if (spec.exists) "" else "not"
        val nullity: String = if (spec.exists) "non-null" else "null"
        val size = if (spec.exists) "${spec.size}" else "no"

        dynamicContainer("${path.quoted} -> ${"$SCHEMA:$path".quoted}",
            listOf(
                dynamicTest("should produce ${"$SCHEMA:$path".quoted}") {
                    expectThat(classPath).get { toString() }.isEqualTo("classpath:$path")
                },
                dynamicTest("should $validity") {
                    expectThat(classPath).get { exists }.isEqualTo(spec.exists)
                },
                dynamicTest("should return resource ${spec.resource.quoted}") {
                    expectThat(classPath).get { resource() }.isEqualTo(spec.resource)
                },
                dynamicTest("should return $nullity resourceStream") {
                    if (spec.exists) expectThat(classPath).get { resourceAsStream() }.isNotNull()
                    else expectThat(classPath).get { resourceAsStream() }.isNull()
                },
                dynamicTest("should read $size bytes from ${path.quoted}") {
                    if (spec.exists) expectThat(classPath).get { readAllBytes().size }.isEqualTo(spec.size)
                    else expectCatching { classPath.readAllBytes() }.isFailure().isA<IOException>()
                },
                dynamicTest("should $not copy ${path.quoted}") {
                    val dest: Path = File.createTempFile("class-path-copy-dest", "").toPath()
                    dest.delete()
                    if (spec.exists) expectThat(classPath.copyTo(dest)).exists()
                    else expectCatching { classPath.copyTo(dest) }.isFailure().isA<IOException>()
                },
            ))
    }
}

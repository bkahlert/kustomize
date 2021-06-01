package com.imgcstmzr.os

import koodies.headers
import koodies.test.testEach
import koodies.unit.Mega
import koodies.unit.bytes
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isNotNull
import java.net.URL

class OperatingSystemsTest {

    @TestFactory
    fun `should have valid download URLs`() = testEach(
        *OperatingSystems.values()
            .filterNot { it.downloadUrl.startsWith("imgcstmzr") || it.fullName.contains("RISC OS") }
            .toTypedArray()
    ) {
        expecting { downloadUrl } that {
            headers {
                isOk()
                contentLength.isNotNull().isGreaterThan(5.Mega.bytes)
            }
        }
    }
}


fun Assertion.Builder<String>.headers(block: Assertion.Builder<Map<String?, String?>>.() -> Unit) =
    get("as URL") { URL(this).headers().mapValues { it.value.firstOrNull() } }.apply(block)

val Assertion.Builder<Map<String?, String?>>.contentLength
    get() = get("content-length") { get("content-length")?.toLong()?.bytes }

fun Assertion.Builder<Map<String?, String?>>.isOk() =
    get("is OK?") { get(null) }.isNotNull().isEqualTo("HTTP/1.1 200 OK")

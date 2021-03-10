package com.imgcstmzr.runtime

import com.imgcstmzr.testEach
import koodies.headers
import koodies.unit.Mega
import koodies.unit.bytes
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isNotNull
import java.net.URL

@Execution(CONCURRENT)
class OperatingSystemsTest {

    @TestFactory
    fun `should have valid download URLs`() =
        OperatingSystems.values()
            .filterNot { it.downloadUrl.startsWith("imgcstmzr") || it.fullName.contains("RISC OS") }
            .testEach { os ->
                expect { downloadUrl }.headers {
                    isOk()
                    contentLength.isNotNull().isGreaterThan(5.Mega.bytes)
                }
            }
}


fun Assertion.Builder<String>.headers(block: Assertion.Builder<Map<String?, String?>>.() -> Unit) =
    get("as URL") { URL(this).headers().mapValues { it.value.firstOrNull() } }.apply(block)

val Assertion.Builder<Map<String?, String?>>.contentLength
    get() = get("content-length") { get("content-length")?.toLong()?.bytes }

fun Assertion.Builder<Map<String?, String?>>.isOk() =
    get("is OK?") { get(null) }.isNotNull().isEqualTo("HTTP/1.1 200 OK")

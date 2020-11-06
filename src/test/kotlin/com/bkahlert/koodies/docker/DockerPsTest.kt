package com.bkahlert.koodies.docker

import com.bkahlert.koodies.boolean.asEmoji
import com.bkahlert.koodies.time.poll
import com.imgcstmzr.util.DockerRequired
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@DockerRequired
@Execution(SAME_THREAD)
class DockerPsTest {

    val containers = mutableListOf<DockerProcess>()

    @OptIn(ExperimentalTime::class)
    @BeforeAll
    fun setUp() {
        listOf(
            "shared-prefix-boot-and-run",
            "shared-prefix-boot-and-run-program-in-user-session",
        ).mapTo(containers) {
            Docker.run {
                run(name = it, image = "busybox", args = listOf("""
                            <<BE-BUSY
                            while true; do
                                sleep 1
                            done
                            BE-BUSY
                        """.trimIndent()))
            }.also { 100.milliseconds.poll { it.isRunning }.forAtMost(5.seconds) { fail("Docker containers did not start") } }
        }
    }

    @Execution(SAME_THREAD)
    @TestFactory
    fun `should correctly read docker ps output`() = mapOf(
        "shared-prefix-boot" to false,
        "shared-prefix-boot-and-run" to true,
        "shared-prefix-boot-and-run-program" to false,
        "shared-prefix-boot-and-run-program-in-user-session" to true,
    ).map { (name, expectedIsRunning) ->
        dynamicTest("${expectedIsRunning.asEmoji} $name") {
            expectThat(Docker.isContainerRunning(name)).isEqualTo(expectedIsRunning)
        }
    }

    @AfterAll
    fun tearDown() {
//        startAsThread { println("containers stopping") }
        containers.forEach { it.destroyForcibly() }
    }
}

package com.bkahlert.koodies.docker

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.concurrent.process.UserInput.enter
import com.bkahlert.koodies.concurrent.synchronized
import com.bkahlert.koodies.shell.toHereDoc
import com.bkahlert.koodies.test.junit.Slow
import com.bkahlert.koodies.time.poll
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.DietPi
import com.imgcstmzr.runtime.OperatingSystems.RiscOsPicoRc5
import com.imgcstmzr.util.DockerRequired
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isLessThan
import strikt.assertions.isTrue
import java.nio.file.Path
import kotlin.reflect.KFunction
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@DockerRequired
@Execution(CONCURRENT)
class DockerTest {

    @Test
    fun `should extract name from docker run command`() {
        expectThat(Docker.extractName {
            run(name = "this-is_what_its-about--123", image = "busybox")
        }).isEqualTo("this-is_what_its-about--123")
    }

    @Nested
    inner class Lifecycle {

        @OptIn(ExperimentalTime::class)
        @Test
        fun `should start docker and pass arguments`(logger: InMemoryLogger<String>) {
            var outputProcessed = false
            val dockerProcess = Docker.run(outputProcessor = {
                logger.logLine { it.formatted }
                if (it.type == OUT && it.unformatted == "test") {
                    outputProcessed = true
                }
            }) {
                run(name = testName(Lifecycle::`should start docker and pass arguments`), volumes = emptyMap(), image = "busybox", args = listOf(
                    listOf(
                        "ping -c 1 \"imgcstmzr.com\"",
                        "sleep 1",
                        "echo 'test'",
                    ).toHereDoc()
                ))
            }

            kotlin.runCatching {
                val startTime = System.currentTimeMillis()
                while ((System.currentTimeMillis() - startTime).milliseconds < 5.seconds) {
                    if (outputProcessed) break
                    Thread.sleep(100)
                }
                expectThat(outputProcessed).isTrue()
            }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
        }

        @OptIn(ExperimentalTime::class)
        @Test
        fun `should start docker and process input`() {
            var outputProcessed = false
            val dockerProcess = Docker.run(outputProcessor = {
                if (it.type == OUT && it.unformatted == "test") {
                    outputProcessed = true
                }
            }) {
                run(name = testName(Lifecycle::`should start docker and process input`), volumes = emptyMap(), image = "busybox")
            }

            kotlin.runCatching {
                dockerProcess.enter("echo 'test'")
                dockerProcess.enter("exit")

                val startTime = System.currentTimeMillis()
                while ((System.currentTimeMillis() - startTime).milliseconds < 8.seconds) {
                    if (outputProcessed) break
                    Thread.sleep(100)
                }

                expectThat(outputProcessed).isTrue()
            }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
        }

        @OptIn(ExperimentalTime::class)
        @Test
        fun `should start docker and process output`(@OS(RiscOsPicoRc5::class) osImage: OperatingSystemImage) {
            var outputProcessed = false
            val dockerProcess =
                runOsImage(testName(Lifecycle::`should start docker and process output`), osImage) { if (it.type == OUT) outputProcessed = true }

            kotlin.runCatching {
                val startTime = System.currentTimeMillis()
                while ((System.currentTimeMillis() - startTime).milliseconds < 8.seconds) {
                    if (outputProcessed) break
                    Thread.sleep(100)
                }

                expectThat(outputProcessed).isTrue()
            }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
        }

        @OptIn(ExperimentalTime::class)
        @Slow @Test
        fun `should start docker and process output produced by own input`(logger: InMemoryLogger<*>) {
            val output = mutableListOf<String>().synchronized()
            val roundtrips = 3
            val dockerProcess = Docker.run(outputProcessor = {
                if (it.type == OUT) {
                    output.add(it.unformatted)
                    if (output.size >= roundtrips) {
                        destroy()
                    }
                    val message = "echo '${it.unformatted} ${it.unformatted.length}'"
                    enter(message)
                }
            }) {
                run(name = testName(Lifecycle::`should start docker and process output produced by own input`), volumes = emptyMap(), image = "busybox")
            }

            kotlin.runCatching {
                dockerProcess.enter("echo 'test'")

                val startTime = System.currentTimeMillis()
                while ((System.currentTimeMillis() - startTime).milliseconds < 30.seconds) {
                    if (output.size >= roundtrips) break
                    Thread.sleep(100)
                }

                expectThat(output).containsExactly("test", "test 4", "test 4 6")
            }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
        }

        @Nested
        inner class IsRunning {

            @OptIn(ExperimentalTime::class)
            @Test
            fun `should return false on not yet started container container`(@OS(RiscOsPicoRc5::class) osImage: OperatingSystemImage) {
                val dockerProcess = runOsImage(testName(IsRunning::`should return false on not yet started container container`), osImage)
                kotlin.runCatching {
                    expectThat(dockerProcess.isRunning).isFalse()
                }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
            }

            @OptIn(ExperimentalTime::class)
            @Test
            fun `should return true on running container`(@OS(RiscOsPicoRc5::class) osImage: OperatingSystemImage) {
                val dockerProcess = runOsImage(testName(IsRunning()::`should return true on running container`), osImage)
                kotlin.runCatching {
                    val startTime = System.currentTimeMillis()
                    while ((System.currentTimeMillis() - startTime).milliseconds < 5.seconds) {
                        if (dockerProcess.isRunning) break
                        Thread.sleep(100)
                    }

                    expectThat(dockerProcess.isRunning).isTrue()
                }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
            }

            @Slow
            @OptIn(ExperimentalTime::class)
            @Test
            fun `should return false on completed container`(@OS(RiscOsPicoRc5::class) osImage: OperatingSystemImage) {
                val dockerProcess = runOsImage(testName(IsRunning::`should return false on completed container`), osImage)
                kotlin.runCatching {
                    val startTime = System.currentTimeMillis()
                    while ((System.currentTimeMillis() - startTime).milliseconds < 5.seconds) {
                        if (dockerProcess.isRunning) break
                        Thread.sleep(100)
                    }
                    expectThat(dockerProcess.isRunning).isTrue()

                    dockerProcess.destroy()
                    val destroyTime = System.currentTimeMillis()
                    while ((System.currentTimeMillis() - destroyTime).milliseconds < 15.seconds) {
                        if (!dockerProcess.isRunning) break
                        Thread.sleep(100)
                    }
                    expectThat(dockerProcess.isRunning).isFalse()
                }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
            }

            @OptIn(ExperimentalTime::class)
            @Test
            fun `should stop started container`(@OS(DietPi::class) osImage: OperatingSystemImage) {
                val dockerProcess = runOsImage(testName(IsRunning::`should stop started container`), osImage)
                kotlin.runCatching {
                    100.milliseconds.poll { dockerProcess.isRunning }.forAtMost(5.seconds) { fail("timed out") }

                    dockerProcess.stop()

                    expectThat(dockerProcess.isRunning).isFalse()
                }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
            }
        }

        @Slow
        @OptIn(ExperimentalTime::class)
        @Test
        fun `should remove docker container after completion`(@OS(RiscOsPicoRc5::class) osImage: OperatingSystemImage) {
            val dockerProcess = runOsImage(testName(Lifecycle::`should remove docker container after completion`), osImage)
            kotlin.runCatching {
                val startTime = System.currentTimeMillis()
                while ((System.currentTimeMillis() - startTime).milliseconds < 5.seconds) {
                    if (dockerProcess.isRunning) break
                    Thread.sleep(100)
                }
                expectThat((System.currentTimeMillis() - startTime).milliseconds).isLessThan(5.seconds)

                dockerProcess.destroy()
                val destroyTime = System.currentTimeMillis()
                while ((System.currentTimeMillis() - destroyTime).milliseconds < 15.seconds) {
                    if (!dockerProcess.isRunning) break
                    Thread.sleep(100)
                }
                expectThat(Docker.exists(dockerProcess.name)).isFalse()
            }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `should not produce incorrect empty lines`(@OS(RiscOsPicoRc5::class) osImage: OperatingSystemImage) {
        val output = mutableListOf<IO>().synchronized()
        val dockerProcess = runOsImage(testName(DockerTest::`should not produce incorrect empty lines`), osImage) { output.add(it) }
        kotlin.runCatching {
            val startTime = System.currentTimeMillis()
            while ((System.currentTimeMillis() - startTime).milliseconds < 8.seconds) {
                Thread.sleep(100)
            }

            expectThat(output.filter { it.isBlank }.size).isLessThan(output.size / 4)
        }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
    }


    private fun testName(test: KFunction<Any>): String = DockerTest::class.simpleName + "-" + test.name
}

@Suppress("SpellCheckingInspection")
private fun runOsImage(name: String, osImage: OperatingSystemImage, outputProcessor: (DockerProcess.(IO) -> Unit)? = null): DockerProcess {
    return Docker.run(outputProcessor = outputProcessor) {
        run(name = name, volumes = listOf(osImage.toAbsolutePath() to Path.of("/sdcard/filesystem.img")).toMap(), image = "lukechilds/dockerpi:vm")
    }
}

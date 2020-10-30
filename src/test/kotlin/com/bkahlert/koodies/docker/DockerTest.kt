package com.bkahlert.koodies.docker

import com.bkahlert.koodies.concurrent.synchronized
import com.bkahlert.koodies.test.junit.Slow
import com.bkahlert.koodies.time.poll
import com.imgcstmzr.process.Output
import com.imgcstmzr.process.Output.Type.OUT
import com.imgcstmzr.process.enter
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.DietPi
import com.imgcstmzr.runtime.OperatingSystems.RiscOsPicoRc5
import com.imgcstmzr.util.DockerRequired
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.containsExactly
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
internal class DockerTest {
    @Nested
    inner class Lifecycle {
        @OptIn(ExperimentalTime::class)
        @Test
        internal fun `should start docker and pass arguments`() {
            var outputProcessed = false
            val dockerProcess = Docker.run(testName(Lifecycle::`should start docker and pass arguments`), emptyMap(), "busybox", listOf("echo 'test'")) {
                if (it.type == OUT && it.unformatted == "test") {
                    outputProcessed = true
                }
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
        internal fun `should start docker and process input`() {
            var outputProcessed = false
            val dockerProcess = Docker.run(testName(Lifecycle::`should start docker and process input`), emptyMap(), "busybox") {
                if (it.type == OUT && it.unformatted == "test") {
                    outputProcessed = true
                }
            }

            kotlin.runCatching {
                dockerProcess.enter("echo 'test'")

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
        internal fun `should start docker and process output`(@OS(RiscOsPicoRc5::class) osImage: OperatingSystemImage) {
            var outputProcessed = false
            val dockerProcess = run(testName(Lifecycle::`should start docker and process output`), osImage) { if (it.type == OUT) outputProcessed = true }

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
        internal fun `should start docker and process output produced by own input`(logger: InMemoryLogger<*>) {
            val output = mutableListOf<String>().synchronized()
            val roundtrips = 3
            val dockerProcess = Docker.run(testName(Lifecycle::`should start docker and process output produced by own input`), emptyMap(), "busybox") {
                if (it.type == OUT) {
                    output.add(it.unformatted)
                    if (output.size >= roundtrips) {
                        destroy()
                    }
                    val message = "echo '${it.unformatted} ${it.unformatted.length}'"
                    enter(message)
                }
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
            internal fun `should return false on not yet started container container`(@OS(RiscOsPicoRc5::class) osImage: OperatingSystemImage) {
                val dockerProcess = run(testName(IsRunning::`should return false on not yet started container container`), osImage)
                kotlin.runCatching {
                    expectThat(dockerProcess.isRunning).isFalse()
                }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
            }

            @OptIn(ExperimentalTime::class)
            @Test
            internal fun `should return true on running container`(@OS(RiscOsPicoRc5::class) osImage: OperatingSystemImage) {
                val dockerProcess = run(testName(IsRunning()::`should return true on running container`), osImage)
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
            internal fun `should return false on completed container`(@OS(RiscOsPicoRc5::class) osImage: OperatingSystemImage) {
                val dockerProcess = run(testName(IsRunning::`should return false on completed container`), osImage)
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
            internal fun `should stop started container`(@OS(DietPi::class) osImage: OperatingSystemImage) {
                val dockerProcess = run(testName(IsRunning::`should stop started container`), osImage)
                kotlin.runCatching {
                    100.milliseconds.poll { dockerProcess.isRunning }.forAtMost(5.seconds)
                    expectThat(dockerProcess.isRunning).isTrue()

                    dockerProcess.stop()

                    expectThat(dockerProcess.isRunning).isFalse()
                }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
            }
        }

        @Slow
        @OptIn(ExperimentalTime::class)
        @Test
        internal fun `should remove docker container after completion`(@OS(RiscOsPicoRc5::class) osImage: OperatingSystemImage) {
            val dockerProcess = run(testName(Lifecycle::`should remove docker container after completion`), osImage)
            kotlin.runCatching {
                val startTime = System.currentTimeMillis()
                while ((System.currentTimeMillis() - startTime).milliseconds < 5.seconds) {
                    if (dockerProcess.isRunning) break
                    Thread.sleep(100)
                }
                expectThat(Docker.exists(dockerProcess.name)).isTrue()

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
    internal fun `should not produce incorrect empty lines`(@OS(RiscOsPicoRc5::class) osImage: OperatingSystemImage) {
        val output = mutableListOf<Output>().synchronized()
        val dockerProcess = run(testName(DockerTest::`should not produce incorrect empty lines`), osImage) { output.add(it) }
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
private fun run(name: String, osImage: OperatingSystemImage, outputProcessor: (DockerProcess.(Output) -> Unit)? = null): DockerProcess {
    return Docker.run(
        name,
        volumes = listOf(osImage.toAbsolutePath() to Path.of("/sdcard/filesystem.img")).toMap(),
        image = "lukechilds/dockerpi:vm",
        outputProcessor = outputProcessor,
    )
}

/**
 * Executes [`docker run`](https://docs.docker.com/engine/reference/run/) and returns
 * the created [DockerProcess].
 */
@Suppress("unused", "RemoveRedundantQualifierName")
fun Docker.run(
    test: KFunction<Any>,
    volumes: Map<Path, Path> = emptyMap(),
    image: String,
    args: List<String> = emptyList(),
    outputProcessor: (DockerProcess.(Output) -> Unit)? = null,
): DockerProcess = Docker.run(
    name = DockerTest::class.simpleName + "-" + test.name,
    volumes = volumes,
    image = image,
    args = args,
    outputProcessor = outputProcessor,
)

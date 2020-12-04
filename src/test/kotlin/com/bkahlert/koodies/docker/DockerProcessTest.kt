package com.bkahlert.koodies.docker

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.concurrent.process.UserInput.enter
import com.bkahlert.koodies.concurrent.synchronized
import com.bkahlert.koodies.test.junit.Slow
import com.bkahlert.koodies.time.poll
import com.bkahlert.koodies.time.sleep
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.DietPi
import com.imgcstmzr.runtime.OperatingSystems.RiscOsPicoRc5
import com.imgcstmzr.util.DockerRequiring
import com.imgcstmzr.util.OS
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isTrue
import kotlin.reflect.KFunction
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class DockerProcessTest {

    @Nested
    inner class Lifecycle {

        @DockerRequiring @Test
        fun `should start docker and pass arguments`() {
            val dockerProcess = DockerProcess(busybox(
                Lifecycle::`should start docker and pass arguments`,
                "echo", "test",
            ))

            kotlin.runCatching {
                poll { dockerProcess.ioLog.logged.any { it.type == OUT && it.unformatted == "test" } }
                    .every(100.milliseconds).noLongerThan({ dockerProcess.isAlive }, 8.seconds) {
                        if (dockerProcess.isAlive) fail("Did not log \"test\" output within 8 seconds.")
                        fail("Process terminated without logging: ${dockerProcess.ioLog.dump()}.")
                    }
            }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
        }

        @DockerRequiring @Test
        fun `should start docker and process input`() {
            val dockerProcess = DockerProcess(busybox(Lifecycle::`should start docker and process input`))

            kotlin.runCatching {
                dockerProcess.enter("echo 'test'")
                poll { dockerProcess.ioLog.logged.any { it.type == OUT && it.unformatted == "test" } }
                    .every(100.milliseconds).forAtMost(8.seconds) { fail("Did not log self-induced \"test\" output within 8 seconds.") }
                dockerProcess.destroy()
            }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
        }

        @DockerRequiring @Test
        fun `should start docker and process output`(@OS(RiscOsPicoRc5) osImage: OperatingSystemImage) {
            val dockerProcess = runOsImage(testName(Lifecycle::`should start docker and process output`), osImage)

            kotlin.runCatching {
                poll { dockerProcess.ioLog.logged.any { it.type == OUT } }
                    .every(100.milliseconds).forAtMost(8.seconds) { fail("Did not log any output within 8 seconds.") }
            }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
        }

        @DockerRequiring @Test
        fun `should start docker and process output produced by own input`() {
            val dockerProcess = DockerProcess(
                commandLine = Docker.image { "busybox" }
                    .run { options { testName(Lifecycle::`should start docker and process output produced by own input`) } },
                processor = {
                    if (it.type == OUT) {
                        if (ioLog.logged.any { output -> output.unformatted == "test 4 6" }) destroy()
                        val message = "echo '${it.unformatted} ${it.unformatted.length}'"
                        enter(message)
                    }
                })

            kotlin.runCatching {
                dockerProcess.enter("echo 'test'")
                poll {
                    dockerProcess.ioLog.logged.mapNotNull { if (it.type == OUT) it.unformatted else null }.containsAll(listOf("test", "test 4", "test 4 6"))
                }
                    .every(100.milliseconds)
                    .forAtMost(30.seconds) { fail("Did not log self-produced \"test\", \"test 4\" and \"test 4 6\" output within 30 seconds.") }
            }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
        }

        @Nested
        inner class IsRunning {

            @DockerRequiring @Test
            fun `should return false on not yet started container container`(@OS(RiscOsPicoRc5) osImage: OperatingSystemImage) {
                val dockerProcess = runOsImage(testName(IsRunning::`should return false on not yet started container container`), osImage)
                kotlin.runCatching {
                    expectThat(dockerProcess.isRunning).isFalse()
                }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
            }

            @DockerRequiring @Test
            fun `should return true on running container`(@OS(RiscOsPicoRc5) osImage: OperatingSystemImage) {
                val dockerProcess = runOsImage(testName(IsRunning()::`should return true on running container`), osImage)
                kotlin.runCatching {
                    poll { dockerProcess.isRunning }
                        .every(100.milliseconds).forAtMost(5.seconds) { fail("Did not start container within 5 seconds.") }
                    expectThat(dockerProcess.isRunning).isTrue()
                }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
            }

            @DockerRequiring @Test
            fun `should return false on completed container`(@OS(RiscOsPicoRc5) osImage: OperatingSystemImage) {
                val dockerProcess = runOsImage(testName(IsRunning::`should return false on completed container`), osImage)
                kotlin.runCatching {
                    poll { dockerProcess.isRunning }
                        .every(100.milliseconds).forAtMost(5.seconds) { fail("Did not start container within 5 seconds.") }

                    dockerProcess.destroy()

                    poll { !dockerProcess.isRunning }
                        .every(100.milliseconds).forAtMost(5.seconds) { fail("Did not stop container within 5 seconds.") }
                    expectThat(dockerProcess.isRunning).isFalse()
                }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
            }

            @DockerRequiring @Test
            fun `should stop started container`(@OS(DietPi) osImage: OperatingSystemImage) {
                val dockerProcess = runOsImage(testName(IsRunning::`should stop started container`), osImage)
                kotlin.runCatching {
                    poll { dockerProcess.isRunning }
                        .every(100.milliseconds).forAtMost(5.seconds) { fail("Did not start container within 5 seconds.") }

                    dockerProcess.stop()

                    poll { !dockerProcess.isRunning }
                        .every(100.milliseconds).forAtMost(5.seconds) { fail("Did not stop container within 5 seconds.") }
                    expectThat(dockerProcess.isRunning).isFalse()
                }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
            }
        }

        @Slow @DockerRequiring @Test
        fun `should remove docker container after completion`(@OS(RiscOsPicoRc5) osImage: OperatingSystemImage) {
            val dockerProcess = runOsImage(testName(Lifecycle::`should remove docker container after completion`), osImage)
            kotlin.runCatching {
                poll { Docker.exists(dockerProcess.name) }
                    .every(100.milliseconds).forAtMost(5.seconds) { fail("Did not start container within 5 seconds.") }
                expectThat(Docker.exists(dockerProcess.name)).isTrue()

                dockerProcess.destroy()

                poll { !Docker.exists(dockerProcess.name) }
                    .every(100.milliseconds).forAtMost(15.seconds) { fail("Did not destroy container within 15 seconds.") }
                expectThat(Docker.exists(dockerProcess.name)).isFalse()
            }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
        }
    }

    @Slow @DockerRequiring @Test
    fun `should not produce incorrect empty lines`(@OS(RiscOsPicoRc5) osImage: OperatingSystemImage) {
        val output = mutableListOf<IO>().synchronized()
        val dockerProcess = runOsImage(testName(DockerProcessTest::`should not produce incorrect empty lines`), osImage) { output.add(it) }
        kotlin.runCatching {
            20.seconds.sleep()
            dockerProcess.destroy()
            expectThat(output).get { size }.isGreaterThan(20)
            expectThat(output.filter { it.isBlank }.size).isLessThan(output.size / 4)
        }.onFailure { dockerProcess.destroyForcibly() }.getOrThrow()
    }
}

fun testName(test: KFunction<Any>): String = DockerProcessTest::class.simpleName + "-" + test.name
fun OptionsBuilder.testName(test: KFunction<Any>) = run { com.bkahlert.koodies.docker.testName(test).let { name { it } } }

@Suppress("SpellCheckingInspection")
private fun runOsImage(name: String, osImage: OperatingSystemImage, ioProcessor: (DockerProcess.(IO) -> Unit)? = null): DockerProcess =
    DockerProcess(
        commandLine = Docker.image { "lukechilds" / "dockerpi" tag "vm" }.run {
            options {
                name { name }
                mounts { osImage.file mountAt "/sdcard/filesystem.img" }
            }
        },
        processor = ioProcessor)

private fun busybox(test: KFunction<Any>, vararg lines: String): DockerRunCommandLine =
    Docker.image { "busybox" }.run {
        options { testName(test) }
        arguments { +lines }
    }

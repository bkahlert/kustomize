package com.bkahlert.koodies.concurrent

import com.bkahlert.koodies.docker.Docker
import com.bkahlert.koodies.docker.DockerProcess
import com.bkahlert.koodies.nio.file.age
import com.bkahlert.koodies.nio.file.list
import com.bkahlert.koodies.nio.file.sameFile
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.appendText
import com.imgcstmzr.util.delete
import com.imgcstmzr.util.isFile
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.minutes

private val cleanUpJobs = object : MutableList<() -> Unit> by mutableListOf() {
    init {
        startOnShutdown {
            val log = sameFile("com.bkahlert.koodies.cleanup.log").apply { delete() }
            forEach { cleanUpJob ->
                kotlin.runCatching {
                    cleanUpJob()
                }.onFailure {
                    log.appendText(it.stackTraceToString())
                }
            }
        }
    }
}

/**
 * Runs this lambda on shutdown.
 */
fun <T : () -> Unit> T.cleanUpOnShutdown(): T = apply { cleanUpJobs.add(this) }

/**
 * Stops and removes this [DockerProcess]'s underlying container on shutdown.
 */
fun <@Suppress("FINAL_UPPER_BOUND") T : DockerProcess> T.cleanUpOnShutdown(): T = apply {
    val containerName = this.name
    cleanUpJobs.add { Docker.remove(containerName, forcibly = true) }
}

/**
 * Deletes this file on shutdown.
 */
fun <T : Path> T.cleanUpOnShutdown(): T = apply { cleanUpJobs.add { this.delete(true) } }

/**
 * Deletes this file on shutdown.
 */
fun cleanUpOnShutdown(path: Path) {
    cleanUpJobs.add { path.delete(true) }
}

/**
 * Builder to specify which files to delete on shutdown.
 */
class CleanUpBuilder(private val jobs: MutableList<() -> Unit>) {
    /**
     * Returns whether this file's name starts with the specified [prefix].
     */
    fun Path.fileNameStartsWith(prefix: String): Boolean = "$fileName".startsWith(prefix)

    /**
     * Returns whether this file's name ends with the specified [suffix].
     */
    fun Path.fileNameEndsWith(suffix: String): Boolean = "$fileName".endsWith(suffix)

    /**
     * Registers a lambda that is called during shutdown and
     * which deletes all files that pass the specified [filter].
     */
    fun tempFiles(filter: Path.() -> Boolean) {
        jobs.add { Paths.TEMP.list().filter { it.isFile }.filter(filter).forEach { it.delete() } }
    }
}

/**
 * Builds and returns a lambda that is called during shutdown.
 */
fun cleanUpOnShutDown(block: CleanUpBuilder.() -> Unit): () -> Unit =
    mutableListOf<() -> Unit>().also { CleanUpBuilder(it).apply(block) }
        .let { jobs -> { jobs.forEach { job -> job() } } }.cleanUpOnShutdown()

/**
 * Convenience function to delete temporary files of the specified [minAge] and
 * who's [fileName][Path.getFileName] matches the specified [prefix] and [suffix].
 *
 * Files matching these criteria are deleted immediately **and** during shutdown.
 */
fun cleanUpOldTempFiles(prefix: String, suffix: String, minAge: Duration = 10.minutes) {
    cleanUpOnShutDown {
        tempFiles { fileNameStartsWith(prefix) && fileNameEndsWith(suffix) && age >= minAge }
    }.apply {
        invoke()
    }
}

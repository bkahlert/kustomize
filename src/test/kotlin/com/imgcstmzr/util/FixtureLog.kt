package com.imgcstmzr.util

import com.bkahlert.koodies.concurrent.process.ShutdownHookUtils.addShutDownHook
import com.bkahlert.koodies.nio.file.appendLine
import com.bkahlert.koodies.nio.file.baseName
import com.bkahlert.koodies.nio.file.delete
import com.bkahlert.koodies.nio.file.exists
import com.bkahlert.koodies.nio.file.extensionOrNull
import com.bkahlert.koodies.nio.file.list
import com.bkahlert.koodies.nio.file.readLines
import com.bkahlert.koodies.nio.file.sameFile
import com.bkahlert.koodies.nio.file.serialized
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.applyLogging
import com.imgcstmzr.runtime.log.singleLineLogging
import java.nio.file.Path

/**
 * A log to keep track to file-based fixture.
 *
 * As soon as an instance is created, all not yet deleted fixtures will be removed.
 */
object FixtureLog : (Path) -> Path {
    val location = sameFile("fixture.log")

    init {
        val renderingLogger = BlockRenderingLogger("Fixture Leftovers Cleanup 🧻", borderedOutput = true)
        renderingLogger.applyLogging { delete() }
        addShutDownHook {
            BlockRenderingLogger("Fixture Leftovers Cleanup 🧻", borderedOutput = true).applyLogging { delete() }
        }
    }

    fun BlockRenderingLogger.delete(): String {
        val entriesPerLines = 1
        var deleted = 0
        paths()
            .flatMap { fixture ->
                if (fixture.extensionOrNull == "img" && fixture.parent != Paths.TEMP) {
                    val fixtureDirectory = fixture.parent
                    val directoryWithFixtureDirectories = fixtureDirectory.parent
                    listOf(fixture).plus(directoryWithFixtureDirectories.list().filter { otherFixtureDirectory ->
                        otherFixtureDirectory.serialized.startsWith(fixtureDirectory.serialized)
                    }.toList().sorted().reversed())
                } else {
                    listOf(fixture)
                }
            }
            .plusElement(location)
            .windowed(entriesPerLines, entriesPerLines).forEach { paths ->
                singleLineLogging("deleting") {
                    paths.forEach {
                        logLine { "$it" }
                        kotlin.runCatching {
                            it.delete(true)
                            deleted++
                        }.getOrElse { logCaughtException { it } }
                    }
                }
            }

        return "$deleted items deleted"
    }

    fun <T : Path> T.deleteOnExit(): T {
        this@FixtureLog.invoke(this)
        return this
    }

    override fun invoke(path: Path): Path = location.appendLine(path.resolveFixture().logLine())

    fun paths(): List<Path> = if (location.exists) location.readLines().map { Path.of(it) } else emptyList()

    private fun Path.logLine() = "${toAbsolutePath()}"

    private fun Path.resolveFixture() = let {
        if (it.hasCommonNameWithParent()) it.parent else it
    }

    private fun Path.hasCommonNameWithParent() = baseName == parent.baseName
}

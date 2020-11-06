package com.imgcstmzr.util

import com.bkahlert.koodies.concurrent.process.ShutdownHookUtils.addShutDownHook
import com.bkahlert.koodies.nio.file.conditioned
import com.bkahlert.koodies.nio.file.exists
import com.bkahlert.koodies.nio.file.list
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.RenderingLogger.Companion.singleLineLogger
import com.imgcstmzr.runtime.log.applyLogging
import java.nio.file.Path

/**
 * A log to keep track to file-based fixture.
 *
 * As soon as an instance is created, all not yet deleted fixtures will be removed.
 */
object FixtureLog : (Path) -> Path {
    val location = Paths.TEST.resolve("fixture.log")

    init {
        val renderingLogger: RenderingLogger<Any> = BlockRenderingLogger<Any>("Fixture Leftovers Cleanup 🧻", borderedOutput = true)
        renderingLogger.applyLogging { delete() }
        addShutDownHook {
            BlockRenderingLogger<Any>("Fixture Leftovers Cleanup 🧻", borderedOutput = true).applyLogging { delete() }
        }
    }

    fun RenderingLogger<Any>.delete(): String {
        val entriesPerLines = 1
        var deleted = 0
        paths()
            .flatMap { fixture ->
                if (fixture.extension == "img" && fixture.parent != Paths.TEMP) {
                    val fixtureDirectory = fixture.parent
                    val directoryWithFixtureDirectories = fixtureDirectory.parent
                    listOf(fixture).plus(directoryWithFixtureDirectories.list().filter { otherFixtureDirectory ->
                        otherFixtureDirectory.conditioned.startsWith(fixtureDirectory.conditioned)
                    }.toList().sorted().reversed())
                } else {
                    listOf(fixture)
                }
            }
            .plusElement(location)
            .windowed(entriesPerLines, entriesPerLines).forEach { paths ->
                singleLineLogger("deleting") {
                    paths.forEach {
                        logLine { "$it" }
                        kotlin.runCatching {
                            check(it.delete(true)) { "$it could not be deleted." }
                            deleted++
                        }.getOrElse { logCaughtException { it } }
                    }
                }
            }

        // TODO kill Docker container
        return "$deleted items deleted"
    }

    fun <T : Path> T.deleteOnExit(): T {
        this@FixtureLog.invoke(this)
        return this
    }

    override fun invoke(path: Path): Path = location.appendLine(path.resolveFixture().logLine())

    fun paths(): List<Path> = if (location.exists) location.readAllLines().map { Path.of(it) } else emptyList()

    private fun Path.logLine() = "${toAbsolutePath()}"

    private fun Path.resolveFixture() = let {
        if (it.hasCommonNameWithParent()) it.parent else it
    }

    private fun Path.hasCommonNameWithParent() = baseName == parent.baseName
}

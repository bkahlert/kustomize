package com.imgcstmzr.util

import java.nio.file.Path

/**
 * A log to keep track to file-based fixture.
 *
 * As soon as an instance is created, all not yet deleted fixtures will be removed.
 */
class FixtureLog(val location: Path) : (Path) -> Unit {
    init {
        paths().forEach { it.delete(true) }
        // TODO delete like in FixtureExtension
        // kill Docker container
        location.delete()
    }

    override fun invoke(path: Path) = location.appendLine(path.resolveFixture().logLine())

    fun paths(): List<Path> = if (location.exists) location.readAllLines().map { Path.of(it) } else emptyList()

    private fun Path.logLine() = "${toAbsolutePath()}"

    private fun Path.resolveFixture() = let { if (it.hasCommonNameWithParent()) it.parent else it }

    private fun Path.hasCommonNameWithParent() = baseName == parent.baseName
}

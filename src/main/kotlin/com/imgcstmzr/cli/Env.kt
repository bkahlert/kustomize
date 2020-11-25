package com.imgcstmzr.cli

import com.bkahlert.koodies.nio.file.exists
import com.bkahlert.koodies.terminal.ansi.AnsiColors.cyan
import com.bkahlert.koodies.terminal.ansi.AnsiColors.yellow
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.bold
import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.runtime.log.RenderingLogger
import com.typesafe.config.ConfigFactory
import java.nio.file.Path

class Env private constructor(logger: RenderingLogger<*>, vararg maps: Map<String, String?>) : Map<String, String?> by maps.flatMap({ it.toList() }).toMap() {
    constructor(logger: RenderingLogger<*>, path: Path) : this(
        logger,
        if (path.exists) {
            ConfigFactory.parseFile(path.toFile()) // TODO remove toFile()
                .root()
                .toMap()
                .mapValues { entry -> entry.value?.unwrapped().toString() }
                .also { logger.logLine { "${it.size} environment properties found in $path".cyan().bold() } }
        } else {
            emptyMap<String, String?>().also { echo("No $path found".yellow()) }
        }.toMap(),
        System.getenv(),
    )
}

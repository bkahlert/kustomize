package com.imgcstmzr.cli

import com.github.ajalt.clikt.output.TermUi.echo
import com.typesafe.config.ConfigFactory
import koodies.logging.RenderingLogger
import koodies.terminal.AnsiColors.cyan
import koodies.terminal.AnsiColors.yellow
import koodies.terminal.AnsiFormats.bold
import java.nio.file.Path
import kotlin.io.path.exists

class Env private constructor(vararg maps: Map<String, String?>) : Map<String, String?> by maps.flatMap({ it.toList() }).toMap() {
    constructor(logger: RenderingLogger, path: Path) : this(
        if (path.exists()) {
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

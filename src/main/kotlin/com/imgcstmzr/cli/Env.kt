package com.imgcstmzr.cli

import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiColors.yellow
import com.github.ajalt.clikt.output.TermUi.echo
import com.typesafe.config.ConfigFactory
import java.nio.file.Path

class Env private constructor(vararg maps: Map<String, String?>) : Map<String, String?> by maps.flatMap({ it.toList() }).toMap() {
    constructor(path: Path) : this(
        if (path.toFile().exists()) {
            ConfigFactory.parseFile(path.toFile())
                .root()
                .toMap()
                .mapValues { entry -> entry.value?.unwrapped().toString() }
                .also { echo((ANSI.termColors.cyan + ANSI.termColors.bold)("${it.size} environment properties found in $path")) }
        } else {
            emptyMap<String, String?>().also { echo("No $path found".yellow()) }
        }.toMap(),
        System.getenv(),
    )
}

package com.imgcstmzr.cli

import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.cli.ColorHelpFormatter.Companion.tc
import com.typesafe.config.ConfigFactory
import java.nio.file.Path

class Env private constructor(vararg maps: Map<String, String?>) : Map<String, String?> by maps.flatMap({ it.toList() }).toMap() {
    constructor(path: Path) : this(
        if (path.toFile().exists()) {
            ConfigFactory.parseFile(path.toFile())
                .root()
                .toMap()
                .mapValues { entry -> entry.value?.unwrapped().toString() }
                .also { echo((tc.cyan + tc.bold)("${it.size} environment properties found in $path")) }
        } else {
            emptyMap<String, String?>().also { echo(tc.yellow("No $path found")) }
        }.toMap(),
        System.getenv(),
    )
}

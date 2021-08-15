package com.bkahlert.kustomize.cli

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigSyntax.CONF
import koodies.text.Semantics.formattedAs
import koodies.tracing.tracing
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.reader

class Env private constructor(vararg maps: Map<String, String?>) : Map<String, String?> by maps.flatMap({ it.toList() }).toMap() {
    constructor(path: Path) : this(
        if (path.exists()) {
            ConfigFactory.parseReader(path.reader(), ConfigParseOptions.defaults().apply {
                syntax = CONF
            })
                .root()
                .toMap()
                .mapValues { entry -> entry.value?.unwrapped().toString() }
                .also { tracing { log("${it.size.formattedAs.input} env properties found in ${path.toUri().formattedAs.input}") } }
        } else {
            tracing { log("${"No env properties loaded.".formattedAs.warning} ${path.toUri().formattedAs.input} does not exist.") }
            emptyMap()
        }.toMap(),
        System.getenv(),
    )
}

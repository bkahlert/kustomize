package com.imgcstmzr.libguestfs

import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.nio.file.toPath
import java.nio.file.Path

open class Option(open val name: String, open val arguments: List<String>) :
    List<String> by listOf(name) + arguments

class DiskOption private constructor(val disk: String) : Option("--add", listOf(disk)) {
    constructor(disk: Path) : this(disk.serialized)

    val path: Path get() = disk.toPath()
}

class ColorsOption : Option("--colors", emptyList())
class QuietOption : Option("--quiet", emptyList())
class VerboseOption : Option("--verbose", emptyList())
class TraceOption : Option("-x", emptyList())


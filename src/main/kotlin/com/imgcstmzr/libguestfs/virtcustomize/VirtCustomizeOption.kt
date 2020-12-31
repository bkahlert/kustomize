package com.imgcstmzr.libguestfs.virtcustomize

import com.imgcstmzr.libguestfs.Option
import koodies.io.path.asString
import java.nio.file.Path


sealed class VirtCustomizeOption(name: String, arguments: List<String>) : Option(name, arguments) {

    class DiskOption(override val disk: Path) : VirtCustomizeOption("--add", listOf(disk.asString())), com.imgcstmzr.libguestfs.DiskOption
    class ColorsOption : VirtCustomizeOption("--colors", emptyList())
    class QuietOption : VirtCustomizeOption("--quiet", emptyList())
    class VerboseOption : VirtCustomizeOption("--verbose", emptyList())
    class TraceOption : VirtCustomizeOption("-x", emptyList())
}

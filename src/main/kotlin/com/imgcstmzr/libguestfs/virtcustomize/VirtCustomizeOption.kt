package com.imgcstmzr.libguestfs.virtcustomize

import com.bkahlert.koodies.nio.file.serialized
import com.imgcstmzr.libguestfs.Option
import java.nio.file.Path


sealed class VirtCustomizeOption(name: String, arguments: List<String>) : Option(name, arguments) {

    class DiskOption(override val disk: Path) : VirtCustomizeOption("--add", listOf(disk.serialized)), com.imgcstmzr.libguestfs.DiskOption
    class ColorsOption : VirtCustomizeOption("--colors", emptyList())
    class QuietOption : VirtCustomizeOption("--quiet", emptyList())
    class VerboseOption : VirtCustomizeOption("--verbose", emptyList())
    class TraceOption : VirtCustomizeOption("-x", emptyList())
}

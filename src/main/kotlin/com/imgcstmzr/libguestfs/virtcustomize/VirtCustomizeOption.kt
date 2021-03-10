package com.imgcstmzr.libguestfs.virtcustomize

import com.imgcstmzr.libguestfs.Option
import koodies.io.path.asString
import java.nio.file.Path


sealed class VirtCustomizeOption(name: String, arguments: List<String>) : Option(name, arguments) {

    /**
     * Not recommend but yet nice to have option to pass options to virt-customize that
     * have no corresponding wrapper.
     */
    class Generic(vararg args: String) : VirtCustomizeOption(args[0], args.drop(1))
    class DiskOption(override val disk: Path) : VirtCustomizeOption("--add", listOf(disk.asString())), com.imgcstmzr.libguestfs.DiskOption
    class ColorsOption : VirtCustomizeOption("--colors", emptyList())
    class QuietOption : VirtCustomizeOption("--quiet", emptyList())
    class VerboseOption : VirtCustomizeOption("--verbose", emptyList())
    class TraceOption : VirtCustomizeOption("-x", emptyList())
}

package com.imgcstmzr.libguestfs.docker

import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeOption
import java.nio.file.Path

class VirtCustomizeDockerAdaptable(val options: List<VirtCustomizeOption>, val customizationOptions: List<VirtCustomizeCustomizationOption>) :
    LibguestfsDockerAdaptable {

    override val command: String get() = VirtCustomizeCommandLine.COMMAND
    override val arguments: List<String>
        get() =
            options.partition { it is VirtCustomizeOption.DiskOption }.second.flatten() + customizationOptions.flatten()
    override val disks: List<Path>
        get() =
            options.partition { it is VirtCustomizeOption.DiskOption }.first.filterIsInstance<VirtCustomizeOption.DiskOption>().map { it.disk }
}

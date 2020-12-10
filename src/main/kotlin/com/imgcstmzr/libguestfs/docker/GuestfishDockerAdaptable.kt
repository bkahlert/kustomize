package com.imgcstmzr.libguestfs.docker

import com.bkahlert.koodies.shell.HereDocBuilder
import com.imgcstmzr.libguestfs.Option
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommand
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine
import com.imgcstmzr.libguestfs.guestfish.GuestfishOption
import java.nio.file.Path

class GuestfishDockerAdaptable(val options: List<Option>, val guestfishCommands: List<GuestfishCommand>) : LibguestfsDockerAdaptable {

    override val command: String get() = GuestfishCommandLine.COMMAND

    override val arguments: List<String> =
        options.filter { it !is GuestfishOption.DiskOption }.flatten() + "--" + commands

    private val commands
        get() = HereDocBuilder.hereDoc {
            guestfishCommands.forEach {
                +it.joinToString(" ")
            }
        }

    override val disks: List<Path>
        get() = options.filterIsInstance<GuestfishOption.DiskOption>().map { it.disk }
}

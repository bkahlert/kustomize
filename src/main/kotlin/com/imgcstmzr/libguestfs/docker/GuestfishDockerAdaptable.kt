package com.imgcstmzr.libguestfs.docker

import com.imgcstmzr.libguestfs.Option
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommand
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine
import com.imgcstmzr.libguestfs.guestfish.GuestfishOption
import koodies.shell.HereDocBuilder.hereDoc
import java.nio.file.Path

class GuestfishDockerAdaptable(
    override val env: Map<String, String>,
    val options: List<Option>,
    val guestfishCommands: List<GuestfishCommand>,
) : LibguestfsDockerAdaptable {

    override val command: String get() = GuestfishCommandLine.COMMAND

    override val arguments: List<String> =
        options.filter { it !is GuestfishOption.DiskOption }.flatten() + "--" + commands

    private val commands
        get() = hereDoc {
            guestfishCommands.forEach {
                +it.joinToString(" ")
            }
        }

    override val disks: List<Path>
        get() = options.filterIsInstance<GuestfishOption.DiskOption>().map { it.disk }
}

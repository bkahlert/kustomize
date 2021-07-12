package com.imgcstmzr.os.linux

import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.CustomizationsBuilder.CustomizationsContext
import com.imgcstmzr.os.LinuxRoot
import koodies.io.path.withDirectoriesCreated
import koodies.text.LineSeparators.lines
import kotlin.io.path.writeLines

/**
 * Creates a [service unit](https://www.freedesktop.org/software/systemd/man/systemd.unit.html)
 * with the given [name] (e.g. `custom.service`), the given [content] and `wantedBy` dependencies
 * for all provided [wantedBys].
 */
fun CustomizationsContext.installService(serviceUnit: ServiceUnit) {
    copyIn(serviceUnit.diskFile) {
        withDirectoriesCreated().writeLines(serviceUnit.text.lines())
    }
    serviceUnit.wantedBy.forEach { wantedBy ->
        val wantedByDirectory = LinuxRoot.etc.systemd.system / "$wantedBy.wants"
        mkdir { wantedByDirectory }

        val wantedByLink = wantedByDirectory / serviceUnit.name
        link { wantedByLink to serviceUnit.diskFile }
    }
}

fun CustomizationsContext.copyIn(serviceScript: ServiceScript) {
    copyIn(serviceScript.diskFile, serviceScript.script.content)
    chmods { "0755" to serviceScript.diskFile }
}

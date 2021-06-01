package com.imgcstmzr.libguestfs

import com.imgcstmzr.libguestfs.FirstBootFix.FIRSTBOOT_SCRIPTS
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.CustomizationsBuilder.CustomizationsContext
import com.imgcstmzr.os.DiskPath
import koodies.io.InMemoryTextFile
import koodies.test.ClassPathFileFixture

/**
 * Script (stored at [ClassPathFileFixture.path]) which fixes the execution
 * order of first boot scripts created using `virt-customize`.
 *
 * See script file itself and corresponding test for more details.
 *
 * @see <a href="https://libguestfs.org/virt-builder.1.html#first-boot-scripts">First Boot Scripts</a>
 * @see <a href="https://libguestfs.org/virt-customize.1.html">virt-customize - Customize a virtual machine</a>
 */
object FirstBootWait : InMemoryTextFile(
    "firstboot-wait.service",
    """
        [Unit]
        Description=Wait for firstboot to finish
        Before=getty@tty1.service getty@tty2.service getty@tty3.service getty@tty4.service getty@tty5.service getty@tty6.service

        [Service]
        Type=oneshot
        ExecStart=while [ -d $FIRSTBOOT_SCRIPTS ] && [ $(ls 2> /dev/null -A $FIRSTBOOT_SCRIPTS) ]; do; echo "Waiting for $FIRSTBOOT_SCRIPTS to be empty"; sleep 5; done
        
    """.trimIndent(),
) {

    private val systemd = DiskPath("/etc/systemd")
    private val serviceFile = systemd.resolve(name)

    fun CustomizationsContext.waitForFirstBootToFinish() {
        copyIn(serviceFile, FirstBootWait.text)
        chmods { "0644" to serviceFile }
    }
}

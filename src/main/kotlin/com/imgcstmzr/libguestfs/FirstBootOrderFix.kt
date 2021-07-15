package com.imgcstmzr.libguestfs

import com.imgcstmzr.os.LinuxRoot
import koodies.io.ClassPathFile

/**
 * Script (stored at [ClassPathFileFixture.path]) which fixes the execution
 * order of first boot scripts created using `virt-customize`.
 *
 * See script file itself and corresponding test for more details.
 *
 * @see <a href="https://libguestfs.org/virt-builder.1.html#first-boot-scripts">First Boot Scripts</a>
 * @see <a href="https://libguestfs.org/virt-customize.1.html">virt-customize - Customize a virtual machine</a>
 */
object FirstBootOrderFix : ClassPathFile("/usr/lib/virt-sysprep/scripts/0000---first-boot-order-fix") {
    /**
     * Directory in which virt-customize
     */
    val VIRT_SYSPREP = LinuxRoot.usr / "lib" / "virt-sysprep"
    val FIRSTBOOT_SCRIPTS = VIRT_SYSPREP / "scripts"
    val FIRSTBOOT_SCRIPTS_DONE = VIRT_SYSPREP / "scripts-done"
    val FIRSTBOOT_FIX = FIRSTBOOT_SCRIPTS / "0000---first-boot-order-fix"
}

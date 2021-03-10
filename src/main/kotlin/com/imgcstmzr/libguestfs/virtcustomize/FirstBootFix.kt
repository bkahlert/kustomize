package com.imgcstmzr.libguestfs.virtcustomize

import com.imgcstmzr.libguestfs.DiskPath
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
object FirstBootFix : ClassPathFileFixture("/usr/lib/virt-sysprep/scripts/0000---fix-order---") {
    /**
     * Directory in which virt-customize
     */
    val VIRT_SYSPREP: DiskPath = DiskPath("/usr/lib/virt-sysprep")
    val FIRSTBOOT_SCRIPTS = VIRT_SYSPREP.resolve("scripts")
    val FIRSTBOOT_FIX = FIRSTBOOT_SCRIPTS.resolve("0000---fix-order---")
}

package com.imgcstmzr.libguestfs

import com.imgcstmzr.libguestfs.FirstBootOrderFix.FIRSTBOOT_SCRIPTS
import com.imgcstmzr.libguestfs.FirstBootOrderFix.FIRSTBOOT_SCRIPTS_DONE
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.CustomizationsBuilder.CustomizationsContext
import com.imgcstmzr.os.DiskDirectory
import com.imgcstmzr.os.linux.ServiceScript
import com.imgcstmzr.os.linux.ServiceUnit
import com.imgcstmzr.os.linux.copyIn
import com.imgcstmzr.os.linux.installService
import koodies.shell.ShellScript
import koodies.text.Banner.banner
import kotlin.io.path.pathString

/**
 * Script that fixes the execution order of first boot scripts created by `virt-customize`.
 *
 * See script file itself and corresponding tests for more details.
 *
 * @see <a href="https://libguestfs.org/virt-builder.1.html#first-boot-scripts">First Boot Scripts</a>
 * @see <a href="https://libguestfs.org/virt-customize.1.html">virt-customize - Customize a virtual machine</a>
 */
object FirstBootWait {

    val serviceName = "firstboot-wait"
    val serviceScript = ServiceScript("$serviceName.sh", waitForEmptyDirectory(FIRSTBOOT_SCRIPTS, FIRSTBOOT_SCRIPTS_DONE))
    val serviceUnit = ServiceUnit("$serviceName.service", """
        [Unit]
        Description=libguestfs firstboot service completion
        After=network.target
        Before=display-manager.service getty.target autologin@tty1.service serial-getty@ttyAMA0.service serial-getty@ttyS0.service
        
        [Service]
        Type=oneshot
        ExecStart=${serviceScript.diskFile}
        RemainAfterExit=yes
        StandardOutput=journal+console
        StandardError=inherit
        
        [Install]
        WantedBy=multi-user.target
    """)

    fun CustomizationsContext.waitForFirstBootToComplete() {
        installService(serviceUnit)
        copyIn(serviceScript)
    }

    fun waitForEmptyDirectory(vararg directories: DiskDirectory): ShellScript = ShellScript {
        shebang
        //language=Shell Script
        """
            fileCount() {
              if [ ! -d "$1" ]; then
                echo "0"
              else
                find "$1" -type f -perm -u+x | wc -l
              fi
            }
            
            fileCounts() {
              sum=0
              for dir in "$@"; do
                count=$(fileCount "${'$'}dir")
                sum=${'$'}((sum + count))
              done
              echo ${'$'}sum 
            }
            
            firstbootRunning() {
              printf '${banner("Checking " + directories.joinToString(", ") { it.fileName.pathString })} … '
              sum=$(fileCounts ${directories.joinToString(" ") { "'$it'" }})
              caption=$([ "${'$'}sum" = "0" ] && echo "completed" || echo "${'$'}sum script(s) to go") 
              printf '%s\n' "${'$'}caption"
              [ ! "${'$'}sum" = "0" ]
            }

            while firstbootRunning; do
                sleep 3
            done
            echo '${banner("All scripts completed. Quitting.")}'
        """.trimIndent()
    }
}

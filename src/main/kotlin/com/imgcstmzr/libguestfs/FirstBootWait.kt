package com.imgcstmzr.libguestfs

import com.imgcstmzr.libguestfs.FirstBootOrderFix.FIRSTBOOT_SCRIPTS
import com.imgcstmzr.libguestfs.FirstBootOrderFix.FIRSTBOOT_SCRIPTS_DONE
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.CustomizationsBuilder.CustomizationsContext
import com.imgcstmzr.os.DiskDirectory
import com.imgcstmzr.os.linux.ServiceScript
import com.imgcstmzr.os.linux.ServiceUnit
import com.imgcstmzr.os.linux.copyIn
import com.imgcstmzr.os.linux.installService
import koodies.io.path.pathString
import koodies.shell.ShellScript
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.Banner.banner

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
    val serviceScript = ServiceScript("$serviceName.sh", trackProgress(FIRSTBOOT_SCRIPTS, FIRSTBOOT_SCRIPTS_DONE))
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

    fun trackProgress(scripts: DiskDirectory, done: DiskDirectory): ShellScript = ShellScript {
        val scriptsToGo = "\$_diff SCRIPT(S) TO GO".ansi.yellow
        val completed = "COMPLETED".ansi.green
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
            
            _total=$(fileCount '$scripts')
            _prevDiff=0
            
            firstbootRunning() {
              scripts=$(fileCount '$scripts')
              done=$(fileCount '$done')
              if [ "${'$'}scripts" = "0" ]; then
                if [ "${'$'}done" = "0" ]; then
                  done=${'$'}_total
                else
                  done=${'$'}((_total - 1))
                fi
              fi
              
              _diff=${'$'}((_total - done))
              
              if [ ! "${'$'}_diff" = "${'$'}_prevDiff" ]; then
                printf '${banner("Checking ${scripts.fileName.pathString} ⮕ ${done.fileName.pathString}", prefix = "")} … '
                
                if [ "${'$'}_diff" = "0" ]; then
                  echo "$completed"
                else
                  echo "$scriptsToGo"
                fi
                
                _prevDiff=${'$'}_diff
              fi
              
              [ ! "${'$'}_diff" = "0" ]
            }

            while firstbootRunning; do
                sleep 3
            done
        """.trimIndent()
    }
}

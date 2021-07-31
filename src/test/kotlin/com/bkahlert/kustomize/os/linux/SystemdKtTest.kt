package com.bkahlert.kustomize.os.linux

import com.bkahlert.kustomize.expectRendered
import com.bkahlert.kustomize.libguestfs.mounted
import com.bkahlert.kustomize.os.OS
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.OperatingSystems.RaspberryPiLite
import com.bkahlert.kustomize.os.boot
import com.bkahlert.kustomize.test.E2E
import koodies.io.path.textContent
import koodies.junit.UniqueId
import koodies.shell.ShellScript
import koodies.text.ansiRemoved
import koodies.text.lines
import koodies.text.matchesCurlyPattern
import koodies.toBaseName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains

class SystemdKtTest {

    @Nested
    inner class InstallService {

        @E2E @Test
        fun `should create service unit and links`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) {
            osImage.virtCustomize {
                installService(ServiceUnit("custom.service", """
                        [Unit]
                        Description=custom description
                        
                        [Service]
                        Type=oneshot
                        ExecStart=echo 'custom service exec'
                        RemainAfterExit=yes
                        StandardOutput=journal+console
                        StandardError=inherit
                        
                        [Install]
                        WantedBy=multi-user.target
                    """))
            }
            osImage.boot(uniqueId.value.toBaseName())
            expectRendered().ansiRemoved {
                contains("Copying: etc/systemd/system/custom.service to /etc/systemd/system")
                contains("Linking: /etc/systemd/system/multi-user.target.wants/custom.service -> /etc/systemd/system/custom.service")
                contains("Starting custom description")
                contains("custom service exec")
                contains("Started custom description")
            }
        }
    }

    @Nested
    inner class CopyIn {

        @E2E @Test
        fun `should copy-in file and make it executable`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
            osImage.virtCustomize {
                copyIn(ServiceScript("service-script.sh", ShellScript { echo("Hello Service!") }))
                firstBoot {
                    !"ls -lisa /etc/systemd/scripts/service-script.sh > /root/dump.txt"
                    shutdown
                }
            }
            osImage.boot()
            expectThat(osImage).mounted {
                path("/root/dump.txt") {
                    @Suppress("SpellCheckingInspection")
                    textContent.lines().any { matchesCurlyPattern("{}-rwxr-xr-x 1 root root {} /etc/systemd/scripts/service-script.sh{}") }
                }
            }
        }
    }
}

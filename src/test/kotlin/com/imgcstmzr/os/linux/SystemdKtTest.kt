package com.imgcstmzr.os.linux

import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystemProcess.Companion.DockerPiImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.os.boot
import com.imgcstmzr.test.OS
import koodies.docker.DockerRequiring
import koodies.logging.InMemoryLogger
import koodies.logging.expectLogged
import koodies.test.FifteenMinutesTimeout
import koodies.test.UniqueId
import koodies.text.ansiRemoved
import koodies.toBaseName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.assertions.contains

class SystemdKtTest {

    @Nested
    inner class InstallService {

        @FifteenMinutesTimeout @DockerRequiring([DockerPiImage::class]) @Test
        fun InMemoryLogger.`should create service unit and links`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) {
            osImage.virtCustomize(this) {
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
            osImage.boot(uniqueId.value.toBaseName(), this)
            expectLogged.ansiRemoved.contains("Copying: etc/systemd/system/custom.service to /etc/systemd/system") and {
                contains("Linking: /etc/systemd/system/multi-user.target.wants/custom.service -> /etc/systemd/system/custom.service")
                contains("Starting custom description")
                contains("custom service exec")
                contains("Started custom description")
            }
        }
    }
}

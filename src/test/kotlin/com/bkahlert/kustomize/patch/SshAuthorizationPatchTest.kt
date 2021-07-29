package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.expectRendered
import com.bkahlert.kustomize.libguestfs.LibguestfsImage
import com.bkahlert.kustomize.libguestfs.mounted
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OS
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.OperatingSystems.RaspberryPiLite
import koodies.docker.DockerRequiring
import koodies.io.path.hasContent
import koodies.test.FifteenMinutesTimeout
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.contains
import koodies.text.ansiRemoved
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.java.exists

class SshAuthorizationPatchTest {

    @FifteenMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @Test
    fun `should add ssh key`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {

        osImage.patch(SshAuthorizationPatch("pi", listOf("123")))

        expectRendered().ansiRemoved {
            contains("SSH key inject: pi")
        }
        expectThat(osImage).mounted {
            path(LinuxRoot.home / "pi" / ".ssh" / "authorized_keys") {
                exists()
                hasContent("123$LF")
            }
        }
    }
}

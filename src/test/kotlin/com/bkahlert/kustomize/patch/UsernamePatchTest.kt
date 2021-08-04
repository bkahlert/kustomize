package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.libguestfs.mounted
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OS
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.OperatingSystems.RaspberryPiLite
import com.bkahlert.kustomize.test.E2E
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.java.exists

class UsernamePatchTest {

    @E2E @Test
    fun `should log in with updated username`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        val newUsername = "john.doe".also { check(it != osImage.defaultCredentials.username) { "$it is already the default username." } }

        osImage.patch(CompositePatch(UsernamePatch(osImage.defaultCredentials.username, newUsername)))

        expectThat(osImage).mounted {
            path(LinuxRoot.home / newUsername) { exists() }
            path(LinuxRoot.home / osImage.defaultCredentials.username) { not { exists() } }
        }
    }
}

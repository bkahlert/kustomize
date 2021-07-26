package com.imgcstmzr

import com.imgcstmzr.cli.CustomizeCommand
import com.imgcstmzr.cli.ProjectDirectory
import com.imgcstmzr.libguestfs.mounted
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystemImage.Companion.based
import com.imgcstmzr.os.OperatingSystems
import com.imgcstmzr.test.E2E
import koodies.io.path.copyToDirectory
import koodies.io.path.extensionOrNull
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.io.path.pathString
import koodies.io.path.textContent
import koodies.io.useRequiredClassPath
import koodies.junit.UniqueId
import koodies.test.Smoke
import koodies.test.SystemProperties
import koodies.test.SystemProperty
import koodies.test.withTempDir
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import strikt.api.expectThat
import strikt.assertions.isNotEmpty

class ImgCstmzrIntegrationTest {

    @SystemProperties(
        SystemProperty("IMG_CSTMZR_USERNAME", "john.doe"),
        SystemProperty("IMG_CSTMZR_PASSWORD", "Password1234"),
        SystemProperty("IMG_CSTMZR_WPA_SUPPLICANT", "entry1\nentry2"),
        SystemProperty("INDIVIDUAL_KEY", "B{1:Βϊ𝌁\uD834\uDF57"),
    )
    @E2E @Smoke @Test
    fun `should apply patches`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val configFile = useRequiredClassPath("sample.conf") { it.copyToDirectory(this) }

        CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString))

        val projectDirectory = ProjectDirectory(ImgCstmzr.Cache).workDirs.minByOrNull { it.age } ?: fail("Failed to locate project directory.")

        val osImage = OperatingSystems.RaspberryPiLite based projectDirectory.single { it.extensionOrNull.equals("img", ignoreCase = true) }

        expectThat(osImage).mounted {
            path(LinuxRoot.etc.hostname) { textContent.matchesCurlyPattern("demo--{}") }
            path(LinuxRoot.home / "john.doe") { get { listDirectoryEntriesRecursively() }.isNotEmpty() }
        }
    }
}

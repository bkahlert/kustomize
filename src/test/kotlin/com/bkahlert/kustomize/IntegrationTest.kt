package com.bkahlert.kustomize

import com.bkahlert.kustomize.Kustomize.WorkingDirectory
import com.bkahlert.kustomize.cli.CustomizeCommand
import com.bkahlert.kustomize.libguestfs.mounted
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.OperatingSystemImage.Companion.based
import com.bkahlert.kustomize.os.OperatingSystems.RaspberryPiLite
import com.bkahlert.kustomize.test.E2E
import koodies.io.path.age
import koodies.io.path.extensionOrNull
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.io.path.textContent
import koodies.test.Smoke
import koodies.test.SystemProperties
import koodies.test.SystemProperty
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import strikt.api.expectThat
import strikt.assertions.isNotEmpty
import kotlin.io.path.div
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

class IntegrationTest {

    @E2E @Smoke @Test
    fun `should customize with sample configuration`() {

        CustomizeCommand().parse(arrayOf("--config-file", "sample.conf", "--cache-dir", ".cache"))

        val osImage = findImage("sample")

        expectThat(osImage).mounted {
            path(LinuxRoot.etc.hostname) { textContent.matchesCurlyPattern("sample--{}") }
        }
    }

    @SystemProperties(
        SystemProperty("SAMPLE_FULL_USERNAME", "john.doe"),
        SystemProperty("SAMPLE_FULL_PASSWORD", "Password1234"),
        SystemProperty("SAMPLE_FULL_WPA_SUPPLICANT", "entry1\nentry2"),
    )
    @E2E @Test
    fun `should customize with full sample configuration`() {
        CustomizeCommand().parse(arrayOf("--config-file", "sample-full.conf", "--cache-dir", ".cache"))
        val osImage = findImage("sample-full")
        expectThat(osImage).mounted {
            path(LinuxRoot.etc.hostname) { textContent.matchesCurlyPattern("sample-full--{}") }
            path(LinuxRoot.home / "john.doe") { get { listDirectoryEntriesRecursively() }.isNotEmpty() }
        }
    }

    private fun findImage(projectName: String): OperatingSystemImage {
        val projectDirectory = WorkingDirectory / ".cache" / projectName
        val workDirectory = projectDirectory.listDirectoryEntries().filter { it.isDirectory() }.minByOrNull { it.age } ?: fail("Work directory not found")
        val image = workDirectory.listDirectoryEntries().singleOrNull { it.extensionOrNull.equals("img", ignoreCase = true) } ?: fail("Image not found")
        val osImage = RaspberryPiLite based image
        return osImage
    }
}

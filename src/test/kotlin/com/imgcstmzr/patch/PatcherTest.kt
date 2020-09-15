package com.imgcstmzr.patch

import com.imgcstmzr.process.Guestfish
import com.imgcstmzr.process.Guestfish.Companion.copyOutCommands
import com.imgcstmzr.util.FixtureExtension
import com.imgcstmzr.util.copyToTempSiblingDirectory
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.exists
import strikt.assertions.isSuccess
import java.nio.file.Path

@Execution(ExecutionMode.CONCURRENT)
@Suppress("RedundantInnerClassModifier")
@ExtendWith(FixtureExtension::class)
internal class PatcherTest {

    @Disabled
    @TestFactory
    fun `patches should not throw`(img: Path) =
        listOf(
            SshPatch(),
            UsbOnTheGoPatch(),
            UsernamePatch("oldUser", "newUser"),
            PasswordPatch("newUser", "newPassword"),
        )
            .map { patch ->
                val root = FixtureExtension.prepareSharedDirectory()
                dynamicTest("${patch.name} on ${root.fileName}") {
                    val patcher = Patcher()
                    expectCatching { patcher(img, patch) }.isSuccess()
                }
            }.toList()

    @Test
    internal fun `should prepare root directory then patch and copy everything back`(img: Path) {
        val patcher = Patcher()

        patcher.invoke(img, SshPatch())

        val guestfish = Guestfish(img.copyToTempSiblingDirectory())
        guestfish.run(copyOutCommands(listOf(Path.of("/boot/ssh"))))
        expectThat(guestfish.guestRootOnHost).get { resolve("boot/ssh") }.exists()
    }
}

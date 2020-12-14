package com.imgcstmzr.patch

import com.bkahlert.koodies.shell.ShellScript
import com.bkahlert.koodies.test.junit.FiveMinutesTimeout
import com.imgcstmzr.E2E
import com.imgcstmzr.libguestfs.resolveOnHost
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.hasContent
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.logging.expectThatLogged
import com.imgcstmzr.util.matches
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.first

@Execution(CONCURRENT)
class ShellScriptPatchTest {

    private val patch = ShellScriptPatch(RaspberryPiLite, ShellScript {
        !"touch /root/shell-script-test.txt"
        !"echo 'Frank was here; went to get beer.' > /root/shell-script-test.txt"
    })

    @Test
    fun `should provide firstboot command`(osImage: OperatingSystemImage) {
        expectThat(patch).matches(customizationOptionsAssertion = {
            first().get { invoke(osImage) }.get {
                osImage.resolveOnHost(this.get(1))
            }.hasContent("""
                touch /root/shell-script-test.txt
                echo 'Frank was here; went to get beer.' > /root/shell-script-test.txt

            """.trimIndent())
        })
    }

    @FiveMinutesTimeout @E2E @Test
    fun InMemoryLogger.`should run shell script`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        with(patch) { patch(osImage) }
        osImage.boot(this, osImage.compileScript("read log", "sudo cat ~root/virt-sysprep-firstboot.log"))
        expectThatLogged().contains("=== Running /usr/lib/virt-sysprep/scripts/0001--shared")
    }
}

package com.imgcstmzr.patch

import com.imgcstmzr.util.FixtureExtension
import com.imgcstmzr.util.containsContent
import com.imgcstmzr.util.containsContentAtMost
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat

@Execution(ExecutionMode.CONCURRENT)
@Suppress("RedundantInnerClassModifier")
internal class UsbOnTheGoPatchTest {

    @Test
    internal fun `should patch configtxt and cmdlinetxt ssh file`() {
        val root = FixtureExtension.prepareSharedDirectory()
            .also { expectThat(it).get { resolve("boot/cmdline.txt") }.not { this.containsContent("g_ether,g_webcam") } }
        val usbOnTheGoPatch = UsbOnTheGoPatch()

        usbOnTheGoPatch(root)

        expectThat(root).get { resolve("boot/cmdline.txt") }.containsContent("g_ether,g_webcam")
    }

    @Test
    internal fun `should not patch twice `() {
        val root = FixtureExtension.prepareSharedDirectory()
        val usbOnTheGoPatch = UsbOnTheGoPatch()

        (0..10).forEach { usbOnTheGoPatch(root) }

        expectThat(root).get { resolve("boot/cmdline.txt") }
            .containsContentAtMost("dwc2", 1)
            .containsContentAtMost("g_ether", 1)
            .containsContentAtMost("g_webcam", 1)
    }
}

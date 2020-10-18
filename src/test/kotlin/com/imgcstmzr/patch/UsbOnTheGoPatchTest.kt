package com.imgcstmzr.patch

import com.imgcstmzr.util.FixtureResolverExtension
import com.imgcstmzr.util.asRootFor
import com.imgcstmzr.util.containsContent
import com.imgcstmzr.util.containsContentAtMost
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.matches
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.hasSize

@Execution(ExecutionMode.CONCURRENT)
internal class UsbOnTheGoPatchTest {

    @Test
    internal fun `should not do anything but patch files n+2 times`() {
        val nModules = (1..4).map { index -> "module-$index" }
        expectThat(UsbOnTheGoPatch(nModules)).matches(fileSystemOperationsAssertion = { hasSize(nModules.size + 2) })
    }

    @Test
    internal fun `should patch configtxt and cmdlinetxt file`(logger: InMemoryLogger<Any>) {
        val root = FixtureResolverExtension.prepareSharedDirectory()
            .also { expectThat(it).get { resolve("boot/cmdline.txt") }.not { this.containsContent("g_ether,g_webcam") } }
        val usbOnTheGoPatch = UsbOnTheGoPatch(listOf("foo", "bar"))

        usbOnTheGoPatch.fileSystemOperations.onEach { op ->
            op(root.asRootFor(op.target), logger)
        }

        expectThat(root).get { resolve("boot/config.txt") }.containsContent("dtoverlay=dwc2")
        expectThat(root).get { resolve("boot/cmdline.txt") }.containsContent("dwc2").and { containsContent("foo,bar") }
    }

    @Test
    internal fun `should not patch twice`(logger: InMemoryLogger<Any>) {
        val root = FixtureResolverExtension.prepareSharedDirectory()
        val usbOnTheGoPatch = UsbOnTheGoPatch(listOf("foo", "bar"))

        (1..10).onEach { i ->
            usbOnTheGoPatch.fileSystemOperations.onEach { op ->
                op(root.asRootFor(op.target), logger)
            }
        }

        expectThat(root).get { resolve("boot/config.txt") }.containsContentAtMost("dtoverlay=dwc2")
        expectThat(root).get { resolve("boot/cmdline.txt") }.containsContentAtMost("dwc2").and { containsContentAtMost("foo,bar") }
    }
}

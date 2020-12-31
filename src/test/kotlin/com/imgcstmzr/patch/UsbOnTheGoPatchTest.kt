package com.imgcstmzr.patch

import com.imgcstmzr.test.FixtureResolverExtension.Companion.prepareSharedDirectory
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.test.containsContent
import com.imgcstmzr.test.containsContentAtMost
import com.imgcstmzr.util.asRootFor
import com.imgcstmzr.withTempDir
import koodies.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.hasSize

@Execution(CONCURRENT)
class UsbOnTheGoPatchTest {

    @Test
    fun `should not do anything but patch files 3 times`() {
        expectThat(UsbOnTheGoPatch("foo")).matches(fileSystemOperationsAssertion = { hasSize(3) })
    }

    @Test
    fun InMemoryLogger.`should patch configtxt and cmdlinetxt file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val root = prepareSharedDirectory()
            .also {
                expectThat(it).get { resolve("boot/cmdline.txt") }.not { containsContent("g_ether,g_webcam") }
            }
        val usbOnTheGoPatch = UsbOnTheGoPatch("foo")

        usbOnTheGoPatch.fileOperations.onEach { op ->
            op(root.asRootFor(op.target), this@`should patch configtxt and cmdlinetxt file`)
        }

        expectThat(root).get { resolve("boot/config.txt") }.containsContent("dtoverlay=dwc2")
        expectThat(root).get { resolve("boot/cmdline.txt") }.containsContent("dwc2,foo")
    }

    @Test
    fun InMemoryLogger.`should not patch twice`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val root = prepareSharedDirectory()
        val usbOnTheGoPatch = UsbOnTheGoPatch("foo")

        (1..10).onEach { i ->
            usbOnTheGoPatch.fileOperations.onEach { op ->
                op(root.asRootFor(op.target), this@`should not patch twice`)
            }
        }

        expectThat(root).get { resolve("boot/config.txt") }.containsContentAtMost("dtoverlay=dwc2")
        expectThat(root).get { resolve("boot/cmdline.txt") }.containsContentAtMost("dwc2,foo")
    }
}

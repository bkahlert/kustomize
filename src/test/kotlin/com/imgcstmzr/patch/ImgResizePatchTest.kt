package com.imgcstmzr.patch

import com.bkahlert.koodies.unit.Mebi
import com.bkahlert.koodies.unit.Size
import com.bkahlert.koodies.unit.bytes
import com.bkahlert.koodies.unit.size
import com.imgcstmzr.runtime.OperatingSystemMock
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.util.DockerRequired
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.matches
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import java.nio.file.Path

@Execution(ExecutionMode.CONCURRENT)
internal class ImgResizePatchTest {

    @Test
    internal fun `should provide commands`(logger: InMemoryLogger<Any>) {
        expectThat(ImgResizePatch(10.Mebi.bytes)).matches(imgOperationsAssertion = {
            hasSize(1)
            get { this[0] }.assert("") { op ->
                val os = object : OperatingSystemMock() {
                    var size: Size? = null
                    var img: Path? = null
                    var runtime: Runtime? = null
                    override fun increaseDiskSpace(
                        size: Size,
                        img: Path,
                        parentLogger: BlockRenderingLogger<Any>?,
                    ): Any {
                        this.size = size
                        this.img = img
                        return size
                    }
                }
                val img = Path.of("foo")
                op(os, img, logger)
                expectThat(os.size).isEqualTo(10.Mebi.bytes)
                expectThat(os.img).isEqualTo(img)
            }
        })
    }

    @Test
    @DockerRequired
    internal fun `should increase size`(@OS(OperatingSystems.RaspberryPiLite::class) img: Path) {
        val oldSize = img.size
        val newSize = img.size + 10.Mebi.bytes
        val patch = ImgResizePatch(newSize)

        patch.patch(img)

        expectThat(img.size)
            .isEqualTo(newSize)
            .isNotEqualTo(oldSize)
    }
}

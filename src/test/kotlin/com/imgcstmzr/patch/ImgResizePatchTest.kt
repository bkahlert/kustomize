package com.imgcstmzr.patch

import com.bkahlert.koodies.unit.Mebi
import com.bkahlert.koodies.unit.bytes
import com.bkahlert.koodies.unit.size
import com.imgcstmzr.util.DockerRequired
import com.imgcstmzr.util.FixtureExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import java.nio.file.Path

@Execution(ExecutionMode.CONCURRENT)
@Suppress("RedundantInnerClassModifier")
@ExtendWith(FixtureExtension::class)
internal class ImgResizePatchTest {

    @Test
    internal fun `should provide commands`() {
        val patch = ImgResizePatch(10.Mebi.bytes)

        val commands = patch.commands

        expectThat(commands)
            .containsExactly("xxx")
    }

    @Test
    @DockerRequired
    internal fun `should increase size`(img: Path) {
        val oldSize = img.size
        val newSize = img.size + 10.Mebi.bytes
        val patch = ImgResizePatch(newSize)

        val commands = Patcher().invoke(img, patch)

        expectThat(img.size)
            .isEqualTo(newSize)
            .isNotEqualTo(oldSize)
    }
}

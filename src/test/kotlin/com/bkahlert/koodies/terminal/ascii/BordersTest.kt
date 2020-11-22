package com.bkahlert.koodies.terminal.ascii

import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.terminal.ascii.Draw.Companion.draw
import com.imgcstmzr.util.containsOnlyCharacters
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class BordersTest {

    @TestFactory
    fun `should provide extended member function with corresponding names to serve as an overview`() =
        Borders.values().flatMap { border: Borders ->
            val matrix = border.matrix
            listOf(
                dynamicTest("${border.name}\n$matrix") {
                    val staticallyRendered = border.name.wrapWithBorder(matrix, 2, 4, ANSI.termColors.hsv(270, 50, 50))

                    val memberFun = "".draw.border::class.members.single { it.name == border.name.decapitalize() }
                    val renderedMember = memberFun.call(border.name.draw.border, 2, 4, ANSI.termColors.hsv(270, 50, 50))

                    expectThat(staticallyRendered)
                        .isEqualTo(renderedMember.toString())
                        .get { removeEscapeSequences() }.containsOnlyCharacters((matrix + border.name).toCharArray())
                }
            )
        }
}


package koodies.logging

import koodies.exec.mock.ExecMock
import koodies.logging.FixedWidthRenderingLogger.Border
import koodies.logging.FixedWidthRenderingLogger.Border.DOTTED
import koodies.logging.FixedWidthRenderingLogger.Border.NONE
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
import koodies.test.output.InMemoryLoggerFactory
import koodies.test.testEach
import koodies.text.Semantics.Symbols
import koodies.text.matchesCurlyPattern
import koodies.toSimpleString
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

private fun InMemoryLoggerFactory.render(border: Border, captionSuffix: String, block: RenderingLogger.() -> Any?): String {
    val logger = createLogger(captionSuffix, border)
    logger.runLogging(block)
    return logger.toString()
}

package com.bkahlert.kustomize.os

import com.bkahlert.kommons.test.testEach
import org.junit.jupiter.api.TestFactory
import strikt.assertions.matches

class OperatingSystemTest {

    @TestFactory
    @Suppress("SpellCheckingInspection")
    fun `should detect dead end line`() = testEach(
        "You are in emergency mode. After logging in, type \"journalctl -xb\" to view",
        "You are in EMERGENCY MODE. After logging in, type \"journalctl -xb\" to view",
        "in emergency mode",
    ) { asserting { matches(OperatingSystem.DEFAULT_DEAD_END_PATTERN) } }

    @TestFactory
    fun `should detect non-dead end line`() = testEach(
        "emergency",
        "",
        "   ",
        "anything",
        "anything else:",
    ) { asserting { not { matches(OperatingSystem.DEFAULT_DEAD_END_PATTERN) } } }
}

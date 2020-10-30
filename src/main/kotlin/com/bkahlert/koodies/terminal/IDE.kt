package com.bkahlert.koodies.terminal

import com.bkahlert.koodies.string.anyContainsAny
import com.github.ajalt.mordant.TermColors
import com.github.ajalt.mordant.TermColors.Level.NONE
import com.github.ajalt.mordant.TermColors.Level.TRUECOLOR
import com.github.ajalt.mordant.TerminalCapabilities.detectANSISupport
import java.lang.management.ManagementFactory.getRuntimeMXBean

object IDE {
    val jvmArgs: List<String> get() = getRuntimeMXBean().inputArguments
    val jvmJavaAgents = jvmArgs.filter { it.startsWith("-javaagent") }

    val intellijTraits = listOf("jetbrains", "intellij", "idea", "idea_rt.jar")
    val isIntelliJ: Boolean get() = kotlin.runCatching { jvmJavaAgents.anyContainsAny(intellijTraits) }.getOrElse { false }

    val ansiSupport: TermColors.Level get() = detectANSISupport().takeUnless { it == NONE } ?: if (isIntelliJ) TRUECOLOR else NONE
}

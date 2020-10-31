package com.bkahlert.koodies.test.junit.debug

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated

/**
 * Annotated [Test] methods are run [Isolated] and sibling tests and their descendants
 * are ignored.
 */
@Isolated
@Execution(ExecutionMode.SAME_THREAD)
annotation class Debug(val includeInReport: Boolean = true)

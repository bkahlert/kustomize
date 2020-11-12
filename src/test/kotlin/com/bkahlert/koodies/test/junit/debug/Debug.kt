package com.bkahlert.koodies.test.junit.debug

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import org.junit.jupiter.api.parallel.Isolated

/**
 * Annotated [Test] methods are run [Isolated] and sibling tests and their descendants
 * are ignored.
 */
@Isolated
@Execution(SAME_THREAD)
annotation class Debug(val includeInReport: Boolean = true)

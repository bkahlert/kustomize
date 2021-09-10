package com.bkahlert.kommons.junit

import com.bkahlert.kommons.test.allTests
import com.bkahlert.kommons.test.isAnnotated
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan

class Verbosity : TestExecutionListener {

    companion object {
        // Hack but wouldn't know how else to get the number of tests
        // in order to activate logging.
        var testCount: Int? = null

        fun ExtensionContext.isVerbose(): Boolean = isAnnotated<Verbose>() || testCount == 1
    }

    override fun testPlanExecutionStarted(testPlan: TestPlan) {
        testCount = testPlan.allTests.size
    }
}

/**
 * By default, the output of a test is only printed
 * if it is the only test in the current test plan.
 *
 * By adding this annotation to the test method,
 * the output is always printed.
 */
annotation class Verbose

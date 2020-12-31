package com.imgcstmzr.test

import com.imgcstmzr.test.debug.Debug
import com.imgcstmzr.test.debug.DebugCondition.Companion.currentIsDebug
import koodies.test.allTests
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan

class Verbosity : TestExecutionListener {
    companion object {
        // Hack but wouldn't know how else to get the number of tests
        // in order to activate logging.
        var testCount: Int? = null

        val ExtensionContext.isVerbose: Boolean get() = currentIsDebug || testCount == 1
        val ParameterContext.isVerbose: Boolean get() = parameter.isA<Debug>()
    }

    override fun testPlanExecutionStarted(testPlan: TestPlan) {
        testCount = testPlan.allTests.size
    }
}

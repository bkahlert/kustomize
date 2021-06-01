package com.imgcstmzr.os

import koodies.exec.mock.ExecMock
import koodies.logging.FixedWidthRenderingLogger
import koodies.text.quoted

class OperatingSystemProcessMock(testName: String, val mock: ExecMock, override val logger: FixedWidthRenderingLogger) :
    OperatingSystemProcess(OperatingSystemMock("mock for test ${testName.quoted}"), mock, logger) {

    fun start(testName: String): OperatingSystemProcessMock =
        OperatingSystemProcessMock(testName, mock, logger)
}

package com.imgcstmzr.runtime

import koodies.logging.RenderingLogger
import koodies.process.ManagedProcessMock
import koodies.text.quoted

class OperatingSystemProcessMock(testName: String, val mock: ManagedProcessMock) :
    OperatingSystemProcess(OperatingSystemMock("mock for test ${testName.quoted}"), mock, mock.logger) {
    override val logger: RenderingLogger = mock.logger

    fun start(testName: String): OperatingSystemProcessMock =
        OperatingSystemProcessMock(testName, mock)
}

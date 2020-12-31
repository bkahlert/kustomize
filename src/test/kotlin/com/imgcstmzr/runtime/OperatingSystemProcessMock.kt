package com.imgcstmzr.runtime

import koodies.logging.BlockRenderingLogger
import koodies.process.ManagedProcessMock
import koodies.text.quoted

class OperatingSystemProcessMock(testName: String, val mock: ManagedProcessMock) :
    OperatingSystemProcess(OperatingSystemMock("mock for test ${testName.quoted}"), mock, mock.logger) {
    override val logger: BlockRenderingLogger = mock.logger

    fun start(testName: String): OperatingSystemProcessMock =
        OperatingSystemProcessMock(testName, mock)
}

package com.imgcstmzr.os

import koodies.exec.mock.ExecMock
import koodies.text.quoted
import koodies.tracing.CurrentSpan

class OperatingSystemProcessMock(testName: String, private val mock: ExecMock, override val span: CurrentSpan) :
    OperatingSystemProcess(OperatingSystemMock("mock for test ${testName.quoted}"), mock, span) {

    fun start(testName: String): OperatingSystemProcessMock =
        OperatingSystemProcessMock(testName, mock, span)
}

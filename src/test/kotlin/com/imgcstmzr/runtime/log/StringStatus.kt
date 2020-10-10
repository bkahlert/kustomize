package com.imgcstmzr.runtime.log

import com.imgcstmzr.runtime.HasStatus

internal inline class StringStatus(val status: String) : HasStatus {
    override fun status(): String = status
}

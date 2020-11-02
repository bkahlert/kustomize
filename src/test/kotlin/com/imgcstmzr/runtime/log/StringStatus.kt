package com.imgcstmzr.runtime.log

import com.imgcstmzr.runtime.HasStatus

inline class StringStatus(val status: String) : HasStatus {
    override fun status(): String = status
}

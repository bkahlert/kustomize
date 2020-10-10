package com.imgcstmzr.runtime.log

import com.imgcstmzr.util.slf4jFormat
import org.slf4j.Marker
import org.slf4j.helpers.SubstituteLogger

/**
 * Logger that redirects all INFO logs to the given consumer.
 */
@Deprecated(message = "Remove after CommandLineRunner is removed")
class InfoAdaptingLogger(name: String, private val consumer: (String) -> Unit) : SubstituteLogger(name, null, true) {
    override fun isInfoEnabled(): Boolean = true

    override fun info(msg: String) {
        consumer(msg)
    }

    override fun info(format: String, arg: Any) {
        this.consumer(slf4jFormat(format, arg))
    }

    override fun info(format: String, arg1: Any, arg2: Any) {
        this.consumer(slf4jFormat(format, arg1, arg2))
    }

    override fun info(format: String, vararg arguments: Any) {
        this.consumer(slf4jFormat(format, arguments))
    }

    override fun info(msg: String, t: Throwable) {
        this.consumer(slf4jFormat(msg, t))
    }

    override fun isInfoEnabled(marker: Marker): Boolean = true
}

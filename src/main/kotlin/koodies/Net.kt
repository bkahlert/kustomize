package koodies

import koodies.collections.caseIgnoringKeys
import koodies.time.toIntMilliseconds
import java.net.URI
import java.net.URL
import kotlin.time.Duration
import kotlin.time.seconds

fun URL.headers(connectTimeout: Duration = 5.seconds, readTimeout: Duration = 5.seconds) = openConnection().run {
    this.connectTimeout = connectTimeout.toIntMilliseconds()
    this.readTimeout = readTimeout.toIntMilliseconds()

    headerFields.toMutableMap().apply {
        put("status", get(null) ?: emptyList())
    }.caseIgnoringKeys()
}

fun URI.headers(connectTimeout: Duration = 5.seconds, readTimeout: Duration = 5.seconds) =
    toURL().headers(connectTimeout, readTimeout)

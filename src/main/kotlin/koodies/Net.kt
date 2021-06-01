package koodies

import koodies.collections.matchKeysByIgnoringCase
import koodies.time.toIntMilliseconds
import koodies.time.seconds
import java.net.URI
import java.net.URL
import kotlin.time.Duration

fun URL.headers(connectTimeout: Duration = 5.seconds, readTimeout: Duration = 5.seconds) = openConnection().run {
    this.connectTimeout = connectTimeout.toIntMilliseconds()
    this.readTimeout = readTimeout.toIntMilliseconds()

    headerFields.toMutableMap().apply {
        put("status", get(null) ?: emptyList())
    }.matchKeysByIgnoringCase()
}

fun URI.headers(connectTimeout: Duration = 5.seconds, readTimeout: Duration = 5.seconds) =
    toURL().headers(connectTimeout, readTimeout)

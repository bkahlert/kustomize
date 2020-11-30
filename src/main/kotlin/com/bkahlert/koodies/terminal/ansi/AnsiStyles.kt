package com.bkahlert.koodies.terminal.ansi

object AnsiStyles {
    /**
     * `·<❮❰❰❰ echo ❱❱❱❯>·`
     */
    fun CharSequence.echo(): String = "·<❮❰❰❰ $this ❱❱❱❯>·"

    /**
     * `͔˱❮❰( saying`
     */
    fun CharSequence.saying(): String = "͔˱❮❰( $this"

    /**
     * `【tag】`
     */
    fun CharSequence.tag(): String = "【$this】"

    /**
     * `【tag1, tag2, tag3, ...】`
     */
    fun <T> Iterable<out T>.tags(transform: (T) -> String = { it.toString() }): String =
        joinToString(prefix = "【", separator = ", ", postfix = "】", transform = transform)

    /**
     * `【tag1, tag2, tag3, ...】`
     */
    fun <T> Array<out T>.tags(serializer: (T) -> String = { it.toString() }): String = toList().tags(transform = serializer)

    /**
     * `❲unit❳`
     */
    fun CharSequence.unit(): String = "❲$this❳"
}

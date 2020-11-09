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
     * `❲unit❳`
     */
    fun CharSequence.unit(): String = "❲$this❳"
}

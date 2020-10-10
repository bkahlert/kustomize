package com.bkahlert.koodies.terminal.ascii

enum class Boxes(var box: String) {
    FAIL("""
        ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄
        ████▌▄▌▄▐▐▌█████
        ████▌▄▌▄▐▐▌▀████
        ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀
    """.trimIndent())
    ;

    override fun toString(): String = box
}

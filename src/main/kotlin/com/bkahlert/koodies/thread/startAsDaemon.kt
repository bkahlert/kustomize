package com.bkahlert.koodies.thread

@JvmName("startReceiverAsDaemon")
fun startAsDaemon(work: () -> Any?): Thread {
    return Thread {
        work()
    }.apply {
        isDaemon = true
        start()
    }
}

fun (() -> Any?).startAsDaemon(): Thread {
    return Thread {
        invoke()
    }.apply {
        isDaemon = true
        start()
    }
}

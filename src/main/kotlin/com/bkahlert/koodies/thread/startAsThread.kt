package com.bkahlert.koodies.thread

@JvmName("startReceiverAsThread")
fun (() -> Any?).startAsThread(): Thread {
    return Thread {
        invoke()
    }.apply { start() }
}

fun startAsThread(work: () -> Any?): Thread {
    return Thread {
        work()
    }.apply { start() }
}

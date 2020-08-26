package com.imgcstmzr.process

import com.github.ajalt.clikt.output.TermUi
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.util.concurrent.TimeUnit

fun runProcess(
    vararg args: String, blocking: Boolean = true,
    inputRedirect: ProcessBuilder.Redirect = ProcessBuilder.Redirect.PIPE,
    outputRedirect: ProcessBuilder.Redirect = ProcessBuilder.Redirect.INHERIT,
    errorRedirect: ProcessBuilder.Redirect = ProcessBuilder.Redirect.PIPE,
): Int? {
    val cmd: String = args.joinToString(" ")
    return CommandLineRunner(blocking)
        .startProcessAndWaitForCompletion(
            File("/bin").toPath(),
            "sh -c '$cmd'",
            inputRedirect,
            outputRedirect,
            errorRedirect,
            TermUi::echo).get()
        ?: throw IllegalStateException("This should never happen")
}

fun Process.input(vararg input: String) {
    val stdin = BufferedWriter(OutputStreamWriter(this.outputStream))
    input.forEach {
        TimeUnit.MILLISECONDS.sleep(10)
        stdin.write(it)
        stdin.flush()
    }
}

fun Process.enter(vararg input: String) {
    input("\r")
}

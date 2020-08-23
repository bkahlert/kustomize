package com.imgcstmzr

import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.CommandLineRunner.Origin
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit


class ArmRuntime(val name: String, val img: File) {
    init {
        var inputWriter: InputWriter? = null
        var state = "init"
        val cmd: String = startCommandLine()
        CommandLineRunner().startProcessAndWaitForCompletion(
            File("/bin").toPath(),
            "sh -c '$cmd'"
        ) { process: Process, origin: Origin, str: String ->
            echo(str)
            if (inputWriter == null) inputWriter = process.inputWriter()
            when (state) {
                "init" -> {
                    if (str.startsWith("raspberrypi login:")) {
                        state = "username"
                    }
                }
                "username" -> {
                    inputWriter?.write("pi\r")
                    state = "password"
                }
                "password" -> {
                    inputWriter?.write("raspberry\r")
                    state = "cat"
                }
                "cat" -> {
                    inputWriter?.write("cat test\r")
                    state = "success"
                }
                "success" -> {
//                    echo("finished")
                }
            }
        }.get() ?: throw IllegalStateException("This should never happen")
    }

    private fun startCommandLine(): String = arrayOf(
        "docker",
        "run",
        "--name", "\"$name\"",
        "--rm",
        "-i",
        "--volume", "\"$img\":/sdcard/filesystem.img",
        "lukechilds/dockerpi:vm"
    ).joinToString(" ")

    fun Process.inputWriter(): InputWriter {
        return object : InputWriter {
            val blockingQueue = LinkedBlockingQueue<String>()
            val thread = Thread {
                BufferedWriter(OutputStreamWriter(outputStream)).use { stdin ->
                    while (true) {
                        try {
                            val input = blockingQueue.take()
                            TimeUnit.MILLISECONDS.sleep(1500)
                            stdin.write(input)
                            stdin.flush()
                        } catch (e: InterruptedException) {
                            echo("Closing $stdin")
                            return@Thread
                        }
                    }
                }
            }.apply { start() }
            val lock = Object()

            override fun write(input: String) {
                synchronized(lock) {
                    blockingQueue.put(input)
                }
            }

            override fun close() {
                synchronized(lock) {
                    thread.interrupt()
                }
            }
        }
    }

    interface InputWriter {
        fun write(input: String)
        fun close()
    }
}

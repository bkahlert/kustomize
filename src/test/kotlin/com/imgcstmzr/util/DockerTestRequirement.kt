package com.imgcstmzr.util

import com.imgcstmzr.cli.ColorHelpFormatter.Companion.tc
import com.imgcstmzr.process.Exec
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.parallel.Isolated

@Isolated
class DockerTestRequirement : BeforeAllCallback, AfterAllCallback {
    val dockerUpAndRunning: Boolean by lazy { runCatching { Exec.Sync.execCommand("docker", "info") }.isSuccess }
    val warning by lazy {
        listOf(
            (tc.yellow + tc.bold)("A running Docker is needed for ImgCstmzr to work."),
            tc.yellow("Since some tests assert a working integration it is most unlikely all tests will pass."))
            .joinToString("\n")
            .border(padding = 2, margin = 4)
    }

    override fun beforeAll(context: ExtensionContext?) {
        if (!dockerUpAndRunning) println(warning)
    }

    override fun afterAll(context: ExtensionContext?) {
        if (!dockerUpAndRunning) println(warning)
    }
}

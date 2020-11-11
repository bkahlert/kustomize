package com.imgcstmzr.util

import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.concurrent.process.Processes
import com.bkahlert.koodies.concurrent.process.Processes.startShellScript
import com.bkahlert.koodies.concurrent.process.RunningProcess
import com.bkahlert.koodies.concurrent.startAsDaemon
import com.bkahlert.koodies.test.junit.Slow
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import java.nio.file.Path
import kotlin.streams.asSequence

@Isolated
@Execution(ExecutionMode.CONCURRENT)
class ProgressBarTest {

    @Slow
    @Test
    fun `should show progress bar`(logger: InMemoryLogger<*>) {
        startShellScript(
            workingDirectory = Path.of("").toAbsolutePath().resolve("src/main/resources"),
            outputProcessor = { if (it.type == OUT) println(System.out) }) { !"progressbar.sh" }
        (0..100).forEach {
            logger.logLine { "\u0013" + "X".repeat(it % 10) }
            Thread.sleep(50)
        }
    }
}


fun main() {
    val ownProcesses = ProcessHandle.allProcesses().asSequence()
        .filter { proc -> proc.info().user().map { user -> user != "root" }.orElse(false) }
        .filter { proc -> proc.info().command().map { cmd -> cmd.contains("docker", ignoreCase = true) }.orElse(false) }
        .filter { proc -> proc.info().commandLine().map { cmdLine -> cmdLine.contains("imgcstmzr", ignoreCase = true) }.orElse(false) }
        .toList()
    println(ownProcesses.map { it.info() })
    println(ownProcesses.size)
    val x: RunningProcess = startShellScript(outputProcessor = {
        println(it)
    }) {
        !"stty size"
        !"printf \"%40s\\n\" \"test\$COLUMNS\""
        !"""
           while [ 1 ]
           do
                  sleep 1
                  echo "running..." 
           done
        """.trimIndent()
    }
    startAsDaemon {
        Thread.sleep(2000)


        val ownProcesses = ProcessHandle.allProcesses().asSequence()
            .filter { proc -> proc.info().user().map { user -> user != "root" }.orElse(false) }
//            .filter { proc -> proc.descendants().toList().contains(proc) }
            .toList()
        println(ownProcesses)
        fun childProcesses(): Sequence<ProcessHandle> = ProcessHandle.current().descendants().asSequence()
        println(Processes.recentChildren.map { it.info() }.toList())
        childProcesses().forEach {
            runCatching { it.destroy() }.onFailure { println(it) }
        }
        println(Processes.recentChildren.map { it.info() }.toList())
        0
    }
    x.destroy()
    startShellScript(outputProcessor = {
        println(it)
    }) {
        !"stty size"
        !"printf \"%40s\\n\" \"test\$COLUMNS\""
        !"""
           while [ 1 ]
           do
                  sleep 1
                  echo "runniXXng..." 
           done
        """.trimIndent()
    }
}

package com.imgcstmzr.util

import com.github.ajalt.clikt.output.TermUi
import com.imgcstmzr.process.Exec
import com.imgcstmzr.process.Output.Type.OUT
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.nio.file.Path

@Execution(ExecutionMode.CONCURRENT)
internal class StringExtensionsKtTest {

    @Test
    @Disabled
    internal fun `should show progress bar`() {
        Exec.Sync.execCommand(command = "sh",
            arguments = arrayOf("progressbar.sh"),
            workingDirectory = Path.of("").toAbsolutePath().resolve("src/main/resources"),
            outputProcessor = { if (it.type == OUT) println(System.out) })
        (0..100).forEach {
            TermUi.echo("\u0013" + "X".repeat(it % 10))
            Thread.sleep(50)
        }
    }
}

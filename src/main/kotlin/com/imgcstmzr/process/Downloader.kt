package com.imgcstmzr.process

import com.bkahlert.koodies.concurrent.process.Processes.startShellScript
import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.util.checkSingleFile
import com.imgcstmzr.util.cleanUp
import java.nio.file.Files
import java.nio.file.Path

//class X(caption: String, borderedOutput: Boolean = false) :
//    BlockRenderingLogger<Int>(caption = caption, borderedOutput = borderedOutput),
//        (RunningProcess, IO) -> Unit {
//    override fun invoke(runningProcess: RunningProcess, io: IO) {
//        logLine { io.formatted }
//    }
//}

object Downloader {
    fun OperatingSystem.download(): Path = download(downloadUrl ?: throw IllegalArgumentException("$this does not provide a download URL"), filename = name)

    fun download(url: String, retries: Int = 5, filename: String? = null): Path {
        val temp = Files.createTempDirectory("imgcstmzr")
        echo("Downloading ${filename ?: url} to $temp...", trailingNewline = false)

        // TODO parse progress and feedback
        startShellScript(workingDirectory = temp) {
            !"for i in ${(1..retries).joinToString(" ")} ; do"
            command(
                "2>&1",
                "curl",
                "--location",
                "--remote-name",
                "--remote-header-name",
                "--compressed",
                "--retry 5",
                "--retry-delay 5",
                "--retry-max-time 300",
                url,
            )
//            command(
//                "wget",
//                "--show-progress --progress=bar:force",
//                "--tries=10",
//                "--timeout=300",
//                "--waitretry=5",
//                "--retry-connrefused",
//                "--compression=auto",
//                "--content-disposition",
//                "--continue",
//                "--ignore-case",
//                "--adjust-extension",
//                "--directory-prefix=$temp",
//                url,
//                "&& break",
//            )
            !"done"
        }.waitForExitCode()

        return temp.checkSingleFile { "Failed to download $url." }.cleanUp("?").also { echo(" Completed.") }
    }
}

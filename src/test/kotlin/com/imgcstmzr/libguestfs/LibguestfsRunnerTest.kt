package com.imgcstmzr.libguestfs

import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import java.nio.file.Path

@Execution(CONCURRENT)
class LibguestfsRunnerTest {

    @Test
    fun `should build proper docker command`() {
        val sshKey = "ssh-rsa ${String.random(20)}== '${String.random(8)}'"

        val osImage: Path = Path.of("/Users/bkahlert/.imgcstmzr.test/guestfish/disk.img")
        val command = com.imgcstmzr.libguestfs.VirtCustomizeCommandLine {
            colors { on }
            disks { +osImage }
            sshInject { "pi" to sshKey }
        }

        expectThat(LibguestfsRunner.adapt(command).toString()).matchesCurlyPattern("""
            docker \
            run \
            --entrypoint \
            virt-customize \
            --name \
            libguestfs \
            -i \
            --mount \
            type=bind,source={}/shared,target=/shared \
            --mount \
            type=bind,source={}/disk.img,target=/images/disk.img \
            bkahlert/libguestfs@sha256{} \
            --add \
            /images/disk.img \
            --colors \
            --ssh-inject \
            "pi:string:\"$sshKey\""
        """.trimIndent())
    }
}

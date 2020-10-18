package com.imgcstmzr.process

import com.bkahlert.koodies.shell.toHereDoc
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.terminal.ANSI
import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.process.Guestfish.Companion.SHARED_DIRECTORY_NAME
import com.imgcstmzr.runtime.OperatingSystems.DietPi
import com.imgcstmzr.util.FixtureResolverExtension
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.copyToTempSiblingDirectory
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.nio.file.Path

@ExtendWith(FixtureResolverExtension::class)
@Execution(ExecutionMode.SAME_THREAD)
@Disabled
internal class GuestfishMountPassthroughTest {

    /**
     * Experiment to [mount disk image to host using Guestfish](https://medium.com/@kumar_pravin/mount-disk-image-to-host-using-guestfish-d5f33c0297e0).
     */
    @TestFactory
    @Execution(ExecutionMode.SAME_THREAD)
    internal fun `should provide access to filesystem`(@OS(DietPi::class, autoDelete = false) img: Path): List<DynamicTest> {
        val dockerImageName = "cmattoon/guestfish"

        return listOf(
//            "lines" to { img: Path ->
//                val imgDir = img.parent
//                val mountDir = Path.of("/work")
//                val mountImg = mountDir.resolve(img.fileName)
//                Exec.execShellScript(workingDirectory = imgDir) {
//                    line("docker run --privileged --name ${img.fileName} --rm -i --volume $imgDir:$mountDir --volume $img:$mountImg $dockerImageName -x")
//                    line("add $mountImg")
//                    line("run")
//                    line("modprobe fuse")
//                    line("mount /dev/sda2 /")
//                    line("mount /dev/sda1 /boot")
//                    line("mount /dev/sda1 /")
//                    line("! mkdir $mountDir/$SHARED_DIRECTORY_NAME")
//                    line("mount-local $mountDir/$SHARED_DIRECTORY_NAME options:nonempty")
//                    line("mount-local-run")
//                }
//            },
//            "heredoc I" to { img: Path ->
//                val imgDir = img.parent
//                val mountDir = Path.of("/work")
//                val mountImg = mountDir.resolve(img.fileName)
//                Exec.execShellScript(workingDirectory = imgDir) {
//                    line("docker run --privileged --name ${img.fileName} --rm -i --volume $imgDir:$mountDir --volume $img:$mountImg $dockerImageName -x")
//                    heredoc(
//                        "add $mountImg",
//                        "run",
//                        "modprobe fuse",
//                        "mount /dev/sda2 /",
//                        "mount /dev/sda1 /boot",
//                        "mount /dev/sda1 /",
//                        "! mkdir $mountDir/$SHARED_DIRECTORY_NAME",
//                        "mount-local $mountDir/$SHARED_DIRECTORY_NAME options:nonempty",
//                        "mount-local-run",
//                    )
//                }
//            },
            "heredoc II" to { img: Path ->
                val imgDir = img.parent
                val mountDir = Path.of("/work")
                val mountImg = mountDir.resolve(img.fileName)
                val result = Exec.Async.startShellScript(workingDirectory = imgDir) {
                    line("docker run --privileged --name ${img.fileName} --rm -i --volume $imgDir:$mountDir --volume $img:$mountImg $dockerImageName -x " + listOf(
                        "add $mountImg format:raw",
                        "run",
                        "list-filesystems",
                        "modprobe fuse",
                        "mount /dev/sda2 /",
                        "mount /dev/sda1 /boot",
                        //                            "! mkdir $mountDir/$SHARED_DIRECTORY_NAME",
                        "mount-local $mountDir/$SHARED_DIRECTORY_NAME options:nonempty",
                        "mount-local-run",
                    )
                        .toHereDoc("HERE-" + String.random(8).toUpperCase()))
                }
                Exec.Sync.execCommand(command = "docker exec -i ${img.fileName} bash -c " + listOf(
                    "ls",
                    "umount $mountDir/$SHARED_DIRECTORY_NAME",
                ).joinToString("\n"), workingDirectory = imgDir) {
                }.also { echo(ANSI.EscapeSequences.termColors.magenta("Result #1: $result\nResult #2: $it")) }
            },
//            "multiline I" to { img: Path ->
//                val imgDir = img.parent
//                val mountDir = Path.of("/work")
//                val mountImg = mountDir.resolve(img.fileName)
//                Exec.execShellScript(workingDirectory = imgDir) {
//                    line("docker run --privileged --name ${img.fileName} --rm -i --volume $imgDir:$mountDir --volume $img:$mountImg $dockerImageName -x " + """
//                        add $mountImg
//                        run
//                        modprobe fuse
//                        mount /dev/sda2 /
//                        mount /dev/sda1 /boot
//                        mount /dev/sda1 /
//                        ! mkdir $mountDir/$SHARED_DIRECTORY_NAME
//                        mount-local $mountDir/$SHARED_DIRECTORY_NAME options:nonempty
//                        mount-local-run
//                    """.trimIndent())
//                }
//            },
//            "multiline II" to { img: Path ->
//                val imgDir = img.parent
//                val mountDir = Path.of("/work")
//                val mountImg = mountDir.resolve(img.fileName)
//                Exec.execShellScript(workingDirectory = imgDir) {
//                    line("docker run --privileged --name ${img.fileName} --rm -i --volume $imgDir:$mountDir --volume $img:$mountImg $dockerImageName -x add $mountImg : run : modprobe fuse : mount /dev/sda2 / : mount /dev/sda1 /boot : mount /dev/sda1 / : ! mkdir $mountDir/$SHARED_DIRECTORY_NAME : mount-local $mountDir/$SHARED_DIRECTORY_NAME options:nonempty: mount-local-run")
//                }
//            },
        ).map { (name, experiment) ->
            dynamicTest(name) {
                val result = experiment(img.copyToTempSiblingDirectory())
                check(result == 0) { "An error while running the following command inside $img" }
            }
        }.toList()
    }
}

/*
sh <<_HERE_1YmaKYdOuUz6YoVt
docker run --privileged --name imgcstmzr-4l3S49EafwyEsnUR.img --rm -i --volume /Users/bkahlert/.imgcstmzr.test/test-4l3S49EafwyEsnUR:/work --volume /Users/bkahlert/.imgcstmzr.test/test-4l3S49EafwyEsnUR/imgcstmzr-4l3S49EafwyEsnUR.img:/work/imgcstmzr-4l3S49EafwyEsnUR.img cmattoon/guestfish -x
add /work/imgcstmzr-4l3S49EafwyEsnUR.img
run
modprobe fuse
mount /dev/sda2 /
mount /dev/sda1 /boot
mount /dev/sda1 /
! mkdir /work/shared.xxx
mount-local /work/shared.xxx options:nonempty
mount-local-run
_HERE_1YmaKYdOuUz6YoVt
 */

//docker exec -it imgcstmzr-4l3S49EafwyEsnUR.img bash

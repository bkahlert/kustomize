package com.imgcstmzr.guestfish

import com.bkahlert.koodies.concurrent.process.Processes
import com.bkahlert.koodies.nio.file.duplicate
import com.bkahlert.koodies.shell.toHereDoc
import com.bkahlert.koodies.string.joinLinesToString
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.terminal.ansi.AnsiColors.magenta
import com.bkahlert.koodies.test.junit.FifteenMinutesTimeout
import com.imgcstmzr.E2E
import com.imgcstmzr.guestfish.Guestfish.Companion.SHARED_DIRECTORY_NAME
import com.imgcstmzr.runtime.OperatingSystems.DietPi
import com.imgcstmzr.util.OS
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import java.nio.file.Path

class GuestfishMountPassthroughTest {

    /**
     * Experiment to [mount disk image to host using Guestfish](https://medium.com/@kumar_pravin/mount-disk-image-to-host-using-guestfish-d5f33c0297e0).
     */
    @FifteenMinutesTimeout @E2E
    fun `should provide access to filesystem`(@OS(DietPi, autoDelete = false) img: Path): List<DynamicTest> {
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
                val result = Processes.startShellScript(workingDirectory = imgDir) {
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
                Processes.startShellScript(workingDirectory = imgDir) {
                    !"docker exec -i ${img.fileName} bash -c ".plus(listOf(
                        "ls",
                        "umount $mountDir/$SHARED_DIRECTORY_NAME",
                    ).joinLinesToString())
                }.loggedProcess.get().let { "Result #1: $result\nResult #2: $it".magenta() }
                result
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
                val result = experiment(img.duplicate())
                check(result.exitValue() == 0) { "An error while running the following command inside $img" }
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

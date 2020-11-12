package com.imgcstmzr.patch.new

import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.string.mapLines
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.terminal.ansi.AnsiColors.magenta
import com.bkahlert.koodies.unit.Size
import com.imgcstmzr.guestfish.Guestfish
import com.imgcstmzr.guestfish.GuestfishOperation
import com.imgcstmzr.patch.PathOperation
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.runtime.Program
import com.imgcstmzr.runtime.RunningOperatingSystem
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.util.quoted
import java.nio.file.Path


interface Patch {
    val name: String
    val preFileImgOperations: List<ImgOperation>
    val guestfishOperations: List<GuestfishOperation>
    val fileSystemOperations: List<PathOperation>
    val postFileImgOperations: List<ImgOperation>
    val programs: List<Program>
}

data class SimplePatch(
    override val name: String,
    override val preFileImgOperations: List<ImgOperation>,
    override val guestfishOperations: List<GuestfishOperation>,
    override val fileSystemOperations: List<PathOperation>,
    override val postFileImgOperations: List<ImgOperation>,
    override val programs: List<Program>,
) : Patch

fun buildPatch(name: String, init: PatchBuilder.() -> Unit): Patch {

    val preFileImgOperations = mutableListOf<ImgOperation>()
    val guestfishOperations = mutableListOf<GuestfishOperation>()
    val fileSystemOperations = mutableListOf<PathOperation>()
    val postFileImgOperations = mutableListOf<ImgOperation>()
    val programs = mutableListOf<Program>()

    PatchBuilder(preFileImgOperations, guestfishOperations, fileSystemOperations, postFileImgOperations, programs).apply(init)
    return SimplePatch(name, preFileImgOperations, guestfishOperations, fileSystemOperations, postFileImgOperations, programs)
}

@DslMarker
annotation class PatchDsl

@PatchDsl
class PatchBuilder(
    private val preFileImgOperations: MutableList<ImgOperation>,
    private val guestfishOperations: MutableList<GuestfishOperation>,
    private val fileSystemOperations: MutableList<PathOperation>,
    private val postFileImgOperations: MutableList<ImgOperation>,
    private val programs: MutableList<Program>,
) {
    fun preFile(init: ImgOperationsCollector.() -> Unit) = ImgOperationsCollector(preFileImgOperations).apply(init)
    fun guestfish(init: GuestfishOperationsCollector.() -> Unit) = GuestfishOperationsCollector(guestfishOperations).apply(init)
    fun files(init: FileSystemOperationsCollector.() -> Unit) = FileSystemOperationsCollector(fileSystemOperations).apply(init)
    fun postFile(init: ImgOperationsCollector.() -> Unit) = ImgOperationsCollector(postFileImgOperations).apply(init)
    fun booted(init: ProgramsBuilder.() -> Unit) = ProgramsBuilder(programs).apply(init)
}

typealias ImgOperation = (osImage: OperatingSystemImage, logger: RenderingLogger<Any>) -> Unit

@PatchDsl
class ImgOperationsCollector(private val imgOperations: MutableList<ImgOperation>) {
    fun resize(size: Size) {
        imgOperations += { osImage: OperatingSystemImage, logger: RenderingLogger<Any> ->
            osImage.increaseDiskSpace(logger, size)
        }
    }

    fun updateUsername(username: String) {
        imgOperations += { osImage: OperatingSystemImage, logger: RenderingLogger<Any> ->
            osImage.credentials = osImage.credentials.copy(username = username)
            logger.logLine { META.format("Username of user ${username.quoted} updated.") }
        }
    }

    fun updatePassword(username: String, password: String) {
        imgOperations += { osImage: OperatingSystemImage, logger: RenderingLogger<Any> ->
            if (osImage.credentials.username == username) {
                osImage.credentials = osImage.credentials.copy(password = password)
                logger.logLine { META.format("Password of user ${password.quoted} updated.") }
            } else {
                logger.logLine { META.format("Password of user ${password.quoted} updated${"*".magenta()}.") }
                logger.logLine { META.format("ImgCstmzr will to continue to use user ${osImage.credentials.username.quoted}.") }
            }
        }
    }
}

@PatchDsl
class GuestfishOperationsCollector(private val guestfishOperations: MutableList<GuestfishOperation>) {
    fun changePassword(username: String, password: String, salt: String = String.random.cryptSalt()) {
        guestfishOperations += Guestfish.changePasswordCommand(username, password, salt)
    }

    fun touch(path: String) {
        guestfishOperations += GuestfishOperation("touch ${path.quoted}")
    }

    fun changeOwner(path: String, userId: Int, groupId: Int) {
        guestfishOperations += GuestfishOperation("chown $userId $groupId ${path.quoted}")
    }

    fun changeMode(path: String, owner: Byte, group: Byte, other: Byte) {
        listOf(owner, group, other).forEach { require(it in 0..7) { "Permission $it must be between 0 and 7 (both inclusive)." } }
        guestfishOperations += GuestfishOperation("chmod 0$owner$group$other ${path.quoted}")
    }

    fun appendToFile(path: String, content: String) {
        guestfishOperations += GuestfishOperation(content.mapLines(ignoreTrailingSeparator = false) { line ->
            "write-append ${path.quoted} ${"$line".replace("\"", "\\\"").quoted}"
        })
    }

    fun rootFile(path: String, content: String) {
        val commands = arrayOf(
            "touch ${path.quoted}",
            "chown 0  0 ${path.quoted}",
            "chmod 0700 ${path.quoted}",
            *content.lines().map() { line ->
                "write-append ${path.quoted} ${"$line\\n".replace("\"", "\\\"").quoted}"
            }.toTypedArray(),
        )
        guestfishOperations += GuestfishOperation(commands)
    }

    fun command(command: String) {
        guestfishOperations += GuestfishOperation(arrayOf(
            "command '${command.replace("'", "\\'")}'"
        ))
    }
}

@PatchDsl
class FileSystemOperationsCollector(private val pathOperations: MutableList<PathOperation>) {
    fun edit(path: String, validator: (Path) -> Any, operations: (Path) -> Any) =
        pathOperations.add(PathOperation(Path.of(path), validator, operations))
}


@PatchDsl
class ProgramsBuilder(private val programs: MutableList<Program>) {

    fun run(program: Program) {
        programs += program
    }

    fun program(
        purpose: String,
        initialState: RunningOperatingSystem.() -> String?,
        vararg states: Pair<String, RunningOperatingSystem.(String) -> String?>,
    ) {
        programs += Program(purpose, initialState, *states)
    }

    fun setupScript(name: String, readyPattern: Regex = OperatingSystems.RaspberryPiLite.readyPattern, setupScript: String) {
        programs += Program.fromSetupScript(name, readyPattern, setupScript)
    }

    fun script(name: String, readyPattern: Regex = OperatingSystems.RaspberryPiLite.readyPattern, vararg commands: String) {
        programs += Program.fromScript(name, readyPattern, *commands)
    }
}

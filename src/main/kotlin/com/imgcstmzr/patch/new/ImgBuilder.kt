package com.imgcstmzr.patch.new

import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.unit.Size
import com.imgcstmzr.patch.ImgOperation
import com.imgcstmzr.patch.PathOperation
import com.imgcstmzr.process.Guestfish
import com.imgcstmzr.process.GuestfishOperation
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.runtime.Program
import com.imgcstmzr.runtime.RunningOS
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import java.nio.file.Path


interface Patch {
    val name: String
    val imgOperations: List<ImgOperation>
    val guestfishOperations: List<GuestfishOperation>
    val fileSystemOperations: List<PathOperation>
    val programs: List<Program>
}

data class SimplePatch(
    override val name: String,
    override val imgOperations: List<ImgOperation>,
    override val guestfishOperations: List<GuestfishOperation>,
    override val fileSystemOperations: List<PathOperation>,
    override val programs: List<Program>,
) : Patch

fun buildPatch(name: String, init: PatchBuilder.() -> Unit): Patch {
    val imgOperations = mutableListOf<ImgOperation>()
    val guestfishOperations = mutableListOf<GuestfishOperation>()
    val fileSystemOperations = mutableListOf<PathOperation>()
    val programs = mutableListOf<Program>()
    PatchBuilder(
        imgOperations,
        guestfishOperations,
        fileSystemOperations,
        programs
    ).apply(init)
    return SimplePatch(name, imgOperations, guestfishOperations, fileSystemOperations, programs)
}

@DslMarker
annotation class PatchDsl

@PatchDsl
class PatchBuilder(
    private val imgOperations: MutableList<ImgOperation>,
    private val guestfishOperations: MutableList<GuestfishOperation>,
    private val fileSystemOperations: MutableList<PathOperation>,
    private val programs: MutableList<Program>,
) {
    fun img(init: ImgOperationsCollector.() -> Unit) = ImgOperationsCollector(imgOperations).apply(init)
    fun guestfish(init: GuestfishOperationsCollector.() -> Unit) = GuestfishOperationsCollector(guestfishOperations).apply(init)
    fun files(init: FileSystemOperationsCollector.() -> Unit) = FileSystemOperationsCollector(fileSystemOperations).apply(init)
    fun booted(init: ProgramsBuilder.() -> Unit) = ProgramsBuilder(programs).apply(init)
}

@PatchDsl
class ImgOperationsCollector(private val imgOperations: MutableList<ImgOperation>) {
    fun resize(size: Size) {
        imgOperations += { os: OperatingSystem, path: Path, parentLogger: BlockRenderingLogger<Any>? ->
            os.increaseDiskSpace(size, path, parentLogger)
        }
    }
}

@PatchDsl
class GuestfishOperationsCollector(private val guestfishOperations: MutableList<GuestfishOperation>) {
    fun changePassword(username: String, password: String, salt: String = String.random.cryptSalt()) {
        guestfishOperations += Guestfish.changePasswordCommand(username, password, salt)
    }
}

@PatchDsl
class FileSystemOperationsCollector(private val pathOperations: MutableList<PathOperation>) {
    fun edit(path: String, validator: (Path) -> Any, operations: (Path) -> Any) =
        pathOperations.add(PathOperation(Path.of(path), validator, operations))
}


@PatchDsl
class ProgramsBuilder(private val programs: MutableList<Program>) {
    fun program(
        purpose: String,
        initialState: RunningOS.(String) -> String?,
        vararg states: Pair<String, RunningOS.(String) -> String?>,
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

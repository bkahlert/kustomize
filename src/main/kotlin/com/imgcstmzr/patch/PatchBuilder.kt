package com.imgcstmzr.patch

import com.bkahlert.koodies.builder.buildListTo
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.string.quoted
import com.bkahlert.koodies.terminal.ansi.AnsiColors.magenta
import com.bkahlert.koodies.unit.Size
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommand
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine
import com.imgcstmzr.libguestfs.guestfish.GuestfishDsl
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine.VirtCustomizeCustomizationOptionsBuilder
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeDsl
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystemProcess
import com.imgcstmzr.runtime.Program
import com.imgcstmzr.runtime.log.RenderingLogger
import java.nio.file.Path

data class SimplePatch(
    override val name: String,
    override val preFileImgOperations: List<ImgOperation>,
    override val customizationOptions: List<VirtCustomizeCustomizationOption>,
    override val guestfishCommands: List<GuestfishCommand>,
    override val fileSystemOperations: List<PathOperation>,
    override val postFileImgOperations: List<ImgOperation>,
    override val programs: List<Program>,
) : Patch

fun buildPatch(os: OperatingSystem, name: String, init: PatchBuilder.() -> Unit): Patch {

    val preFileImgOperations = mutableListOf<ImgOperation>()
    val customizationOptions = mutableListOf<VirtCustomizeCustomizationOption>()
    val guestfishCommands = mutableListOf<GuestfishCommand>()
    val fileSystemOperations = mutableListOf<PathOperation>()
    val postFileImgOperations = mutableListOf<ImgOperation>()
    val programs = mutableListOf<Program>()

    PatchBuilder(os, preFileImgOperations, customizationOptions, guestfishCommands, fileSystemOperations, postFileImgOperations, programs).apply(init)
    return SimplePatch(name, preFileImgOperations, customizationOptions, guestfishCommands, fileSystemOperations, postFileImgOperations, programs)
}

@DslMarker
annotation class PatchDsl

@PatchDsl
@VirtCustomizeDsl
@GuestfishDsl
class PatchBuilder(
    private val os: OperatingSystem,
    private val preFileImgOperations: MutableList<ImgOperation>,
    private val virtCustomizeCustomizationOptions: MutableList<VirtCustomizeCustomizationOption>,
    private val guestfishCommands: MutableList<GuestfishCommand>,
    private val fileSystemOperations: MutableList<PathOperation>,
    private val postFileImgOperations: MutableList<ImgOperation>,
    private val programs: MutableList<Program>,
) {
    fun preFile(init: ImgOperationsCollector.() -> Unit) = ImgOperationsCollector(preFileImgOperations).apply(init)

    @VirtCustomizeDsl
    fun customize(init: VirtCustomizeCustomizationOptionsBuilder.() -> Unit) = init.buildListTo(virtCustomizeCustomizationOptions)

    @GuestfishDsl
    fun guestfish(init: GuestfishCommandLine.GuestfishCommandsBuilder.() -> Unit) = init.buildListTo(guestfishCommands)
    fun files(init: FileSystemOperationsCollector.() -> Unit) = FileSystemOperationsCollector(fileSystemOperations).apply(init)
    fun postFile(init: ImgOperationsCollector.() -> Unit) = ImgOperationsCollector(postFileImgOperations).apply(init)
    fun booted(init: ProgramsBuilder.() -> Unit) = ProgramsBuilder(os, programs).apply(init)
}

typealias ImgOperation = (osImage: OperatingSystemImage, logger: RenderingLogger) -> Unit

@PatchDsl
class ImgOperationsCollector(private val imgOperations: MutableList<ImgOperation>) {
    fun resize(size: Size) {
        imgOperations += { osImage: OperatingSystemImage, logger: RenderingLogger ->
            osImage.increaseDiskSpace(logger, size)
        }
    }

    fun updateUsername(username: String) {
        imgOperations += { osImage: OperatingSystemImage, logger: RenderingLogger ->
            osImage.credentials = osImage.credentials.copy(username = username)
            logger.logLine { META.format("Username of user ${username.quoted} updated.") }
        }
    }

    fun updatePassword(username: String, password: String) {
        imgOperations += { osImage: OperatingSystemImage, logger: RenderingLogger ->
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
class FileSystemOperationsCollector(private val pathOperations: MutableList<PathOperation>) {
    fun edit(path: String, validator: (Path) -> Unit, operations: (Path) -> Unit) =
        pathOperations.add(PathOperation(Path.of(path), validator, operations))

    fun create(validator: (Path) -> Unit, operations: (Path) -> Unit) =
        pathOperations.add(PathOperation(Path.of("/"), validator, operations))
}


@PatchDsl
class ProgramsBuilder(private val os: OperatingSystem, private val programs: MutableList<Program>) {

    fun run(program: Program) {
        programs += program
    }

    fun program(
        purpose: String,
        initialState: OperatingSystemProcess.() -> String?,
        vararg states: Pair<String, OperatingSystemProcess.(String) -> String?>,
    ) {
        programs += Program(purpose, initialState, *states)
    }

    fun setupScript(name: String, commandBlocks: String) {
        programs += os.compileSetupScript(name, commandBlocks)
    }

    fun script(name: String, vararg commandLines: String) {
        programs += os.compileScript(name, *commandLines)
    }
}

package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.libguestfs.HostPath
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommand
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine
import com.imgcstmzr.libguestfs.guestfish.GuestfishDsl
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine.VirtCustomizeCustomizationOptionsBuilder
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeDsl
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystemProcess
import com.imgcstmzr.runtime.Program
import koodies.builder.ListBuilder
import koodies.builder.buildListTo
import koodies.concurrent.process.IO.Type.META
import koodies.logging.RenderingLogger
import koodies.terminal.AnsiColors.magenta
import koodies.text.quoted
import koodies.unit.Size
import java.util.concurrent.atomic.AtomicBoolean

data class SimplePatch(
    override var trace: Boolean,
    override val name: String,
    override val diskPreparations: List<DiskOperation>,
    override val diskCustomizations: List<(OperatingSystemImage) -> VirtCustomizeCustomizationOption>,
    override val diskOperations: List<(OperatingSystemImage) -> GuestfishCommand>,
    override val fileOperations: List<FileOperation>,
    override val osPreparations: List<DiskOperation>,
    override val osBoot: Boolean,
    override val osOperations: List<(OperatingSystemImage) -> Program>,
) : Patch {
    override val operationCount by lazy {
        diskPreparations.size + diskCustomizations.size + diskOperations.size + fileOperations.size + osOperations.size
    }
}

fun buildPatch(name: String, init: PatchBuilder.() -> Unit): Patch {

    val preFileImgOperations = mutableListOf<DiskOperation>()
    val customizationOptions = mutableListOf<(OperatingSystemImage) -> VirtCustomizeCustomizationOption>()
    val guestfishCommands = mutableListOf<(OperatingSystemImage) -> GuestfishCommand>()
    val fileSystemOperations = mutableListOf<FileOperation>()
    val postFileImgOperations = mutableListOf<DiskOperation>()
    val boot = AtomicBoolean(false)
    val programs = mutableListOf<(OperatingSystemImage) -> Program>()

    PatchBuilder(preFileImgOperations, customizationOptions, guestfishCommands, fileSystemOperations, postFileImgOperations, boot, programs).apply(init)
    return SimplePatch(false,
        name,
        preFileImgOperations,
        customizationOptions,
        guestfishCommands,
        fileSystemOperations,
        postFileImgOperations,
        boot.get(),
        programs)
}

@DslMarker
annotation class PatchDsl

@PatchDsl
@VirtCustomizeDsl
@GuestfishDsl
class PatchBuilder(
    private val diskPreparations: MutableList<DiskOperation>,
    private val diskCustomizations: MutableList<(OperatingSystemImage) -> VirtCustomizeCustomizationOption>,
    private val diskOperations: MutableList<(OperatingSystemImage) -> GuestfishCommand>,
    private val fileOperations: MutableList<FileOperation>,
    private val osPreparations: MutableList<DiskOperation>,
    private var osBoot: AtomicBoolean,
    private val osOperations: MutableList<(OperatingSystemImage) -> Program>,
) {
    fun prepareDisk(init: ImgOperationsCollector.() -> Unit) = ImgOperationsCollector(diskPreparations).apply(init)

    @VirtCustomizeDsl
    fun customizeDisk(init: VirtCustomizeCustomizationOptionsBuilder.() -> Unit) = init.buildListTo(diskCustomizations)

    @GuestfishDsl
    fun guestfish(init: GuestfishCommandLine.GuestfishCommandsBuilder.() -> Unit) = init.buildListTo(diskOperations)
    fun files(init: FileSystemOperationsCollector.() -> Unit) = FileSystemOperationsCollector(fileOperations).apply(init)
    fun osPrepare(init: ImgOperationsCollector.() -> Unit) = ImgOperationsCollector(osPreparations).apply(init)
    fun boot() = osBoot.set(true)
    fun os(init: ProgramsBuilder.() -> Unit) = init.buildListTo(osOperations)
}

typealias DiskOperation = (osImage: OperatingSystemImage, logger: RenderingLogger) -> Unit

@PatchDsl
class ImgOperationsCollector(private val diskOperations: MutableList<DiskOperation>) {
    fun resize(size: Size) {
        diskOperations += { osImage: OperatingSystemImage, logger: RenderingLogger ->
            osImage.increaseDiskSpace(logger, size)
        }
    }

    fun updateUsername(oldUsername: String, newUsername: String) {
        diskOperations += { osImage: OperatingSystemImage, logger: RenderingLogger ->
            osImage.credentials = osImage.credentials.copy(username = newUsername)
            logger.logLine { META.format("Username of user ${oldUsername.quoted} updated to ${newUsername.quoted}.") }
        }
    }

    fun updatePassword(username: String, password: String) {
        diskOperations += { osImage: OperatingSystemImage, logger: RenderingLogger ->
            if (osImage.credentials.username == username) {
                osImage.credentials = osImage.credentials.copy(password = password)
                logger.logLine { META.format("Password of user ${username.quoted} updated.") }
            } else {
                logger.logLine { META.format("Password of user ${password.quoted} updated${"*".magenta()}.") }
                logger.logLine { META.format("ImgCstmzr will to continue to use user ${osImage.credentials.username.quoted}.") }
            }
        }
    }
}

@PatchDsl
class FileSystemOperationsCollector(private val fileOperations: MutableList<FileOperation>) {
    fun edit(path: DiskPath, validator: (HostPath) -> Unit, operations: (HostPath) -> Unit) =
        fileOperations.add(FileOperation(path, validator, operations))
}

@PatchDsl
class ProgramsBuilder : ListBuilder<(OperatingSystemImage) -> Program>() {

    fun program(
        purpose: String,
        initialState: OperatingSystemProcess.() -> String?,
        vararg states: Pair<String, OperatingSystemProcess.(String) -> String?>,
    ) = list.add { Program(purpose, initialState, *states) }

    fun script(name: String, vararg commandLines: String) =
        list.add { it.compileScript(name, *commandLines) }
}


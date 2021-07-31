package com.bkahlert.kustomize.os

import com.bkahlert.kustomize.cli.Layouts
import com.bkahlert.kustomize.cli.PATCH_INNER_DECORATION_FORMATTER
import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine
import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine.GuestfishCommand
import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine.GuestfishCommandsBuilder
import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine.GuestfishCommandsBuilder.GuestfishCommandsContext
import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine.GuestfishOptions
import com.bkahlert.kustomize.libguestfs.LibguestfsImage
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.Customization
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.CustomizationsBuilder
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.CustomizationsBuilder.CustomizationsContext
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.Options
import koodies.Exceptions.IAE
import koodies.builder.Init
import koodies.builder.buildList
import koodies.docker.ContainerPath
import koodies.docker.DockerContainer
import koodies.docker.DockerExec
import koodies.docker.DockerRunCommandLine
import koodies.docker.MountOptions
import koodies.docker.dockerized
import koodies.docker.ubuntu
import koodies.exec.CommandLine
import koodies.exec.RendererProviders.compact
import koodies.io.path.asPath
import koodies.io.path.duplicate
import koodies.io.path.getSize
import koodies.io.path.pathString
import koodies.runtime.isDebugging
import koodies.text.ANSI.Formatter
import koodies.text.Semantics.formattedAs
import koodies.text.withRandomSuffix
import koodies.tracing.spanning
import koodies.unit.BinaryPrefixes
import koodies.unit.Size
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isReadable
import kotlin.io.path.moveTo

/**
 * An [path] holding an [operatingSystem] that can be accessed using the specified [credentials].
 */
class OperatingSystemImage(
    val operatingSystem: OperatingSystem,
    private val path: Path,
) : OperatingSystem by operatingSystem {

    override val fullName: String get() = "${operatingSystem.fullName} ／ ${path.toUri()}"
    val shortName: String get() = "${operatingSystem.fullName} ／ ${path.fileName}"
    val fileName: String = path.fileName.pathString
    val readable: Boolean get() = path.isReadable()
    val directory: Path get() = path.parent
    val file: Path get() = path
    val size: Size get() = path.getSize()
    fun duplicate(): OperatingSystemImage = operatingSystem based (path.duplicate())

    /**
     * Contains the directory used to exchange with this image.
     *
     * Example: For `project/os.img` the exchange directory
     * would be `project/shared`.
     */
    val exchangeDirectory: Path get() = mountRootForDisk(file)

    /**
     * Returns the given [diskPath] mapped to its location in the [exchangeDirectory].
     *
     * Example: For `/var/file` the mapped location would be
     * `project/shared/var/file`.
     */
    fun hostPath(diskPath: DiskPath): Path = diskPath.hostPath(exchangeDirectory)

    fun copyOut(path: String, vararg paths: String, trace: Boolean = false): Path =
        spanning("Copying out ${path.formattedAs.input}" + if (paths.isNotEmpty()) " and ${paths.size} other files" else "") {
            guestfish(trace) {
                copyOut { LinuxRoot / path }
                paths.forEach { copyOut { _ -> LinuxRoot / it } }
            }
            hostPath(LinuxRoot / path)
        }

    /**
     * @see <a href="https://libguestfs.org/">libguestfs—tools for accessing and modifying virtual machine disk images</a>
     */
    fun guestfish(
        trace: Boolean = false,
        vararg guestfishCommands: GuestfishCommand,
        init: Init<GuestfishCommandsContext> = {},
    ): DockerExec = GuestfishCommandLine(
        GuestfishOptions(file, trace = trace),
        buildList {
            addAll(GuestfishCommandsBuilder(this@OperatingSystemImage).build(init))
            addAll(guestfishCommands)
        }
    ).exec.logging(exchangeDirectory,
        nameFormatter = Formatter.ToCharSequence,
        decorationFormatter = PATCH_INNER_DECORATION_FORMATTER,
        layout = Layouts.DESCRIPTION)

    /**
     * @see <a href="https://libguestfs.org/">libguestfs—tools for accessing and modifying virtual machine disk images</a>
     */
    fun virtCustomize(
        trace: Boolean = false,
        vararg customizations: Customization,
        init: Init<CustomizationsContext> = {},
    ): DockerExec = VirtCustomizeCommandLine(
        Options(file, colors = !isDebugging, trace = trace),
        buildList {
            addAll(CustomizationsBuilder(this@OperatingSystemImage).build(init))
            addAll(customizations)
        }
    ).exec.logging(exchangeDirectory,
        nameFormatter = Formatter.ToCharSequence,
        decorationFormatter = PATCH_INNER_DECORATION_FORMATTER,
        layout = Layouts.DESCRIPTION)

    fun resize(size: Size): Size = spanning("Change disk space") {
        val missing = size - path.getSize()

        require(missing >= Size.ZERO) {
            throw IAE(
                "Requested disk space is ${-missing} smaller than current size of ${path.fileName} (${path.getSize().toString(BinaryPrefixes)}).",
                "Decreasing an image's disk space is currently not supported.",
            )
        }

        val intermediaryImage = "resized.img"

        directory.ubuntu("Creating intermediary image", renderer = compact {
            copy(
                nameFormatter = Formatter.ToCharSequence,
                decorationFormatter = PATCH_INNER_DECORATION_FORMATTER,
                layout = Layouts.DESCRIPTION,
            )
        }) { command("truncate", "-s", "${size.wholeBytes}", intermediaryImage) }

        CommandLine("virt-resize", "--expand", "/dev/sda2", path.fileName.pathString, intermediaryImage, name = "Resizing image")
            .dockerized(LibguestfsImage, DockerRunCommandLine.Options(
                name = DockerContainer.from("virt-resize".withRandomSuffix()),
                autoCleanup = true,
                mounts = MountOptions {
                    directory mountAt "/images"
                },
                workingDirectory = ContainerPath("/images".asPath())
            )).exec.logging(directory,
                nameFormatter = Formatter.ToCharSequence,
                decorationFormatter = PATCH_INNER_DECORATION_FORMATTER,
                layout = Layouts.DESCRIPTION)

        directory.resolve(intermediaryImage).moveTo(path, overwrite = true).getSize()
    }

    override fun toString(): String = fullName

    companion object {
        infix fun OperatingSystem.based(path: Path): OperatingSystemImage = OperatingSystemImage(
            operatingSystem = this,
            path = path
        )

        private const val EXCHANGE_DIRECTORY_NAME = "shared"

        /**
         * Returns the directory used to share (i.e. for copying-in and -out)
         * files between this host and the [OperatingSystem] contained in the
         * given [disk] file.
         *
         * Example: For [disk] `project/os.img` the mount root
         * would be `project/shared`.
         */
        fun mountRootForDisk(disk: Path): Path =
            disk.resolveSibling(EXCHANGE_DIRECTORY_NAME).createDirectories()
    }
}

package com.bkahlert.kustomize.os

import com.bkahlert.kommons.Exceptions.IAE
import com.bkahlert.kommons.builder.Init
import com.bkahlert.kommons.builder.buildList
import com.bkahlert.kommons.docker.ContainerPath
import com.bkahlert.kommons.docker.DockerContainer
import com.bkahlert.kommons.docker.DockerExec
import com.bkahlert.kommons.docker.DockerRunCommandLine
import com.bkahlert.kommons.docker.MountOptions
import com.bkahlert.kommons.docker.dockerized
import com.bkahlert.kommons.docker.ubuntu
import com.bkahlert.kommons.exec.CommandLine
import com.bkahlert.kommons.exec.RendererProviders.compact
import com.bkahlert.kommons.io.path.asPath
import com.bkahlert.kommons.io.path.duplicate
import com.bkahlert.kommons.io.path.getSize
import com.bkahlert.kommons.io.path.pathString
import com.bkahlert.kommons.io.path.touch
import com.bkahlert.kommons.runtime.isDebugging
import com.bkahlert.kommons.text.ANSI.Formatter
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.text.withRandomSuffix
import com.bkahlert.kommons.tracing.runSpanning
import com.bkahlert.kommons.unit.BinaryPrefixes
import com.bkahlert.kommons.unit.Size
import com.bkahlert.kustomize.cli.Layouts
import com.bkahlert.kustomize.cli.PATCH_INNER_DECORATION_FORMATTER
import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine
import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine.GuestfishCommand
import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine.GuestfishCommandsBuilder
import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine.GuestfishCommandsBuilder.GuestfishCommandsContext
import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine.GuestfishOptions
import com.bkahlert.kustomize.libguestfs.LibguestfsImage
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.Options
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomizationsBuilder
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomizationsBuilder.VirtCustomizationsContext
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
        runSpanning("Copying out ${path.formattedAs.input}" + if (paths.isNotEmpty()) " and ${paths.size} other files" else "") {
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
        vararg virtCustomizations: VirtCustomization,
        init: Init<VirtCustomizationsContext> = {},
    ): DockerExec = VirtCustomizeCommandLine(
        Options(file, colors = !isDebugging, trace = trace),
        buildList {
            addAll(VirtCustomizationsBuilder(this@OperatingSystemImage).build(init))
            addAll(virtCustomizations)
        }
    ).exec.logging(exchangeDirectory,
        nameFormatter = Formatter.ToCharSequence,
        decorationFormatter = PATCH_INNER_DECORATION_FORMATTER,
        layout = Layouts.DESCRIPTION)

    fun resize(size: Size): Size = runSpanning("Change disk space") {
        val missing = size - path.getSize()

        require(missing >= Size.ZERO) {
            throw IAE(
                "Requested disk space is ${-missing} smaller than current size of ${path.fileName} (${path.getSize().toString(BinaryPrefixes)}).",
                "Decreasing an image's disk space is currently not supported.",
            )
        }

        val intermediaryImage = "resized.img".also {
            // make sure we create the image ourselves to not end up with a root owned Docker file
            directory.resolve(it).touch()
        }

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

package com.bkahlert.kustomize.cli

import com.bkahlert.kustomize.cli.CustomizationConfig.DefaultUser
import com.bkahlert.kustomize.cli.CustomizationConfig.FileOperation
import com.bkahlert.kustomize.cli.CustomizationConfig.Hostname
import com.bkahlert.kustomize.cli.CustomizationConfig.Samba
import com.bkahlert.kustomize.cli.CustomizationConfig.SetupScript
import com.bkahlert.kustomize.cli.CustomizationConfig.Ssh
import com.bkahlert.kustomize.cli.CustomizationConfig.Tweaks
import com.bkahlert.kustomize.cli.CustomizationConfig.UsbGadget
import com.bkahlert.kustomize.cli.CustomizationConfig.UsbGadget.Ethernet
import com.bkahlert.kustomize.cli.CustomizationConfig.Wifi
import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine
import com.bkahlert.kustomize.os.DiskPath
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OperatingSystem
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.OperatingSystems
import com.bkahlert.kustomize.patch.AppendToFilesPatch
import com.bkahlert.kustomize.patch.CompositePatch
import com.bkahlert.kustomize.patch.CopyFilesPatch
import com.bkahlert.kustomize.patch.FirstBootPatch
import com.bkahlert.kustomize.patch.HostnamePatch
import com.bkahlert.kustomize.patch.PasswordPatch
import com.bkahlert.kustomize.patch.PhasedPatch
import com.bkahlert.kustomize.patch.ResizePatch
import com.bkahlert.kustomize.patch.RootShare
import com.bkahlert.kustomize.patch.RootShare.none
import com.bkahlert.kustomize.patch.SambaPatch
import com.bkahlert.kustomize.patch.ShellScriptPatch
import com.bkahlert.kustomize.patch.SshAuthorizationPatch
import com.bkahlert.kustomize.patch.SshEnablementPatch
import com.bkahlert.kustomize.patch.SshPortPatch
import com.bkahlert.kustomize.patch.TimeZonePatch
import com.bkahlert.kustomize.patch.TweaksPatch
import com.bkahlert.kustomize.patch.UsbEthernetGadgetPatch
import com.bkahlert.kustomize.patch.UsernamePatch
import com.bkahlert.kustomize.patch.WifiAutoReconnectPatch
import com.bkahlert.kustomize.patch.WifiPowerSafeModePatch
import com.bkahlert.kustomize.patch.WpaSupplicantPatch
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import koodies.builder.buildList
import koodies.io.ls
import koodies.io.path.asPath
import koodies.net.IPAddress
import koodies.net.IPSubnet
import koodies.net.ipSubnetOf
import koodies.net.toIP
import koodies.shell.ShellScript
import koodies.text.LineSeparators.LF
import koodies.text.levenshteinDistance
import koodies.text.minus
import koodies.text.takeUnlessBlank
import koodies.unit.Size
import koodies.unit.toSize
import java.net.URI
import java.nio.file.Path
import java.util.TimeZone
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText

/**
 * Representation of a `.conf` file.
 */
data class CustomizationConfig(
    /**
     * Whether to enable verbose logging (i.e. [VirtCustomizeCommandLine] and [GuestfishCommandLine]).
     */
    val trace: Boolean,
    /**
     * Name of the configuration (i.e. used as the name for the [Cache] directory).
     */
    val name: String,
    /**
     * The [TimeZone] to be applied to the image.
     */
    val timeZone: TimeZone?,
    /**
     * The [Hostname] to be applied to the image.
     */
    val hostname: Hostname?,
    /**
     * The [Wifi] settings to be applied to the image.
     */
    val wifi: Wifi?,
    /**
     * The [OperatingSystem] to be used.
     */
    val os: OperatingSystems,
    /**
     * The [Size] the image to be scaled to.
     */
    val size: Size?,
    /**
     * The [Ssh] settings to be applied to the image.
     */
    val ssh: Ssh?,
    /**
     * The [DefaultUser] settings to be applied to the image.
     */
    val defaultUser: DefaultUser?,
    /**
     * The [Samba] settings to be applied to the image.
     */
    val samba: Samba?,
    /**
     * The [UsbGadget] settings to be applied to the image.
     */
    val usbGadgets: List<UsbGadget>,
    /**
     * The [Tweaks] to be applied to the image.
     */
    val tweaks: Tweaks?,
    /**
     * The [FileOperation] list to be applied to the image.
     */
    val files: List<FileOperation>,
    /**
     * The [SetupScript] list to be applied to the image.
     */
    val setup: List<SetupScript>,
    /**
     * The [ShellScript] list to be started on first boot
     * (the moment the image is booted after it has been customized).
     */
    val firstBoot: List<ShellScript>,
) {

    companion object {

        /**
         * Loads a [CustomizationConfig] based on the specified [configFiles].
         */
        fun load(configFile: Path, vararg configFiles: Path?): CustomizationConfig =
            ConfigFactory.parseString(configFile.readText())
                .withFallback(ConfigFactory.systemProperties())
                .let {
                    configFiles
                        .filterNotNull()
                        .map { ConfigFactory.parseString(it.readText()) }
                        .fold(it, Config::withFallback)
                }
                .resolve()
                .run {
                    CustomizationConfig(
                        extract("trace") ?: false,
                        extract("name") ?: configFile.fileName.nameWithoutExtension,
                        (extract("timeZone") ?: extract<String?>("timezone"))?.let { TimeZone.getTimeZone(it) },
                        extract<IntermediaryHostname?>("hostname")?.run { Hostname(name, randomSuffix ?: true) },
                        extract("wifi"),
                        kotlin.runCatching { extract<String?>("os") }.getOrNull().run {
                            val os = this?.takeUnlessBlank() ?: throw IllegalArgumentException("Missing configuration: ${"os".asParam()}")
                            val ranking: Map<Int, List<OperatingSystems>> = OperatingSystems.values().groupBy {
                                minOf(os.levenshteinDistance(it.name), os.levenshteinDistance(it.fullName))
                            }
                            val (_, matches) = ranking.minByOrNull { it.key } ?: error("They are no configured operating systems to choose from.")
                            when (matches.size) {
                                0 -> throw IllegalArgumentException("$os is not supported.")
                                1 -> matches.first()
                                else -> throw IllegalArgumentException(
                                    "$os is not supported. Did you mean any of the following? ${
                                        matches.joinToString("") {
                                            "$LF- ${it.name}: ${it.fullName}"
                                        }
                                    }")
                            }
                        },
                        extract<String?>("size")?.takeUnless { it.isBlank() }?.toSize(),
                        extract<IntermediarySsh?>("ssh")?.run {
                            val fileBasedKeys = (authorizedKeys?.files ?: emptyList()).flatMap { glob ->
                                com.bkahlert.kustomize.Kustomize.HomeDirectory.ls(glob).map { file -> file.readText() }
                                    .filter { content -> content.startsWith("ssh-") }
                            }
                            val stringBasedKeys = (authorizedKeys?.keys ?: emptyList()).map { it.trim() }
                            Ssh(enabled, port, fileBasedKeys + stringBasedKeys)
                        },
                        extract<IntermediaryDefaultUser?>("defaultUser")?.run {
                            DefaultUser(username, newUsername, newPassword
                                ?.takeIf { it.matches(".*\\^\\d+".toRegex()) }
                                ?.let {
                                    val password = it.dropLastWhile { char -> char.isDigit() }.dropLast(1)
                                    val offset = it.takeLastWhile { char -> char.isDigit() }.toInt()
                                    password - offset
                                } ?: newPassword)
                        },
                        extract<IntermediarySamba?>("samba")?.run { Samba(homeShare ?: false, rootShare ?: none) },
                        extract<IntermediaryUsbGadgets?>("usbGadgets")?.run {
                            listOfNotNull(ethernet?.run {
                                val subnet: IPSubnet<out IPAddress> = ipSubnetOf(dhcpRange)
                                val ip: IPAddress? = deviceAddress?.toIP()
                                Ethernet(subnet, ip, hostAsDefaultGateway, enableSerialConsole, manufacturer, product)
                            })
                        } ?: emptyList(),
                        extract("tweaks"),
                        extract<List<IntermediaryFileOperation>?>("files")?.map {
                            FileOperation(
                                it.append?.trimIndent(),
                                it.hostPath?.asPath()?.also { path ->
                                    require(path.exists()) { "$path does not exist." }
                                },
                                LinuxRoot / it.diskPath,
                            )
                        } ?: emptyList(),
                        extract<List<IntermediarySetupScript>?>("setup")?.map {
                            SetupScript(it.name, it.sources ?: emptyList(), it.scripts?.map { (name, content) ->
                                ShellScript(name, content ?: "")
                            } ?: emptyList())
                        } ?: emptyList(),
                        extract<List<IntermediaryShellScript>?>("firstBoot")?.map {
                            ShellScript(it.name, it.content ?: "")
                        } ?: emptyList(),
                    )
                }

        data class IntermediaryHostname(val name: String, val randomSuffix: Boolean?)
        data class IntermediarySsh(val enabled: Boolean?, val port: Int?, val authorizedKeys: AuthorizedKeys?) {
            data class AuthorizedKeys(val files: List<String>?, val keys: List<String>?)
        }

        data class IntermediaryDefaultUser(val username: String?, val newUsername: String?, val newPassword: String?)
        data class IntermediarySamba(val homeShare: Boolean?, val rootShare: RootShare?)

        data class IntermediaryUsbGadgets(val ethernet: IntermediaryEthernet?) {
            data class IntermediaryEthernet(
                val dhcpRange: String,
                val deviceAddress: String?,
                val hostAsDefaultGateway: Boolean?,
                val enableSerialConsole: Boolean?,
                val manufacturer: String?,
                val product: String?,
            )
        }

        data class IntermediaryFileOperation(val append: String?, val hostPath: String?, val diskPath: String)
        data class IntermediarySetupScript(val name: String, val sources: List<URI>?, val scripts: List<IntermediaryShellScript>?)
        data class IntermediaryShellScript(val name: String?, val content: String?)
    }

    data class Hostname(val name: String, val randomSuffix: Boolean)
    data class Wifi(val wpaSupplicant: String?, val autoReconnect: Boolean?, val powerSafeMode: Boolean?)
    data class Ssh(val enabled: Boolean?, val port: Int?, val authorizedKeys: List<String>)
    data class DefaultUser(val username: String?, val newUsername: String?, val newPassword: String?)
    data class Samba(val homeShare: Boolean, val rootShare: RootShare)

    sealed class UsbGadget {
        data class Ethernet(
            val dhcpRange: IPSubnet<out IPAddress>,
            val deviceAddress: IPAddress?,
            val hostAsDefaultGateway: Boolean? = null,
            val enableSerialConsole: Boolean? = null,
            val manufacturer: String? = null,
            val product: String? = null,
        ) : UsbGadget()
    }

    data class Tweaks(val aptRetries: Int?)

    data class FileOperation(val append: String?, val hostPath: Path?, val diskPath: DiskPath) {
        init {
            require((append != null) || (hostPath != null)) {
                "At least one of the options ${::append.asParam()} and ${::hostPath.asParam()} must be specified."
            }
        }
    }

    /**
     * A collection of [scripts] that gets executed withs reboots in between,
     * that is, two setup scripts each run on their own with one reboot in between.
     */
    class SetupScript(
        val name: String,
        val sources: List<URI>,
        val scripts: List<ShellScript>,
    ) : List<ShellScript> by scripts

    /**
     * Returns a list of patches that reflect this configuration.
     */
    fun toPatches(): List<(OperatingSystemImage) -> PhasedPatch> = buildList {
        size?.apply { add(ResizePatch(this)) }

        hostname?.apply { add(HostnamePatch(name, randomSuffix)) }

        timeZone?.apply { add(TimeZonePatch(this)) }

        wifi?.apply {
            wpaSupplicant?.also { add(WpaSupplicantPatch(it)) }
            autoReconnect?.takeIf { it }?.also { add(WifiAutoReconnectPatch()) }
            powerSafeMode?.takeIf { !it }
                ?.also { add(WifiPowerSafeModePatch()) }
        }

        defaultUser?.apply {
            val username = username ?: os.defaultCredentials.username
            if (newUsername != null) add(UsernamePatch(username, newUsername))
            if (newPassword != null) add(PasswordPatch(newUsername ?: username, newPassword))
        }

        ssh?.apply {
            enabled?.takeIf { it }.apply { add(SshEnablementPatch()) }
            port?.apply { add(SshPortPatch(this)) }
            if (authorizedKeys.isNotEmpty()) {
                val sshKeyUser = defaultUser
                    ?.newUsername
                    ?: os.defaultCredentials.takeUnless { it == OperatingSystem.Credentials.empty }?.username ?: "root"
                add(SshAuthorizationPatch(sshKeyUser, authorizedKeys))
            }
        }

        samba?.apply {
            val sambaPassword = defaultUser?.newPassword
            require(sambaPassword != null) { "Samba configuration requires a set password." }
            val sambaUsername = defaultUser?.newUsername ?: defaultUser?.username ?: os.defaultCredentials.username
            add(SambaPatch(sambaUsername, sambaPassword, homeShare, rootShare))
        }

        usbGadgets.forEach {
            when (it) {
                is Ethernet -> add(
                    UsbEthernetGadgetPatch(
                        dhcpRange = it.dhcpRange,
                        deviceAddress = it.deviceAddress ?: it.dhcpRange.firstUsableHost,
                        hostAsDefaultGateway = it.hostAsDefaultGateway ?: false,
                        enableSerialConsole = it.enableSerialConsole ?: false,
                        manufacturer = it.manufacturer ?: "Björn Kahlert",
                        product = it.product ?: "USB Gadget")
                )
            }
        }

        tweaks?.aptRetries?.apply { add(TweaksPatch(this)) }

        files.forEach {
            it.hostPath?.apply { add(CopyFilesPatch({ this } to it.diskPath)) }
            it.append?.apply { add(AppendToFilesPatch(this to it.diskPath)) }
        }

        setup.forEach { +ShellScriptPatch(it) }
        firstBoot.also { +FirstBootPatch(it) }
    }

    /**
     * Returns a list of patches that have been tested to work together.
     */
    fun toOptimizedPatches(): List<(OperatingSystemImage) -> PhasedPatch> = with(toPatches().toMutableList()) {
        listOf(
            CompositePatch(extract<ResizePatch>()
                + extract<TweaksPatch>()
                + extract<HostnamePatch>()
                + extract<TimeZonePatch>()
                + extract<UsernamePatch>()
                + extract<SshEnablementPatch>()
                + extract<WpaSupplicantPatch>()
            ),
            CompositePatch(extract<CopyFilesPatch>()
                + extract<AppendToFilesPatch>()
                + extract<WifiAutoReconnectPatch>()
                + extract<WifiPowerSafeModePatch>()
                + extract<SambaPatch>()
            ),
            CompositePatch(extract<PasswordPatch>()
                + extract<SshAuthorizationPatch>()
                + extract<SshPortPatch>()
                + extract<UsbEthernetGadgetPatch>()
                + extract<ShellScriptPatch>()
            ),
            *toTypedArray()
        )
    }

    private inline fun <reified T : (OperatingSystemImage) -> PhasedPatch> MutableList<(OperatingSystemImage) -> PhasedPatch>.extract(): List<T> =
        filterIsInstance<T>().also { removeAll(it) }
}

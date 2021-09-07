package com.bkahlert.kustomize.cli

import com.bkahlert.kommons.builder.buildList
import com.bkahlert.kommons.io.path.asPath
import com.bkahlert.kommons.net.IPAddress
import com.bkahlert.kommons.net.IPSubnet
import com.bkahlert.kommons.net.ipSubnetOf
import com.bkahlert.kommons.net.toIP
import com.bkahlert.kommons.shell.ShellScript
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.levenshteinDistance
import com.bkahlert.kommons.text.minus
import com.bkahlert.kommons.text.takeUnlessBlank
import com.bkahlert.kommons.unit.Size
import com.bkahlert.kommons.unit.toSize
import com.bkahlert.kustomize.cli.CustomizationConfig.BluetoothProfile
import com.bkahlert.kustomize.cli.CustomizationConfig.BluetoothProfile.PersonalAreaNetwork
import com.bkahlert.kustomize.cli.CustomizationConfig.DefaultUser
import com.bkahlert.kustomize.cli.CustomizationConfig.FileOperation
import com.bkahlert.kustomize.cli.CustomizationConfig.Hostname
import com.bkahlert.kustomize.cli.CustomizationConfig.Samba
import com.bkahlert.kustomize.cli.CustomizationConfig.SetupScript
import com.bkahlert.kustomize.cli.CustomizationConfig.Ssh
import com.bkahlert.kustomize.cli.CustomizationConfig.Tweaks
import com.bkahlert.kustomize.cli.CustomizationConfig.UsbDevice
import com.bkahlert.kustomize.cli.CustomizationConfig.UsbDevice.Gadget
import com.bkahlert.kustomize.cli.CustomizationConfig.Wifi
import com.bkahlert.kustomize.libguestfs.GuestfishCommandLine
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine
import com.bkahlert.kustomize.os.DiskPath
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OperatingSystem
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.OperatingSystems
import com.bkahlert.kustomize.patch.AppendToFilesPatch
import com.bkahlert.kustomize.patch.BluetoothPersonalAreaNetworkPatch
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
import com.bkahlert.kustomize.patch.UsbGadgetPatch
import com.bkahlert.kustomize.patch.UsernamePatch
import com.bkahlert.kustomize.patch.WifiAutoReconnectPatch
import com.bkahlert.kustomize.patch.WifiPowerSafeModePatch
import com.bkahlert.kustomize.patch.WpaSupplicantPatch
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import java.net.URI
import java.nio.file.Path
import java.util.TimeZone
import kotlin.io.path.exists
import kotlin.io.path.isReadable
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
     * The [UsbDevice] settings to be applied to the image.
     */
    val usbDevices: List<UsbDevice>,
    /**
     * The [BluetoothProfile] settings to be applied to the image.
     */
    val bluetoothProfiles: List<BluetoothProfile>,
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
                        extract<IntermediaryHostname?>("hostname")?.run {
                            Hostname(name, randomSuffix ?: true, prettyName, iconName ?: "computer", chassis ?: "embedded")
                        },
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
                            val fileBasedKeys = (authorizedKeys?.files ?: emptyList()).map { keyFile ->
                                keyFile.asPath()
                                    .also { require(it.exists()) { "SSH key file $it does not exist" } }
                                    .also { require(it.isReadable()) { "SSH key file $it cannot be read" } }
                                    .readText()
                                    .trim()
                            }.filter { content -> content.startsWith("ssh-") }
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
                        extract<IntermediaryUsbDevice?>("usb")?.run {
                            listOfNotNull(gadget?.run {
                                val subnet: IPSubnet<out IPAddress> = ipSubnetOf(dhcpRange)
                                val ip: IPAddress? = deviceAddress?.toIP()
                                Gadget(subnet, ip, hostAsDefaultGateway, enableSerialConsole, manufacturer, product)
                            })
                        } ?: emptyList(),
                        extract<IntermediaryBluetoothProfile?>("bluetooth")?.run {
                            listOfNotNull(pan?.run {
                                val subnet: IPSubnet<out IPAddress> = ipSubnetOf(dhcpRange)
                                val ip: IPAddress? = deviceAddress?.toIP()
                                PersonalAreaNetwork(subnet, ip)
                            })
                        } ?: emptyList(),
                        extract("tweaks"),
                        extract<List<IntermediaryFileOperation>?>("files")?.map {
                            FileOperation(
                                it.append?.trimIndent(),
                                it.hostPath?.asPath()?.toAbsolutePath()?.also { path ->
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

        data class IntermediaryHostname(val name: String, val randomSuffix: Boolean?, val prettyName: String?, val iconName: String?, val chassis: String?)

        data class IntermediarySsh(val enabled: Boolean?, val port: Int?, val authorizedKeys: AuthorizedKeys?) {
            data class AuthorizedKeys(val files: List<String>?, val keys: List<String>?)
        }

        data class IntermediaryDefaultUser(val username: String?, val newUsername: String?, val newPassword: String?)
        data class IntermediarySamba(val homeShare: Boolean?, val rootShare: RootShare?)

        data class IntermediaryUsbDevice(val gadget: IntermediaryGadget?) {
            data class IntermediaryGadget(
                val dhcpRange: String,
                val deviceAddress: String?,
                val hostAsDefaultGateway: Boolean?,
                val enableSerialConsole: Boolean?,
                val manufacturer: String?,
                val product: String?,
            )
        }

        data class IntermediaryBluetoothProfile(val pan: IntermediaryPAN?) {
            data class IntermediaryPAN(
                val dhcpRange: String,
                val deviceAddress: String?,
            )
        }

        data class IntermediaryFileOperation(val append: String?, val hostPath: String?, val diskPath: String)
        data class IntermediarySetupScript(val name: String, val sources: List<URI>?, val scripts: List<IntermediaryShellScript>?)
        data class IntermediaryShellScript(val name: String?, val content: String?)
    }

    data class Hostname(val name: String, val randomSuffix: Boolean, val prettyName: String?, val iconName: String?, val chassis: String?)

    data class Wifi(val wpaSupplicant: String?, val autoReconnect: Boolean?, val powerSafeMode: Boolean?)
    data class Ssh(val enabled: Boolean?, val port: Int?, val authorizedKeys: List<String>)
    data class DefaultUser(val username: String?, val newUsername: String?, val newPassword: String?)
    data class Samba(val homeShare: Boolean, val rootShare: RootShare)

    sealed class UsbDevice {
        data class Gadget(
            val dhcpRange: IPSubnet<out IPAddress>,
            val deviceAddress: IPAddress?,
            val hostAsDefaultGateway: Boolean? = null,
            val enableSerialConsole: Boolean? = null,
            val manufacturer: String? = null,
            val product: String? = null,
        ) : UsbDevice()
    }

    sealed class BluetoothProfile {
        data class PersonalAreaNetwork(
            val dhcpRange: IPSubnet<out IPAddress>,
            val deviceAddress: IPAddress?,
        ) : BluetoothProfile()
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

        hostname?.apply { add(HostnamePatch(name, randomSuffix, prettyName, iconName, chassis)) }

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

        usbDevices.forEach {
            when (it) {
                is Gadget -> add(
                    UsbGadgetPatch(
                        dhcpRange = it.dhcpRange,
                        deviceAddress = it.deviceAddress ?: it.dhcpRange.firstUsableHost,
                        hostAsDefaultGateway = it.hostAsDefaultGateway ?: false,
                        enableSerialConsole = it.enableSerialConsole ?: false,
                        manufacturer = it.manufacturer ?: "Björn Kahlert",
                        product = it.product ?: "USB Gadget")
                )
            }
        }

        bluetoothProfiles.forEach {
            when (it) {
                is PersonalAreaNetwork -> add(
                    BluetoothPersonalAreaNetworkPatch(
                        dhcpRange = it.dhcpRange,
                        deviceAddress = it.deviceAddress ?: it.dhcpRange.firstUsableHost,
                    )
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
                + extract<UsbGadgetPatch>()
                + extract<BluetoothPersonalAreaNetworkPatch>()
                + extract<ShellScriptPatch>()
            ),
            *toTypedArray()
        )
    }

    private inline fun <reified T : (OperatingSystemImage) -> PhasedPatch> MutableList<(OperatingSystemImage) -> PhasedPatch>.extract(): List<T> =
        filterIsInstance<T>().also { removeAll(it) }
}

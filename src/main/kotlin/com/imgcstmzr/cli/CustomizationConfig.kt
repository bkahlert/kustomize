package com.imgcstmzr.cli

import com.imgcstmzr.ImgCstmzr
import com.imgcstmzr.cli.Convertible.Companion.converter
import com.imgcstmzr.cli.CustomizationConfig.Hostname
import com.imgcstmzr.cli.CustomizationConfig.Samba
import com.imgcstmzr.cli.CustomizationConfig.SetupScript
import com.imgcstmzr.cli.CustomizationConfig.Ssh
import com.imgcstmzr.cli.CustomizationConfig.Tweaks
import com.imgcstmzr.cli.CustomizationConfig.UsbGadget
import com.imgcstmzr.cli.CustomizationConfig.UsbGadget.Ethernet
import com.imgcstmzr.cli.CustomizationConfig.Wifi
import com.imgcstmzr.os.DiskPath
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystem
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystems
import com.imgcstmzr.patch.AppendToFilesPatch
import com.imgcstmzr.patch.CompositePatch
import com.imgcstmzr.patch.CopyFilesPatch
import com.imgcstmzr.patch.FirstBootPatch
import com.imgcstmzr.patch.HostnamePatch
import com.imgcstmzr.patch.ImgResizePatch
import com.imgcstmzr.patch.PasswordPatch
import com.imgcstmzr.patch.PhasedPatch
import com.imgcstmzr.patch.RootShare
import com.imgcstmzr.patch.RootShare.none
import com.imgcstmzr.patch.SambaPatch
import com.imgcstmzr.patch.ShellScriptPatch
import com.imgcstmzr.patch.SshAuthorizationPatch
import com.imgcstmzr.patch.SshEnablementPatch
import com.imgcstmzr.patch.SshPortPatch
import com.imgcstmzr.patch.TimeZonePatch
import com.imgcstmzr.patch.TweaksPatch
import com.imgcstmzr.patch.UsbEthernetGadgetPatch
import com.imgcstmzr.patch.UsernamePatch
import com.imgcstmzr.patch.WifiAutoReconnectPatch
import com.imgcstmzr.patch.WifiPowerSafeModePatch
import com.imgcstmzr.patch.WpaSupplicantPatch
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import koodies.builder.buildList
import koodies.io.ls
import koodies.net.IPAddress
import koodies.net.IPSubnet
import koodies.net.ipSubnetOf
import koodies.net.toIP
import koodies.shell.ShellScript
import koodies.text.minus
import koodies.unit.Size
import koodies.unit.toSize
import java.net.URI
import java.nio.file.Path
import java.util.TimeZone
import kotlin.io.path.exists
import kotlin.io.path.readText

data class CustomizationConfig(
    val trace: Boolean,
    val name: String,
    val timeZone: TimeZone?,
    val hostname: Hostname?,
    val wifi: Wifi?,
    val os: OperatingSystems,
    val size: Size?,
    val ssh: Ssh?,
    val defaultUser: DefaultUser?,
    val samba: Samba?,
    val usbGadgets: List<UsbGadget>,
    val tweaks: Tweaks?,
    val files: List<FileOperation>,
    /**
     * Setup scripts of which each gets executed with a consecutive reboot.
     */
    val setup: List<SetupScript>,
    /**
     * Scripts that will be installed as firstboot scripts, that is,
     * their are not executed before the prepared image is started on the
     * target device.
     */
    val firstBoot: List<ShellScript>,
) {

    companion object {

        /**
         * Loads a [CustomizationConfig] based on the specified [configFiles].
         */
        fun load(vararg configFiles: Path): CustomizationConfig = configFiles
            .map { ConfigFactory.parseString(it.readText()) }
            .run { load(first(), *drop(1).toTypedArray()) }

        /**
         * Loads a [CustomizationConfig] based on the mandatory [config]
         * and the specified optional [fallbacks].
         */
        fun load(
            config: Config,
            vararg fallbacks: Config = arrayOf(ConfigFactory.parseString(Path.of(".env").readText())),
        ): CustomizationConfig {
            val resolve = ConfigFactory.systemProperties()
                .withFallback(config)
                .run { fallbacks.fold(this, Config::withFallback) }
                .resolve()
            val extract = resolve
                .extract<UnsafeImgCstmzrConfig>("img")
            val convert = extract
                .convert()
            return convert
        }
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
     * A collection of [scripts] that gets executed with a consecutive reboot,
     * that is, two setup scripts each run on their own with one reboot in between.
     */
    class SetupScript(
        val name: String,
        val sources: List<URI>,
        scripts: List<ShellScript>,
    ) : List<ShellScript> by scripts

    fun toPatches(): List<(OperatingSystemImage) -> PhasedPatch> = buildList {
        size?.apply { add(ImgResizePatch(this)) }

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
                        manufacturer = it.manufacturer ?: "ImgCstmzr",
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

    fun toOptimizedPatches(): List<(OperatingSystemImage) -> PhasedPatch> = with(toPatches().toMutableList()) {
        listOf(
            CompositePatch(extract<TweaksPatch>()
                + extract<HostnamePatch>()
                + extract<TimeZonePatch>()
                + extract<ImgResizePatch>()
                + extract<UsernamePatch>()
                + extract<SshEnablementPatch>()
                + extract<WpaSupplicantPatch>()),
            CompositePatch(extract<CopyFilesPatch>()
                + extract<AppendToFilesPatch>()
                + extract<WifiAutoReconnectPatch>()
                + extract<WifiPowerSafeModePatch>()
                + extract<SambaPatch>()),
            CompositePatch(extract<PasswordPatch>()
                + extract<SshAuthorizationPatch>()
                + extract<SshPortPatch>()
                + extract<UsbEthernetGadgetPatch>()),
            *toTypedArray()
        )
    }

    private inline fun <reified T : (OperatingSystemImage) -> PhasedPatch> MutableList<(OperatingSystemImage) -> PhasedPatch>.extract(): List<T> =
        filterIsInstance<T>().also { removeAll(it) }
}


data class UnsafeImgCstmzrConfig(
    val trace: Boolean = false,
    val name: String?,
    val timeZone: String?,
    val timezone: String?,
    val hostname: Hostname?,
    val wifi: Wifi?,
    val os: OperatingSystems?,
    val size: String?,
    val ssh: Ssh?,
    val defaultUser: DefaultUser?,
    val samba: Samba?,
    val usbGadgets: UsbGadgets?,
    val tweaks: Tweaks?,
    val files: List<FileOperation>?,
    val setup: List<SetupScript>?,
    val firstBoot: List<ShellScript>?,
    val flashDisk: String?,
) : Convertible<CustomizationConfig> by converter({
    CustomizationConfig(
        trace = trace,
        name = requireNotNull(name) { "Missing configuration ${"img.name".asParam()}" },
        timeZone = (timeZone ?: timezone)?.let { TimeZone.getTimeZone(it) },
        hostname = hostname.convert(),
        wifi = wifi,
        os = requireNotNull(os) { "Missing configuration ${"img.os".asParam()}" },
        size = size?.takeUnless { it.isBlank() }?.toSize(),
        ssh = ssh.convert(),
        defaultUser = defaultUser.convert(),
        samba = samba.convert(),
        usbGadgets = usbGadgets.convert() ?: emptyList(),
        tweaks = tweaks,
        files = files?.run { map { it.convert() } } ?: emptyList(),
        setup = setup?.map { it.convert() } ?: emptyList(),
        firstBoot = firstBoot?.map { it.convert() } ?: emptyList(),
    )
}) {
    data class Hostname(val name: String, val randomSuffix: Boolean?) :
        Convertible<CustomizationConfig.Hostname> by converter({
            CustomizationConfig.Hostname(name, randomSuffix ?: true)
        })

    data class Ssh(val enabled: Boolean?, val port: Int?, val authorizedKeys: AuthorizedKeys?) :
        Convertible<CustomizationConfig.Ssh> by converter({
            val fileBasedKeys = (authorizedKeys?.files ?: emptyList()).flatMap { glob ->
                ImgCstmzr.HomeDirectory.ls(glob).map { file -> file.readText() }.filter { content -> content.startsWith("ssh-") }
            }
            val stringBasedKeys = (authorizedKeys?.keys ?: emptyList()).map { it.trim() }
            Ssh(enabled, port, fileBasedKeys + stringBasedKeys)
        }) {
        data class AuthorizedKeys(val files: List<String>?, val keys: List<String>?)
    }

    data class DefaultUser(val username: String?, val newUsername: String?, val newPassword: String?) :
        Convertible<CustomizationConfig.DefaultUser> by converter({
            CustomizationConfig.DefaultUser(username, newUsername, newPassword.takeIf { it != null && it.matches(".*\\^\\d+".toRegex()) }
                ?.let {
                    val password = it.dropLastWhile { char -> char.isDigit() }.dropLast(1)
                    val offset = it.takeLastWhile { char -> char.isDigit() }.toInt()
                    password - offset
                } ?: newPassword)
        })

    data class Samba(val homeShare: Boolean?, val rootShare: RootShare?) : Convertible<CustomizationConfig.Samba> by converter({
        CustomizationConfig.Samba(homeShare ?: false, rootShare ?: none)
    })


    data class UsbGadgets(val ethernet: Ethernet?) : Convertible<List<UsbGadget>> {
        override fun convert(): List<UsbGadget> =
            listOfNotNull(
                ethernet.convert(),
            )

        class Ethernet(
            dhcpRange: String,
            deviceAddress: String?,
            hostAsDefaultGateway: Boolean?,
            enableSerialConsole: Boolean?,
            manufacturer: String?,
            product: String?,
        ) : Convertible<UsbGadget> by converter({
            val subnet: IPSubnet<out IPAddress> = ipSubnetOf(dhcpRange)
            val ip: IPAddress? = deviceAddress?.toIP()
            Ethernet(
                subnet,
                ip,
                hostAsDefaultGateway,
                enableSerialConsole,
                manufacturer,
                product,
            )
        })
    }

    class FileOperation(
        append: String?,
        hostPath: String?,
        diskPath: String,
    ) : Convertible<CustomizationConfig.FileOperation> by converter({
        CustomizationConfig.FileOperation(
            append?.trimIndent(),
            hostPath?.let { path ->
                val ls = ImgCstmzr.WorkingDirectory.ls(path)
                ls.firstOrNull { it.exists() }
                    ?: error("The resolved expression $hostPath would point to at least one existing file.")
            },
            LinuxRoot / diskPath,
        )
    })

    data class SetupScript(
        val name: String,
        val sources: List<URI>?,
        val scripts: List<ShellScript>?,
    ) : Convertible<CustomizationConfig.SetupScript> by converter({
        SetupScript(name, sources ?: emptyList(), scripts?.map { it.convert() } ?: emptyList())
    })

    data class ShellScript(
        val name: String?,
        val content: String?,
    ) : Convertible<koodies.shell.ShellScript> by converter({
        koodies.shell.ShellScript(name, content ?: "")
    })
}


fun interface Convertible<T> {
    fun convert(): T

    companion object {
        inline fun <reified T> converter(crossinline converter: () -> T): Convertible<T> =
            Convertible { converter() }
    }
}

inline fun <reified T> Convertible<T>?.convert(): T? = this?.convert()

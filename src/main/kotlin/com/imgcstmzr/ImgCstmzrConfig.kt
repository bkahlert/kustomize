package com.imgcstmzr

import com.imgcstmzr.cli.asParam
import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.patch.AppendToFilesPatch
import com.imgcstmzr.patch.CompositePatch
import com.imgcstmzr.patch.CopyFilesPatch
import com.imgcstmzr.patch.FirstBootPatch
import com.imgcstmzr.patch.HostnamePatch
import com.imgcstmzr.patch.ImgResizePatch
import com.imgcstmzr.patch.PasswordPatch
import com.imgcstmzr.patch.Patch
import com.imgcstmzr.patch.RootShare
import com.imgcstmzr.patch.SambaPatch
import com.imgcstmzr.patch.ShellScriptPatch
import com.imgcstmzr.patch.SshAuthorizationPatch
import com.imgcstmzr.patch.SshEnablementPatch
import com.imgcstmzr.patch.SshPortPatch
import com.imgcstmzr.patch.TimeZonePatch
import com.imgcstmzr.patch.TweaksPatch
import com.imgcstmzr.patch.UsbOnTheGoPatch
import com.imgcstmzr.patch.UsernamePatch
import com.imgcstmzr.patch.WifiAutoReconnectPatch
import com.imgcstmzr.patch.WifiPowerSafeModePatch
import com.imgcstmzr.patch.WpaSupplicantPatch
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.runtime.OperatingSystems
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import koodies.builder.ListBuilder.Companion.buildList
import koodies.io.path.Locations
import koodies.io.path.Locations.ls
import koodies.shell.ShellScript
import koodies.text.minus
import koodies.unit.Size
import koodies.unit.toSize
import java.net.URI
import java.nio.file.Path
import java.util.TimeZone
import kotlin.io.path.exists
import kotlin.io.path.readText

class ImgCstmzrConfig(
    val trace: Boolean = false,
    val name: String,
    val timeZone: String?,
    val timezone: String?,
    val hostname: Hostname?,
    val wifi: Wifi?,
    val os: OperatingSystems,
    size: String?,
    val ssh: Ssh?,
    val defaultUser: DefaultUser?,
    val samba: Samba?,
    val usbOtg: String?,
    val usbOtgOptions: List<String>?,
    val tweaks: Tweaks?,
    val files: List<FileOperation>?,
    val setup: List<SetupScript>?,
    val firstBoot: List<ShellScript>?,
    val flashDisk: String?,
) {

    companion object {

        fun load(vararg configFiles: Path): ImgCstmzrConfig = configFiles
            .map { ConfigFactory.parseString(it.readText()) }
            .run { load(first(), *drop(1).toTypedArray()) }

        fun load(
            config: Config,
            vararg fallbacks: Config = arrayOf(ConfigFactory.parseString(Path.of(".env").readText())),
        ): ImgCstmzrConfig =
            ConfigFactory.systemProperties()
                .withFallback(config)
                .run { fallbacks.fold(this, Config::withFallback) }
                .resolve().extract("img-cstmztn")
    }

    data class Hostname(val name: String, val randomSuffix: Boolean = true)

    data class Wifi(val wpaSupplicant: String?, val autoReconnect: Boolean?, val powerSafeMode: Boolean?)

    val imgSize: Size? = size?.takeUnless { it.isBlank() }?.toSize()

    class Ssh(val enabled: Boolean?, val port: Int?, authorizedKeys: AuthorizedKeys?) {
        private val fileBasedKeys = (authorizedKeys?.files ?: emptyList()).flatMap { glob ->
            Locations.ls(glob)
                .map { file -> file.readText() }
                .filter { content -> content.startsWith("ssh-") }
        }
        private val stringBasedKeys = (authorizedKeys?.keys ?: emptyList()).map { it.trim() }
        val authorizedKeys: List<String> = fileBasedKeys + stringBasedKeys
    }

    data class AuthorizedKeys(val files: List<String>?, val keys: List<String>?)

    data class DefaultUser(val username: String?, val newUsername: String?, private val newPassword: String?) {
        val unshiftPassword: String? = newPassword.takeIf { it != null && it.matches(".*\\^\\d+".toRegex()) }
            ?.let {
                val password = it.dropLastWhile { char -> char.isDigit() }.dropLast(1)
                val offset = it.takeLastWhile { char -> char.isDigit() }.toInt()
                password - offset
            } ?: newPassword
    }

    data class Samba(val homeShare: Boolean?, val rootShare: RootShare?) {
        val sanitizedHomeShare: Boolean get() = homeShare ?: false
        val sanitizedRootShare: RootShare get() = rootShare ?: RootShare.none
    }

    data class Tweaks(val aptRetries: Int?)

    data class FileOperation(
        val append: String?,
        val hostPath: String?,
        val diskPath: String,
    ) {
        init {
            require((append != null) xor (hostPath != null)) {
                """
                You can only specify ${::append.asParam()} or ${::append.asParam()} as an option per directive.
                """.trimIndent()
            }
        }

        val sanitizedAppend = append?.trimIndent()

        val resolvedHostPath = hostPath?.let {
            ls(it).filter { it.exists() }.firstOrNull()
                ?: error("The resolved expression $hostPath would point to at least one existing file.")
        }
        val sanitizedDiskPath = DiskPath(diskPath)
    }

    class SetupScript(
        val name: String,
        val sources: List<URI>?,
        scripts: List<ShellScript>,
    ) : List<ShellScript> by scripts

    fun toPatches(): List<Patch> = buildList {
        imgSize?.apply { +ImgResizePatch(this) }

        hostname?.apply { +HostnamePatch(name, randomSuffix) }

        timeZone ?: timezone?.apply { +TimeZonePatch(TimeZone.getTimeZone(this)) }

        wifi?.apply {
            wpaSupplicant?.also { +WpaSupplicantPatch(it) }
            autoReconnect?.takeIf { it }?.also { +WifiAutoReconnectPatch() }
            powerSafeMode?.takeIf { !it }?.also { +WifiPowerSafeModePatch() }
        }

        defaultUser?.apply {
            val username = username ?: os.defaultCredentials.username
            if (newUsername != null) +UsernamePatch(username, newUsername)
            if (unshiftPassword != null) +PasswordPatch(newUsername ?: username, unshiftPassword)
        }

        ssh?.apply {
            enabled?.takeIf { it }.apply { +SshEnablementPatch() }
            port?.apply { +SshPortPatch(this) }
            if (authorizedKeys.isNotEmpty()) {
                val sshKeyUser = defaultUser?.newUsername
                    ?: os.defaultCredentials.takeUnless { it == OperatingSystem.Credentials.empty }?.username ?: "root"
                +SshAuthorizationPatch(sshKeyUser, authorizedKeys)
            }
        }

        samba?.apply {
            val sambdaPassword = defaultUser?.unshiftPassword
            require(sambdaPassword != null) { "Samba configuration requires a set password." }
            val sambaUsername = defaultUser?.newUsername ?: defaultUser?.username ?: os.defaultCredentials.username
            +SambaPatch(sambaUsername, sambdaPassword, sanitizedHomeShare, sanitizedRootShare)
        }

        usbOtg?.apply {
            +UsbOnTheGoPatch()
        } // TODO fix so that the DNS is no more fucked up
        tweaks?.aptRetries?.apply { +TweaksPatch(this) }
        files?.apply {
            partition { it.sanitizedAppend != null }.let { (appendOperations, copyOperations) ->
                appendOperations.takeIf { it.isNotEmpty() }?.let { +AppendToFilesPatch(it.map { it.sanitizedAppend!! to it.sanitizedDiskPath }.toMap()) }
                copyOperations.takeIf { it.isNotEmpty() }?.let { +CopyFilesPatch(it.map { it.resolvedHostPath!! to it.sanitizedDiskPath }.toMap()) }
            }
        }
        setup?.forEach { +ShellScriptPatch(it) }
        firstBoot?.also { +FirstBootPatch(it) }
    }

    fun toOptimizedPatches(): List<Patch> = with(toPatches().toMutableList()) {
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
                + extract<UsbOnTheGoPatch>()),
            *toTypedArray()
        ).filter { it.isNotEmpty }
    }

    private inline fun <reified T : Patch> MutableList<Patch>.extract(): List<T> =
        filterIsInstance<T>().also { this.removeAll(it) }

    override fun toString(): String =
        "ImageCustomization(trace=$trace, name='$name', os=$os, ssh=$ssh, defaultUser=$defaultUser, usbOtg=$usbOtg, setup=$setup, imgSize=$imgSize)"
}

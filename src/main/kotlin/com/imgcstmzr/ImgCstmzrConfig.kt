package com.imgcstmzr

import com.imgcstmzr.patch.CompositePatch
import com.imgcstmzr.patch.HostnamePatch
import com.imgcstmzr.patch.ImgResizePatch
import com.imgcstmzr.patch.PasswordPatch
import com.imgcstmzr.patch.Patch
import com.imgcstmzr.patch.RootShare
import com.imgcstmzr.patch.SambaPatch
import com.imgcstmzr.patch.ShellScriptPatch
import com.imgcstmzr.patch.SshAuthorizationPatch
import com.imgcstmzr.patch.SshEnablementPatch
import com.imgcstmzr.patch.UsbOnTheGoPatch
import com.imgcstmzr.patch.UsernamePatch
import com.imgcstmzr.patch.WpaSupplicantPatch
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.runtime.OperatingSystems
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import koodies.builder.ListBuilder.Companion.buildList
import koodies.concurrent.process.IO
import koodies.concurrent.script
import koodies.shell.ShellScript
import koodies.text.minus
import koodies.unit.Size
import koodies.unit.toSize
import java.nio.file.Path
import kotlin.io.path.readText

class ImgCstmzrConfig(
    val trace: Boolean = false,
    val name: String,
    val hostname: Hostname?,
    val wpaSupplicant: String?,
    val os: OperatingSystems,
    size: String?,
    val ssh: Ssh,
    val defaultUser: DefaultUser?,
    val samba: Samba?,
    val usbOtg: String?,
    val setup: List<SetupScript>?,
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

    val imgSize: Size? = size?.takeUnless { it.isBlank() }?.toSize()

    class Ssh(val enabled: Boolean, authorizedKeys: AuthorizedKeys) {
        val authorizedKeys: List<String> = (authorizedKeys.files ?: emptyList()).mapNotNull { file ->
            script { !"cat $file" }.ioLog.logged.first { it.type == IO.Type.OUT }
                .let { it.unformatted.takeIf { it.startsWith("ssh-") } }
        } + (authorizedKeys.keys ?: emptyList()).map { it.trim() }
    }

    data class AuthorizedKeys(val files: List<String>?, val keys: List<String>?)

    data class DefaultUser(val username: String?, val newUsername: String?, val newPassword: String?) {
        val unshiftPassword: String? = newPassword.takeIf { it != null && it.matches(".*^\\d+".toRegex()) }
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

    class SetupScript(val name: String, scripts: List<ShellScript>) : List<ShellScript> by scripts

    fun toPatches(): List<Patch> = buildList {
        if (imgSize != null) +ImgResizePatch(imgSize)

        if (hostname != null) {
            +HostnamePatch(hostname.name, hostname.randomSuffix)
        }

        if (wpaSupplicant != null) {
            +WpaSupplicantPatch(wpaSupplicant)
        }

        if (defaultUser != null) {
            val username = defaultUser.username ?: os.defaultCredentials.username
            if (defaultUser.newUsername != null) +UsernamePatch(username, defaultUser.newUsername)
            if (defaultUser.unshiftPassword != null) +PasswordPatch(
                os,
                defaultUser.newUsername ?: username,
                defaultUser.unshiftPassword
            )
        }

        if (ssh.enabled) +SshEnablementPatch()
        if (ssh.authorizedKeys.isNotEmpty()) {
            val sshKeyUser = defaultUser?.newUsername
                ?: os.defaultCredentials.takeUnless { it == OperatingSystem.Credentials.empty }?.username ?: "root"
            +SshAuthorizationPatch(sshKeyUser, ssh.authorizedKeys)
        }

        if (samba != null) {
            val sambdaPassword = defaultUser?.unshiftPassword
            require(sambdaPassword != null) { "Samba configuration requires a set password." }
            +SambaPatch(defaultUser?.username ?: os.defaultCredentials.username, sambdaPassword, samba.sanitizedHomeShare, samba.sanitizedRootShare)
        }

        if (usbOtg != null) +UsbOnTheGoPatch(usbOtg)

        setup?.forEach {
            +ShellScriptPatch(it)
        } ?: emptyList<Patch>()
    }

    fun toOptimizedPatches(): List<Patch> = with(toPatches().toMutableList()) {
        listOf(
            CompositePatch(extract<HostnamePatch>() + extract<ImgResizePatch>() + extract<UsernamePatch>() + extract<SshEnablementPatch>() + extract<WpaSupplicantPatch>() + extract<SambaPatch>()),
            CompositePatch(extract<PasswordPatch>() + extract<SshAuthorizationPatch>() + extract<UsbOnTheGoPatch>()),
            *toTypedArray()
        )
    }

    private inline fun <reified T : Patch> MutableList<Patch>.extract(): List<T> =
        filterIsInstance<T>().also { this.removeAll(it) }

    override fun toString(): String =
        "ImageCustomization(trace=$trace, name='$name', os=$os, ssh=$ssh, defaultUser=$defaultUser, usbOtg=$usbOtg, setup=$setup, imgSize=$imgSize)"
}


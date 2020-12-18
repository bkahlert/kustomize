package com.imgcstmzr

import com.bkahlert.koodies.builder.ListBuilder.Companion.buildList
import com.bkahlert.koodies.concurrent.process.Processes.evalToOutput
import com.bkahlert.koodies.nio.file.readText
import com.bkahlert.koodies.shell.ShellScript
import com.bkahlert.koodies.unit.Size
import com.bkahlert.koodies.unit.Size.Companion.toSize
import com.imgcstmzr.patch.CompositePatch
import com.imgcstmzr.patch.ImgResizePatch
import com.imgcstmzr.patch.PasswordPatch
import com.imgcstmzr.patch.Patch
import com.imgcstmzr.patch.ShellScriptPatch
import com.imgcstmzr.patch.SshAuthorizationPatch
import com.imgcstmzr.patch.SshEnablementPatch
import com.imgcstmzr.patch.UsbOnTheGoPatch
import com.imgcstmzr.patch.UsernamePatch
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.runtime.OperatingSystems
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import java.nio.file.Path

class ImgCstmzrConfig(
    val trace: Boolean = false,
    val name: String,
    val os: OperatingSystems,
    size: String?,
    val ssh: Ssh,
    val defaultUser: DefaultUser?,
    val usbOtg: UsbOtgCstmztn?,
    val setup: List<SetupScript>?,
) {
    companion object {

        fun load(vararg configFiles: Path): ImgCstmzrConfig = configFiles
            .map { ConfigFactory.parseString(it.readText()) }
            .run { load(first(), *drop(1).toTypedArray()) }

        fun load(config: Config, vararg fallbacks: Config = arrayOf(ConfigFactory.parseString(Path.of(".env").readText()))): ImgCstmzrConfig =
            ConfigFactory.systemProperties()
                .withFallback(config)
                .run { fallbacks.fold(this, Config::withFallback) }
                .resolve().extract("img-cstmztn")
    }

    val imgSize: Size? = size?.takeUnless { it.isBlank() }?.toSize()

    class Ssh(val enabled: Boolean, authorizedKeys: AuthorizedKeys) {
        val authorizedKeys: List<String> = (authorizedKeys.files ?: emptyList()).mapNotNull { file ->
            ShellScript { !"cat $file" }.evalToOutput().takeIf { it.trim().startsWith("ssh-") }
        } + (authorizedKeys.keys ?: emptyList()).map { it.trim() }
    }

    data class AuthorizedKeys(val files: List<String>?, val keys: List<String>?)

    data class DefaultUser(val username: String?, val newUsername: String?, val newPassword: String?)
    data class UsbOtgCstmztn(val profiles: List<String>)
    class SetupScript(val name: String, scripts: List<ShellScript>) : List<ShellScript> by scripts

    fun toPatches(): List<Patch> = buildList {
        if (imgSize != null) +ImgResizePatch(imgSize)
        if (defaultUser != null) {
            val username = defaultUser.username ?: os.defaultCredentials.username
            if (defaultUser.newUsername != null) +UsernamePatch(username, defaultUser.newUsername)
            if (defaultUser.newPassword != null) +PasswordPatch(os, defaultUser.newUsername ?: username, defaultUser.newPassword)
        }

        if (ssh.enabled) +SshEnablementPatch()
        if (ssh.authorizedKeys.isNotEmpty()) {
            val sshKeyUser = defaultUser?.newUsername ?: os.defaultCredentials.takeUnless { it == OperatingSystem.Credentials.empty }?.username ?: "root"
            +SshAuthorizationPatch(sshKeyUser, ssh.authorizedKeys)
        }

        usbOtg?.let {
            if (it.profiles.isNotEmpty()) {
                +UsbOnTheGoPatch(usbOtg.profiles)
            }
        }

        setup?.forEach {
            +ShellScriptPatch(it)
        } ?: emptyList<Patch>()
    }

    fun toOptimizedPatches(): List<Patch> = with(toPatches().toMutableList()) {
        listOf(
            CompositePatch(extract<ImgResizePatch>() + extract<UsernamePatch>() + extract<SshEnablementPatch>() + extract<UsbOnTheGoPatch>()),
            CompositePatch(extract<PasswordPatch>() + extract<SshAuthorizationPatch>()),
            *toTypedArray()
        )
    }

    private inline fun <reified T : Patch> MutableList<Patch>.extract(): List<T> = filterIsInstance<T>().also { this.removeAll(it) }

    override fun toString(): String =
        "ImageCustomization(trace=$trace, name='$name', os=$os, ssh=$ssh, defaultUser=$defaultUser, usbOtg=$usbOtg, setup=$setup, imgSize=$imgSize)"
}


package com.imgcstmzr

import com.bkahlert.koodies.builder.ListBuilder.Companion.build
import com.bkahlert.koodies.concurrent.process.Processes.evalToOutput
import com.bkahlert.koodies.nio.file.readText
import com.bkahlert.koodies.shell.ShellScript
import com.bkahlert.koodies.test.junit.ThirtyMinutesTimeout
import com.bkahlert.koodies.test.junit.systemproperties.SystemProperties
import com.bkahlert.koodies.test.junit.systemproperties.SystemProperty
import com.bkahlert.koodies.unit.Giga
import com.bkahlert.koodies.unit.Size
import com.bkahlert.koodies.unit.Size.Companion.bytes
import com.bkahlert.koodies.unit.Size.Companion.toSize
import com.imgcstmzr.patch.CompositePatch
import com.imgcstmzr.patch.ImgResizePatch
import com.imgcstmzr.patch.PasswordPatch
import com.imgcstmzr.patch.Patch
import com.imgcstmzr.patch.SshAuthorizationPatch
import com.imgcstmzr.patch.SshEnablementPatch
import com.imgcstmzr.patch.UsbOnTheGoPatch
import com.imgcstmzr.patch.UsernamePatch
import com.imgcstmzr.patch.booted
import com.imgcstmzr.patch.buildPatch
import com.imgcstmzr.runtime.OperatingSystem.Credentials.Companion.empty
import com.imgcstmzr.runtime.OperatingSystem.Credentials.Companion.withPassword
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.runtime.Program
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.logging.InMemoryLogger
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.nio.file.Path

@SystemProperties(
    SystemProperty("IMG_CSTMZR_USERNAME", "FiFi"),
    SystemProperty("IMG_CSTMZR_PASSWORD", "TRIXIbelle1234")
)
@Execution(CONCURRENT)
class ImgCstmzrConfigTest {

    private fun loadImgCstmztn(): ImageCustomization {
        val config = ConfigFactory.systemProperties()
            .withFallback(ConfigFactory.parseResources("sample.conf"))
            .withFallback(ConfigFactory.parseString(Path.of(".env").readText()))
            .resolve()

        val imgCstmztn = config.extract<ImageCustomization>("img-cstmztn")
        return imgCstmztn
    }

    @Test
    fun `should deserialize`() {
        val imgCstmztn = loadImgCstmztn()

        expectThat(imgCstmztn).compose("has equal properties") {
            get { name }.isEqualTo("Sample Project")
            get { os }.isEqualTo(OperatingSystems.RaspberryPiLite)
            get { imgSize }.isEqualTo(4.Giga.bytes)
            get { ssh.enabled }.isTrue()
            @Suppress("SpellCheckingInspection")
            get { ssh.authorizedKeys }.contains(
                listOf("""ssh-rsa MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKbic/EEoiSu09lYR1y5001NA1K63M/Jd+IV1b2YpoXJxWDrkzQ/3v/SE84/cSayWAy4LVEXUodrt1WkPZ/NjE8CAwEAAQ== "John Doe 2020-12-10 btw, Corona sucks""""))
            get { defaultUser }.isEqualTo(ImageCustomization.DefaultUser(null, "FiFi", "TRIXIbelle1234"))
            get { usbOtg.profiles }.containsExactlyInAnyOrder("g_acm_ms", "g_ether", "g_webcam")
            get { setup[0].name }.isEqualTo("the basics")
            get { setup[0] }.containsExactly(
                ShellScript(
                    name = "Configuring SSH port",
                    content = """sed -i 's/^\#Port 22${'$'}/Port 1234/g' /etc/ssh/sshd_config"""
                ),
                ShellScript(
                    name = "very-----------------long",
                    content = """"""
                ),
                ShellScript(
                    name = "middle",
                    content = """echo ''"""
                ),
                ShellScript(
                    name = "s",
                    content = """echo ''"""
                )
            )
            get { setup[1].name }.isEqualTo("leisure")
            get { setup[1] }.containsExactly(
                ShellScript(
                    name = "The Title",
                    content = """
                                # I rather explain things
                                echo "I'm writing a poem __〆(￣ー￣ )"
                                cat <<EOF >poem-for-you
                                Song of the Witches: “Double, double toil and trouble”
                                BY WILLIAM SHAKESPEARE
                                (from Macbeth)
                                Double, double toil and trouble;
                                Fire burn and caldron bubble.
                                Fillet of a fenny snake,
                                In the caldron boil and bake;
                                Eye of newt and toe of frog,
                                Wool of bat and tongue of dog,
                                Adder's fork and blind-worm's sting,
                                Lizard's leg and howlet's wing,
                                For a charm of powerful trouble,
                                Like a hell-broth boil and bubble.
                                Double, double toil and trouble;
                                Fire burn and caldron bubble.
                                Cool it with a baboon's blood,
                                Then the charm is firm and good.
                                EOF
                                
                            """.trimIndent()
                ),
            )
        }.then { if (allPassed) pass() else fail() }
    }

    @Test
    fun `should create patch`() {
        val imgCstmztn = loadImgCstmztn()
        val patch = imgCstmztn.createPatch()
        expectThat(CompositePatch(patch)) {
            get { name }.contains("Increasing Disk Space: 4.00 GB").contains("Change Username").contains("Mass storage and Serial")
            get { preFileImgOperations }.hasSize(1)
            get { customizationOptions }.hasSize(4)
            get { guestfishCommands }.hasSize(1)
            get { fileSystemOperations }.hasSize(11)
            get { postFileImgOperations }.hasSize(2)
            get { programs }.hasSize(5)
        }
    }

    @ThirtyMinutesTimeout @E2E @Test
    fun InMemoryLogger.`should apply patch`(@OS(OperatingSystems.RaspberryPiLite) osImage: OperatingSystemImage) {
        val imgCstmztn = loadImgCstmztn()
        val patches = imgCstmztn.createPatch()

        patches.forEach { patch ->
            with(patch) {
                patch(osImage)
            }
        }

        expect {
            that(osImage) {
                get { credentials }.isEqualTo("FiFi" withPassword "TRIXIbelle1234")
                booted(this@`should apply patch`) {
                    command("echo '👏 🤓 👋'");
                    { true }
                }
            }
        }
    }
}

class ImageCustomization(
    val name: String,
    val os: OperatingSystems,
    size: String?,
    val ssh: Ssh,
    val defaultUser: DefaultUser?,
    val usbOtg: UsbOtgCstmztn,
    val setup: List<SetupScript>,
) {
    val imgSize: Size? = size?.takeUnless { it.isBlank() }?.toSize()

    class Ssh(val enabled: Boolean, authorizedKeys: AuthorizedKeys) {
        val authorizedKeys: List<String> = authorizedKeys.files.mapNotNull { file ->
            ShellScript { !"cat $file" }.evalToOutput().takeIf { it.trim().startsWith("ssh-") }
        } + authorizedKeys.keys.map { it.trim() }
    }

    data class AuthorizedKeys(val files: List<String>, val keys: List<String>)

    data class DefaultUser(val username: String?, val newUsername: String?, val newPassword: String?)
    data class PasswordCstmztn(val username: String, val new: String)
    data class UsbOtgCstmztn(val profiles: List<String>)
    class SetupScript(val name: String, scripts: List<ShellScript>) : List<ShellScript> by scripts

    fun createPatch(): List<Patch> = build {
        if (imgSize != null) +ImgResizePatch(os, imgSize)
        if (defaultUser != null) {
            val username = defaultUser.username ?: os.defaultCredentials.username
            if (defaultUser.newUsername != null) +UsernamePatch(os, username, defaultUser.newUsername)
            if (defaultUser.newPassword != null) +PasswordPatch(os, defaultUser.newUsername ?: username, defaultUser.newPassword)
        }

        if (ssh.enabled) +SshEnablementPatch(os)
        if (ssh.authorizedKeys.isNotEmpty()) {
            val sshKeyUser = defaultUser?.newUsername ?: os.defaultCredentials.takeUnless { it == empty }?.username ?: "root"
            +SshAuthorizationPatch(os, sshKeyUser, ssh.authorizedKeys)
        }

        if (usbOtg.profiles.isNotEmpty()) {
            +UsbOnTheGoPatch(os, usbOtg.profiles)
        }

        setup.forEach {
            val x: String = it.map { ": ${it.name}\n" + it.build() }.joinToString("\n\n")
            val programs: List<Program> = os.compileSetupScript(it.name, x).toList()
            val patch = buildPatch(os, it.name) {
                booted {
                    programs.forEach {
                        run(it)
                    }
                }
            }
            +patch
        }
        // TODO run patches together
        // TODO optimize (model) scripts
        // TODO run bother-you
    }

    override fun toString(): String =
        "ImageCustomization(name='$name', os=$os, ssh=$ssh, defaultUser=$defaultUser, usbOtg=$usbOtg, setup=$setup, imgSize=$imgSize)"
}


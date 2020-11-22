package com.imgcstmzr

import com.bkahlert.koodies.concurrent.process.ShellScript
import com.bkahlert.koodies.test.junit.systemproperties.SystemProperties
import com.bkahlert.koodies.test.junit.systemproperties.SystemProperty
import com.bkahlert.koodies.unit.Giga
import com.bkahlert.koodies.unit.Size
import com.bkahlert.koodies.unit.Size.Companion.bytes
import com.bkahlert.koodies.unit.Size.Companion.toSize
import com.imgcstmzr.patch.ImgResizePatch
import com.imgcstmzr.patch.PasswordPatch
import com.imgcstmzr.patch.Patch
import com.imgcstmzr.patch.SshEnablementPatch
import com.imgcstmzr.patch.UsernamePatch
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.util.readAll
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.nio.file.Path

@Execution(CONCURRENT)
class ImgCstmzrConfigTest {

    @SystemProperties(
        SystemProperty("IMG_CSTMZR_USERNAME", "FiFi"),
        SystemProperty("IMG_CSTMZR_PASSWORD", "TRIXIbelle1234")
    )
    @Test
    fun `should deserialize`() {
        val config = ConfigFactory.systemProperties()
            .withFallback(ConfigFactory.parseResources("sample.conf"))
            .withFallback(ConfigFactory.parseString(Path.of(".env").readAll()))
            .resolve()
        val imgCstmztn = config.extract<ImageCustomization>("img-cstmztn")
        expectThat(imgCstmztn).compose("has equal properties") {
            get { name }.isEqualTo("Sample Project")
            get { os }.isEqualTo(OperatingSystems.RaspberryPiLite)
            get { imgSize }.isEqualTo(4.Giga.bytes)
            get { ssh.enabled }.isTrue()
            @Suppress("SpellCheckingInspection")
            get { ssh.authorizedKeys }.containsExactly("""ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDFqErnUaNo+UYn3dITTebC6mZMVtsUapSl4lGzG3HWKbqmYwfQUO7JBgwENmrNQMH7+F6YNvArnnezRk6iwBP8Z08cqsedq0zXJA16MaiegAWhcoEAidRqWXx8xml0gc80X3yzXP6PyP7/xbMIcIMGpX6rGnUSo8zaLTLOiEQ3LL1nXSPi1j+BAEOVMhIPe84V31HAvnBby37Ii4wfdBOEILBxBRAuYo7NM3C4gLZJyqqJZH1z1iocxXydxehlOApKDSIqZPCgCaMqw7WVJKTrg1SIcjAJqiepWq8oWPf3dMHf9Z+Yi+p/1DX4s/CUV6NOvBpE2f1+DZnf7r0o/CCtxkXwl1G7ur4DF0T+M+veNMBiqTGHBSKR+3j4HvM3Bs8AnDNxqS9ywxeeWuKHKJIbruZmYuEMZH0ubDSgViBbZ79nOZPN5SyqfRDD7Y4/X47JnLoMO7htDilDvn/KU3niEoRsV2wrtLhdO9kfM8m3/yJw27VuHRQxTF/iZN7qRKr+hW9O3OKmnwV5GXScqX0dyP2Eb12xmwyCdnyHbkdyNe0gqgDtpFRrFHoaonlM7iT1tuenX5ot5rkJbJIBHRevAhRcVXZG+jZwOvyoJMmUH2qmITchmJ+9SRTvdt3pyOFkQMwXXD7VA9ENbwhJIJbS5A5nWJ4/SLLesHzcw4exuQ== "Dr. Björn Kahlert, RSA key, 2020-08"""")
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

    class Ssh(val enabled: Boolean, authorizedKeys: List<String>) {
        val authorizedKeys = authorizedKeys.map { it.trim() }
    }

    data class DefaultUser(val username: String?, val newUsername: String?, val newPassword: String?)
    data class PasswordCstmztn(val username: String, val new: String)
    data class UsbOtgCstmztn(val profiles: List<String>)
    class SetupScript(val name: String, scripts: List<ShellScript>) : List<ShellScript> by scripts

    fun createPatch() {
        val patches = mutableListOf<Patch>().apply {
            if (imgSize != null) add(ImgResizePatch(imgSize))
            if (defaultUser != null) {
                val username = defaultUser.username ?: os.defaultCredentials.username
                if (defaultUser.newUsername != null) add(UsernamePatch(username, defaultUser.newUsername))
                if (defaultUser.newPassword != null) add(PasswordPatch(defaultUser.newUsername ?: username, defaultUser.newPassword))
            }

            if (ssh.enabled) add(SshEnablementPatch())
//            SshAuthorizationPatch(ssh.enabled), // TODO
        }
    }

    override fun toString(): String {
        return "ImageCustomization(name='$name', os=$os, ssh=$ssh, defaultUser=$defaultUser, usbOtg=$usbOtg, setup=$setup, imgSize=$imgSize)"
    }
}


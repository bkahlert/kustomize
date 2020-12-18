package com.imgcstmzr

import com.bkahlert.koodies.shell.ShellScript
import com.bkahlert.koodies.test.junit.ThirtyMinutesTimeout
import com.bkahlert.koodies.test.junit.systemproperties.SystemProperties
import com.bkahlert.koodies.test.junit.systemproperties.SystemProperty
import com.bkahlert.koodies.unit.Giga
import com.bkahlert.koodies.unit.Size.Companion.bytes
import com.imgcstmzr.patch.CompositePatch
import com.imgcstmzr.patch.ImgResizePatch
import com.imgcstmzr.patch.PasswordPatch
import com.imgcstmzr.patch.ShellScriptPatch
import com.imgcstmzr.patch.SshAuthorizationPatch
import com.imgcstmzr.patch.SshEnablementPatch
import com.imgcstmzr.patch.UsbOnTheGoPatch
import com.imgcstmzr.patch.UsernamePatch
import com.imgcstmzr.patch.booted
import com.imgcstmzr.patch.patch
import com.imgcstmzr.runtime.OperatingSystem.Credentials.Companion.withPassword
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.logging.InMemoryLogger
import com.typesafe.config.ConfigFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

@SystemProperties(
    SystemProperty("IMG_CSTMZR_USERNAME", "FiFi"),
    SystemProperty("IMG_CSTMZR_PASSWORD", "TRIXIbelle1234")
)
@Execution(CONCURRENT)
class ImgCstmzrConfigTest {

    private fun loadImgCstmztn(): ImgCstmzrConfig =
        ImgCstmzrConfig.load(ConfigFactory.parseResources("sample.conf"))

    @Test
    fun `should deserialize`() {
        val imgCstmztn = loadImgCstmztn()

        expectThat(imgCstmztn).compose("has equal properties") {
            get { trace }.isTrue()
            get { name }.isEqualTo("Sample Project")
            get { os }.isEqualTo(OperatingSystems.RaspberryPiLite)
            get { imgSize }.isEqualTo(4.Giga.bytes)
            get { ssh.enabled }.isTrue()
            @Suppress("SpellCheckingInspection")
            get { ssh.authorizedKeys }.contains(
                listOf("""ssh-rsa MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKbic/EEoiSu09lYR1y5001NA1K63M/Jd+IV1b2YpoXJxWDrkzQ/3v/SE84/cSayWAy4LVEXUodrt1WkPZ/NjE8CAwEAAQ== "John Doe 2020-12-10 btw, Corona sucks""""))
            get { defaultUser }.isEqualTo(ImgCstmzrConfig.DefaultUser(null, "FiFi", "TRIXIbelle1234"))
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
        val patch = imgCstmztn.toPatches()
        expectThat(CompositePatch(patch)) {
            get { name }.contains("Increasing Disk Space: 4.00 GB").contains("Change Username").contains("Mass storage and Serial")
            get { diskPreparations }.hasSize(1)
            get { diskCustomizations }.hasSize(13)
            get { diskOperations }.hasSize(1)
            get { fileOperations }.hasSize(5)
            get { osPreparations }.hasSize(2)
            get { osOperations }.hasSize(4)
        }
    }

    @Test
    fun `should optimize patches`() {
        val imgCstmztn = loadImgCstmztn()
        val patches = imgCstmztn.toOptimizedPatches()
        expectThat(patches) {
            hasSize(4)
            get { get(0) }.isA<CompositePatch>().get { this.patches.map { it::class } }
                .contains(ImgResizePatch::class, UsernamePatch::class, SshEnablementPatch::class, UsbOnTheGoPatch::class)
            get { get(1) }.isA<CompositePatch>().get { this.patches.map { it::class } }
                .contains(PasswordPatch::class, SshAuthorizationPatch::class)
            get { get(2) }.isA<ShellScriptPatch>()
            get { get(3) }.isA<ShellScriptPatch>()
        }
    }

    @ThirtyMinutesTimeout @E2E @Test
    fun InMemoryLogger.`should apply patches`(@OS(OperatingSystems.RaspberryPiLite) osImage: OperatingSystemImage) {
        val imgCstmztn = loadImgCstmztn()
        val patches = imgCstmztn.toOptimizedPatches()

        patches.forEach { patch ->
            patch(osImage, patch)
        }

        expect {
            that(osImage) {
                get { credentials }.isEqualTo("FiFi" withPassword "TRIXIbelle1234")
                booted(this@`should apply patches`) {
                    command("echo '👏 🤓 👋'");
                    { true }
                }
            }
            that(logged).contains("__〆(￣ー￣ )")
        }
    }
}

package com.imgcstmzr.patch//import static org.assertj.core.api.Assertions.assertThat;
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.CopyInOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.MkdirOption
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.LinuxRoot.etc.wpa_supplicant.wpa_supplicant_conf
import com.imgcstmzr.os.OperatingSystemImage
import koodies.io.path.hasContent
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.containsExactly

class WpaSupplicantPatchTest {

    @Test
    @Suppress("SpellCheckingInspection")
    fun `should provide wpasupplicant conf copying command`(osImage: OperatingSystemImage) {

        val patch = WpaSupplicantPatch("entry1\nentry2").invoke(osImage)

        expect {
            that(patch).diskCustomizations {
                containsExactly(
                    MkdirOption(LinuxRoot.etc.wpa_supplicant),
                    CopyInOption(
                        osImage.hostPath(wpa_supplicant_conf),
                        LinuxRoot.etc / "wpa_supplicant"
                    ))
            }
            that(osImage.hostPath(wpa_supplicant_conf)) {
                hasContent("entry1\nentry2\n")
            }
        }
    }
}

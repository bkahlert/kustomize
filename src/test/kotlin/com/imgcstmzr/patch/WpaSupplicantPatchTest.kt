package com.imgcstmzr.patch//import static org.assertj.core.api.Assertions.assertThat;
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.CopyInOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.MkdirOption
import com.imgcstmzr.os.DiskPath
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.patch.WpaSupplicantPatch.Companion.WPA_SUPPLICANT
import koodies.io.path.hasContent
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.containsExactly

class WpaSupplicantPatchTest {

    @Test
    fun `should provide wpasupplicant conf copying command`(osImage: OperatingSystemImage) {
        val patch = WpaSupplicantPatch("entry1\nentry2")

        expect {
            that(patch).customizations(osImage) {
                containsExactly(
                    MkdirOption(DiskPath("/etc/wpa_supplicant")),
                    CopyInOption(
                        osImage.hostPath(WPA_SUPPLICANT),
                        DiskPath("/etc/wpa_supplicant")
                    ))
            }
            that(osImage.hostPath(WPA_SUPPLICANT)) {
                hasContent("entry1\nentry2\n")
            }
        }
    }
}

package com.imgcstmzr.patch//import static org.assertj.core.api.Assertions.assertThat;
import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.libguestfs.Libguestfs.Companion.hostPath
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.CopyInOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.MkdirOption
import com.imgcstmzr.patch.WpaSupplicantPatch.Companion.WPA_SUPPLICANT
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.test.hasContent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.assertions.containsExactly

@Execution(CONCURRENT)
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

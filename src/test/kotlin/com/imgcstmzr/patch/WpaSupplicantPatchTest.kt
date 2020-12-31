package com.imgcstmzr.patch//import static org.assertj.core.api.Assertions.assertThat;
import com.imgcstmzr.libguestfs.resolveOnHost
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.test.hasContent
import koodies.io.path.toPath
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class WpaSupplicantPatchTest {

    @Test
    fun `should provide wpasupplicant conf copying command`(osImage: OperatingSystemImage) {
        val patch = WpaSupplicantPatch("entry1\nentry2")

        val expected = VirtCustomizeCustomizationOption.CopyInOption(
            "/shared/etc/wpa_supplicant/wpa_supplicant.conf".toPath(),
            "/etc/wpa_supplicant".toPath()
        )
        expect {
            that(patch).matches(customizationOptionsAssertion = {
                get { first().invoke(osImage) }.isEqualTo(expected)
            })
            that(osImage.resolveOnHost("/etc/wpa_supplicant/wpa_supplicant.conf")) {
                hasContent("entry1\nentry2\n")
            }
        }
    }
}

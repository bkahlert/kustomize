package com.imgcstmzr.patch//import static org.assertj.core.api.Assertions.assertThat;
import com.imgcstmzr.libguestfs.resolveOnHost
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.CopyInOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.FirstBootCommandOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.FirstBootInstallOption
import com.imgcstmzr.runtime.OperatingSystemImage
import koodies.io.path.toPath
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.assertions.isEqualTo
import kotlin.io.path.readText

@Execution(CONCURRENT)
class SambaPatchTest {

    @Test
    fun `should provide samba conf copying command`(osImage: OperatingSystemImage) {
        val patch = SambaPatch("the-user", "the-password", true, RootShare.`read-write`)

        val expected = listOf(
            FirstBootInstallOption("samba"),
            CopyInOption("/shared/etc/samba/smb.conf".toPath(), "/etc/samba".toPath()),
            FirstBootCommandOption("""echo -ne "the-password\nthe-password\n" | smbpasswd -a -s "the-user""""))
        expect {
            that(patch).matches(customizationOptionsAssertion = {
                get { get(0).invoke(osImage) }.isEqualTo(expected[0])
                get { get(1).invoke(osImage) }.isEqualTo(expected[1])
                get { get(2).invoke(osImage) }.isEqualTo(expected[2])
            })
            that(osImage.resolveOnHost("/etc/samba/smb.conf").readText()).isEqualTo("""
                    [home]
                    path = /home/the-user
                    writeable=Yes
                    create mask=0744
                    directory mask=0744
                    public=no

                    [/]
                    path = /
                    writeable=Yes
                    create mask=0740
                    directory mask=0740
                    public=no
                    
                    
                """.trimIndent())
        }
    }
}

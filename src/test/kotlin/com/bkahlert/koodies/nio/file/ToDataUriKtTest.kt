package com.bkahlert.koodies.nio.file

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class ToDataUriKtTest {

    @Test
    fun `should create data URI`() {
        @Suppress("SpellCheckingInspection")
        val logo =
            """
data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFQAAAARCAMAAAB0IHssAAABYlBMVEVMaXEmq+J4TIfCHnMmq+LCHnMmq+Imq+LCHnMmq+LCHn
Mmq+Imq+Imq+Imq+J2U40mq+Imq+LCHnMmq+LCHnPCHnPCHnPCHnMmq+KNbp04otcmq+Jna5/CHnPCHnPCHnNaiLtqN3ySMHl8b6GSc6HCHnPCHnNilccmq
+JImc1HntKpLnhfhri5LHefToeqSoRtap1yaZ1NndCFeadikMJ8M3pfeqywPX52fKzCHnOEYJWVTIZ3NXtxT4mOMHlqcaN6U4yqKHaRPX9Sir5gf7KCXZKDS
YSxR4JWlcijQoCIWY9vf69lWZGhO32cR4OmOHxrdqhojb6sM3p8iLabVotwY5hlL3gmq+LCHnNYb6SUKXarJHRKjcFiSodkOX1ClcmfJ3W8H3NjQYFyLneDL
Hd+LHdgUox3LXdWd6uaKHVbaJ6JK3axI3SPKnY4nNFdYZkypNlfWpJrL3hPhblUfbKlJXWlLrboAAAAVnRSTlMAMO/AwIDwQECAMBAgsHDr0KAg4NDwoFCQc
MRQ6OCwcND++LA8YJBMYMiI6MSXoHTg2HlkgvvikJQQwMT88/jc5/To6NTP6EyeuMig+tS+0M50zC6U5O6Ej+cAAAAJcEhZcwAAAWIAAAFiAV8n0FMAAAKfS
URBVDiNrdNnU9tAEAbgtXxyb2AwxkCA0Anpvffe27sryb1jesn/z9xZ4CHwicl+0Eg63TOrfeforIqJjNK0yIUzV89ZEREhWyT4P9H7tj1NMdseOy8wC6yL1
B7tbCeJSMTrMicXRkRkdmc7RWTLOtCRzboTSsREZCTAzBUAFU7pW1NDcQrxcQUIgCuyAZRvGXTDYW7XRESA8pxGXQDiAU5Yj0WuMHMRQJGj4WMnQwOTwxq9a
D+sA01bo7V9h3uoioxM6ZVhg/acxedAuY96FeaXrVZrRgU0molGM8w8Meg0lNDog0CI642azlqqcHaBztQwURdwcwZlpnEAS320xPxWRCJEYdMWxbPMUSLS0
8ibmQKYzPAhNsV02oRzB8BVIuIStkbJli2NLgG42UfhcOQESqqP6hFEj9Cys4eOjA4TUQ1w7jaBG/oTB2gVrrWg0fcA/N8HKmuu634MGHSib59CTXkx/VQFJ
nNVIOujKOpLqa2pukFzl4CSWYkaLaTMNANnoo1RrXaA64t/AP3RADXplysGjYz7G4opGqQ/RP+ie0PvDsqoXiCaB+BoppIn4h6a+mA1USqZSfbRcA9wPc8bp
0GnWTqFTsbjIQcNIboHoHug5SzRbh3uiJ9+0EP9COUSvEFQE7Tsx3US/UC0WgLWY5HHAGbU7QbgjEfagDvto2MecJjy0Up5/UT6IWZ1CnWDU9IAPtU2AVSHa
azWADYEwIv+iWImHY85SGth5q6ZKed9NMrMiVOoiFRRrrhbAC4T0cITYF+A9uox+kszGp3RUtugykcTR0kdo88s62swGMx9K6gvn630nHk5/zT9Jvj6lQoQj
QV//FZ5+pm2rJUVy7K+J5RShbSVLqjleFIl40QUVSqltyWVChAR/QWZSglyrdNAqgAAAABJRU5ErkJggg==
""".trimIndent().lines().joinToString("")

        expectThat(classPath("BKAHLERT.png") { toDataUri() }).isEqualTo(logo)
    }
}

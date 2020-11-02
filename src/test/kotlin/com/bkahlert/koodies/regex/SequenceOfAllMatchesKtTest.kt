package com.bkahlert.koodies.regex

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.containsExactly

@Execution(CONCURRENT)
class SequenceOfAllMatchesKtTest {
    @Test
    fun `should find all matches`() {
        @Suppress("SpellCheckingInspection")
        expectThat(RegularExpressions.urlRegex.sequenceOfAllMatches(htmlLinkList).toList()).containsExactly(
            "https://textfancy.com/font-converter/",
            "https://textfancy.com",
            "http://qaz.wtf/u/convert.cgi?text=CUSTOM+FONTS",
            "https://qaz.wtf",
            "https://smalltext.io",
            "https://smalltext.io",
            "https://eng.getwisdom.io/awesome-unicode/",
            "https://eng.getwisdom.io",
            "https://codepoints.net/U+0085?lang=de",
            "https://codepoints.net",
            "https://www.compart.com/en/unicode/U+3164",
            "https://compart.com",
            "https://github.com/Wisdom/Awesome-Unicode",
            "https://github.com",
        )
    }

    companion object {
        @Suppress("SpellCheckingInspection", "LongLine")
        val htmlLinkList: String = """
            <ul class="bookmark-widget__list"><li class="bookmark-item bookmark-item_mode_list bookmark-item_size_small bookmark-item_description_first bookmark-item_title_show"><!----> <a href="https://textfancy.com/font-converter/" title="𝕿𝖊𝖝𝖙 🎀ԲΔ₪ς¥🎀
▀█▀▒██▀░▀▄▀░▀█▀ ᕕ(ಠ‿ಠ)ᕗ
░█▒░█▄▄░█▒█░▒█  🎀ԲΔ₪ς¥🎀" class="bookmark-item__link"><div class="bookmark-item__icon-wrapper"><img loading="lazy" src="https://textfancy.com" class="bookmark-item__icon" data-imu-valid="true" data-imu-supported="false"> <!----></div> <div class="bookmark-item__info"><span class="bookmark-item__title-container"><span class="bookmark-item__title">
          𝕿𝖊𝖝𝖙 🎀ԲΔ₪ς¥🎀
        </span> <!----></span> <!----></div></a> <!----></li><li class="bookmark-item bookmark-item_mode_list bookmark-item_size_small bookmark-item_description_first bookmark-item_title_show"><!----> <a href="http://qaz.wtf/u/convert.cgi?text=CUSTOM+FONTS" title="𝚄𝚗𝚒𝚌𝚘𝚍𝚎 𝕿𝖊𝖝𝖙 𝓒𝓸𝓷𝓿𝓮𝓻𝓽𝓮𝓻" class="bookmark-item__link"><div class="bookmark-item__icon-wrapper"><img loading="lazy" src="https://qaz.wtf" class="bookmark-item__icon" data-imu-valid="true" data-imu-supported="false"> <!----></div> <div class="bookmark-item__info"><span class="bookmark-item__title-container"><span class="bookmark-item__title">
          𝚄𝚗𝚒𝚌𝚘𝚍𝚎 𝕿𝖊𝖝𝖙 𝓒𝓸𝓷𝓿𝓮𝓻𝓽𝓮𝓻
        </span> <!----></span> <!----></div></a> <!----></li><li class="bookmark-item bookmark-item_mode_list bookmark-item_size_small bookmark-item_description_first bookmark-item_title_show"><!----> <a href="https://smalltext.io" title="sᴍᴀʟʟ ᴛᴇxᴛ ᵍᵉⁿᵉʳᵃᵗᵒʳ" class="bookmark-item__link"><div class="bookmark-item__icon-wrapper"><img loading="lazy" src="https://smalltext.io" class="bookmark-item__icon" data-imu-valid="true" data-imu-supported="false"> <!----></div> <div class="bookmark-item__info"><span class="bookmark-item__title-container"><span class="bookmark-item__title">
          sᴍᴀʟʟ ᴛᴇxᴛ ᵍᵉⁿᵉʳᵃᵗᵒʳ
        </span> <!----></span> <!----></div></a> <!----></li><li class="bookmark-item bookmark-item_mode_list bookmark-item_size_small bookmark-item_description_first bookmark-item_title_show"><!----> <a href="https://eng.getwisdom.io/awesome-unicode/" title="Greek question mark ; and code compatible -ㅤ- space
A curated list of delightful Unicode tidbits, packages and resources.  Foreword Unicode is Awesome! Prior to Unicode, international communication was grueling- everyone had defined their separate e..." class="bookmark-item__link"><div class="bookmark-item__icon-wrapper"><img loading="lazy" src="https://eng.getwisdom.io"  class="bookmark-item__icon" data-imu-valid="true" data-imu-supported="false"> <!----></div> <div class="bookmark-item__info"><span class="bookmark-item__title-container"><span class="bookmark-item__title">
          Greek question mark ; and code compatible -ㅤ- space
        </span> <!----></span> <!----></div></a> <!----></li><li class="bookmark-item bookmark-item_mode_list bookmark-item_size_small bookmark-item_description_first bookmark-item_title_show"><!----> <a href="https://codepoints.net/U+0085?lang=de" title="U+0085 NEXT LINE (NEL)* – Codepoints
�, Codepunkt U+0085 NEXT LINE (NEL)* in Unicode, liegt im Block „Latin-1 Supplement“. Es gehört zur Allgemein-Schrift und ist ein Kontrollzeichen." class="bookmark-item__link"><div class="bookmark-item__icon-wrapper"><img loading="lazy" src="https://codepoints.net" class="bookmark-item__icon" data-imu-valid="true" data-imu-supported="false"> <!----></div> <div class="bookmark-item__info"><span class="bookmark-item__title-container"><span class="bookmark-item__title">
          U+0085 NEXT LINE (NEL)* – Codepoints
        </span> <!----></span> <!----></div></a> <!----></li><li class="bookmark-item bookmark-item_mode_list bookmark-item_size_small bookmark-item_description_first bookmark-item_title_show"><!----> <a href="https://www.compart.com/en/unicode/U+3164" title="“ㅤ” non-whitespace whitespace
U+3164 is the unicode hex value of the character Hangul Filler. Char U+3164, Encodings, HTML Entitys:ㅤ,ㅤ, UTF-8 (hex), UTF-16 (hex), UTF-32 (hex)" class="bookmark-item__link"><div class="bookmark-item__icon-wrapper"><img loading="lazy" src="https://compart.com" class="bookmark-item__icon" data-imu-valid="true" data-imu-supported="false"> <!----></div> <div class="bookmark-item__info"><span class="bookmark-item__title-container"><span class="bookmark-item__title">
          “ㅤ” non-whitespace whitespace
        </span> <!----></span> <!----></div></a> <!----></li><li class="bookmark-item bookmark-item_mode_list bookmark-item_size_small bookmark-item_description_first bookmark-item_title_show"><!----> <a href="https://github.com/Wisdom/Awesome-Unicode" title="Wisdom/Awesome-Unicode: A curated list of delightful Unicode tidbits, packages and resources.
:joy: :ok_hand: A curated list of delightful Unicode tidbits, packages and resources. - Wisdom/Awesome-Unicode" class="bookmark-item__link"><div class="bookmark-item__icon-wrapper"><img loading="lazy" src="https://github.com" class="bookmark-item__icon" data-imu-valid="true" data-imu-supported="false"> <!----></div> <div class="bookmark-item__info"><span class="bookmark-item__title-container"><span class="bookmark-item__title">
          Wisdom/Awesome-Unicode: A curated list of delightful Unicode tidbits, packages and resources.
        </span> <!----></span> <!----></div></a> <!----></li></ul>
        """.trimIndent()
    }
}

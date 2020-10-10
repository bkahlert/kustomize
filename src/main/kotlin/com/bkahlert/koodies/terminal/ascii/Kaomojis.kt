@file:Suppress("SpellCheckingInspection")

package com.bkahlert.koodies.terminal.ascii

import com.bkahlert.koodies.terminal.ANSI.EscapeSequences.termColors
import com.bkahlert.koodies.terminal.colorize
import kotlin.properties.PropertyDelegateProvider
import kotlin.reflect.KProperty

object Kaomojis {
    enum class Generator(
        val leftArm: List<String>,
        val rightArm: List<String>,
        val leftEye: List<String>,
        val rightEye: List<String>,
        val mouth: List<String>,
    ) {
        INDIFFERENCE(
            leftArm = listOf("ヽ", "┐", "╮", "ᕕ", "¯\\_"),
            rightArm = listOf("ノ", "┌", "╭", "ᕗ", "_/¯"),
            leftEye = listOf("ー", " ´ ", "︶", "￣", "´", " ˘ ", "‘"),
            rightEye = listOf("ー", " ` ", "︶", "￣", "´", " ˘ ", "` "),
            mouth = listOf("_", "ヘ", "～", "д", "▽", "ヮ", "ー", "︿", "､")),
        JOY(
            leftArm = listOf("╰", "＼", "٩", "<"),
            rightArm = listOf("ﾉ", "ノ", "o", "／"),
            leftEye = listOf("▔", "^", "¯", "☆"),
            rightEye = listOf("▔", "^", "¯", "☆"),
            mouth = listOf("▽", "ω", "ヮ", "∀")),
        LOVE(
            leftArm = listOf("", "♡╰", "ヽ", "♡＼", "٩", "❤ "),
            rightArm = listOf("", "ノ", "♡", "╯♡", " ♡", " ❤", "/ ♡", "ノ～ ♡", "۶"),
            leftEye = listOf("─", "´ ", "• ", "*", "˘", "μ", "￣", " ◡", "°", "♡", "◕", "˙", "❤", "´• ", "≧"),
            rightEye = listOf("─", " `", "• ", "*", "˘", "μ", "￣", " ◡", "°", "♡", "◕", "˙", "❤", " •`", "≦"),
            mouth = listOf("з", "_", "‿‿", "ω", "︶", "◡", "▽", "ε", "∀", "ᵕ", "‿", "³")),
        SADNESS(
            leftArm = listOf("", "o", ".･ﾟﾟ･", "。゜゜", "｡･ﾟﾟ*", "｡･ﾟ", ".｡･ﾟﾟ･", "｡ﾟ", "･ﾟ･", "｡ﾟ･ "),
            rightArm = listOf("", "o", "･ﾟﾟ･.", " ゜゜。", "*ﾟﾟ･｡", "･｡", "･ﾟﾟ･｡.", "･ﾟ･", "･ﾟ｡"),
            leftEye = listOf("μ", "T", "╥", "〒", "-", " ; ", "个", "╯", "ಥ", ">", "｡•́", "╯"),
            rightEye = listOf("μ", "T", "╥", "〒", "-", " ; ", "个", "╯", "ಥ", "<。", "•̀｡", "<、"),
            mouth = listOf("_", "ヘ", "ω", "﹏", "Д", "︿", "-ω-", "︵", "╭╮", "Ｏ", "><")),
        ;

        fun random(
            fixedLeftArm: String = leftArm.random(),
            fixedLeftEye: String = leftEye.random(),
            fixedMouth: String = mouth.random(),
            fixedRightEye: String = rightEye.random(),
            fixedRightArm: String = rightArm.random(),
        ): String = "$fixedLeftArm$fixedLeftEye$fixedMouth$fixedRightEye$fixedRightArm"

        companion object {
            val leftArms: List<String> = values().flatMap { it.leftArm }
            val rightArms: List<String> = values().flatMap { it.rightArm }
            val leftEyes: List<String> = values().flatMap { it.leftEye }
            val rightEyes: List<String> = values().flatMap { it.rightEye }
            val mouths: List<String> = values().flatMap { it.mouth }
        }
    }

    fun random(): String = with(Generator) { listOf(leftArms, leftEyes, mouths, rightEyes, rightArms).joinToString("") { it.random() } }

    data class Kaomoji(
        private val template: String,
        private val leftArm: IntRange? = null,
        private val rightArm: IntRange? = null,
        private val leftEye: IntRange? = null,
        private val rightEye: IntRange? = null,
        private val mouth: IntRange? = null,
        private val wand: IntRange? = null,
    ) {
        fun random(): String {
            val listOfNotNull: List<IntRange> = listOfNotNull(leftArm, leftEye, mouth, rightEye, rightArm)
            return listOfNotNull.foldRight(template) { intRange, acc ->
                acc.substring(0 until intRange.first) + Generator.mouths.random() + acc.subSequence(intRange.last, acc.lastIndex)
            }
        }

        fun withMagic(): String {
            val listOfNotNull: List<IntRange> = listOfNotNull(wand)
            return listOfNotNull.fold(template) { acc, intRange ->
                acc.substring(0 until intRange.first) + termColors.colorize(acc.substring(intRange)) + acc.subSequence(intRange.last, acc.lastIndex + 1)
            }
        }

        operator fun getValue(kaomojis: Kaomojis, property: KProperty<*>): Kaomoji = this
        override fun toString(): String = copy(template = withMagic()).random()
    }

    @Suppress("KDocMissingDocumentation", "ObjectPropertyName", "NonAsciiCharacters")
    val `(＃￣_￣)o︠・━・・━・━━・━☆`: Kaomoji by five(mouth = 3..4, rightArm = 6..7, wand = 8..16)

    fun five(
        leftArm: IntRange? = null, rightArm: IntRange? = null, leftEye: IntRange? = null, rightEye: IntRange? = null, mouth: IntRange? = null,
        wand: IntRange? = null,
    ): PropertyDelegateProvider<Kaomojis, Kaomoji> =
        PropertyDelegateProvider<Kaomojis, Kaomoji> { thisRef, property -> Kaomoji(property.name, leftArm, leftEye, rightEye, rightArm, mouth, wand) }

    val Wizards: List<Any> = listOf(
        "(ﾉ>ω<)ﾉ :｡･:*:･ﾟ’★,｡･:*:･ﾟ’☆",
        `(＃￣_￣)o︠・━・・━・━━・━☆`,
        "(/￣‿￣)/~~☆’.･.･:★’.･.･:☆",
        "(∩ᄑ_ᄑ)⊃━☆ﾟ*･｡*･:≡( ε:)",
        "(ノ ˘_˘)ノ ζζζ  ζζζ  ζζζ",
        "(ノ°∀°)ノ⌒･*:.｡. .｡.:*･゜ﾟ･*☆",
        "(⊃｡•́‿•̀｡)⊃━✿✿✿✿✿✿",
    )

    val writing: List<String> = listOf(
        "( ￣ー￣)φ__",
    )

    val cheerLeaders: List<String> = listOf(
        "✺◟( • ω • )◞✺",
    )

    val salutators: List<String> = listOf(
        "(￣^￣)ゞ",
    )

    val tableTakers: List<String> = listOf(
        "(╮°-°)╮┳━━┳ ",
    )

    val tableThrowers: List<String> = listOf(
        "( ╯°□°)╯ ┻━━┻",
        "╯‵Д′)╯彡┻━┻ ",
        "(ノಠ益ಠ)ノ彡┻━┻ ",
    )

    val smokers: List<String> = listOf(
        "౦０o ｡ (‾́。‾́ )y~~",
    )

    val rain: List<String> = listOf(
        "｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ",
        "｀ヽ｀((((( ( ⊃・ω・)⊃☂｀(´ω｀u)))ヽ｀、",
    )

    val tv: List<String> = listOf(
        "【 TV 】      -o(.￣ )",
    )

    val fish: List<String> = listOf(
        "ϵ( 'Θ' )϶",
        "∋(°O°)∈",
        "(〠_〠)",
    )

    val weapons: List<String> = listOf(
        "̿ ̿̿'̿'\\̵͇̿̿\\=(•̪●)=/̵͇̿̿/'̿̿ ̿ ̿ ̿",
    )

    val babies: List<String> = listOf(
        "ლ(´ڡ`ლ)",
        "ლ(́◉◞౪◟◉‵ლ)",
        "(●´ω｀●)",
    )

    val money: List<String> = listOf(
        "[̲̅\$̲̅(̲̅5̲̅)̲̅\$̲̅]",
    )

    val screaming: List<String> = listOf(
        "ヽ(๏∀๏ )ﾉ",
        "ヽ(｀Д´)ﾉ",
        "ヽ(ｏ`皿′ｏ)ﾉ",
        "ヽ(`Д´)ﾉ",
    )
    val why: List<String> = listOf(
        "щ（ﾟДﾟщ",
        "щ(ಠ益ಠщ)",
        "щ(ಥДಥщ)",
    )
    val goofy: List<String> = listOf(
        "(ό‿ὸ)ﾉ",
    )

    val geeks: List<String> = listOf(
        "◖⎚∠⎚◗",
        "[⸌º~°]⸌",
        "◘-◘",
        "[¬⫏-⫐]¬",
        "ㄖꏁㄖ",
        "╰(⊡-⊡)و✎⮹",
        "(⌐■_■)┐",
        "(.づ▣ ͜ʖ▣)づ.",
        "◙‿◙",
        "◪_◪",
        "☐_☐",
        "( •_•)>⌐■-■",
        "<【☯】‿【☯】>",
    )

    val pointing: List<String> = listOf(
        "☜ق❂Ⴢ❂ق☞",
        "☜(⌒▽⌒)☞",
        "☜(ﾟヮﾟ☜)",
        "☜-(ΘLΘ)-☞",
    )

    val dance: List<String> = listOf(
        "┏(‘▀_▀’)ノ♬♪",
        "ヾ(*´ ∇ `)ﾉ",
        "ヽ(⌐■_■)ノ♪♬"
    )

    val chasing: List<String> = listOf(
        "(○｀д´)ﾉｼ Σ(っﾟДﾟ)っ",
        "☎Σ⊂⊂(☉ω☉∩)",

        )

    val celebrities: List<String> = listOf(
        "⪿ ↂ ˒̫̮ ↂ ⫀", // Elton John
    )

    val excitement: List<String> = listOf(
        "ヽ( ★ω★)ノ",
    )

    val heroes: List<String> = listOf(
        "─=≡Σ(([ ⊐•̀⌂•́]⊐",
    )

    object Dogs {
        fun random(): String {
            val type = listOf(uTypeDogs, აTypeDogs, otherDogs)[(Math.random() * 3).toInt()]
            return type[(Math.random() * type.size).toInt()]
        }

        val uTypeDogs: List<String> = listOf(
            "▼・ᴥ・▼",
            "▼(´ᴥ`)▼",
            "U ´ᴥ` U",
            "U・ᴥ・U",
            "U・ﻌ・U",
            "U ´x` U",
            "(U・x・U)",
            "υ´• ﻌ •`υ",
        )

        val `აTypeDogs`: List<String> = listOf(
            "૮ ・ﻌ・ა",
            "໒・ﻌ・७",
            "૮ ºﻌºა",
            "૮ ･ ﻌ･ა",
            "૮ ♡ﻌ♡ა",
            "૮ ˙ ﻌ˙ ა",
            "૮ – ﻌ–ა",
            "૮ ˘ﻌ˘ ა",
            "૮ ˆﻌˆ ა",
            "૮ ’ﻌ｀ა",
            "૮ ˶′ﻌ ‵˶ ა",
            "૮ ´• ﻌ ´• ა",
            "૮ ⚆ﻌ⚆ა",
            "૮ ᴖﻌᴖა",
        )

        val otherDogs: List<String> = listOf(
            "(❍ᴥ❍ʋ)",
            "( ͡° ᴥ ͡° ʋ)",
            "V●ω●V",
            "V✪ω✪V",
            "V✪⋏✪V",
            "∪ ̿–⋏ ̿–∪",
            "∪･ω･∪",
            "໒( ●ܫฺ ●)ʋ",
            "໒( = ᴥ =)ʋ",
            "໒( ̿･ ᴥ ̿･ )ʋ",
            "໒( ̿❍ ᴥ ̿❍)ʋ",
            "▽･ｪ･▽ﾉ”",
            "ଘ(∪・ﻌ・∪)ଓ",
            "∪◕ฺᴥ◕ฺ∪",
            "໒(＾ᴥ＾)७",
            "ฅU=ﻌ =Uฅ",
            "ᐡ ・ ﻌ ・ ᐡ",
            "ᐡ ᐧ ﻌ ᐧ ᐡ",
        )
    }
}

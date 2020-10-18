@file:Suppress("SpellCheckingInspection", "ObjectPropertyName", "HardCodedStringLiteral")

package com.bkahlert.koodies.terminal.ascii

import com.bkahlert.koodies.terminal.ANSI.EscapeSequences.termColors
import com.bkahlert.koodies.terminal.colorize
import kotlin.properties.PropertyDelegateProvider
import kotlin.reflect.KProperty

object Kaomojis {
    @Suppress("unused")
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
    ) : CharSequence by toString() {
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
        PropertyDelegateProvider { thisRef, property -> Kaomoji(property.name, leftArm, leftEye, rightEye, rightArm, mouth, wand) }

    @Suppress("unused")
    val Wizards: List<CharSequence> = listOf(
        "(ﾉ>ω<)ﾉ :｡･:*:･ﾟ’★,｡･:*:･ﾟ’☆",
        `(＃￣_￣)o︠・━・・━・━━・━☆`,
        "(/￣‿￣)/~~☆’.･.･:★’.･.･:☆",
        "(∩ᄑ_ᄑ)⊃━☆ﾟ*･｡*･:≡( ε:)",
        "(ノ ˘_˘)ノ ζζζ  ζζζ  ζζζ",
        "(ノ°∀°)ノ⌒･*:.｡. .｡.:*･゜ﾟ･*☆",
        "(⊃｡•́‿•̀｡)⊃━✿✿✿✿✿✿",
        "ଘ(੭ˊᵕˋ)੭* ੈ✩‧₊˚",
    )

    @Suppress("unused")
    val Proud: List<CharSequence> = listOf(
        "（咒）",
        "(⌤⌗)",
        "(￣^￣)",
        "(-、-)",
        "(꒵꜅꒵)",
        "(｀へ′)",
        "（’へ’）",
        "╭⚈¬⚈╮",
        "(￣ω￣)",
        "(｀ڼ´)",
        "⚈ ̫ ⚈",
        "（￣へ￣）",
        "（￣＾￣）",
        "(´꒳`)",
        "(꒡ꜙ꒡)",
        "(^～^)",
        "(´Å｀)",
        "§ԾᴗԾ§",
        "（￣ー￣）",
        "(｀^´)",
        "（・―・）",
        "(￣‥￣)",
        "(￣ー￣)",
        "（｀ー´）",
        "(￣⊿￣)",
        "(-∀-)",
        "( ´ｰ`)",
        "（；￣︶￣）",
        "(￣︶￣;)",
        "( ⊙‿⊙)",
    )

    @Suppress("unused")
    object Named {
        /**
         * ```kaomoji
         * (* ^ ω ^)
         * ```
         */
        const val joy: String = """(* ^ ω ^)"""

        /**
         * ```kaomoji
         * (ﾉ´ з `)ノ
         * ```
         */
        const val love: String = """(ﾉ´ з `)ノ"""

        /**
         * ```kaomoji
         * (⌒_⌒;)
         * ```
         */
        const val confusion: String = """(⌒_⌒;)"""

        /**
         * ```kaomoji
         * (ノ_<。)ヾ(´ ▽ ` )
         * ```
         */
        const val sympathy: String = """(ノ_<。)ヾ(´ ▽ ` )"""

        /**
         * ```kaomoji
         * (＃＞＜)
         * ```
         */
        const val discontent: String = """(＃＞＜)"""

        /**
         * ```kaomoji
         * (＃`Д´)
         * ```
         */
        const val anger: String = """(＃`Д´)"""

        /**
         * ```kaomoji
         * (ノ_<。)
         * ```
         */
        const val sadness: String = """(ノ_<。)"""

        /**
         * ```kaomoji
         * ~(>_<~)
         * ```
         */
        const val pain: String = """~(>_<~)"""

        /**
         * ```kaomoji
         * (ノωヽ)
         * ```
         */
        const val fear: String = """(ノωヽ)"""

        /**
         * ```kaomoji
         * ヽ(ー_ー )ノ
         * ```
         */
        const val apathy: String = """ヽ(ー_ー )ノ"""

        /**
         * ```kaomoji
         * (￣ω￣;)
         * ```
         */
        const val embarrassment: String = """(￣ω￣;)"""

        /**
         * ```kaomoji
         * (￢_￢)
         * ```
         */
        const val doubt: String = """(￢_￢)"""

        /**
         * ```kaomoji
         * w(°ｏ°)w
         * ```
         */
        const val surprise: String = """w(°ｏ°)w"""

        /**
         * ```kaomoji
         * (*・ω・)ﾉ
         * ```
         */
        const val greeting: String = """(*・ω・)ﾉ"""

        /**
         * ```kaomoji
         * (づ￣ ³￣)づ
         * ```
         */
        const val hugs: String = """(づ￣ ³￣)づ"""

        /**
         * ```kaomoji
         * (^_~)
         * ```
         */
        const val wink: String = """(^_~)"""

        /**
         * ```kaomoji
         * (シ_ _)シ
         * ```
         */
        const val sorry: String = """(シ_ _)シ"""

        /**
         * ```kaomoji
         * (*￣ii￣)
         * ```
         */
        const val `blood from the nose`: String = """(*￣ii￣)"""

        /**
         * ```kaomoji
         * |･ω･)
         * ```
         */
        const val `hide and seek`: String = """|･ω･)"""

        /**
         * ```kaomoji
         * __φ(．．)
         * ```
         */
        const val letter: String = """__φ(．．)"""

        /**
         * ```kaomoji
         * ☆ﾐ(o*･ω･)ﾉ
         * ```
         */
        const val run: String = """☆ﾐ(o*･ω･)ﾉ"""

        /**
         * ```kaomoji
         * [(－－)]..zzZ
         * ```
         */
        const val sleap: String = """[(－－)]..zzZ"""

        /**
         * ```kaomoji
         * (=^･ω･^=)
         * ```
         */
        const val cat: String = """(=^･ω･^=)"""

        /**
         * ```kaomoji
         * ( ´(ｴ)ˋ )
         * ```
         */
        const val bear: String = """( ´(ｴ)ˋ )"""

        /**
         * ```kaomoji
         * ∪＾ェ＾∪
         * ```
         */
        const val dog: String = """∪＾ェ＾∪"""

        /**
         * ```kaomoji
         * ／(≧ x ≦)＼
         * ```
         */
        const val rabbit: String = """／(≧ x ≦)＼"""

        /**
         * ```kaomoji
         * ( ´(00)ˋ )
         * ```
         */
        const val pig: String = """( ´(00)ˋ )"""

        /**
         * ```kaomoji
         * (￣Θ￣)
         * ```
         */
        const val bird: String = """(￣Θ￣)"""

        /**
         * ```kaomoji
         * (°)#))<<
         * ```
         */
        const val fish: String = """(°)#))<<"""

        /**
         * ```kaomoji
         * /╲/\╭(ఠఠ益ఠఠ)╮/\╱\
         * ```
         */
        const val spider: String = """/╲/\╭(ఠఠ益ఠఠ)╮/\╱\"""

        /**
         * ```kaomoji
         * ヾ(・ω・)メ(・ω・)ノ
         * ```
         */
        const val friends: String = """ヾ(・ω・)メ(・ω・)ノ"""

        /**
         * ```kaomoji
         * ヽ( ･∀･)ﾉ_θ彡☆Σ(ノ `Д´)ノ
         * ```
         */
        const val enemies: String = """ヽ( ･∀･)ﾉ_θ彡☆Σ(ノ `Д´)ノ"""

        /**
         * ```kaomoji
         * ( ・∀・)・・・--------☆
         * ```
         */
        const val weapon: String = """( ・∀・)・・・--------☆"""

        /**
         * ```kaomoji
         * (ノ ˘_˘)ノ　ζ|||ζ　ζ|||ζ　ζ|||ζ
         * ```
         */
        const val magic: String = """(ノ ˘_˘)ノ　ζ|||ζ　ζ|||ζ　ζ|||ζ"""

        /**
         * ```kaomoji
         * (っ˘ڡ˘ς)
         * ```
         */
        const val food: String = """(っ˘ڡ˘ς)"""

        /**
         * ```kaomoji
         * ヾ(´〇`)ﾉ♪♪♪
         * ```
         */
        const val music: String = """ヾ(´〇`)ﾉ♪♪♪"""

        /**
         * ```kaomoji
         * ( ^^)p_____|_o____q(^^ )
         * ```
         */
        const val games: String = """( ^^)p_____|_o____q(^^ )"""

        /**
         * ```kaomoji
         * (ʘ ͜ʖ ʘ)
         * ```
         */
        const val faces: String = """(ʘ ͜ʖ ʘ)"""

        /**
         * ```kaomoji
         * ٩(ˊ〇ˋ*)و
         * ```
         */
        const val `wakes up`: String = """٩(ˊ〇ˋ*)و"""

        /**
         * ```kaomoji
         * (￣^￣)ゞ
         * ```
         */
        const val salutes: String = """(￣^￣)ゞ"""

        /**
         * ```kaomoji
         * (－‸ლ)
         * ```
         */
        const val facepalm: String = """(－‸ლ)"""

        /**
         * ```kaomoji
         * (╯°益°)╯彡┻━┻
         * ```
         */
        const val `in anger throws a table`: String = """(╯°益°)╯彡┻━┻"""

        /**
         * ```kaomoji
         * (╮°-°)╮┳━━┳ ( ╯°□°)╯ ┻━━┻
         * ```
         */
        const val `took the table and made a riot`: String = """(╮°-°)╮┳━━┳ ( ╯°□°)╯ ┻━━┻"""

        /**
         * ```kaomoji
         * ┬─┬ノ( º _ ºノ)
         * ```
         */
        const val `put the table in place`: String = """┬─┬ノ( º _ ºノ)"""

        /**
         * ```kaomoji
         * (oT-T)尸
         * ```
         */
        const val surrender: String = """(oT-T)尸"""

        /**
         * ```kaomoji
         * ( ͡° ͜ʖ ͡°)
         * ```
         */
        const val `Lenny face`: String = """( ͡° ͜ʖ ͡°)"""

        /**
         * ```kaomoji
         * [̲̅$̲̅(̲̅ ͡° ͜ʖ ͡°̲̅)̲̅$̲̅]
         * ```
         */
        const val money: String = """[̲̅$̲̅(̲̅ ͡° ͜ʖ ͡°̲̅)̲̅$̲̅]"""

        /**
         * ```kaomoji
         * (ಠ_ಠ)
         * ```
         */
        const val `a look of disapproval`: String = """(ಠ_ಠ)"""

        /**
         * ```kaomoji
         * ౦０o ｡ (‾́。‾́ )y~~
         * ```
         */
        const val smokes: String = """౦０o ｡ (‾́。‾́ )y~~"""

        /**
         * ```kaomoji
         * (￣﹃￣)
         * ```
         */
        const val hungry: String = """(￣﹃￣)"""

        /**
         * ```kaomoji
         * (x(x_(x_x(O_o)x_x)_x)x)
         * ```
         */
        const val `live among zombies`: String = """(x(x_(x_x(O_o)x_x)_x)x)"""

        /**
         * ```kaomoji
         * (　･ω･)☞
         * ```
         */
        const val indicates: String = """(　･ω･)☞"""

        /**
         * ```kaomoji
         * (⌐■_■)
         * ```
         */
        const val spectacled: String = """(⌐■_■)"""

        /**
         * ```kaomoji
         * (◕‿◕✿)
         * ```
         */
        const val sweet: String = """(◕‿◕✿)"""

        /**
         * ```kaomoji
         * (　￣.)o-　　【　TV　】
         * ```
         */
        const val `watching TV`: String = """(　￣.)o-　　【　TV　】"""

        /**
         * ```kaomoji
         * ｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ
         * ```
         */
        const val `catching umbrella in the rain`: String = """｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ"""

        /**
         * ```kaomoji
         * ‿︵‿︵‿︵‿ヽ(°□° )ノ︵‿︵‿︵‿︵
         * ```
         */
        const val drowning: String = """‿︵‿︵‿︵‿ヽ(°□° )ノ︵‿︵‿︵‿︵"""

        /**
         * ```kaomoji
         * ( • )( • )ԅ(≖‿≖ԅ)
         * ```
         */
        const val `touches boobs`: String = """( • )( • )ԅ(≖‿≖ԅ)"""

        /**
         * ```kaomoji
         * ( ＾▽＾)っ✂╰⋃╯
         * ```
         */
        const val `punishment for cheating`: String = """( ＾▽＾)っ✂╰⋃╯"""

        /**
         * ```kaomoji
         * 〜〜(／￣▽)／　〜ф
         * ```
         */
        const val `chasing a butterfly`: String = """〜〜(／￣▽)／　〜ф"""

        /**
         * ```kaomoji
         * ଘ(੭ˊᵕˋ)੭* ੈ✩‧₊˚
         * ```
         */
        const val angel: String = """ଘ(੭ˊᵕˋ)੭* ੈ✩‧₊˚"""

        /**
         * ```kaomoji
         * ଘ(੭ˊ꒳ˋ)੭✧
         * ```
         */
        const val `little angel`: String = """ଘ(੭ˊ꒳ˋ)੭✧"""

        /**
         * ```kaomoji
         * _(:3 」∠)_
         * ```
         */
        const val `lying smiley`: String = """_(:3 」∠)_"""

        /**
         * ```kaomoji
         * ∠( ᐛ 」∠)＿
         * ```
         */
        const val lay: String = """∠( ᐛ 」∠)＿"""

        /**
         * ```kaomoji
         * (n_n)
         * ```
         */
        const val smile: String = """(n_n)"""

        /**
         * ```kaomoji
         * (^_^)
         * ```
         */
        const val happiness: String = """(^_^)"""

        /**
         * ```kaomoji
         * (<_>)
         * ```
         */
        const val sorrow: String = """(<_>)"""

        /**
         * ```kaomoji
         * (^ ^)
         * ```
         */
        const val `smile through power`: String = """(^ ^)"""

        /**
         * ```kaomoji
         * (>_<)
         * ```
         */
        const val fatigue: String = """(>_<)"""

        /**
         * ```kaomoji
         * (>_>)
         * ```
         */
        const val skepticism: String = """(>_>)"""

        /**
         * ```kaomoji
         * (-_-")
         * ```
         */
        const val tangle: String = """(-_-")"""

        /**
         * ```kaomoji
         * ^_"
         * ```
         */
        const val discomfiture: String = """^_^""""

        /**
         * ```kaomoji
         * *^_^*
         * ```
         */
        const val `embarrassment with redness`: String = """*^_^*"""

        /**
         * ```kaomoji
         * (-_-#)
         * ```
         */
        const val rage: String = """(-_-#)"""

        /**
         * ```kaomoji
         * (0_0)
         * ```
         */
        const val `strong surprise`: String = """(0_0)"""

        /**
         * ```kaomoji
         * (O_o)
         * ```
         */
        const val `eyes twisted`: String = """(O_o)"""

        /**
         * ```kaomoji
         * (V_v)
         * ```
         */
        const val `unpleasant surprise`: String = """(V_v)"""

        /**
         * ```kaomoji
         * (@_@)
         * ```
         */
        const val awesome: String = """(@_@)"""

        /**
         * ```kaomoji
         * (%_%)
         * ```
         */
        const val `eye strain`: String = """(%_%)"""

        /**
         * ```kaomoji
         * (u_u)
         * ```
         */
        const val depression: String = """(u_u)"""

        /**
         * ```kaomoji
         * (>x<!)
         * ```
         */
        const val `heck!`: String = """(>x<!)"""

        /**
         * ```kaomoji
         * 8(>_<)8
         * ```
         */
        const val jealous: String = """8(>_<)8"""

        /**
         * ```kaomoji
         * (>>)
         * ```
         */
        const val `side glance`: String = """(>>)"""

        /**
         * ```kaomoji
         * (0_<)
         * ```
         */
        const val `nervous tick`: String = """(0_<)"""

        /**
         * ```kaomoji
         * (*_*)
         * ```
         */
        const val delight: String = """(*_*)"""

        /**
         * ```kaomoji
         * -__-
         * ```
         */
        const val phlegm: String = """-__-"""

        /**
         * ```kaomoji
         * (9_9)
         * ```
         */
        const val `stayed up all night`: String = """(9_9)"""

        /**
         * ```kaomoji
         * =__=
         * ```
         */
        const val sleepy: String = """=__="""

        /**
         * ```kaomoji
         * (-.-)Zzz.
         * ```
         */
        const val sleeping: String = """(-.-)Zzz."""

        /**
         * ```kaomoji
         * (-_-;)
         * ```
         */
        const val soreness: String = """(-_-;)"""

        /**
         * ```kaomoji
         * (Х_х)
         * ```
         */
        const val corpse: String = """(Х_х)"""

        /**
         * ```kaomoji
         * (^_~)
         * ```
         */
        const val `give a wink`: String = """(^_~)"""

        /**
         * ```kaomoji
         * (;_;)
         * ```
         */
        const val cry: String = """(;_;)"""

        /**
         * ```kaomoji
         * \..\ ^_^ /../
         * ```
         */
        const val `Sign of the horns`: String = """\..\ ^_^ /../ """

        /**
         * ```kaomoji
         * ^__^
         * ```
         */
        const val Haruhi: String = """^__^"""

        /**
         * ```kaomoji
         * (=^.^=)
         * ```
         */
        const val catwoman: String = """(=^.^=)"""

        /**
         * ```kaomoji
         * (O,o)
         * ```
         */
        const val owl: String = """(O,o)"""

        /**
         * ```kaomoji
         * ///_Т
         * ```
         */
        const val `emo kid`: String = """///_Т"""

        /**
         * ```kaomoji
         * ^_T
         * ```
         */
        const val Triad: String = """^_T"""

        /**
         * ```kaomoji
         * ("\(о_О)/")
         * ```
         */
        const val Hey: String = """("\(о_О)/")"""

        /**
         * ```kaomoji
         * (^>,<^)
         * ```
         */
        const val raccoon: String = """(^>,<^)"""

        /**
         * ```kaomoji
         * ^}{^
         * ```
         */
        const val kiss: String = """^}{^"""

        /**
         * ```kaomoji
         * =X=
         * ```
         */
        const val handshake: String = """=X="""

        /**
         * ```kaomoji
         * @-_-@
         * ```
         */
        const val Aries: String = """@-_-@"""

        /**
         * ```kaomoji
         * (-(-_(-_-(О_о)-_-)_-)-)
         * ```
         */
        const val `someone woke up in the subway`: String = """(-(-_(-_-(О_о)-_-)_-)-)"""

        /**
         * ```kaomoji
         * m9(^Д^)
         * ```
         */
        const val `mocking laugh`: String = """m9(^Д^)"""

        /**
         * ```kaomoji
         * m(_ _)m
         * ```
         */
        const val Kowtow: String = """m(_ _)m"""

        /**
         * ```kaomoji
         * (´･ω･`)
         * ```
         */
        const val `feeling ignored or unimportant`: String = """(´･ω･`)"""

        /**
         * ```kaomoji
         * <`∀´>
         * ```
         */
        @Suppress("ObjectPropertyName", "NonAsciiCharacters")
        const val `stereotypical Korean character (Nidā)`: String = """<`∀´>"""

        /**
         * ```kaomoji
         * (`･ω･´)
         * ```
         */
        const val impudence: String = """(`･ω･´)"""

        /**
         * ```kaomoji
         * ＿|￣|○
         * ```
         */
        const val `give up`: String = """＿|￣|○"""

        /**
         * ```kaomoji
         * (´；ω；`)
         * ```
         */
        const val `terribly sad`: String = """(´；ω；`)"""

        /**
         * ```kaomoji
         * ヽ(´ー｀)ﾉ
         * ```
         */
        const val `peace of mind`: String = """ヽ(´ー｀)ﾉ"""

        /**
         * ```kaomoji
         * ヽ(`Д´)ﾉ
         * ```
         */
        const val irritation: String = """ヽ(`Д´)ﾉ"""

        /**
         * ```kaomoji
         * （ ´Д｀）
         * ```
         */
        const val scream: String = """（ ´Д｀）"""

        /**
         * ```kaomoji
         * （　ﾟДﾟ）
         * ```
         */
        const val `blatant tone`: String = """（　ﾟДﾟ）"""

        /**
         * ```kaomoji
         * ┐('～`；)┌
         * ```
         */
        const val `I do not know`: String = """┐('～`；)┌"""

        /**
         * ```kaomoji
         * （´∀｀）
         * ```
         */
        const val carelessness: String = """（´∀｀）"""

        /**
         * ```kaomoji
         * Σ(゜д゜;)
         * ```
         */
        const val shock: String = """Σ(゜д゜;)"""

        /**
         * ```kaomoji
         * (ﾟヮﾟ)
         * ```
         */
        const val `a good mood`: String = """(ﾟヮﾟ)"""

        /**
         * ```kaomoji
         * キタ━━━(゜∀゜)━━━!!!!!
         * ```
         */
        const val `slang "Kita!"`: String = """キタ━━━(゜∀゜)━━━!!!!!"""

        /**
         * ```kaomoji
         * ｷﾀﾜァ*･゜ﾟ･*:.｡..｡.:*･゜(n‘∀‘)ηﾟ･*:.｡. .｡.:*･゜ﾟ･* !!!!!
         * ```
         */
        const val `girl version of "Kita!"`: String = """ｷﾀﾜァ*･゜ﾟ･*:.｡..｡.:*･゜(n‘∀‘)ηﾟ･*:.｡. .｡.:*･゜ﾟ･* !!!!!"""

        /**
         * ```kaomoji
         * ⊂二二二（　＾ω＾）二⊃
         * ```
         */
        const val `indifferent expression (Bu-n)`: String = """⊂二二二（　＾ω＾）二⊃"""

        /**
         * ```kaomoji
         * (*´Д`)ﾊｧﾊｧ
         * ```
         */
        const val `erotic excitement`: String = """(*´Д`)ﾊｧﾊｧ"""

        /**
         * ```kaomoji
         * (　´Д｀)ﾉ(´･ω･`)　ﾅﾃﾞﾅﾃﾞ
         * ```
         */
        const val pat: String = """(　´Д｀)ﾉ(´･ω･`)　ﾅﾃﾞﾅﾃﾞ"""

        /**
         * ```kaomoji
         * ((((；ﾟДﾟ)))
         * ```
         */
        const val `frightening face`: String = """((((；ﾟДﾟ)))"""

        /**
         * ```kaomoji
         * (´∀｀)σ)∀`)
         * ```
         */
        const val `poke someone on the cheek`: String = """(´∀｀)σ)∀`)"""

        /**
         * ```kaomoji
         * （・∀・ ）ヾ(- -；)コラコラ
         * ```
         */
        const val `swear word "wow, I'll show you!"`: String = """（・∀・ ）ヾ(- -；)コラコラ"""

        /**
         * ```kaomoji
         * (ﾟдﾟ)
         * ```
         */
        const val amazement: String = """(ﾟдﾟ)"""

        /**
         * ```kaomoji
         * (´ー`)y-~~
         * ```
         */
        const val smoke: String = """(´ー`)y-~~"""

        /**
         * ```kaomoji
         * （ ^_^）o自自o（^_^ ）
         * ```
         */
        const val toast: String = """（ ^_^）o自自o（^_^ ）"""

        /**
         * ```kaomoji
         * m9(・∀・)
         * ```
         */
        const val `intuition flash`: String = """m9(・∀・)"""

        /**
         * ```kaomoji
         * ヽ(´ー`)人(´∇｀)人(`Д´)ノ
         * ```
         */
        const val friendliness: String = """ヽ(´ー`)人(´∇｀)人(`Д´)ノ"""

        /**
         * ```kaomoji
         * ('A`)
         * ```
         */
        const val loneliness: String = """('A`)"""

        /**
         * ```kaomoji
         * （ ´,_ゝ`)
         * ```
         */
        const val `indifferent dissatisfaction`: String = """（ ´,_ゝ`)"""

        /**
         * ```kaomoji
         * （´-`）.｡oO(…)
         * ```
         */
        const val `thinking process`: String = """（´-`）.｡oO(…)"""

        /**
         * ```kaomoji
         * (ﾟДﾟ;≡;ﾟДﾟ)
         * ```
         */
        const val inattention: String = """(ﾟДﾟ;≡;ﾟДﾟ)"""

        /**
         * ```kaomoji
         * (´д)ﾋｿ(´Д｀)ﾋｿ(Д｀)
         * ```
         */
        const val whisper: String = """(´д)ﾋｿ(´Д｀)ﾋｿ(Д｀)"""

        /**
         * ```kaomoji
         * （･∀･)つ⑩
         * ```
         */
        const val `gives money`: String = """（･∀･)つ⑩"""

        /**
         * ```kaomoji
         * ⊂（ﾟДﾟ⊂⌒｀つ≡≡≡(´⌒;;;≡≡≡
         * ```
         */
        const val `belly slide`: String = """⊂（ﾟДﾟ⊂⌒｀つ≡≡≡(´⌒;;;≡≡≡"""

        /**
         * ```kaomoji
         * (ﾟ⊿ﾟ)
         * ```
         */
        const val `"I don't need it"`: String = """(ﾟ⊿ﾟ)"""

        /**
         * ```kaomoji
         * щ(ﾟДﾟщ)(屮ﾟДﾟ)屮
         * ```
         */
        const val `"Come on"`: String = """щ(ﾟДﾟщ)(屮ﾟДﾟ)屮"""

        /**
         * ```kaomoji
         * （・∀・）
         * ```
         */
        const val ridicule: String = """（・∀・）"""

        /**
         * ```kaomoji
         * （・Ａ・）
         * ```
         */
        const val `"This is bad"`: String = """（・Ａ・）"""

        /**
         * ```kaomoji
         * (ﾟ∀ﾟ)
         * ```
         */
        const val foolishness: String = """(ﾟ∀ﾟ)"""

        /**
         * ```kaomoji
         * エェェ(´д｀)ェェエ
         * ```
         */
        const val `"unconvincing"`: String = """エェェ(´д｀)ェェエ"""

        /**
         * ```kaomoji
         * (￣ー￣)
         * ```
         */
        const val grin: String = """(￣ー￣)"""

        /**
         * ```kaomoji
         * (ﾟ∀ﾟ)ｱﾊﾊ八八ﾉヽﾉヽﾉヽﾉ ＼ / ＼/ ＼
         * ```
         */
        const val `evil laugh`: String = """(ﾟ∀ﾟ)ｱﾊﾊ八八ﾉヽﾉヽﾉヽﾉ ＼ / ＼/ ＼"""

        /**
         * ```kaomoji
         * [ﾟдﾟ]
         * ```
         */
        const val Deflagged: String = """[ﾟдﾟ]"""

        /**
         * ```kaomoji
         * ♪┏(・o･)┛♪┗ (･o･) ┓♪┏ () ┛♪┗ (･o･) ┓♪┏(･o･)┛♪
         * ```
         */
        const val `dance to music`: String = """♪┏(・o･)┛♪┗ (･o･) ┓♪┏ () ┛♪┗ (･o･) ┓♪┏(･o･)┛♪"""

        /**
         * ```kaomoji
         * d(*⌒▽⌒*)b
         * ```
         */
        const val `happy expression`: String = """d(*⌒▽⌒*)b"""

        /**
         * ```kaomoji
         * OTZ
         * ```
         */
        const val despair: String = """OTZ"""

        /**
         * ```kaomoji
         * (╬ ಠ益ಠ)
         * ```
         */
        const val `extreme disgust`: String = """(╬ ಠ益ಠ)"""

        /**
         * ```kaomoji
         * (ΘεΘ;)
         * ```
         */
        const val `sleeping with boredom`: String = """(ΘεΘ;)"""

        /**
         * ```kaomoji
         * お(^o^)や(^O^)す(^｡^)みぃ(^-^)ﾉﾞ
         * ```
         */
        const val `"good night"`: String = """お(^o^)や(^O^)す(^｡^)みぃ(^-^)ﾉﾞ"""

        /**
         * ```kaomoji
         * ＼| ￣ヘ￣|／＿＿＿＿＿＿＿θ☆(*o*)/
         * ```
         */
        const val kick: String = """＼| ￣ヘ￣|／＿＿＿＿＿＿＿θ☆(*o*)/"""

        /**
         * ```kaomoji
         * （‐＾▽＾‐）オーホッホ
         * ```
         */
        const val giggle: String = """（‐＾▽＾‐）オーホッホ"""

        /**
         * ```kaomoji
         * ┌(；`～,)┐
         * ```
         */
        const val perplexity: String = """┌(；`～,)┐"""

        /**
         * ```kaomoji
         * ヽ(ｏ`皿′ｏ)ﾉ
         * ```
         */
        const val fury: String = """ヽ(ｏ`皿′ｏ)ﾉ"""

        /**
         * ```kaomoji
         * o/ o_ o/ o_
         * ```
         */
        const val `"It's here"`: String = """o/ o_ o/ o_"""

        /**
         * ```kaomoji
         * (☞ﾟヮﾟ)☞
         * ```
         */
        const val `"Do it"`: String = """(☞ﾟヮﾟ)☞"""
    }

    @Suppress("unused")
    val writing: List<String> = listOf(
        "( ￣ー￣)φ__",
    )

    @Suppress("unused")
    val cheerLeaders: List<String> = listOf(
        "✺◟( • ω • )◞✺",
    )

    @Suppress("unused")
    val salutators: List<String> = listOf(
        "(￣^￣)ゞ",
    )

    @Suppress("unused")
    val tableTakers: List<String> = listOf(
        "(╮°-°)╮┳━━┳ ",
    )

    @Suppress("unused")
    val tableThrowers: List<String> = listOf(
        "( ╯°□°)╯ ┻━━┻",
        "╯‵Д′)╯彡┻━┻ ",
        "(ノಠ益ಠ)ノ彡┻━┻ ",
    )

    @Suppress("unused")
    val smokers: List<String> = listOf(
        "౦０o ｡ (‾́。‾́ )y~~",
    )

    @Suppress("unused")
    val rain: List<String> = listOf(
        "｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ",
        "｀ヽ｀((((( ( ⊃・ω・)⊃☂｀(´ω｀u)))ヽ｀、",
    )

    @Suppress("unused")
    val tv: List<String> = listOf(
        "【 TV 】      -o(.￣ )",
    )

    @Suppress("unused")
    val fish: List<String> = listOf(
        "ϵ( 'Θ' )϶",
        "∋(°O°)∈",
        "(〠_〠)",
    )

    @Suppress("unused")
    val weapons: List<String> = listOf(
        "̿ ̿̿'̿'\\̵͇̿̿\\=(•̪●)=/̵͇̿̿/'̿̿ ̿ ̿ ̿",
    )

    @Suppress("unused")
    val babies: List<String> = listOf(
        "ლ(´ڡ`ლ)",
        "ლ(́◉◞౪◟◉‵ლ)",
        "(●´ω｀●)",
    )

    @Suppress("unused")
    val money: List<String> = listOf(
        "[̲̅\$̲̅(̲̅5̲̅)̲̅\$̲̅]",
    )

    @Suppress("unused")
    val screaming: List<String> = listOf(
        "ヽ(๏∀๏ )ﾉ",
        "ヽ(｀Д´)ﾉ",
        "ヽ(ｏ`皿′ｏ)ﾉ",
        "ヽ(`Д´)ﾉ",
    )

    @Suppress("unused")
    val why: List<String> = listOf(
        "щ（ﾟДﾟщ",
        "щ(ಠ益ಠщ)",
        "щ(ಥДಥщ)",
    )

    @Suppress("unused")
    val goofy: List<String> = listOf(
        "(ό‿ὸ)ﾉ",
    )

    @Suppress("unused")
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

    @Suppress("unused")
    val pointing: List<String> = listOf(
        "☜ق❂Ⴢ❂ق☞",
        "☜(⌒▽⌒)☞",
        "☜(ﾟヮﾟ☜)",
        "☜-(ΘLΘ)-☞",
    )

    @Suppress("unused")
    val dance: List<String> = listOf(
        "┏(‘▀_▀’)ノ♬♪",
        "ヾ(*´ ∇ `)ﾉ",
        "ヽ(⌐■_■)ノ♪♬"
    )

    @Suppress("unused")
    val chasing: List<String> = listOf(
        "(○｀д´)ﾉｼ Σ(っﾟДﾟ)っ",
        "☎Σ⊂⊂(☉ω☉∩)",

        )

    @Suppress("unused")
    val celebrities: List<String> = listOf(
        "⪿ ↂ ˒̫̮ ↂ ⫀", // Elton John
    )

    @Suppress("unused")
    val excitement: List<String> = listOf(
        "ヽ( ★ω★)ノ",
    )

    @Suppress("unused")
    val heroes: List<String> = listOf(
        "─=≡Σ(([ ⊐•̀⌂•́]⊐",
    )

    @Suppress("unused")
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

        @Suppress("ObjectPropertyName", "NonAsciiCharacters")
        val აTypeDogs: List<String> = listOf(
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

    @Suppress("unused")
    val stereoTypes = listOf(
        //O RLY?
        """
            ,___,
            [O.o] - O RLY?
            /)__)
            -"--"-
        """.trimIndent(), // hare
        """
            (\_/)
            (O.o)
            (> <)
            /_|_\
        """.trimIndent(), // Cthulhu
        """
              (jIj)
              (;,;)
             (o,.,O)
            Y(O,,,,O)Y
        """.trimIndent(), // steteotypical North Korean character: Kiga
        """
            ［　(★)　］
             <丶´Д｀>
        """.trimIndent(), // steteotypical Japanese character: Mona
        """
                ∧＿∧
            （ ；´Д｀）
        """.trimIndent(), // steteotypical Chinese Korean character: Sina
        """
                ∧∧
              ／ 中＼
            （ 　｀ハ´）
        """.trimIndent(), // steteotypical Taiwanese character: Wana
        """
                  ∧∧
             　 ／　台＼
            　（　＾∀＾）
        """.trimIndent(), // steteotypical Vietamese character: Vena
        """
               ∧∧
             ／ 越 ＼
            （ ・∀・ ）
        """.trimIndent(), // steteotypical Indian character: Monaste
        """
              γ~三ヽ 
             (三彡０ﾐ) 
            （　´∀｀）
        """.trimIndent(), // steteotypical American character: Kiga
        """
              |￣￣| 
             ＿☆☆☆＿ 
            （ ´_⊃｀）
        """.trimIndent(), // stereotypical Jewish character: Yuda
        """
              　 ┏━┓
               ━━━━━━
               ﾐΘc_Θ-ﾐ
        """.trimIndent(), // stereotypical English character: Yaku
        """
               ＿＿ 
              │〓.│ 
              ━━━━━
            ﾐ　´_＞｀）
        """.trimIndent(), // stereotypical French character: Toriri
        """
              ____ 
            （〓__＞
            ξ ・_>・）
        """.trimIndent(), // stereotypical German character: Gerumandanu
        """
             _、,_ 
            ﾐ　　_⊃）
        """.trimIndent(), // stereotypical Austrian character: Osuto
        """
               ≡≡彡
            彡 ´_)｀ ）
        """.trimIndent(), // stereotypical Russian character: Rosuki
        """
             ,,,,,,,,,,,,, 
            　ﾐ;;;,,,,,,,ﾐ　 
              （　｀_っ´）
        """.trimIndent(), // stereotypical Mexican character: Amigo
        """
                _γ⌒ヽ_
              lXXXXXXXXl
            　（　´ｍ｀）
        """.trimIndent(), // stereotypical Persian character: Jujo
        """
                     _
               <(o0o)>
            (>ミ — ミ)>
        """.trimIndent()

    )
}

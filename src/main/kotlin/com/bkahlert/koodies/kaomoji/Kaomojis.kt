@file:Suppress("SpellCheckingInspection", "ObjectPropertyName", "HardCodedStringLiteral")

package com.bkahlert.koodies.kaomoji

import com.bkahlert.koodies.kaomoji.Kaomojis.Generator.Companion.removeRightArm
import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.hidden
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

            fun CharSequence.removeRightArm(): CharSequence {
                val rightArm = rightArms.dropWhile { !this.endsWith(it) }
                return if (rightArm.isNotEmpty()) this.removeSuffix(rightArm.first()) else this
            }
        }
    }

    fun random(): String = with(Generator) { listOf(leftArms, leftEyes, mouths, rightEyes, rightArms).joinToString("") { it.random() } }

    data class Kaomoji(
        private val template: String,
        private val leftArmRange: IntRange? = null,
        private val rightArmRange: IntRange? = null,
        private val leftEyeRange: IntRange? = null,
        private val rightEyeRange: IntRange? = null,
        private val mouthRange: IntRange? = null,
        private val wandRange: IntRange? = null,
    ) : CharSequence {
        val leftArm: CharSequence = leftArmRange?.let { template.subSequence(it) } ?: ""
        val rightArm: CharSequence = rightArmRange?.let { template.subSequence(it) } ?: ""
        val leftEye: CharSequence = leftEyeRange?.let { template.subSequence(it) } ?: ""
        val rightEye: CharSequence = rightEyeRange?.let { template.subSequence(it) } ?: ""
        val mouth: CharSequence = mouthRange?.let { template.subSequence(it) } ?: ""
        val wand: CharSequence = wandRange?.let { template.subSequence(it) } ?: ""

        fun random(): String {
            val listOfNotNull: List<IntRange> = listOfNotNull(leftArmRange, leftEyeRange, mouthRange, rightEyeRange, rightArmRange)
            return listOfNotNull.foldRight(template) { intRange, acc ->
                acc.substring(0 until intRange.first) + Generator.mouths.random() + acc.subSequence(intRange.last, acc.lastIndex)
            }
        }

        fun withMagic(): String {
            val listOfNotNull: List<IntRange> = listOfNotNull(wandRange)
            return listOfNotNull.fold(template) { acc, intRange ->
                acc.substring(0 until intRange.first) + ANSI.termColors.colorize(acc.substring(intRange)) + acc.subSequence(intRange.last, acc.lastIndex + 1)
            }
        }

        /**
         * Returns a fishing [Kaomoji] of the form:
         *
         * ```
         * （♯▼皿▼）o/￣￣￣<゜)))彡
         * ```
         */
        fun fishing(fish: Kaomoji = (Fish + Whales).random()): String {
            val fishingRod = "/￣￣￣"
            val fishingArm = "o"
            val notFishingKaomoji = this.removeSuffix(fishingRod)
            val armLessFisher = notFishingKaomoji.removeRightArm()
            return "$armLessFisher$fishingArm$fishingRod$fish"
        }

        operator fun getValue(kaomojis: Any, property: KProperty<*>): Kaomoji = this
        override val length: Int get() = toString().length
        override fun get(index: Int): Char = toString().get(index)
        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = toString().subSequence(startIndex, endIndex)
        override fun toString(): String = wandRange?.let { copy(template = withMagic()).random() } ?: template
    }

    @Suppress("KDocMissingDocumentation", "ObjectPropertyName", "NonAsciiCharacters")
    val `(＃￣_￣)o︠・━・・━・━━・━☆`: Kaomoji by five(mouth = 3..4, rightArm = 6..7, wand = 8..16)

    fun five(
        leftArm: IntRange? = null, rightArm: IntRange? = null, leftEye: IntRange? = null, rightEye: IntRange? = null, mouth: IntRange? = null,
        wand: IntRange? = null,
    ): PropertyDelegateProvider<Any, Kaomoji> =
        PropertyDelegateProvider { _, property -> Kaomoji(property.name, leftArm, leftEye, rightEye, rightArm, mouth, wand) }


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

    /**
     * Returns a thinking [Kaomoji] of the form:
     *
     * ```
     *           ͚͔˱ ❨ ( something )
     * (^～^) ˙
     * ```
     */
    fun CharSequence.thinking(value: String): String {
        val kaomoji = this
        val thinkLine = "${kaomoji.hidden()}   ͚͔˱ ❨ ( $value )"
        return "$thinkLine\n$kaomoji ˙"
    }

    @Suppress("KDocMissingDocumentation", "unused")
    val Angry = com.bkahlert.koodies.kaomoji.categories.Angry

    @Suppress("KDocMissingDocumentation", "unused")
    val BadMood = com.bkahlert.koodies.kaomoji.categories.BadMood

    @Suppress("KDocMissingDocumentation", "unused")
    val Bear = com.bkahlert.koodies.kaomoji.categories.Bear

    @Suppress("KDocMissingDocumentation", "unused")
    val Beg = com.bkahlert.koodies.kaomoji.categories.Beg

    @Suppress("KDocMissingDocumentation", "unused")
    val Blush = com.bkahlert.koodies.kaomoji.categories.Blush

    @Suppress("KDocMissingDocumentation", "unused")
    val Cat = com.bkahlert.koodies.kaomoji.categories.Cat

    @Suppress("KDocMissingDocumentation", "unused")
    val Confused = com.bkahlert.koodies.kaomoji.categories.Confused

    @Suppress("KDocMissingDocumentation", "unused")
    val Cry = com.bkahlert.koodies.kaomoji.categories.Cry

    @Suppress("KDocMissingDocumentation", "unused")
    val Cute = com.bkahlert.koodies.kaomoji.categories.Cute

    @Suppress("KDocMissingDocumentation", "unused")
    val Dance = com.bkahlert.koodies.kaomoji.categories.Dance

    @Suppress("KDocMissingDocumentation", "unused")
    val Depressed = com.bkahlert.koodies.kaomoji.categories.Depressed

    @Suppress("KDocMissingDocumentation", "unused")
    val Devil = com.bkahlert.koodies.kaomoji.categories.Devil

    @Suppress("KDocMissingDocumentation", "unused")
    val Disappointed = com.bkahlert.koodies.kaomoji.categories.Disappointed

    @Suppress("KDocMissingDocumentation", "unused")
    val Drool = com.bkahlert.koodies.kaomoji.categories.Drool

    @Suppress("KDocMissingDocumentation", "unused")
    val Eat = com.bkahlert.koodies.kaomoji.categories.Eat

    @Suppress("KDocMissingDocumentation", "unused")
    val Evil = com.bkahlert.koodies.kaomoji.categories.Evil

    @Suppress("KDocMissingDocumentation", "unused")
    val Excited = com.bkahlert.koodies.kaomoji.categories.Excited

    @Suppress("KDocMissingDocumentation", "unused")
    val FallDown = com.bkahlert.koodies.kaomoji.categories.FallDown

    @Suppress("KDocMissingDocumentation", "unused")
    val Feces = com.bkahlert.koodies.kaomoji.categories.Feces

    @Suppress("KDocMissingDocumentation", "unused")
    val Feminine = com.bkahlert.koodies.kaomoji.categories.Feminine

    @Suppress("KDocMissingDocumentation", "unused")
    val FlipTable = com.bkahlert.koodies.kaomoji.categories.FlipTable

    @Suppress("KDocMissingDocumentation", "unused")
    val Flower = com.bkahlert.koodies.kaomoji.categories.Flower

    @Suppress("KDocMissingDocumentation", "unused")
    val Funny = com.bkahlert.koodies.kaomoji.categories.Funny

    @Suppress("KDocMissingDocumentation", "unused")
    val Glasses = com.bkahlert.koodies.kaomoji.categories.Glasses

    @Suppress("KDocMissingDocumentation", "unused")
    val Grin = com.bkahlert.koodies.kaomoji.categories.Grin

    @Suppress("KDocMissingDocumentation", "unused")
    val Gross = com.bkahlert.koodies.kaomoji.categories.Gross

    @Suppress("KDocMissingDocumentation", "unused")
    val Happy = com.bkahlert.koodies.kaomoji.categories.Happy

    @Suppress("KDocMissingDocumentation", "unused")
    val Heart = com.bkahlert.koodies.kaomoji.categories.Heart

    @Suppress("KDocMissingDocumentation", "unused")
    val Hello = com.bkahlert.koodies.kaomoji.categories.Hello

    @Suppress("KDocMissingDocumentation", "unused")
    val Helpless = com.bkahlert.koodies.kaomoji.categories.Helpless

    @Suppress("KDocMissingDocumentation", "unused")
    val Hide = com.bkahlert.koodies.kaomoji.categories.Hide

    @Suppress("KDocMissingDocumentation", "unused")
    val Hug = com.bkahlert.koodies.kaomoji.categories.Hug

    @Suppress("KDocMissingDocumentation", "unused")
    val Kiss = com.bkahlert.koodies.kaomoji.categories.Kiss

    @Suppress("KDocMissingDocumentation", "unused")
    val Laugh = com.bkahlert.koodies.kaomoji.categories.Laugh

    @Suppress("KDocMissingDocumentation", "unused")
    val LennyFace = com.bkahlert.koodies.kaomoji.categories.LennyFace

    @Suppress("KDocMissingDocumentation", "unused")
    val Love = com.bkahlert.koodies.kaomoji.categories.Love

    @Suppress("KDocMissingDocumentation", "unused")
    val Magic = com.bkahlert.koodies.kaomoji.categories.Magic

    @Suppress("KDocMissingDocumentation", "unused")
    val MakeUpMyMind = com.bkahlert.koodies.kaomoji.categories.MakeUpMyMind

    @Suppress("KDocMissingDocumentation", "unused")
    val MiddleFinger = com.bkahlert.koodies.kaomoji.categories.MiddleFinger

    @Suppress("KDocMissingDocumentation", "unused")
    val Monkey = com.bkahlert.koodies.kaomoji.categories.Monkey

    @Suppress("KDocMissingDocumentation", "unused")
    val Music = com.bkahlert.koodies.kaomoji.categories.Music

    @Suppress("KDocMissingDocumentation", "unused")
    val Nervious = com.bkahlert.koodies.kaomoji.categories.Nervious

    @Suppress("KDocMissingDocumentation", "unused")
    val PeaceSign = com.bkahlert.koodies.kaomoji.categories.PeaceSign

    @Suppress("KDocMissingDocumentation", "unused")
    val Proud = com.bkahlert.koodies.kaomoji.categories.Proud

    @Suppress("KDocMissingDocumentation", "unused")
    val Punch = com.bkahlert.koodies.kaomoji.categories.Punch

    @Suppress("KDocMissingDocumentation", "unused")
    val Rabbit = com.bkahlert.koodies.kaomoji.categories.Rabbit

    @Suppress("KDocMissingDocumentation", "unused")
    val RogerThat = com.bkahlert.koodies.kaomoji.categories.RogerThat

    @Suppress("KDocMissingDocumentation", "unused")
    val RollOver = com.bkahlert.koodies.kaomoji.categories.RollOver

    @Suppress("KDocMissingDocumentation", "unused")
    val Run = com.bkahlert.koodies.kaomoji.categories.Run

    @Suppress("KDocMissingDocumentation", "unused")
    val Sad = com.bkahlert.koodies.kaomoji.categories.Sad

    @Suppress("KDocMissingDocumentation", "unused")
    val Salute = com.bkahlert.koodies.kaomoji.categories.Salute

    @Suppress("KDocMissingDocumentation", "unused")
    val Scared = com.bkahlert.koodies.kaomoji.categories.Scared

    @Suppress("KDocMissingDocumentation", "unused")
    val Sheep = com.bkahlert.koodies.kaomoji.categories.Sheep

    @Suppress("KDocMissingDocumentation", "unused")
    val Shocked = com.bkahlert.koodies.kaomoji.categories.Shocked

    @Suppress("KDocMissingDocumentation", "unused")
    val Shrug = com.bkahlert.koodies.kaomoji.categories.Shrug

    @Suppress("KDocMissingDocumentation", "unused")
    val Shy = com.bkahlert.koodies.kaomoji.categories.Shy

    @Suppress("KDocMissingDocumentation", "unused")
    val Sleep = com.bkahlert.koodies.kaomoji.categories.Sleep

    @Suppress("KDocMissingDocumentation", "unused")
    val Smile = com.bkahlert.koodies.kaomoji.categories.Smile

    @Suppress("KDocMissingDocumentation", "unused")
    val Sparkle = com.bkahlert.koodies.kaomoji.categories.Sparkle

    @Suppress("KDocMissingDocumentation", "unused")
    val Spin = com.bkahlert.koodies.kaomoji.categories.Spin

    @Suppress("KDocMissingDocumentation", "unused")
    val Surprised = com.bkahlert.koodies.kaomoji.categories.Surprised

    @Suppress("KDocMissingDocumentation", "unused")
    val Sweat = com.bkahlert.koodies.kaomoji.categories.Sweat

    @Suppress("KDocMissingDocumentation", "unused")
    val TakeABow = com.bkahlert.koodies.kaomoji.categories.TakeABow

    @Suppress("KDocMissingDocumentation", "unused")
    val ThatsIt = com.bkahlert.koodies.kaomoji.categories.ThatsIt

    @Suppress("KDocMissingDocumentation", "unused")
    val ThumbsUp = com.bkahlert.koodies.kaomoji.categories.ThumbsUp

    @Suppress("KDocMissingDocumentation", "unused")
    val Tired = com.bkahlert.koodies.kaomoji.categories.Tired

    @Suppress("KDocMissingDocumentation", "unused")
    val Tremble = com.bkahlert.koodies.kaomoji.categories.Tremble

    @Suppress("KDocMissingDocumentation", "unused")
    val TryMyBest = com.bkahlert.koodies.kaomoji.categories.TryMyBest

    @Suppress("KDocMissingDocumentation", "unused")
    val Unicode = com.bkahlert.koodies.kaomoji.categories.Unicode

    @Suppress("KDocMissingDocumentation", "unused")
    val Upset = com.bkahlert.koodies.kaomoji.categories.Upset

    @Suppress("KDocMissingDocumentation", "unused")
    val Vomit = com.bkahlert.koodies.kaomoji.categories.Vomit

    @Suppress("KDocMissingDocumentation", "unused")
    val Weird = com.bkahlert.koodies.kaomoji.categories.Weird

    @Suppress("KDocMissingDocumentation", "unused")
    val Wink = com.bkahlert.koodies.kaomoji.categories.Wink

    @Suppress("KDocMissingDocumentation", "unused")
    val Writing = com.bkahlert.koodies.kaomoji.categories.Writing

    @Suppress("KDocMissingDocumentation", "unused")
    val Smoking = com.bkahlert.koodies.kaomoji.categories.Smoking

    @Suppress("KDocMissingDocumentation", "unused")
    val Rain = com.bkahlert.koodies.kaomoji.categories.Rain

    @Suppress("KDocMissingDocumentation", "unused")
    val TV = com.bkahlert.koodies.kaomoji.categories.TV

    @Suppress("KDocMissingDocumentation", "unused")
    val Fishing = com.bkahlert.koodies.kaomoji.categories.Fishing

    @Suppress("KDocMissingDocumentation", "unused")
    val Fish = com.bkahlert.koodies.kaomoji.categories.Fish

    @Suppress("KDocMissingDocumentation", "unused")
    val Whales = com.bkahlert.koodies.kaomoji.categories.Whales

    @Suppress("KDocMissingDocumentation", "unused")
    val Weapons = com.bkahlert.koodies.kaomoji.categories.Weapons

    @Suppress("KDocMissingDocumentation", "unused")
    val Babies = com.bkahlert.koodies.kaomoji.categories.Babies

    @Suppress("KDocMissingDocumentation", "unused")
    val Money = com.bkahlert.koodies.kaomoji.categories.Money

    @Suppress("KDocMissingDocumentation", "unused")
    val Screaming = com.bkahlert.koodies.kaomoji.categories.Screaming

    @Suppress("KDocMissingDocumentation", "unused")
    val Why = com.bkahlert.koodies.kaomoji.categories.Why

    @Suppress("KDocMissingDocumentation", "unused")
    val Geeks = com.bkahlert.koodies.kaomoji.categories.Geeks

    @Suppress("KDocMissingDocumentation", "unused")
    val Pointing = com.bkahlert.koodies.kaomoji.categories.Pointing

    @Suppress("KDocMissingDocumentation", "unused")
    val Chasing = com.bkahlert.koodies.kaomoji.categories.Chasing

    @Suppress("KDocMissingDocumentation", "unused")
    val Celebrities = com.bkahlert.koodies.kaomoji.categories.Celebrities

    @Suppress("KDocMissingDocumentation", "unused")
    val Heroes = com.bkahlert.koodies.kaomoji.categories.Heroes

    @Suppress("KDocMissingDocumentation", "unused")
    val Dogs = com.bkahlert.koodies.kaomoji.categories.Dogs

    @Suppress("KDocMissingDocumentation", "unused")
    val StereoTypes = com.bkahlert.koodies.kaomoji.categories.StereoTypes

    /**
     * Returns a random fishing [Kaomoji] of the form:
     *
     * ```
     * （♯▼皿▼）o/￣￣￣<゜)))彡
     * ```
     */
    fun fishing(fish: Kaomoji = (Fish + Whales).random()): String =
        Fishing.random().fishing(fish)
}

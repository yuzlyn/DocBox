package com.yuzlyn.mdeditor.ui.i18n

import com.yuzlyn.mdeditor.data.settings.AppLanguage

/**
 * 說明頁的正文。
 *
 * 這一頁刻意**不列功能**：功能看得到、摸得到，不需要再用文字複述一遍；留給說明頁的是
 * 這個軟體為什麼長成現在這樣——漢字與讀音的關係、聲律感知的退化、方言的處境，以及
 * 為什麼最後沒有做「小學」。四份語言的段落一一對應，改一處就要四處一起改。
 *
 * @param paragraphs 正文段落，一段一個 Text；不要在這裡塞條列或小標。
 */
data class HelpDoc(
    val paragraphs: List<String>,
    /**
     * 頁尾的素材出處。
     *
     * 這一頁不放功能介紹，但授權署名不能省：桌面圖示的字形來自 CC BY 4.0 的
     * JGW 甲骨骨架，必須保留姓名與 DOI。字級壓到 labelSmall，讓它讀起來是註腳。
     */
    val credit: String,
    val linksTitle: String,
    val repoLabel: String,
    val githubLabel: String
)

/**
 * 專案與作者的公開資訊。
 *
 * 說明頁的頁尾與作者頁都從這裡拿。要更新個人資料只改這一處——
 * 分散在各個畫面裡的話，下次改信箱就會漏掉一頁。
 */
const val PROJECT_REPO_URL = "https://github.com/yuzlyn/DocBox"
const val AUTHOR_GITHUB_URL = "https://github.com/yuzlyn"
const val AUTHOR_GITHUB_USER = "yuzlyn"
const val AUTHOR_EMAIL = "cebrayu@qq.com"

fun AppLanguage.helpDoc(): HelpDoc = when (this.resolved()) {
    AppLanguage.SYSTEM -> ZhTwHelp
    AppLanguage.ZH_TW -> ZhTwHelp
    AppLanguage.ZH_CN -> ZhCnHelp
    AppLanguage.EN_US -> EnUsHelp
    AppLanguage.FR_FR -> FrFrHelp
}

private val ZhTwHelp = HelpDoc(
    paragraphs = listOf(
        "漢字與讀音合而成漢語。文字簡化、同音兼併雖有損耗，尚在其次；真正拉開今人與古詩詞距離的，是聲律感知的退化。",
        "古音有平、上、去、入。北方官話早在元代就失去了入聲，這是音變常態。今人讀古文，既少辨入聲之耳，又乏吟誦之功，遂覺格律艱深，難窺音韻之妙。當今語文教學多重析句解題，輕吟詠聲律，真懂作詩填詞的人已不多。",
        "早前民間發起「上傳方言」，竟被疑為蒐集情報，實屬無稽。方言日漸式微，大勢難回。若連地方腔調都散佚，舊日語彙與聲調便失去活態土壤，人與古韻的距離只會更遠。如今仍操方言、愛方言而願守望者，皆獨釣寒江雪。",
        "環顧諸語，文法與讀音皆趨簡化。英語日趨隨意，法蘭西學院力圖守住法語規範；漢語受西風浸染，句法詞彙亦在變動。語言流變本不可擋。",
        "於本軟體，我本想開發「小學」：從千字文學音韻，依平仄組詞、對對子，再造句寫詩。無奈我不識韻律，故刪除此功能。"
    ),
    credit = "桌面圖示的字形取自 hanzi-glyphs-svg（漢典摹本 CC0；JGW 甲骨骨架 1,203 條 CC BY 4.0，Li Qing-sheng、Bian Yu-lin，DOI 10.5281/zenodo.22467946）。",
    linksTitle = "專案連結",
    repoLabel = "專案倉庫",
    githubLabel = "yuzlyn"
)

private val ZhCnHelp = HelpDoc(
    paragraphs = listOf(
        "汉字与读音合而成汉语。文字简化、同音兼并虽有损耗，尚在其次；真正拉开今人与古诗词距离的，是声律感知的退化。",
        "古音有平、上、去、入。北方官话早在元代就失去了入声，这是音变常态。今人读古文，既少辨入声之耳，又乏吟诵之功，遂觉格律艰深，难窥音韵之妙。当今语文教学多重析句解题，轻吟咏声律，真懂作诗填词的人已不多。",
        "早前民间发起「上传方言」，竟被疑为搜集情报，实属无稽。方言日渐式微，大势难回。若连地方腔调都散佚，旧日词汇与声调便失去活态土壤，人与古韵的距离只会更远。如今仍操方言、爱方言而愿守望者，皆独钓寒江雪。",
        "环顾诸语，文法与读音皆趋简化。英语日趋随意，法兰西学院力图守住法语规范；汉语受西风浸染，句法词汇亦在变动。语言流变本不可挡。",
        "于本软件，我本想开发「小学」：从千字文学音韵，依平仄组词、对对子，再造句写诗。无奈我不识韵律，故删除此功能。"
    ),
    credit = "桌面图标字形取自 hanzi-glyphs-svg（汉典摹本 CC0；JGW 甲骨骨架 1,203 条 CC BY 4.0，Li Qing-sheng、Bian Yu-lin，DOI 10.5281/zenodo.22467946）。",
    linksTitle = "项目链接",
    repoLabel = "项目仓库",
    githubLabel = "yuzlyn"
)

private val EnUsHelp = HelpDoc(
    paragraphs = listOf(
        "Chinese characters and pronunciation together form the Chinese language. The simplification of characters and the merging of homophones, though involving some loss, are secondary; what truly widens the distance between modern people and classical poetry is the decline in our sensitivity to sound and rhythm.",
        "Ancient Chinese had four tones: level, rising, departing, and entering. Northern Mandarin lost the entering tone as early as the Yuan dynasty—this is a natural process of phonetic change. Today, when people read classical texts, they neither distinguish the entering tone by ear nor practice chanting, and so find the rules of versification difficult, missing the beauty of phonology. Current Chinese-language education emphasizes sentence analysis and exam questions while neglecting chanting and sound patterns; few truly understand how to compose poetry or lyrics.",
        "Some years ago, a grassroots initiative to “upload dialects” was absurdly suspected of gathering intelligence. Dialects are steadily fading, a trend hard to reverse. If local accents vanish as well, many old words and tones will lose their living soil, and the distance from ancient rhyme will only grow greater. Those who still speak dialects, love them, and wish to preserve them are like solitary anglers fishing in the cold river snow.",
        "Looking at languages around the world, both grammar and pronunciation tend toward simplification. English grows ever more casual and fluid, while the Académie Française strives to uphold the norms of French; Chinese, influenced by the West, is also changing in syntax and vocabulary. Linguistic evolution cannot be stopped.",
        "In this software I originally planned to develop a “Xiaoxue” (classical philology) feature: starting from the Thousand Character Classic to learn phonology, forming words by tonal patterns, composing couplets, and then writing sentences and poetry. Unfortunately I myself do not understand prosody, so I removed the feature."
    ),
    credit = "Launcher icon glyphs from hanzi-glyphs-svg (Handian tracings CC0; 1,203 JGW oracle skeletons CC BY 4.0 by Li Qing-sheng and Bian Yu-lin, DOI 10.5281/zenodo.22467946).",
    linksTitle = "Project links",
    repoLabel = "Repository",
    githubLabel = "yuzlyn"
)

private val FrFrHelp = HelpDoc(
    paragraphs = listOf(
        "Les caractères chinois et la prononciation forment ensemble la langue chinoise. La simplification des caractères et la fusion des homophones, bien qu’elles entraînent certaines pertes, restent secondaires ; ce qui éloigne vraiment les modernes de la poésie classique, c’est le déclin de notre sensibilité aux sons et au rythme.",
        "Le chinois ancien distinguait quatre tons : plat, montant, descendant et « entrant ». Le mandarin du Nord a perdu le ton entrant dès la dynastie Yuan — c’est un changement phonétique naturel. Aujourd’hui, en lisant les textes classiques, on ne sait plus reconnaître le ton entrant à l’oreille et l’on pratique rarement le chant récitatif ; les règles de la versification paraissent donc ardues, et la beauté de la phonologie échappe. L’enseignement actuel du chinois privilégie l’analyse des phrases et les exercices d’examen, au détriment du chant et des lois sonores ; peu de gens savent vraiment composer des poèmes ou des paroles.",
        "Il y a quelques années, une initiative populaire visant à « téléverser des dialectes » a été absurdement soupçonnée de collecter des renseignements. Les dialectes s’effacent peu à peu, tendance difficile à inverser. Si les accents locaux disparaissent aussi, bien des mots et des tons d’autrefois perdront leur terreau vivant, et la distance avec les rimes anciennes ne fera que s’accroître. Ceux qui parlent encore les dialectes, les aiment et veulent les préserver sont comme des pêcheurs solitaires sur la neige d’une rivière glacée.",
        "Dans le monde entier, grammaire et prononciation tendent à se simplifier. L’anglais devient de plus en plus libre et fluide, tandis que l’Académie française s’efforce de maintenir les normes du français ; le chinois, influencé par l’Occident, évolue aussi dans sa syntaxe et son vocabulaire. L’évolution des langues est inéluctable.",
        "Dans ce logiciel, j’avais d’abord voulu développer une fonction « Xiaoxue » (philologie classique) : partir du Classique des Mille Caractères pour apprendre la phonologie, former des mots selon les tons, composer des couplets, puis écrire des phrases et des poèmes. Malheureusement, je ne connais pas moi-même la prosodie, aussi ai-je supprimé cette fonction."
    ),
    credit = "Glyphes des icônes de lancement : hanzi-glyphs-svg (tracés Handian CC0 ; 1 203 squelettes oracle JGW CC BY 4.0, Li Qing-sheng et Bian Yu-lin, DOI 10.5281/zenodo.22467946).",
    linksTitle = "Liens du projet",
    repoLabel = "Dépôt",
    githubLabel = "yuzlyn"
)

package com.yuzlyn.mdeditor.data.book

/**
 * 一套拿來讀的實際配色：底紙、正文、連結。
 *
 * 只有 Long，沒有 Compose 型別——`ChapterHtml` 要把這三個值寫進 CSS，
 * 那是純 JVM 的邏輯，配色也就能跟著排版一起進單元測試。
 */
data class ReadingColors(
    val background: Long,
    val foreground: Long,
    val link: Long,
    /** 深底淺字。網頁要宣告 color-scheme，捲軸與內建控件才會跟著轉深。 */
    val dark: Boolean = false
) {
    companion object {
        /** 拿不到莫奈配色時的退路（純 JVM 測試、或主題還沒準備好的一瞬）。 */
        val PAPER = ReadingColors(0xFFFFFF, 0x1F1D1B, 0x2E6DA4)
    }
}

/**
 * 閱讀主題：**身分**，不是顏色。
 *
 * 這裡只留 id、名稱、以及「這套是深底還是淺底」；真正的三個顏色由
 * [ReadingColors] 從外面帶進來。因為配色已經改成莫奈：同一套主題在不同壁紙、
 * 不同種子色下本來就是不同顏色，寫死在 enum 裡只會和 App 打架——
 * 介面是藍的、書卻是米黃的，看起來像兩個 App。
 *
 * [DEFAULT] 就是「現在的莫奈配色」本身：底紙用 App 的 surface、正文用 onSurface，
 * 翻開書就是 App 的一頁，而不是另一套紙。
 */
enum class ReadingTheme(
    val id: String,
    val labelZh: String,
    val labelEn: String,
    /** 這套主題偏深色。只用來挑派生配色時要淺色還是深色方案。 */
    val prefersDark: Boolean
) {
    DEFAULT("default", "默认", "Default", false),
    PAPER("paper", "素白", "Plain", false),
    CREAM("cream", "淡彩", "Tinted", false),
    NIGHT("night", "夜色", "Dim", true),
    INK("ink", "墨黑", "Ink", true);

    fun style(
        colors: ReadingColors,
        textScale: Int = 100,
        vertical: Boolean = false,
        margin: Int = 20,
        lineHeight: Float = 1.7f,
        mode: ReaderMode = ReaderMode.PAGED,
        cornerQuotes: Boolean = false,
        fontFamilyCss: String = ""
    ): ReaderStyle = ReaderStyle(
        background = colors.background,
        foreground = colors.foreground,
        link = colors.link,
        textScale = textScale,
        vertical = vertical,
        margin = margin,
        lineHeight = lineHeight,
        mode = mode,
        dark = colors.dark,
        cornerQuotes = cornerQuotes,
        fontFamilyCss = fontFamilyCss
    )

    companion object {
        /**
         * 依 id 取主題。
         *
         * 認不得的 id 一律回 [DEFAULT]：舊版存的是 paper/cream/…，這些 id 都還在；
         * 真的遇到不認識的值（改過名字、手改過設定檔），跟著莫奈走最不容易出錯。
         */
        fun fromId(id: String?): ReadingTheme =
            entries.firstOrNull { it.id == id } ?: LEGACY[id] ?: DEFAULT

        /**
         * 舊 id 的落點。
         *
         * 改版前「青瓷」是一套綠色紙；莫奈化之後綠色紙不再成立，但直接把它
         * 推回「預設」會讓老使用者覺得書突然變了樣，所以接到同樣是淺色的「淡彩」。
         */
        private val LEGACY = mapOf("celadon" to CREAM)

        /** 依介面語言挑名稱；沒有對應翻譯就用英文。 */
        fun label(theme: ReadingTheme, chinese: Boolean): String =
            if (chinese) theme.labelZh else theme.labelEn
    }
}

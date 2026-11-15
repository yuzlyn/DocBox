package com.yuzlyn.mdeditor.data.book

/**
 * 把使用者匯入的字型接進閱讀器的 CSS。
 *
 * 為什麼要 `!important`：出版轉檔的書十之八九在 `body`、`p` 或 `font` 標籤上
 * 寫死 font-family，不蓋就等於沒選。但 `pre`／`code` 刻意不蓋——程式碼區塊
 * 用等寬字才對得齊，那是內容的一部分，不是「這本書的字型」。
 */
object ReaderFontCss {

    /** 覆蓋用的 family 名稱。用雙底線開頭，避免撞到書自己的字型名。 */
    private const val FAMILY = "__rd_custom_font"

    fun css(fileName: String): String {
        if (fileName.isBlank() || fileName == "system") return ""
        val url = ReaderPaths.ORIGIN + ReaderPaths.fontFile(fileName)
        return """
            @font-face {
              font-family: "$FAMILY";
              src: url("$url");
              font-display: block;
            }
            body, body p, body div, body span, body li, body td, body th,
            body h1, body h2, body h3, body h4, body h5, body h6,
            body blockquote, body dd, body dt, body section, body article, body font {
              font-family: "$FAMILY", serif !important;
            }
        """.trimIndent()
    }
}

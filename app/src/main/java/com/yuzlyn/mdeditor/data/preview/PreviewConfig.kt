package com.yuzlyn.mdeditor.data.preview

/**
 * md-render 的 config.json 在应用内的对应物。
 * flavor 对应 rich-text skill 的 commonmark / github 语法矩阵。
 */
data class PreviewConfig(
    val fontScale: Float = 1f,
    val showToc: Boolean = true,
    val syntaxHighlight: Boolean = true,
    val headingAnchors: Boolean = false,
    val math: Boolean = true,
    /**
     * 中文彎引號 “” 改排直角引號「」。
     *
     * 與閱讀器那個開關**各自獨立**：筆記是自己寫的、書是別人的成品，
     * 兩邊的口味不一定一樣。只改預覽的顯示，Markdown 原檔一個字都不動。
     */
    val cornerQuotes: Boolean = true,
    val flavor: PreviewFlavor = PreviewFlavor.GITHUB
)

enum class PreviewFlavor {
    COMMONMARK,
    GITHUB
}

val FontScaleOptions = listOf(
    0.9f to "小",
    1f to "標準",
    1.15f to "大",
    1.3f to "超大"
)

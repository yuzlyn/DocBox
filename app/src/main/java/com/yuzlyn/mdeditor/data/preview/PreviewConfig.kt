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

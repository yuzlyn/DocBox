package com.yuzlyn.mdeditor.data.export

/**
 * 匯出格式。MIME 與副檔名放在一起，分享與「儲存副本」共用同一份定義。
 */
enum class NoteExportFormat(
    val label: String,
    val extension: String,
    val mimeType: String
) {
    MARKDOWN("Markdown", "md", "text/markdown"),
    TEXT("純文字", "txt", "text/plain"),
    WORD(
        "Word 文件",
        "docx",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    ),
    PDF("PDF 文件", "pdf", "application/pdf"),
    IMAGE("圖片", "png", "image/png")
}

/**
 * 列級規則：一定要用「水平空白」而不是 `\s`。
 * `\s*` 會把上一行結尾的換行也吃掉，清單項目一多就會少一個空行。
 */
private val HeadingLine = Regex("""^[ \t]{0,3}#{1,6}[ \t]+""")
private val QuoteLine = Regex("""^[ \t]{0,3}>[ \t]?""")
private val ListItem = Regex("""^[ \t]*[-*+][ \t]+""")
private val TableSeparator = Regex("""^[ \t]*\|?[ \t:|-]+\|[ \t:|-]*$""")
private val HorizontalRule = Regex("""^([-*_][ \t]*){3,}$""")

private val ImageSyntax = Regex("""!\[([^\]]*)]\([^)]*\)""")
private val LinkSyntax = Regex("""\[([^\]]*)]\([^)]*\)""")
private val InlineCode = Regex("""`([^`]*)`""")
private val BoldStar = Regex("""\*\*(.+?)\*\*""")
private val BoldUnderscore = Regex("""__(.+?)__""")
private val Strike = Regex("""~~(.+?)~~""")
private val Highlight = Regex("""==(.+?)==""")
private val ItalicStar = Regex("""(?<![\w*])\*(?!\s)([^*\n]+?)(?<!\s)\*(?![\w*])""")
private val ItalicUnderscore = Regex("""(?<![\w_])_(?!\s)([^_\n]+?)(?<!\s)_(?![\w_])""")

/**
 * 把 Markdown 清成純文字。
 *
 * 逐行處理，不是整篇套 regex：行級規則（標題、清單、表格分隔列）一旦用
 * 多行 regex，很容易把換行一起吃進去，清單或表格就會少一行空白。
 * Word／PDF／圖片都不吃 Markdown 記號，直接印出 `**粗體**` 會很醜。
 */
internal fun markdownToPlainText(markdown: String): String {
    val cleaned = markdown.lineSequence().mapNotNull { raw ->
        val line = raw.trimEnd()
        when {
            line.trimStart().startsWith("```") -> null
            TableSeparator.matches(line) -> null
            HorizontalRule.matches(line.trim()) -> null
            else -> {
                var text = line
                    .replace(HeadingLine, "")
                    .replace(QuoteLine, "")
                    .replace(ListItem, "・")
                text = ImageSyntax.replace(text, "$1")
                text = LinkSyntax.replace(text, "$1")
                text = InlineCode.replace(text, "$1")
                text = BoldStar.replace(text, "$1")
                text = BoldUnderscore.replace(text, "$1")
                text = Strike.replace(text, "$1")
                text = Highlight.replace(text, "$1")
                text = ItalicStar.replace(text, "$1")
                text = ItalicUnderscore.replace(text, "$1")
                text
            }
        }
    }.joinToString("\n")

    // 收掉替換後留下的多餘空白行（最多保留一個空行）。
    return cleaned.replace(Regex("""\n{3,}"""), "\n\n").trim()
}

/** 檔名：去掉各平台不接受的字元，並限制長度，避免 SAF 直接拒收。 */
internal fun sanitizeFileName(raw: String, fallback: String = "note"): String {
    val cleaned = raw
        .replace(Regex("""[\\/:*?"<>|\n\r\t]"""), "_")
        .trim()
        .trim('.')
        .take(40)
    return cleaned.ifBlank { fallback }
}

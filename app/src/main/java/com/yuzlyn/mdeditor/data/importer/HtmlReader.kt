package com.yuzlyn.mdeditor.data.importer

/**
 * HTML → Markdown。
 *
 * 匯入 HTML／EPUB 時不能只是把標籤扒掉留下一團文字——標題、清單、連結、圖片、
 * 程式碼區塊都是結構資訊，直接丟掉會讓匯入結果變成無法編輯的麵條。
 * 這裡做的是「保留結構的降級」：能對應到 Markdown 的就轉過去，對應不到的才退成純文字。
 *
 * 設計上刻意不用完整的 HTML 解析器：
 * - 電子書章節動輒上百 KB，正則掃一遍比建 DOM 快一個量級；
 * - 匯入只需要「大致正確」，遇到破損 HTML 也要能吐出東西，而不是整個失敗。
 */
object HtmlReader {

    fun toMarkdown(html: String): String {
        var text = html.replace("\r\n", "\n").replace('\r', '\n')

        // 1) 先把程式碼區塊整段抽走：裡面的空白、標籤、星號都不能被後面的規則動到。
        val codeBlocks = mutableListOf<String>()
        text = Regex("""(?is)<pre\b[^>]*>(.*?)</pre>""").replace(text) { match ->
            val inner = match.groupValues[1]
            val language = LANGUAGE.find(inner)?.groupValues?.get(1)?.lowercase()
            val body = decodeEntities(inner.replace(TAG, ""))
                .lineSequence()
                .joinToString("\n") { it.trimEnd() }
                .trim('\n')
            codeBlocks += "```" + (language ?: "") + "\n" + body + "\n```"
            "\n\n\u0000CODE${codeBlocks.lastIndex}\u0000\n\n"
        }

        // 2) 整段丟掉的：head／script／style／註解／noscript。
        text = Regex("""(?is)<head\b[^>]*>.*?</head>""").replace(text, "")
        text = Regex("""(?is)<(script|style|noscript)\b[^>]*>.*?</\1>""").replace(text, "")
        text = Regex("""(?s)<!--.*?-->""").replace(text, "")

        // 3) 表格要先處理：等到 <td> 被當成一般標籤扒掉就分不出欄了。
        text = TABLE.replace(text) { match -> tableToMarkdown(match.groupValues[1]) }

        // 4) 標題。
        text = Regex("""(?is)<h([1-6])\b[^>]*>(.*?)</h\1>""").replace(text) { match ->
            val level = match.groupValues[1].toInt()
            val body = match.groupValues[2].trim()
            "\n\n" + "#".repeat(level) + " " + body + "\n\n"
        }

        // 6) 有序清單先編號，再處理剩下的無序清單。
        text = Regex("""(?is)<ol\b[^>]*>(.*?)</ol>""").replace(text) { match ->
            var index = 0
            val body = Regex("""(?is)<li\b[^>]*>(.*?)</li>""").replace(match.groupValues[1]) { item ->
                index++
                "\n$index. " + item.groupValues[1].trim()
            }
            "\n\n" + body.trim() + "\n\n"
        }
        text = Regex("""(?i)<li\b[^>]*>""").replace(text, "\n- ")
        text = Regex("""(?i)</li>""").replace(text, "")

        // 7) 行內樣式。
        text = Regex("""(?is)<(strong|b)\b[^>]*>(.*?)</\1>""").replace(text) { "**${it.groupValues[2].trim()}**" }
        text = Regex("""(?is)<(em|i)\b[^>]*>(.*?)</\1>""").replace(text) { "*${it.groupValues[2].trim()}*" }
        text = Regex("""(?is)<(del|s|strike)\b[^>]*>(.*?)</\1>""").replace(text) { "~~${it.groupValues[2].trim()}~~" }
        text = Regex("""(?is)<mark\b[^>]*>(.*?)</mark>""").replace(text) { "==${it.groupValues[1].trim()}==" }
        text = Regex("""(?is)<sup\b[^>]*>(.*?)</sup>""").replace(text) { "^${it.groupValues[1].trim()}^" }
        text = Regex("""(?is)<sub\b[^>]*>(.*?)</sub>""").replace(text) { "~${it.groupValues[1].trim()}~" }
        text = Regex("""(?is)<code\b[^>]*>(.*?)</code>""").replace(text) {
            "`" + it.groupValues[1].replace("`", "ˋ").trim() + "`"
        }
        // <u> 沒有 Markdown 對應語法，但預覽支援內聯 HTML，直接留著比丟掉好。
        text = Regex("""(?is)</?u\b[^>]*>""").replace(text, "")
        text = text.replace(Regex("""(?is)<a\b[^>]*href\s*=\s*["']([^"']*)["'][^>]*>(.*?)</a>""")) { match ->
            val href = match.groupValues[1].trim()
            val label = match.groupValues[2].trim()
            if (href.isEmpty() || href.startsWith("javascript:", true) || label.isEmpty()) {
                label
            } else {
                "[$label]($href)"
            }
        }
        text = Regex("""(?is)<img\b[^>]*>""").replace(text) { match ->
            val tag = match.value
            val src = ATTR_SRC.find(tag)?.groupValues?.get(1)?.trim().orEmpty()
            val alt = ATTR_ALT.find(tag)?.groupValues?.get(1)?.trim().orEmpty()
            // data: 圖片塞進 Markdown 會變成幾百 KB 的亂碼，直接放棄。
            if (src.isEmpty() || src.startsWith("data:", true)) "" else "![$alt]($src)"
        }

        // 8) 引用要放在行內樣式之後：這樣 > 裡面也能保留粗體、連結。
        text = Regex("""(?is)<blockquote\b[^>]*>(.*?)</blockquote>""").replace(text) { match ->
            val body = match.groupValues[1]
                .replace(BLOCK_TAG, "\n")
                .replace(TAG, "")
                .lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (body.isEmpty()) "" else "\n\n" + body.joinToString("\n") { "> $it" } + "\n\n"
        }

        // 9) 區塊邊界 → 換行；其餘標籤（div/section/span/p/…）扒掉只留內容。
        text = Regex("""(?i)<(?:br|hr)\s*/?>""").replace(text, "\n")
        text = BLOCK_TAG.replace(text, "\n\n")
        text = TAG.replace(text, "")

        // 10) 實體還原（放在扒標籤之後，免得還原出的 < 又被當成標籤）。
        text = decodeEntities(text)

        // 11) 把程式碼區塊放回去。
        text = Regex("""\u0000CODE(\d+)\u0000""").replace(text) { match ->
            codeBlocks.getOrNull(match.groupValues[1].toInt()) ?: ""
        }

        return normalize(text)
    }

    private fun tableToMarkdown(inner: String): String {
        val rows = Regex("""(?is)<tr\b[^>]*>(.*?)</tr>""").findAll(inner).map { row ->
            Regex("""(?is)<t([dh])\b[^>]*>(.*?)</t\1>""").findAll(row.groupValues[1]).map { cell ->
                decodeEntities(cell.groupValues[2].replace(TAG, ""))
                    .replace("|", "\\|")
                    .replace(Regex("""\s+"""), " ")
                    .trim()
            }.toList()
        }.filter { it.isNotEmpty() }.toList()

        if (rows.isEmpty()) return ""
        val columns = rows.maxOf { it.size }
        return buildString {
            append("\n\n")
            rows.forEachIndexed { index, row ->
                append("| ")
                append((0 until columns).joinToString(" | ") { row.getOrElse(it) { "" } })
                append(" |\n")
                if (index == 0) {
                    append("| ")
                    append((0 until columns).joinToString(" | ") { "---" })
                    append(" |\n")
                }
            }
            append('\n')
        }
    }

    private fun normalize(text: String): String {
        val lines = text.replace("\u00A0", " ").split('\n').map { line ->
            if (line.startsWith("    ") || line.startsWith("\t")) line.trimEnd() else line.trim()
        }
        val builder = StringBuilder()
        var blanks = 0
        lines.forEach { line ->
            if (line.isEmpty()) {
                blanks++
                if (blanks <= 1) builder.append('\n')
            } else {
                blanks = 0
                builder.append(line).append('\n')
            }
        }
        return builder.toString().trim()
    }

    internal fun decodeEntities(text: String): String {
        var result = text
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&hellip;", "…")
            .replace("&amp;", "&")
        result = Regex("""&#(\d+);""").replace(result) { match ->
            val code = match.groupValues[1].toIntOrNull() ?: return@replace match.value
            runCatching { String(Character.toChars(code)) }.getOrDefault(match.value)
        }
        result = Regex("""&#x([0-9a-fA-F]+);""").replace(result) { match ->
            val code = match.groupValues[1].toIntOrNull(16) ?: return@replace match.value
            runCatching { String(Character.toChars(code)) }.getOrDefault(match.value)
        }
        return result
    }

    private val TAG = Regex("""(?s)<[^>]+>""")
    private val TABLE = Regex("""(?is)<table\b[^>]*>(.*?)</table>""")
    private val BLOCK_TAG = Regex(
        """(?i)</?(?:p|div|section|article|main|header|footer|figure|figcaption|dl|dt|dd|ul|ol|h[1-6])\b[^>]*>"""
    )
    private val LANGUAGE = Regex("""(?i)language-([\w+#.-]+)""")
    private val ATTR_SRC = Regex("""(?i)\bsrc\s*=\s*["']([^"']*)["']""")
    private val ATTR_ALT = Regex("""(?i)\balt\s*=\s*["']([^"']*)["']""")
}

package com.yuzlyn.mdeditor.data.book

/**
 * 彎引號 “” 改排成直角引號「」。
 *
 * 中文排版的引號傳統上是「」與『』；彎引號是西文標點，夾在漢字裡會留出半形空白、
 * 行首行尾也對不齊。這裡只改**顯示**這一步：書檔一個位元組都不動，
 * 關掉設定就回到原樣，匯出、複製、搜尋也都不受影響。
 *
 * 三條必須守住的邊界：
 *
 * 1. **只碰文字**。`alt="說「」"` 這種屬性值、`<style>` 裡的 `content: ""`、
 *    註解、腳本都不是正文，改了會讓替代文字、錨點或樣式跑掉。所以這裡用一個
 *    最小標籤掃描器，而不是直接對整份 HTML 做字串替換。
 * 2. **只碰中文**。判斷以**整章**為單位：這一章裡有漢字（或日文假名）才動手，
 *    純西文書整章原樣放過。不逐節點判斷，是因為中文書裡引用英文原句很常見，
 *    逐節點會出現「同一段前一個引號換了、後一個沒換」的半吊子結果。
 * 3. **實體也算**。EPUB 很常見 `&ldquo;` / `&#8220;` 這種寫法，與字面引號一視同仁。
 */
internal object CornerQuotes {

    private const val CURLY_OPEN = '\u201C'
    private const val CURLY_CLOSE = '\u201D'
    private const val CORNER_OPEN = '「'
    private const val CORNER_CLOSE = '」'

    /** 兩個成對的引號實體，含十進位與十六進位寫法。 */
    private val QUOTE_ENTITY = Regex(
        """&(ldquo|rdquo|#8220|#8221|#x201c|#x201d);""",
        RegexOption.IGNORE_CASE
    )

    /**
     * 轉換 [html] 裡的正文引號；標籤、屬性、`<style>` / `<script>` 區塊原樣保留。
     *
     * 沒有任何可轉的字元、或整章沒有一個東亞文字（西文書）時，直接回傳原字串。
     */
    fun apply(html: String): String {
        if (!needsRewrite(html) || !hasEastAsian(html)) return html
        val out = StringBuilder(html.length + 16)
        var index = 0
        while (index < html.length) {
            val tagStart = html.indexOf('<', index)
            if (tagStart < 0) {
                appendText(out, html, index, html.length)
                break
            }
            if (tagStart > index) appendText(out, html, index, tagStart)
            index = when {
                html.startsWith("<!--", tagStart) -> {
                    val end = html.indexOf("-->", tagStart)
                    val stop = if (end < 0) html.length else end + 3
                    out.append(html, tagStart, stop)
                    stop
                }
                else -> copyTag(out, html, tagStart)
            }
        }
        return out.toString()
    }

    private fun needsRewrite(html: String): Boolean =
        html.indexOf(CURLY_OPEN) >= 0 ||
            html.indexOf(CURLY_CLOSE) >= 0 ||
            html.contains("&ldquo", ignoreCase = true) ||
            html.contains("&rdquo", ignoreCase = true) ||
            html.contains("&#8220", ignoreCase = true) ||
            html.contains("&#8221", ignoreCase = true) ||
            html.contains("&#x201c", ignoreCase = true) ||
            html.contains("&#x201d", ignoreCase = true)

    /**
     * 複製一個標籤（屬性值裡的 `>` 不算結束），回傳下一個要處理的位置。
     *
     * `<style>` / `<script>` 是原始文字元素，內容不是正文，整段跳過。
     */
    private fun copyTag(out: StringBuilder, html: String, start: Int): Int {
        var index = start + 1
        var quote = '\u0000'
        while (index < html.length) {
            val ch = html[index]
            if (quote != '\u0000') {
                if (ch == quote) quote = '\u0000'
            } else if (ch == '"' || ch == '\'') {
                quote = ch
            } else if (ch == '>') {
                index++
                break
            }
            index++
        }
        out.append(html, start, index)
        if (index >= html.length) return index
        val name = tagName(html, start, index)
        if (name != "style" && name != "script") return index
        val close = html.indexOf("</$name", index, ignoreCase = true)
        if (close < 0) return index
        out.append(html, index, close)
        return close
    }

    private fun tagName(html: String, start: Int, end: Int): String {
        var index = start + 1
        if (index < end && html[index] == '/') index++
        val from = index
        while (index < end) {
            val ch = html[index]
            if (ch == '>' || ch == '/' || ch.isWhitespace()) break
            index++
        }
        return html.substring(from, index).lowercase()
    }

    /** 一個文本節點：只換引號本身，其餘字元原樣寫回。 */
    private fun appendText(out: StringBuilder, html: String, start: Int, end: Int) {
        val raw = html.substring(start, end)
        val converted = raw
            .replace(CURLY_OPEN, CORNER_OPEN)
            .replace(CURLY_CLOSE, CORNER_CLOSE)
        if (!QUOTE_ENTITY.containsMatchIn(converted)) {
            out.append(converted)
            return
        }
        out.append(
            QUOTE_ENTITY.replace(converted) { match ->
                val name = match.groupValues[1].lowercase()
                if (name == "ldquo" || name == "#8220" || name == "#x201c") {
                    CORNER_OPEN.toString()
                } else {
                    CORNER_CLOSE.toString()
                }
            }
        )
    }

    /**
     * 這段內容有漢字或日文假名嗎。
     *
     * 假名也算：日文的標準引號本來就是「」，換過去只會更對，不會更錯。
     */
    private fun hasEastAsian(text: String): Boolean = text.any { ch ->
        val code = ch.code
        code in 0x3400..0x4DBF || // 擴充 A
            code in 0x4E00..0x9FFF || // 基本區
            code in 0xF900..0xFAFF || // 相容表意文字
            code in 0x3040..0x30FF // 平假名、片假名
    }
}

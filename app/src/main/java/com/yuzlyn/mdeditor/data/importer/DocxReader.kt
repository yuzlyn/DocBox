package com.yuzlyn.mdeditor.data.importer

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 把 .docx 轉成 Markdown。
 *
 * 只讀 `word/document.xml`，不碰樣式表與圖片：
 * - 標題（`w:pStyle` Heading1-6、`Title`、`w:outlineLvl`）→ `#` 開頭
 * - 清單（`w:numPr`）→ `- `
 * - 粗體／斜體（`w:rPr/w:b`、`w:i`）→ `**`、`*`
 * - 表格（`w:tbl`）→ Markdown 表格
 *
 * 這樣匯入後就是可以繼續編輯的 Markdown，再匯出回 Word 也不會走鐘太多。
 */
object DocxReader {

    private const val W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"

    /** 只認 ZIP 開頭（PK\x03\x04）；後面交給解析器判斷。 */
    fun looksLikeDocx(bytes: ByteArray): Boolean =
        bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()

    /** @return Markdown 內容；不是合法的 docx 時回 null。 */
    fun toMarkdown(bytes: ByteArray): String? {
        val xml = readDocumentXml(bytes) ?: return null
        val body = parseBody(xml) ?: return null

        val blocks = mutableListOf<String>()
        for (index in 0 until body.childNodes.length) {
            val node = body.childNodes.item(index)
            if (node !is Element || node.namespaceURI != W_NS) continue
            when (node.localName) {
                "p" -> blocks += paragraphMarkdown(node)
                "tbl" -> blocks += tableMarkdown(node)
            }
        }
        return joinBlocks(blocks).ifBlank { null }
    }

    private fun readDocumentXml(bytes: ByteArray): String? {
        if (!looksLikeDocx(bytes)) return null
        return runCatching {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var found: String? = null
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.name == "word/document.xml") {
                        found = zip.readBytes().toString(Charsets.UTF_8)
                        break
                    }
                }
                found
            }
        }.getOrNull()
    }

    private fun parseBody(xml: String): Element? = runCatching {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            // 不解析外部實體：檔案來自使用者選的任何地方，沒必要讓它去讀本機路徑。
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        }
        // XML 宣告前面不能有空白或 BOM；有些工具產生的檔案會夾帶，這裡先削掉。
        val cleaned = xml.trimStart('\uFEFF', ' ', '\t', '\n', '\r')
        val document = factory.newDocumentBuilder()
            .parse(ByteArrayInputStream(cleaned.toByteArray(Charsets.UTF_8)))
        val bodies = document.getElementsByTagNameNS(W_NS, "body")
        bodies.item(0) as? Element
    }.getOrNull()

    private fun paragraphMarkdown(paragraph: Element): String {
        val properties = paragraph.firstChildElement("pPr")
        val style = properties?.firstChildElement("pStyle")
            ?.getAttributeNS(W_NS, "val")
            .orEmpty()
        val outline = properties?.firstChildElement("outlineLvl")
            ?.getAttributeNS(W_NS, "val")
            ?.toIntOrNull()
        val isListItem = properties?.firstChildElement("numPr") != null

        val text = runsText(paragraph)
        if (text.isBlank()) return ""

        val level = when {
            style.startsWith("Heading") -> style.removePrefix("Heading").toIntOrNull() ?: 1
            style == "Title" -> 1
            outline != null -> outline + 1
            else -> 0
        }
        return when {
            level > 0 -> "#".repeat(level.coerceIn(1, 6)) + " " + text
            isListItem -> "- $text"
            else -> text
        }
    }

    /** 逐個 run 取文字，粗體／斜體轉成 Markdown 記號。 */
    private fun runsText(paragraph: Element): String = buildString {
        val runs = paragraph.getElementsByTagNameNS(W_NS, "r")
        for (index in 0 until runs.length) {
            val run = runs.item(index) as? Element ?: continue
            val properties = run.firstChildElement("rPr")
            val bold = properties?.flag("b") == true
            val italic = properties?.flag("i") == true

            val text = StringBuilder()
            for (child in 0 until run.childNodes.length) {
                val node = run.childNodes.item(child)
                if (node.nodeType != Node.ELEMENT_NODE || node.namespaceURI != W_NS) continue
                when (node.localName) {
                    "t" -> text.append(node.textContent)
                    "tab" -> text.append('\t')
                    "br" -> text.append('\n')
                }
            }
            if (text.isEmpty()) continue

            append(
                when {
                    bold && italic -> "***$text***"
                    bold -> "**$text**"
                    italic -> "*$text*"
                    else -> text
                }
            )
        }
    }.trim()

    private fun tableMarkdown(table: Element): String {
        val rows = table.getElementsByTagNameNS(W_NS, "tr")
        if (rows.length == 0) return ""
        val cells = mutableListOf<List<String>>()
        for (rowIndex in 0 until rows.length) {
            val row = rows.item(rowIndex) as? Element ?: continue
            val cellNodes = row.getElementsByTagNameNS(W_NS, "tc")
            val values = mutableListOf<String>()
            for (cellIndex in 0 until cellNodes.length) {
                val cell = cellNodes.item(cellIndex) as? Element ?: continue
                val paragraphNodes = cell.getElementsByTagNameNS(W_NS, "p")
                val text = buildString {
                    for (p in 0 until paragraphNodes.length) {
                        val paragraph = paragraphNodes.item(p) as? Element ?: continue
                        val value = runsText(paragraph)
                        if (value.isNotEmpty()) {
                            if (isNotEmpty()) append(' ')
                            append(value)
                        }
                    }
                }.replace("|", "\\|")
                values += text
            }
            if (values.isNotEmpty()) cells += values
        }
        if (cells.isEmpty()) return ""

        val columns = cells.maxOf { it.size }
        val out = StringBuilder()
        cells.forEachIndexed { index, row ->
            out.append("| ")
            for (column in 0 until columns) {
                out.append(row.getOrElse(column) { "" })
                out.append(if (column == columns - 1) " |" else " | ")
            }
            out.append('\n')
            if (index == 0) {
                out.append("| ")
                repeat(columns) { column ->
                    out.append("---")
                    out.append(if (column == columns - 1) " |" else " | ")
                }
                out.append('\n')
            }
        }
        return out.toString().trimEnd()
    }

    /** 連續的清單項目併成同一段，其餘區塊之間留一個空行。 */
    private fun joinBlocks(blocks: List<String>): String {
        val out = StringBuilder()
        var previousWasListItem = false
        blocks.forEach { block ->
            if (block.isBlank()) return@forEach
            val isListItem = block.startsWith("- ")
            when {
                out.isEmpty() -> out.append(block)
                isListItem && previousWasListItem -> out.append('\n').append(block)
                else -> out.append("\n\n").append(block)
            }
            previousWasListItem = isListItem
        }
        return out.toString().trim()
    }

    private fun Element.firstChildElement(localName: String): Element? {
        for (index in 0 until childNodes.length) {
            val node = childNodes.item(index)
            if (node is Element && node.namespaceURI == W_NS && node.localName == localName) {
                return node
            }
        }
        return null
    }

    /** `w:b` 這種開關屬性：沒有 val 就是 true，`w:val="0"` 才是 false。 */
    private fun Element.flag(localName: String): Boolean {
        val element = firstChildElement(localName) ?: return false
        val value = element.getAttributeNS(W_NS, "val")
        return value.isEmpty() || value == "true" || value == "1"
    }
}

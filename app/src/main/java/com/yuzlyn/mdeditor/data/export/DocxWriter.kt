package com.yuzlyn.mdeditor.data.export

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Word 段落；[headingLevel] 1～3 表示標題，0 表示正文。 */
internal data class DocxBlock(val text: String, val headingLevel: Int = 0)

/**
 * 產生最基本的 .docx（OOXML）。
 *
 * 只用到三個部件：`[Content_Types].xml`、`_rels/.rels`、`word/document.xml`。
 * Word、WPS、Google Docs 都能開啟；沒有樣式表，標題用粗體＋字級直接標在 run 上，
 * 這樣就不必再維護一份 styles.xml。
 */
internal object DocxWriter {

    fun build(blocks: List<DocxBlock>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(CONTENT_TYPES.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(ROOT_RELS.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write(documentXml(blocks).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    private fun documentXml(blocks: List<DocxBlock>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
        append("<w:body>")
        blocks.forEach { block ->
            append("<w:p>")
            when {
                block.headingLevel > 0 -> {
                    val size = when (block.headingLevel) {
                        1 -> 36
                        2 -> 32
                        else -> 28
                    }
                    append("""<w:pPr><w:spacing w:before="240" w:after="120"/></w:pPr>""")
                    append("<w:r><w:rPr><w:b/><w:sz w:val=\"$size\"/></w:rPr>")
                    append("""<w:t xml:space="preserve">${escapeXml(block.text)}</w:t>""")
                    append("</w:r>")
                }

                block.text.isNotEmpty() -> {
                    append("""<w:r><w:t xml:space="preserve">${escapeXml(block.text)}</w:t></w:r>""")
                }

                else -> Unit // 空行只留一個空段落
            }
            append("</w:p>")
        }
        append(
            """<w:sectPr><w:pgSz w:w="11906" w:h="16838"/>""" +
                    """<w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" """ +
                    """w:header="720" w:footer="720" w:gutter="0"/></w:sectPr>"""
        )
        append("</w:body></w:document>")
    }

    /** XML 只認得這五個實體；控制字元會讓 Word 直接判定檔案損毀，一併濾掉。 */
    fun escapeXml(value: String): String = buildString(value.length) {
        value.forEach { ch ->
            when {
                ch == '&' -> append("&amp;")
                ch == '<' -> append("&lt;")
                ch == '>' -> append("&gt;")
                ch == '"' -> append("&quot;")
                ch == '\'' -> append("&apos;")
                ch.code < 0x20 && ch != '\t' -> Unit
                else -> append(ch)
            }
        }
    }

    private const val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

    private const val ROOT_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
}

/** Markdown → Word 段落：標題保留層級，其餘把行內記號清掉。 */
internal fun markdownToDocxBlocks(markdown: String): List<DocxBlock> =
    markdown.lineSequence().map { raw ->
        val line = raw.trimEnd()
        val level = when {
            line.startsWith("### ") -> 3
            line.startsWith("## ") -> 2
            line.startsWith("# ") -> 1
            else -> 0
        }
        val text = if (level > 0) {
            markdownToPlainText(line.drop(level + 1))
        } else {
            markdownToPlainText(line)
        }
        DocxBlock(text, level)
    }.toList()

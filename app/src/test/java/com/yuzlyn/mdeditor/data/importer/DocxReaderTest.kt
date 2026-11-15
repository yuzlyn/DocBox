package com.yuzlyn.mdeditor.data.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * .docx → Markdown 的解析規則。
 * 這些案例直接對應 Word 實際產生的 XML 結構（w:p / w:rPr / w:tbl）。
 */
class DocxReaderTest {

    @Test
    fun `標題與段落轉成 Markdown`() {
        val markdown = DocxReader.toMarkdown(
            docx(
                """
                <w:p><w:pPr><w:pStyle w:val="Heading1"/></w:pPr><w:r><w:t>標題</w:t></w:r></w:p>
                <w:p><w:r><w:t>第一段</w:t></w:r></w:p>
                """.trimIndent()
            )
        )

        assertEquals("# 標題\n\n第一段", markdown)
    }

    @Test
    fun `二級標題與大綱層級都認得`() {
        val markdown = DocxReader.toMarkdown(
            docx(
                """
                <w:p><w:pPr><w:pStyle w:val="Heading2"/></w:pPr><w:r><w:t>小標</w:t></w:r></w:p>
                <w:p><w:pPr><w:outlineLvl w:val="2"/></w:pPr><w:r><w:t>大綱三</w:t></w:r></w:p>
                """.trimIndent()
            )
        )

        assertEquals("## 小標\n\n### 大綱三", markdown)
    }

    @Test
    fun `粗體與斜體轉成對應記號`() {
        val markdown = DocxReader.toMarkdown(
            docx(
                """
                <w:p>
                  <w:r><w:rPr><w:b/></w:rPr><w:t>粗體</w:t></w:r>
                  <w:r><w:t>普通</w:t></w:r>
                  <w:r><w:rPr><w:i/></w:rPr><w:t>斜體</w:t></w:r>
                </w:p>
                """.trimIndent()
            )
        )

        assertEquals("**粗體**普通*斜體*", markdown)
    }

    @Test
    fun `b 設成 0 不算粗體`() {
        val markdown = DocxReader.toMarkdown(
            docx(
                """
                <w:p><w:r><w:rPr><w:b w:val="0"/></w:rPr><w:t>不是粗體</w:t></w:r></w:p>
                """.trimIndent()
            )
        )

        assertEquals("不是粗體", markdown)
    }

    @Test
    fun `清單項目合成一段`() {
        val markdown = DocxReader.toMarkdown(
            docx(
                """
                <w:p><w:pPr><w:numPr/></w:pPr><w:r><w:t>甲</w:t></w:r></w:p>
                <w:p><w:pPr><w:numPr/></w:pPr><w:r><w:t>乙</w:t></w:r></w:p>
                """.trimIndent()
            )
        )

        assertEquals("- 甲\n- 乙", markdown)
    }

    @Test
    fun `表格轉成 Markdown 表格`() {
        val markdown = DocxReader.toMarkdown(
            docx(
                """
                <w:tbl>
                  <w:tr>
                    <w:tc><w:p><w:r><w:t>欄一</w:t></w:r></w:p></w:tc>
                    <w:tc><w:p><w:r><w:t>欄二</w:t></w:r></w:p></w:tc>
                  </w:tr>
                  <w:tr>
                    <w:tc><w:p><w:r><w:t>1</w:t></w:r></w:p></w:tc>
                    <w:tc><w:p><w:r><w:t>2</w:t></w:r></w:p></w:tc>
                  </w:tr>
                </w:tbl>
                """.trimIndent()
            )
        )

        assertEquals("| 欄一 | 欄二 |\n| --- | --- |\n| 1 | 2 |", markdown)
    }

    @Test
    fun `跳脫字元由 XML 還原`() {
        val markdown = DocxReader.toMarkdown(
            docx("""<w:p><w:r><w:t>A &amp; B &lt; C</w:t></w:r></w:p>""")
        )

        assertEquals("A & B < C", markdown)
    }

    @Test
    fun `不是 docx 時回傳 null`() {
        assertNull(DocxReader.toMarkdown("這不是壓縮檔".toByteArray()))
        assertFalse(DocxReader.looksLikeDocx(byteArrayOf(1, 2, 3, 4)))
    }

    @Test
    fun `空白文件回傳 null`() {
        assertNull(DocxReader.toMarkdown(docx("<w:p><w:r><w:t>  </w:t></w:r></w:p>")))
    }

    @Test
    fun `ZIP 開頭會被認成 docx`() {
        assertTrue(DocxReader.looksLikeDocx(docx("<w:p/>")))
    }

    private val W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"

    private fun docx(bodyXml: String): ByteArray {
        // 一定要直接拼接：raw string 的縮排會留在 XML 宣告前面，宣告就失效了。
        val document = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<w:document xmlns:w=\"$W_NS\"><w:body>$bodyXml</w:body></w:document>"
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write(document.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return out.toByteArray()
    }
}

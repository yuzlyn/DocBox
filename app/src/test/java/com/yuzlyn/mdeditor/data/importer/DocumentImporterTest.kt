package com.yuzlyn.mdeditor.data.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 匯入前的格式判斷。
 *
 * 這一步錯了，後面就是用錯的解析器渲染：HTML 被當純文字、EPUB 被當壓縮檔。
 */
class DocumentImporterTest {

    @Test
    fun `副檔名對應到正確格式`() {
        assertEquals(ImportFormat.DOCX, ImportFormat.fromFileName("報告.docx"))
        assertEquals(ImportFormat.PDF, ImportFormat.fromFileName("說明.PDF"))
        assertEquals(ImportFormat.MARKDOWN, ImportFormat.fromFileName("note.md"))
        assertEquals(ImportFormat.TEXT, ImportFormat.fromFileName("a.txt"))
        assertEquals(ImportFormat.EPUB, ImportFormat.fromFileName("book.epub"))
        assertEquals(ImportFormat.MOBI, ImportFormat.fromFileName("book.mobi"))
        assertEquals(ImportFormat.AZW3, ImportFormat.fromFileName("book.azw3"))
        assertEquals(ImportFormat.HTML, ImportFormat.fromFileName("page.html"))
        assertEquals(ImportFormat.HTML, ImportFormat.fromFileName("page.htm"))
    }

    @Test
    fun `沒有副檔名或認不得時回 null`() {
        assertNull(ImportFormat.fromFileName("README"))
        assertNull(ImportFormat.fromFileName(null))
        assertNull(ImportFormat.fromFileName("movie.mp4"))
    }

    @Test
    fun `id 可以還原成格式`() {
        ImportFormat.entries.forEach { format ->
            assertEquals(format, ImportFormat.fromId(format.id))
        }
        assertNull(ImportFormat.fromId(""))
        assertNull(ImportFormat.fromId("unknown"))
    }

    @Test
    fun `檔頭是 PDF 就認得`() {
        val bytes = "%PDF-1.7\n...".toByteArray()

        assertTrue(DocumentImporter.isPdf(bytes))
        assertEquals(ImportFormat.PDF, DocumentImporter.detect(bytes))
    }

    @Test
    fun `HTML 沒有副檔名也能認出來`() {
        val bytes = "<!DOCTYPE html><html><body>嗨</body></html>".toByteArray()

        assertTrue(DocumentImporter.looksLikeHtml(bytes))
        assertEquals(ImportFormat.HTML, DocumentImporter.detect(bytes))
    }

    @Test
    fun `ZIP 裡有 word 目錄就是 docx`() {
        val bytes = zip(mapOf("word/document.xml" to "<w:document/>"))

        assertEquals(ImportFormat.DOCX, DocumentImporter.detect(bytes))
    }

    @Test
    fun `EPUB 不再被當成可匯入的文件`() {
        val bytes = zip(
            mapOf(
                "META-INF/container.xml" to "<container/>",
                "OEBPS/a.xhtml" to "<html/>"
            )
        )

        // 書要走原生閱讀器；這裡若回報格式，就會被拆成 Markdown 筆記。
        assertNull(DocumentImporter.detect(bytes))
    }

    @Test
    fun `純文字認不出格式時回 null 由呼叫端當文字處理`() {
        assertNull(DocumentImporter.detect("一般的筆記內容".toByteArray()))
    }

    private fun zip(entries: Map<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }
}

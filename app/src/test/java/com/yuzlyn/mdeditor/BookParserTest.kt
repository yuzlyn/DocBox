package com.yuzlyn.mdeditor

import com.yuzlyn.mdeditor.data.book.Book
import com.yuzlyn.mdeditor.data.book.BookFormat
import com.yuzlyn.mdeditor.data.book.BookOpener
import com.yuzlyn.mdeditor.data.book.ChapterHtml
import com.yuzlyn.mdeditor.data.book.EpubBook
import com.yuzlyn.mdeditor.data.book.MobiBook
import com.yuzlyn.mdeditor.data.book.PalmDocDecoder
import com.yuzlyn.mdeditor.data.book.ReaderPaths
import com.yuzlyn.mdeditor.data.book.ReaderStyle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 原生電子書解析的驗證。
 *
 * 樣本由 `tools/make_test_books.py` 生成：真的 ZIP、真的 PalmDB、真的 KF8 索引表。
 * 這裡不 mock 任何東西 —— 解析器最怕的就是「單元測試全綠、真書全崩」。
 */
class BookParserTest {

    private fun sample(name: String): File {
        val url = javaClass.classLoader!!.getResource("books/$name")
        assertNotNull("缺少測試樣本 books/$name", url)
        return File(url!!.toURI())
    }

    @Test
    fun `EPUB 章節數與中繼資料`() = runBlocking {
        EpubBook.open(sample("sample.epub")).use { book ->
            // spine 裡有 4 項，但 notes.xhtml 是 linear="no"，不算閱讀序列。
            assertEquals(3, book.chapterCount)
            assertEquals("測試書名", book.metadata.title)
            assertEquals("測試作者", book.metadata.author)
            assertEquals("zh-TW", book.metadata.language)
            val first = book.readChapter(0)
            assertTrue(first.html.contains("第一章"))
            assertEquals("OEBPS/Text", first.basePath)
        }
    }

    @Test
    fun `EPUB 目錄展開巢狀層級`() = runBlocking {
        EpubBook.open(sample("sample.epub")).use { book ->
            assertEquals(4, book.toc.size)
            assertEquals("第一章", book.toc[0].title)
            assertEquals(0, book.toc[0].depth)
            assertEquals("第一節", book.toc[1].title)
            assertEquals(1, book.toc[1].depth)
            assertEquals(0, book.toc[1].chapter)
            assertEquals("s1", book.toc[1].anchor)
            assertEquals(1, book.toc[2].chapter)
        }
    }

    @Test
    fun `EPUB 相對路徑解析與資源讀取`() = runBlocking {
        EpubBook.open(sample("sample.epub")).use { book ->
            val key = book.resolveHref("OEBPS/Text", "../Images/pic.png")
            assertEquals("OEBPS/Images/pic.png", key)
            val resource = book.readResource(key!!)
            assertNotNull(resource)
            assertEquals("image/png", resource!!.mime)
            assertTrue(resource.bytes[0] == 0x89.toByte())
            // 外部網址不進閱讀器
            assertEquals(null, book.resolveHref("OEBPS/Text", "https://example.com"))
            // 章節連結要認得出來是哪一章
            assertEquals(1, book.chapterOfResource(book.resolveHref("OEBPS/Text", "ch2.xhtml")!!))
        }
    }

    @Test
    fun `EPUB 的 CSS 內 url 會被改寫`() = runBlocking {
        EpubBook.open(sample("sample.epub")).use { book ->
            val css = book.readResource("OEBPS/Styles/main.css")
            assertNotNull(css)
            val text = String(css!!.bytes, Charsets.UTF_8)
            assertTrue(text.contains(ReaderPaths.absoluteResource("OEBPS/Images/pic.png")))
        }
    }

    @Test
    fun `MOBI 分章與 recindex 改寫`() = runBlocking {
        MobiBook.open(sample("sample.mobi")).use { book ->
            assertEquals(BookFormat.MOBI, book.metadata.format)
            assertEquals("MOBI 样本", book.metadata.title)
            assertEquals(3, book.chapterCount)
            val first = book.readChapter(0)
            assertTrue(first.html.contains("这是第一章"))
            val second = book.readChapter(1)
            assertTrue(second.html.contains("这是第二章"))
            // recindex 要换成可用的 src
            assertTrue(second.html.contains("src=\"embed:1\""))
            assertEquals("embed:1", book.resolveHref("", "kindle:embed:0001?mime=image/png"))
        }
    }

    @Test
    fun `MOBI 圖片資源可以讀出來`() = runBlocking {
        MobiBook.open(sample("sample.mobi")).use { book ->
            val resource = book.readResource("embed:1")
            assertNotNull(resource)
            assertEquals("image/png", resource!!.mime)
            assertTrue(resource.bytes[0] == 0x89.toByte())
        }
    }

    @Test
    fun `AZW3 的骨架与碎片能拼回完整章节`() = runBlocking {
        MobiBook.open(sample("sample.azw3")).use { book ->
            assertEquals(BookFormat.AZW3, book.metadata.format)
            assertEquals(2, book.chapterCount)
            val first = book.readChapter(0)
            // 碎片内容必须被插进骨架里，这两段同时出现才说明组装正确。
            assertTrue("缺少碎片正文：${first.html}", first.html.contains("第一章的内容"))
            assertTrue("骨架结构丢失：${first.html}", first.html.contains("<body>"))
            assertTrue(first.html.contains("kindle:embed:0001"))

            val second = book.readChapter(1)
            assertTrue(second.html.contains("第二章的内容"))
        }
    }

    @Test
    fun `AZW3 的图片走资源记录`() = runBlocking {
        MobiBook.open(sample("sample.azw3")).use { book ->
            val key = book.resolveHref("", "kindle:embed:0001?mime=image/png")
            assertEquals("embed:1", key)
            val resource = book.readResource(key!!)
            assertNotNull(resource)
            assertTrue(resource!!.bytes[0] == 0x89.toByte())
            // 封面是 EXTH 201，0 基
            assertNotNull(book.cover())
        }
    }

    @Test
    fun `PalmDOC 解压支持回溯与空格缩写`() {
        val input = byteArrayOf(
            'a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte(),
            0x80.toByte(), 0x1B, // distance = 3, length = 6 → 再补 abcabc
            0xC1.toByte(),       // 空格 + 'A'
            0x00                 // 字面 0
        )
        val text = String(PalmDocDecoder.decode(input), Charsets.ISO_8859_1)
        assertEquals("abcabcabc A\u0000", text)
    }

    @Test
    fun `ReaderPaths 编解码可逆`() {
        val keys = listOf(
            "OEBPS/Images/图 1.png",
            "embed:1",
            "flow:2",
            "a?b#c%d"
        )
        keys.forEach { key ->
            val encoded = ReaderPaths.encode(key)
            assertTrue("編碼後不該出現非法字元：$encoded", encoded.all { it.isLetterOrDigit() || it == '-' || it == '_' })
            assertEquals(key, ReaderPaths.decode(encoded))
            assertEquals(key, ReaderPaths.resourceKeyFromPath("raw/$encoded"))
            // WebView 送進來的會是帶前綴的完整路徑，兩邊都要認得。
            assertEquals(key, ReaderPaths.resourceKeyFromPath(ReaderPaths.absoluteResource(key)))
        }
        assertEquals(3, ReaderPaths.chapterFromPath("chapter/3"))
        assertEquals(3, ReaderPaths.chapterFromPath(ReaderPaths.chapter(3)))
    }

    @Test
    fun `章节 HTML 会剥掉脚本并改写资源路径`() = runBlocking {
        EpubBook.open(sample("sample.epub")).use { book ->
            val content = book.readChapter(0)
            val html = ChapterHtml.build(
                book,
                content,
                ReaderStyle(background = 0xFFFAF6EF, foreground = 0xFF241F19, link = 0xFF7A5320)
            )
            assertTrue("腳本必須被剝掉", !html.contains("alert('evil')"))
            assertTrue("必須有翻頁容器", html.contains("id=\"__stage\""))
            assertTrue("圖片路徑必須被改寫", html.contains(ReaderPaths.absoluteResource("OEBPS/Images/pic.png")))
            assertTrue("章節連結必須指向閱讀器路由", html.contains("/reader/chapter/1"))
            assertTrue("樣式表連結必須被改寫", html.contains(ReaderPaths.absoluteResource("OEBPS/Styles/main.css")))
        }
    }

    @Test
    fun `直排样式会切换书写模式`() {
        val horizontal = ReaderStyle(0xFFFFFFFF, 0xFF000000, 0xFF0000FF).css()
        val vertical = ReaderStyle(0xFFFFFFFF, 0xFF000000, 0xFF0000FF, vertical = true).css()
        assertTrue(horizontal.contains("writing-mode: horizontal-tb"))
        assertTrue(vertical.contains("writing-mode: vertical-rl"))
        assertTrue(vertical.contains("right: 0"))
    }

    @Test
    fun `格式嗅探认得出没有副档名的书`() {
        assertEquals(BookFormat.EPUB, BookFormat.sniff(sample("sample.epub")))
        assertEquals(BookFormat.MOBI, BookFormat.sniff(sample("sample.mobi")))
        assertEquals(BookFormat.AZW3, BookFormat.sniff(sample("sample.azw3")))
    }
}

package com.yuzlyn.mdeditor.data.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 彎引號改排直角引號時的邊界。
 *
 * 這個轉換動的是**正文的字**，所以「哪裡不能動」比「哪裡要動」更重要：
 * 屬性值、`<style>` 內容、註解全都是結構的一部分，改了就排版跑掉；
 * 而純西文的引號本來就該是彎的，換成「」才是錯的。
 */
class CornerQuotesTest {

    @Test
    fun `中文段落裡的彎引號換成直角引號`() {
        val html = "<p>他說：“今天不去了。”</p>"
        assertEquals("<p>他說：「今天不去了。」</p>", CornerQuotes.apply(html))
    }

    @Test
    fun `標籤與屬性一個字都不動`() {
        val html = """<p class="a“b”c" title="“提示”">“正文”</p>"""
        assertEquals(
            """<p class="a“b”c" title="“提示”">「正文」</p>""",
            CornerQuotes.apply(html)
        )
    }

    @Test
    fun `屬性值裡的尖括號不讓標籤提早結束`() {
        // alt 裡有一個 `>`：若掃描器只找第一個 `>`，後面的「正文」會被當成屬性。
        val html = """<img alt="a > “說明”" src="x.png"><p>“正文”</p>"""
        assertEquals(
            """<img alt="a > “說明”" src="x.png"><p>「正文」</p>""",
            CornerQuotes.apply(html)
        )
    }

    @Test
    fun `樣式區塊裡的引號不動`() {
        val html = """<style>p::before { content: "“引號”"; }</style><p>“正文”</p>"""
        assertEquals(
            """<style>p::before { content: "“引號”"; }</style><p>「正文」</p>""",
            CornerQuotes.apply(html)
        )
    }

    @Test
    fun `註解裡的引號不動`() {
        val html = "<!-- “保留” --><p>“換掉”</p>"
        assertEquals("<!-- “保留” --><p>「換掉」</p>", CornerQuotes.apply(html))
    }

    @Test
    fun `純西文的一章整章不動`() {
        val html = "<p>\u201CHello,\u201D she said. \u201CFine.\u201D</p>"
        assertSame(html, CornerQuotes.apply(html))
    }

    @Test
    fun `中文章節裡的純西文段落也一起換`() {
        // 章級判斷：這一章有中文，整章的引號就按中文排版走，
        // 不會出現「上一段換了、這一段沒換」的斷層。
        val html = "<p>作者原文：</p><p>\u201CTo be, or not to be.\u201D</p>"
        assertEquals("<p>作者原文：</p><p>「To be, or not to be.」</p>", CornerQuotes.apply(html))
    }

    @Test
    fun `引號實體與字面引號一樣處理`() {
        val html = "<p>&ldquo;實體&rdquo;與&#8220;數字&#8221;</p>"
        assertEquals("<p>「實體」與「數字」</p>", CornerQuotes.apply(html))
    }

    @Test
    fun `西文段落裡的實體也不動`() {
        val html = "<p>&ldquo;Hello&rdquo;</p>"
        assertEquals(html, CornerQuotes.apply(html))
    }

    @Test
    fun `沒有可換的字元時原樣回傳同一個物件`() {
        val html = "<p>普通的一段話，沒有任何引號。</p>"
        assertSame(html, CornerQuotes.apply(html))
    }

    @Test
    fun `同一段裡中英混排時整段的引號一起換`() {
        // 只要這一段有漢字，段內的引號就都按中文排版處理——
        // 中文書引用英文原句，通常也是用中文的引號層級。
        val html = "<p>原文是 “to be or not to be”，中文裡也這樣引。</p>"
        assertEquals("<p>原文是 「to be or not to be」，中文裡也這樣引。</p>", CornerQuotes.apply(html))
    }

    // ── 接到生成鏈路上：開關真的有效，而且主文件與換章片段一致 ──────────

    private val chapter = ChapterContent(
        html = """
            <html><head><style>p::before { content: "“樣式”"; }</style></head>
            <body><p>他說：“走吧。”</p></body></html>
        """.trimIndent(),
        basePath = "OEBPS"
    )

    private val book = object : Book {
        override val metadata = BookMetadata(title = "測試書", format = BookFormat.EPUB)
        override val chapterCount = 1
        override val toc = emptyList<TocEntry>()
        override val readingDirection = "ltr"
        override suspend fun readChapter(index: Int) = chapter
        override fun resolveHref(basePath: String, href: String): String? = null
        override fun chapterOfResource(key: String): Int? = null
        override suspend fun readResource(key: String): BookResource? = null
        override suspend fun cover(): ByteArray? = null
        override fun close() = Unit
    }

    private val style = ReaderStyle(background = 0xFFFFFF, foreground = 0x1F1D1B, link = 0x2E6DA4)

    @Test
    fun `開啟時主文件與換章片段都換成直角引號`() {
        val html = ChapterHtml.build(book, chapter, style.copy(cornerQuotes = true), chapter = 0)
        assertTrue("主文件正文要換：$html", "<p>他說：「走吧。」</p>" in html)
        // 書自己的樣式表不是正文，動它就是排版事故。
        assertTrue("樣式區塊要保留原樣：$html", "\"“樣式”\"" in html)
        val fragment = ChapterHtml.fragment(book, chapter, chapter = 0, cornerQuotes = true)
        assertTrue("換章片段也要換，否則是章界前後不一致：$fragment", "<p>他說：「走吧。」</p>" in fragment)
    }

    @Test
    fun `關掉時主文件與換章片段都維持原樣`() {
        val html = ChapterHtml.build(book, chapter, style.copy(cornerQuotes = false), chapter = 0)
        assertTrue("關掉就不該動：$html", "<p>他說：\u201C走吧。\u201D</p>" in html)
        assertFalse("關掉就不該出現直角引號：$html", "「走吧」" in html)
        val fragment = ChapterHtml.fragment(book, chapter, chapter = 0, cornerQuotes = false)
        assertTrue("片段也維持原樣：$fragment", "<p>他說：\u201C走吧。\u201D</p>" in fragment)
    }
}

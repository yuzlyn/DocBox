package com.yuzlyn.mdeditor.data.book

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 章節自帶的樣式表要**跟著章節走**，不能一進 <head> 就永遠留著。
 *
 * 真實案例：《唐宋詞鑑賞辭典》的封面章用 `css/fixed.css`，那份樣式表寫著
 * `html, body { font-size: 0 }`（固定版面只放一張圖）。舊版只往 <head> 加、
 * 從不移除，於是翻到正文時整份文件仍是 0 號字——每一頁都空白，但頁數照算、
 * 翻頁照走，看起來像「書壞了」。
 *
 * 這裡守的是提供給腳本的那兩個入口：[ChapterHtml.build] 送進 <head> 的首章
 * 樣式表要帶 `data-ch-style` 記號，[ChapterHtml.fragment] 的片段則要帶著自己
 * 這一章的樣式表，讓 `adoptLinks` 有東西可以做差集。
 */
class ChapterStyleLinkTest {

    private val fixedCss = "OEBPS/css/fixed.css"
    private val storyCss = "OEBPS/css/styles.css"

    private val cover = ChapterContent(
        html = """
            <html><head>
              <link href="css/fixed.css" rel="stylesheet" type="text/css"/>
            </head><body>
              <svg viewBox="0 0 1400 1820"><image href="image/cover.jpg"/></svg>
            </body></html>
        """.trimIndent(),
        basePath = "OEBPS"
    )

    private val story = ChapterContent(
        html = """
            <html><head>
              <link href="css/styles.css" rel="stylesheet" type="text/css"/>
            </head><body><p>楊冠卿　卜算子</p></body></html>
        """.trimIndent(),
        basePath = "OEBPS"
    )

    private val book = object : Book {
        override val metadata = BookMetadata(title = "測試書", format = BookFormat.EPUB)
        override val chapterCount = 2
        override val toc = emptyList<TocEntry>()
        override val readingDirection = "ltr"
        override suspend fun readChapter(index: Int) = cover
        override fun resolveHref(basePath: String, href: String): String? =
            href.takeIf { it == "css/fixed.css" || it == "css/styles.css" }
                ?.let { "$basePath/$it" }

        override fun chapterOfResource(key: String): Int? = null
        override suspend fun readResource(key: String): BookResource? = null
        override suspend fun cover(): ByteArray? = null
        override fun close() = Unit
    }

    private val style = ReaderStyle(background = 0xFFFFFF, foreground = 0x1F1D1B, link = 0x2E6DA4)

    /** 書裡的 href 會被改寫成閱讀器的 URL 空間（base64url），所以要比對改寫後的位址。 */
    private val fixedUrl = ReaderPaths.absoluteResource(fixedCss)
    private val storyUrl = ReaderPaths.absoluteResource(storyCss)

    @Test
    fun `首章樣式表帶上 data-ch-style 記號`() {
        val html = ChapterHtml.build(book, cover, style, chapter = 0, chapterCount = 2)
        assertTrue("主文件裡應該有封面章帶來的樣式表：$html", fixedUrl in html)
        val link = html.substringBefore(fixedUrl).substringAfterLast("<link")
        assertTrue("樣式表要標記成「章節帶來的」，換章時才撤得掉：$link", "data-ch-style" in link)
    }

    @Test
    fun `正文片段帶著自己的樣式表 才能把封面的換掉`() {
        val fragment = ChapterHtml.fragment(book, story, chapter = 1)
        assertTrue("片段要帶上這一章的樣式表：$fragment", storyUrl in fragment)
        assertTrue("正文片段不該帶封面的樣式表", fixedUrl !in fragment)
    }

    @Test
    fun `封面片段也不帶走別人的樣式表`() {
        val fragment = ChapterHtml.fragment(book, cover, chapter = 0)
        assertTrue("封面章自己只有 fixed.css", fixedUrl in fragment)
        assertTrue("封面章不該引用正文的樣式表", storyUrl !in fragment)
    }
}

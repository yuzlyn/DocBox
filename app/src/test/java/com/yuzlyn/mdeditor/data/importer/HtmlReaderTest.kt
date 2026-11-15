package com.yuzlyn.mdeditor.data.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HTML → Markdown 的迴歸測試。
 *
 * 這些案例就是「HTML 匯進來變成一大坨」的時候會壞掉的地方：
 * 換行、標題階層、清單、程式碼區塊、表格、實體。
 */
class HtmlReaderTest {

    @Test
    fun `br 轉成換行而不是被吃掉`() {
        val markdown = HtmlReader.toMarkdown("<p>第一行<br>第二行</p>")

        assertEquals("第一行\n第二行", markdown)
    }

    @Test
    fun `標題保留階層`() {
        val markdown = HtmlReader.toMarkdown(
            "<h1>大標</h1><h2>中標</h2><h3>小標</h3>"
        )

        assertEquals("# 大標\n\n## 中標\n\n### 小標", markdown)
    }

    @Test
    fun `粗體斜體與行內程式碼`() {
        val markdown = HtmlReader.toMarkdown(
            "<p>這是 <strong>粗體</strong>、<em>斜體</em> 與 <code>code</code></p>"
        )

        assertEquals("這是 **粗體**、*斜體* 與 `code`", markdown)
    }

    @Test
    fun `連結與圖片`() {
        val markdown = HtmlReader.toMarkdown(
            """<p><a href="https://example.com">範例</a></p><p><img src="a.png" alt="圖"></p>"""
        )

        assertEquals("[範例](https://example.com)\n\n![圖](a.png)", markdown)
    }

    @Test
    fun `無序清單轉成破折號項目`() {
        val markdown = HtmlReader.toMarkdown("<ul><li>一</li><li>二</li></ul>")

        assertEquals("- 一\n- 二", markdown)
    }

    @Test
    fun `有序清單保留編號`() {
        val markdown = HtmlReader.toMarkdown("<ol><li>甲</li><li>乙</li></ol>")

        assertEquals("1. 甲\n2. 乙", markdown)
    }

    @Test
    fun `程式碼區塊轉成圍籬並帶語言`() {
        val markdown = HtmlReader.toMarkdown(
            """<pre><code class="language-kotlin">val a = 1
if (a &lt; 2) return</code></pre>"""
        )

        assertEquals("```kotlin\nval a = 1\nif (a < 2) return\n```", markdown)
    }

    @Test
    fun `表格轉成 Markdown 表格`() {
        val markdown = HtmlReader.toMarkdown(
            "<table><tr><th>欄一</th><th>欄二</th></tr><tr><td>a</td><td>b</td></tr></table>"
        )

        assertEquals("| 欄一 | 欄二 |\n| --- | --- |\n| a | b |", markdown)
    }

    @Test
    fun `引用轉成 blockquote`() {
        val markdown = HtmlReader.toMarkdown("<blockquote><p>引文</p></blockquote>")

        assertEquals("> 引文", markdown)
    }

    @Test
    fun `script 與 style 不會漏進正文`() {
        val markdown = HtmlReader.toMarkdown(
            "<html><head><style>p{color:red}</style></head>" +
                    "<body><script>alert(1)</script><p>正文</p></body></html>"
        )

        assertEquals("正文", markdown)
        assertFalse(markdown.contains("alert"))
        assertFalse(markdown.contains("color"))
    }

    @Test
    fun `實體還原成字元`() {
        val markdown = HtmlReader.toMarkdown("<p>&lt;標籤&gt; &amp; &quot;引號&quot; &nbsp;空格</p>")

        assertTrue(markdown.contains("<標籤> & \"引號\""))
    }

    @Test
    fun `連續空白不會留下整片空白`() {
        val markdown = HtmlReader.toMarkdown(
            "<div>   \n\n   <p>甲</p>\n\n\n\n<p>乙</p>   </div>"
        )

        assertEquals("甲\n\n乙", markdown)
    }

    @Test
    fun `完整文件只取 body 內容`() {
        val markdown = HtmlReader.toMarkdown(
            """
            <!DOCTYPE html>
            <html lang="zh">
            <head><meta charset="utf-8"><title>標題不要進來</title></head>
            <body>
              <h2>章節</h2>
              <p>段落一</p>
              <p>段落二</p>
            </body>
            </html>
            """.trimIndent()
        )

        assertEquals("## 章節\n\n段落一\n\n段落二", markdown)
    }
}

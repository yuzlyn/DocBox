package com.yuzlyn.mdeditor

import com.yuzlyn.mdeditor.data.book.ChapterHtml
import com.yuzlyn.mdeditor.data.book.EpubBook
import com.yuzlyn.mdeditor.data.book.ReaderMode
import com.yuzlyn.mdeditor.data.book.ReaderStyle
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File

/**
 * 把真實的書排出來，寫成 HTML 檔，方便用桌面瀏覽器逐項比對版面。
 *
 * 不是斷言型測試——書頁排版要靠眼睛看（分頁位置、留白、直排方向），
 * 這裡只負責產出檔案；命令列拿 Edge / Chrome 的 headless 截圖即可。
 * 樣書不在 (`build/bookdump/real.epub`) 就整支跳過，不會讓 CI 紅。
 */
class LayoutDumpTest {

    private val samples = listOf(
        // 真機（1080×2376 @ dpr3 = 360×792 CSS）的實際組合。
        // 留白 16 是使用者在真機上調到的那一檔，橫排分頁曾在這裡出過
        // 「右邊切字、版心偏右」的版面，所以固定用這一檔比對。
        Triple("h-paged-m16", false, ReaderMode.PAGED),
        Triple("v-paged-m16", true, ReaderMode.PAGED),
        Triple("h-scrollv-m16", false, ReaderMode.SCROLL_VERTICAL),
        Triple("h-scrollh-m16", false, ReaderMode.SCROLL_HORIZONTAL),
        Triple("v-scrollh-m16", true, ReaderMode.SCROLL_HORIZONTAL),
        Triple("v-scrollv-m16", true, ReaderMode.SCROLL_VERTICAL),
        Triple("h-paged-m20", false, ReaderMode.PAGED)
    )

    /**
     * 首屏樣本：打開書時真正載入的那一份文件（**只有一章**）。
     *
     * 「打開書卡不卡」就看這裡。整本書塞進一份文件時，WebView 的渲染行程
     * 會直接卡死（真機 `Skipped 10599 frames`），所以這個檔案的大小與
     * 排版耗時是回歸指標——它應該只有幾十 KB，而不是幾 MB。
     */
    @Test
    fun dumpOpening() {
        val epub = File("../build/bookdump/real.epub")
        if (!epub.exists()) {
            println("樣書不存在，略過首屏輸出")
            return
        }
        val out = File("../build/layout").apply { mkdirs() }
        EpubBook.open(epub).use { book ->
            val content = runBlocking { book.readChapter(0) }
            listOf(
                Triple("v-scrollh", true, ReaderMode.SCROLL_HORIZONTAL),
                Triple("h-paged", false, ReaderMode.PAGED)
            ).forEach { (name, vertical, mode) ->
                val style = ReaderStyle(
                    background = 0xFFFFFF,
                    foreground = 0x1F1D1B,
                    link = 0x2E6DA4,
                    vertical = vertical,
                    margin = 16,
                    lineHeight = 1.7f,
                    mode = mode
                )
                val html = ChapterHtml.build(
                    book = book,
                    content = content,
                    style = style,
                    chapter = 0,
                    chapterCount = book.chapterCount
                )
                File(out, "opening-$name.html").writeText(html)
                println("首屏樣本 $name：${html.toByteArray().size} bytes / 全書 ${book.chapterCount} 章")
            }
        }
    }

    @Test
    fun dumpRealBook() {
        // 單測的工作目錄是模組根（app/），樣書放在專案根的 build 底下。
        val epub = File("../build/bookdump/real.epub")
        if (!epub.exists()) {
            println("樣書不存在，略過排版輸出")
            return
        }
        val out = File("../build/layout").apply { mkdirs() }
        EpubBook.open(epub).use { book ->
            // 使用者截圖顯示的是第 15 章（索引 14），直接排這一章才對得上。
            val chapter = 14
            val content = runBlocking { book.readChapter(chapter) }
            samples.forEach { (name, vertical, mode) ->
                val style = ReaderStyle(
                    background = 0xFFFFFF,
                    foreground = 0x1F1D1B,
                    link = 0x2E6DA4,
                    vertical = vertical,
                    margin = if (name.endsWith("-m16")) 16 else 20,
                    lineHeight = 1.7f,
                    mode = mode
                )
                val html = ChapterHtml.build(
                    book = book,
                    content = content,
                    style = style,
                    chapter = chapter,
                    chapterCount = book.chapterCount
                )
                File(out, "$name.html").writeText(html)
            }
            val cover = runBlocking { book.readChapter(0) }
            File(out, "cover.html").writeText(
                ChapterHtml.build(
                    book = book,
                    content = cover,
                    style = ReaderStyle(
                        background = 0xFFFFFF,
                        foreground = 0x1F1D1B,
                        link = 0x2E6DA4
                    ),
                    chapter = chapter,
                    chapterCount = book.chapterCount
                )
            )
        }
        println("版面樣本已寫到 ${out.absolutePath}")
    }
}

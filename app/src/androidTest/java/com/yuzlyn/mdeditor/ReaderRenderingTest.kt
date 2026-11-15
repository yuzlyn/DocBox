package com.yuzlyn.mdeditor

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yuzlyn.mdeditor.data.book.Book
import com.yuzlyn.mdeditor.data.book.BookOpener
import com.yuzlyn.mdeditor.data.book.ChapterHtml
import com.yuzlyn.mdeditor.data.book.ReaderPaths
import com.yuzlyn.mdeditor.data.book.ReadingColors
import com.yuzlyn.mdeditor.data.book.ReadingTheme
import com.yuzlyn.mdeditor.ui.reader.ReaderResourceHandler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * 書頁渲染的**真機**驗證。
 *
 * 為什麼要儀器測試：翻頁、直排、資源載入全部發生在 WebView 裡，
 * JVM 單元測試只能證明 HTML 字串長得對，證明不了瀏覽器真的排得出來。
 * 這幾個點（整章當一條長條平移、直排的 `width: max-content`、攔截器餵圖）
 * 也是最容易「看起來對、實際一片空白」的地方，必須在真機上跑一次。
 */
@RunWith(AndroidJUnit4::class)
class ReaderRenderingTest {

    @Test
    fun epub_章節能排版出頁數且圖片載得進來() {
        val result = render("sample.epub", vertical = false)
        assertTrue("橫排應該至少有一頁", result.pages >= 1)
        assertTrue("整章要有實際高度", result.extent > 0)
        // 圖片是經過攔截器從 EPUB 的 ZIP 裡取出來的；載不到會是 0。
        assertTrue("書裡的圖片應該載得進來（naturalWidth=${result.imageWidth}）", result.imageWidth > 0)
        assertTrue("不該有 JS 錯誤：${result.console}", result.console.isEmpty())
    }

    @Test
    fun epub_直排也能排出頁數() {
        val result = render("sample.epub", vertical = true)
        assertTrue("直排應該至少有一頁", result.pages >= 1)
        assertTrue("直排的長條要有實際寬度（width: max-content）", result.extent > 0)
        assertTrue("不該有 JS 錯誤：${result.console}", result.console.isEmpty())
    }

    @Test
    fun azw3_骨架碎片組出來的章節能渲染() {
        val result = render("sample.azw3", vertical = false)
        assertTrue("AZW3 應該至少有一頁", result.pages >= 1)
        assertTrue("AZW3 的圖片應該載得進來（naturalWidth=${result.imageWidth}）", result.imageWidth > 0)
        assertTrue("不該有 JS 錯誤：${result.console}", result.console.isEmpty())
    }

    private class Rendered(
        val pages: Int,
        val extent: Int,
        val imageWidth: Int,
        val console: String
    )

    private fun render(assetName: String, vertical: Boolean): Rendered {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        // 樣本是測資，放在測試 APK 的 assets 裡，不在被測 App 的 assets 裡。
        val file = File(context.cacheDir, "reader-test-$assetName")
        instrumentation.context.assets.open("books/$assetName").use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }

        val book: Book = runBlocking { BookOpener.open(context, Uri.fromFile(file), assetName) }
        val content = runBlocking { book.readChapter(0) }
        val html = ChapterHtml.build(
            book,
            content,
            ReadingTheme.PAPER.style(ReadingColors.PAPER, 100, vertical),
            initialAnchor = null
        )

        val pages = AtomicInteger(0)
        val extent = AtomicInteger(0)
        val imageWidth = AtomicInteger(0)
        val console = AtomicReference("")
        val loaded = CountDownLatch(1)
        val main = Handler(Looper.getMainLooper())
        // 只回傳「長度,圖片寬」這種純文字，省得在 Kotlin 裡拆 JSON。
        val measureScript = StringBuilder("(function(){")
            .append("var s=document.getElementById('__stage');")
            .append("var img=document.querySelector('img');")
            .append("var extent=s?(")
            .append(if (vertical) "Math.max(s.offsetWidth,s.scrollWidth)" else "Math.max(s.offsetHeight,s.scrollHeight)")
            .append("):0;")
            .append("return extent+','+(img?img.naturalWidth:0);")
            .append("})()")
            .toString()

        // 必須掛在真的 Activity 上：離屏的 WebView 不會排版，頁數與尺寸都會是 0。
        val scenario = ActivityScenario.launch(ReaderHostActivity::class.java)
        scenario.onActivity { activity ->
            val view = activity.mountWebView()
            view.settings.javaScriptEnabled = true
            view.addJavascriptInterface(
                object {
                    @JavascriptInterface
                    fun onReady() {
                        // 量一次「整章有多長」與圖片的實際像素寬。
                        main.post {
                            view.evaluateJavascript(measureScript) { raw ->
                                val parts = raw.trim().trim('"').split(',')
                                extent.set(parts.getOrNull(0)?.toIntOrNull() ?: 0)
                                imageWidth.set(parts.getOrNull(1)?.toIntOrNull() ?: 0)
                                loaded.countDown()
                            }
                        }
                    }

                    @JavascriptInterface
                    fun onProgress(page: Int, total: Int, fraction: Float) {
                        pages.set(total)
                    }

                    @JavascriptInterface
                    fun onChapter(delta: Int) = Unit

                    @JavascriptInterface
                    fun onTap(zone: String?) = Unit
                },
                "Android"
            )
            view.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                    if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                        console.set(message.message())
                    }
                    return true
                }
            }
            val handler = ReaderResourceHandler(book)
            view.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    v: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? = handler.response(request.url)
            }
            view.loadDataWithBaseURL(
                ReaderPaths.ORIGIN + ReaderPaths.chapter(0),
                html,
                "text/html",
                "utf-8",
                null
            )
        }

        val ok = loaded.await(15, TimeUnit.SECONDS)
        scenario.close()
        runCatching { book.close() }
        assertTrue("15 秒內 WebView 應該要回報版面結果", ok)
        return Rendered(pages.get(), extent.get(), imageWidth.get(), console.get())
    }
}

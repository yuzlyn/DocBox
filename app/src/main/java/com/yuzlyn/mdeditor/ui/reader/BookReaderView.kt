package com.yuzlyn.mdeditor.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.yuzlyn.mdeditor.data.book.Book
import com.yuzlyn.mdeditor.data.book.BookResource
import com.yuzlyn.mdeditor.data.book.ChapterContent
import com.yuzlyn.mdeditor.data.book.ChapterHtml
import com.yuzlyn.mdeditor.data.fonts.FontStore
import com.yuzlyn.mdeditor.data.book.ReaderPaths
import com.yuzlyn.mdeditor.data.book.ReaderStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayInputStream

/**
 * 書頁的渲染層：一個 WebView，加上「把書裡的資源餵進去」的攔截器。
 *
 * 為什麼用 WebView 而不是自己畫：書的排版（EPUB 的 CSS、KF8 的樣式表、圖片、表格）
 * 是它內容的一部分，只有瀏覽器引擎能把它渲染對。Readest / foliate 也是同一條路。
 *
 * 三件事必須自己管：
 * 1. **資源由 [Book] 現拿**，不落地、不整包載入。請求進來才去 ZIP / 記錄裡讀，
 *    所以一本 200MB 的書開起來也只有當前章節在記憶體裡。
 * 2. **腳本由我們注入**，書自己的 `<script>` 早在 [ChapterHtml] 就被剝掉了；
 *    攔截器再把所有「不是書」的網址擋掉，書就沒有管道連外。
 * 3. **一次只裝一章**（Readest / foliate 的做法）。整本書塞進一份文件會讓
 *    WebView 的渲染行程卡死——真機日誌是 `Skipped 10599 frames`。
 *    跨章由頁內腳本調 [ReaderBridge.chapterFragment] 就地換內容，
 *    不 reload、不白屏，所以「一路讀到底」照樣成立。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BookReaderView(
    book: Book,
    /** 打開時定位到這一章；之後章號由書頁自己回報，不再觸發重載。 */
    chapter: Int,
    /** 全書章數，頁內腳本靠它判斷「還有沒有下一章」。 */
    chapterCount: Int,
    /** 章內錨點；配合 [locateToken] 遞增可以重複跳到同一個錨點。 */
    anchor: String?,
    /** 跳到某章（絕對章號）。[locateToken] 遞增一次觸發一次。 */
    locateChapter: Int = -1,
    locateFraction: Float = 0f,
    locateAnchor: String? = null,
    locateToken: Int = 0,
    /** 上次讀到的章內比例（0..1）。 */
    initialFraction: Float = 0f,
    style: ReaderStyle,
    onOpenChapter: (chapter: Int, anchor: String?) -> Unit,
    onProgress: (page: Int, pages: Int, fraction: Float) -> Unit,
    /** 目前讀到全書第幾章、章內比例。 */
    onLocation: (chapter: Int, fraction: Float) -> Unit,
    onCenterTap: () -> Unit,
    onLoaded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    // 攔截器與 JS 介面活在 WebView 自己的執行緒，拿不到最新的 Compose 值，
    // 所以整包放進 State 讓它們每次自己取。
    val hooks = rememberUpdatedState(
        ReaderHooks(
            book = book,
            onOpenChapter = onOpenChapter,
            onProgress = onProgress,
            onLocation = onLocation,
            onCenterTap = onCenterTap,
            onLoaded = onLoaded
        )
    )
    val currentStyle = rememberUpdatedState(style)
    // 換方向／換翻頁方式會重載文件，那時要回到「使用者正在讀的這一章」。
    val currentChapter = rememberUpdatedState(chapter)
    val currentFraction = rememberUpdatedState(initialFraction)
    val context = LocalContext.current
    val handler = remember(book, context) {
        val appContext = context.applicationContext
        ReaderResourceHandler(book) { fileName ->
            FontStore.fileOf(appContext, fileName)?.let { file ->
                runCatching { file.readBytes() }.getOrNull()
            }
        }
    }

    key(book) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(argb(style.background))
                    configureForReading()
                    addJavascriptInterface(ReaderBridge(hooks), "Android")
                    webViewClient = ReaderClient(ctx, hooks, handler)
                    webViewRef.value = this
                }
            },
            onRelease = { view ->
                webViewRef.value = null
                runCatching { view.removeJavascriptInterface("Android") }
                runCatching { view.stopLoading() }
                runCatching { view.destroy() }
            }
        )
    }

    val webView = webViewRef.value

    // 只在「換書／換排版方向／換翻頁方式」時才重新載入，而且只載入**一章**。
    // **章號刻意不在 key 裡**：跨章由頁內腳本就地換內容，不該重載文件。
    LaunchedEffect(webView, style.vertical, style.mode) {
        if (webView == null) return@LaunchedEffect
        val target = currentChapter.value
        val content = withContext(Dispatchers.IO) {
            runCatching { book.readChapter(target) }.getOrElse {
                // 單章讀壞不該讓整本書打不開：留一個空章佔位，章號仍然對得上。
                ChapterContent("<p></p>", "")
            }
        }
        val html = ChapterHtml.build(
            book = book,
            content = content,
            style = currentStyle.value,
            chapter = target,
            chapterCount = chapterCount,
            initialAnchor = anchor,
            initialFraction = currentFraction.value
        )
        webView.loadDataWithBaseURL(
            ReaderPaths.ORIGIN + ReaderPaths.chapter(target),
            html,
            "text/html",
            "utf-8",
            null
        )
    }

    // 只換顏色與字級：改 CSS + textZoom，不重載文件，閱讀位置才不會跳掉。
    LaunchedEffect(webView, style) {
        val view = webView ?: return@LaunchedEffect
        view.setBackgroundColor(argb(style.background))
        if (view.settings.textZoom != style.textScale) {
            // textZoom 會觸發整份文件重排，所以重排完要再量一次頁數。
            view.settings.textZoom = style.textScale
            view.postDelayed({
                view.evaluateJavascript("window.__reader && window.__reader.repaginate()", null)
            }, REPAGINATE_DELAY_MS)
        }
        view.evaluateJavascript(
            "window.__reader && window.__reader.setStyle(${JSONObject.quote(style.css())})",
            null
        )
    }

    // 目錄／書內連結跳章時走這裡。目標章不在 DOM 裡時，腳本會就地換內容；
    // 就在當前章時只是重新定位，不換 DOM。
    LaunchedEffect(webView, locateToken) {
        val view = webView ?: return@LaunchedEffect
        if (locateToken == 0 || locateChapter < 0 || locateChapter >= chapterCount) {
            return@LaunchedEffect
        }
        val jump = String.format(
            java.util.Locale.US,
            "window.__reader && window.__reader.goChapter(%d, %.4f)",
            locateChapter,
            locateFraction.coerceIn(0f, 1f)
        )
        if (locateAnchor != null) {
            view.evaluateJavascript(
                "$jump; window.__reader && window.__reader.goAnchor(${JSONObject.quote(locateAnchor)})",
                null
            )
        } else {
            view.evaluateJavascript(jump, null)
        }
    }
}

/** `ReaderStyle` 給的是 0xRRGGBB，Android 要 ARGB，少了 alpha 會變成全透明。 */
private fun argb(color: Long): Int = (0xFF000000L or (color and 0xFFFFFF)).toInt()

/** 字級變了之後等 WebView 把版面重排完再重新計算頁數。 */
private const val REPAGINATE_DELAY_MS = 90L

private fun WebView.configureForReading() {
    val settings = this.settings
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = false
    settings.databaseEnabled = false
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    settings.loadsImagesAutomatically = true
    settings.blockNetworkImage = false
    settings.cacheMode = WebSettings.LOAD_NO_CACHE
    // 字級是我們自己在 CSS 裡控的，不要讓 WebView 再套一層系統字級／雙擊縮放。
    settings.textZoom = 100
    settings.setSupportZoom(false)
    settings.builtInZoomControls = false
    settings.displayZoomControls = false
    settings.mediaPlaybackRequiresUserGesture = true
    isVerticalScrollBarEnabled = false
    isHorizontalScrollBarEnabled = false
    overScrollMode = WebView.OVER_SCROLL_NEVER
    setOnLongClickListener { true }
}

/** 給 WebView 執行緒回呼主執行緒用的一包資料。 */
private class ReaderHooks(
    val book: Book,
    val onOpenChapter: (Int, String?) -> Unit,
    val onProgress: (Int, Int, Float) -> Unit,
    val onLocation: (Int, Float) -> Unit,
    val onCenterTap: () -> Unit,
    val onLoaded: () -> Unit
)

/** 頁內腳本呼叫的入口；全部切回主執行緒，才能直接改 Compose 狀態。 */
private class ReaderBridge(private val hooks: State<ReaderHooks>) {

    private val main = Handler(Looper.getMainLooper())

    /**
     * 已經生成好的章節片段。
     *
     * 換章要**同步**回傳給 JS（JS 正在等），而讀 ZIP + 改寫資源路徑是毫秒級但非零，
     * 前後各留幾章的快取就够把「來回翻」的延遲壓到 0。
     */
    private val fragmentCache = LinkedHashMap<Int, String>()

    /** 正在背景生成的章；避免同一章被重複排隊。 */
    private val pending = java.util.Collections.synchronizedSet(HashSet<Int>())

    private companion object {
        const val FRAGMENT_CACHE_SIZE = 8
    }

    @JavascriptInterface
    fun onReady() {
        main.post { hooks.value.onLoaded() }
    }

    @JavascriptInterface
    fun onProgress(page: Int, pages: Int, fraction: Float) {
        main.post {
            hooks.value.onProgress(
                page.coerceAtLeast(0),
                pages.coerceAtLeast(1),
                fraction.coerceIn(0f, 1f)
            )
        }
    }

    @JavascriptInterface
    fun onChapter(delta: Int) {
        if (delta == 0) return
        // 換章現在由頁內腳本自己做（就地換內容），正常不會再走到這裡。
        // 留著是為了在日誌裡看見「腳本那邊居然要求換章」這種異常。
        Log.d("ReaderLayout", "onChapter delta=$delta")
    }

    /**
     * 取某一章的內容片段，供頁內腳本換章時塞進 `#__stage`。
     *
     * 這是「整本書一路讀到底」的關鍵：腳本滑到章界時同步向這裡要下一章，
     * 拿到的片段直接換掉 DOM 子樹——沒有 `loadData`、沒有白屏，
     * 也沒有重新解析書的樣式表。
     *
     * 回傳空字串代表這一章取不到（章號越界或解壓失敗），腳本會停住不動。
     *
     * 這裡用 [runBlocking] 是刻意的：`@JavascriptInterface` 跑在 WebView 的
     * 橋接執行緒，JS 正同步等這個回傳，切去別的執行緒只是多一次排隊。
     * 讀一章是幾毫秒的事，加上 [fragmentCache] 之後同一章只做一次。
     */
    @JavascriptInterface
    fun chapterFragment(chapter: Int): String {
        val hooks = hooks.value
        val count = hooks.book.chapterCount
        if (chapter < 0 || chapter >= count) return ""
        synchronized(fragmentCache) { fragmentCache[chapter]?.let { return it } }
        val html = buildFragment(hooks, chapter)
        if (html.isNotEmpty()) synchronized(fragmentCache) { putFragment(chapter, html) }
        return html
    }

    private fun buildFragment(hooks: ReaderHooks, chapter: Int): String = runCatching {
        val content = runBlocking { hooks.book.readChapter(chapter) }
        ChapterHtml.fragment(hooks.book, content, chapter)
    }.getOrElse { "" }

    private fun putFragment(chapter: Int, html: String) {
        if (fragmentCache.size >= FRAGMENT_CACHE_SIZE) {
            fragmentCache.keys.firstOrNull()?.let { fragmentCache.remove(it) }
        }
        fragmentCache[chapter] = html
    }

    /**
     * 背景預取相鄰章。
     *
     * 換章要**同步**回傳，這是唯一會讓「一路滑到底」頓一下的地方。趁讀者還在
     * 當前章時把下一章先做好放進 [fragmentCache]，到章界時就是純記憶體查找。
     * 只在前進方向預取，往回翻時才順手帶上前一章（讀者往回翻的機率低得多）。
     */
    private fun prefetch(hooks: ReaderHooks, chapter: Int) {
        val count = hooks.book.chapterCount
        if (chapter < 0 || chapter >= count) return
        synchronized(fragmentCache) {
            if (fragmentCache.containsKey(chapter)) return
        }
        if (!pending.add(chapter)) return
        Thread({
            try {
                val html = buildFragment(hooks, chapter)
                if (html.isNotEmpty()) synchronized(fragmentCache) { putFragment(chapter, html) }
            } finally {
                pending.remove(chapter)
            }
        }, "reader-prefetch-$chapter").start()
    }

    /**
     * 目前讀到全書第幾章、章內比例。
     *
     * 換章在頁內完成，所以這是「位置事件」而不是「載入事件」——
     * Compose 那邊拿到它只更新標題與進度，不會重載文件。
     */
    @JavascriptInterface
    fun onLocation(chapter: Int, fraction: Float) {
        main.post {
            hooks.value.onLocation(chapter, fraction.coerceIn(0f, 1f))
        }
        val f = fraction.coerceIn(0f, 1f)
        val h = hooks.value
        prefetch(h, chapter + 1)
        if (f < 0.3f) prefetch(h, chapter - 1)
    }

    @JavascriptInterface
    fun onTap(zone: String?) {
        Log.d("ReaderLayout", "onTap zone=$zone")
        if (zone != "center") return
        main.post { hooks.value.onCenterTap() }
    }

    /**
     * 版面量測值。只寫 log——排版本來就該在真實 WebView 裡量，
     * 出問題時 `adb logcat -s ReaderLayout` 就能看到實際欄寬、欄距、頁數，
     * 不必靠截圖反推。
     */
    @JavascriptInterface
    fun onMetrics(json: String?) {
        if (json.isNullOrEmpty()) return
        Log.d("ReaderLayout", json)
    }
}

/**
 * 只認得自己人：書內資源回內容，其餘一律擋掉。
 *
 * 這一層同時是安全邊界——書裡若有遠端圖片或追蹤像素，會在發出去之前就被拒絕，
 * 使用者看書的過程不會有任何連外流量。
 *
 * 抽成獨立類別是為了讓儀器測試能直接餵真的 URL 進驗證（WebView 的攔截行為
 * 只有真機跑得出來）。
 */
internal class ReaderResourceHandler(
    book: Book,
    /** 字型檔名的內容；由呼叫端提供，這一層不碰檔案系統。 */
    private val fontBytes: (String) -> ByteArray? = { null }
) {

    private val cache = ResourceCache(book)

    fun response(url: android.net.Uri): WebResourceResponse? {
        if (url.host != ReaderPaths.HOST) {
            return empty(403, "Blocked")
        }
        ReaderPaths.fontNameFromPath(url.path.orEmpty())?.let { name ->
            val bytes = fontBytes(name) ?: return empty(404, "Not Found")
            val mime = if (name.endsWith(".otf", ignoreCase = true)) "font/otf" else "font/ttf"
            return WebResourceResponse(
                mime,
                null,
                200,
                "OK",
                mapOf("Cache-Control" to "no-store"),
                ByteArrayInputStream(bytes)
            )
        }
        val key = ReaderPaths.resourceKeyFromPath(url.path.orEmpty())
            ?: return empty(404, "Not Found")
        val resource = cache.get(key) ?: return empty(404, "Not Found")
        return WebResourceResponse(
            resource.mime,
            null,
            200,
            "OK",
            mapOf("Cache-Control" to "no-store"),
            ByteArrayInputStream(resource.bytes)
        )
    }

    private fun empty(code: Int, message: String) = WebResourceResponse(
        "text/plain",
        "utf-8",
        code,
        message,
        emptyMap(),
        ByteArrayInputStream(ByteArray(0))
    )
}

private class ReaderClient(
    private val context: Context,
    private val hooks: State<ReaderHooks>,
    private val handler: ReaderResourceHandler
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        // 主文件是 loadDataWithBaseURL 塞進來的，不走網路，這裡攔不到也不必攔。
        if (request.isForMainFrame) return null
        return handler.response(request.url)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url
        if (url.host == ReaderPaths.HOST) {
            val target = ReaderPaths.chapterFromPath(url.path.orEmpty()) ?: return false
            hooks.value.onOpenChapter(target, url.fragment)
            return true
        }
        if (url.scheme == "http" || url.scheme == "https") {
            // 書裡的超連結指向別人的網站，交給瀏覽器，不在閱讀器裡開。
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
        return true
    }

    private fun empty(code: Int, message: String) = WebResourceResponse(
        "text/plain",
        "utf-8",
        code,
        message,
        emptyMap(),
        ByteArrayInputStream(ByteArray(0))
    )
}

/**
 * 資源快取。
 *
 * 同一章的圖片、字型會被反覆請求（重排、換主題都會重畫），每次都回 ZIP 裡挖一次
 * 太浪費；但也不能無限存——漫畫 EPUB 一本就幾百 MB，所以按位元組數封頂。
 */
internal class ResourceCache(private val book: Book) {

    private val entries = object : LinkedHashMap<String, BookResource>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BookResource>) =
            false
    }

    private var bytes = 0L

    /**
     * 這裡用 [runBlocking] 是刻意的：`shouldInterceptRequest` 跑在 WebView 自己的
     * 背景執行緒，本來就該同步回答，切去別的執行緒只是多一次排隊。
     */
    @Synchronized
    fun get(key: String): BookResource? {
        entries[key]?.let { return it }
        val resource = runCatching { runBlocking { book.readResource(key) } }.getOrNull() ?: return null
        entries[key] = resource
        bytes += resource.bytes.size
        trim()
        return resource
    }

    private fun trim() {
        val iterator = entries.entries.iterator()
        while (bytes > MAX_BYTES && iterator.hasNext()) {
            val entry = iterator.next()
            bytes -= entry.value.bytes.size
            iterator.remove()
        }
    }

    private companion object {
        const val MAX_BYTES = 12L * 1024 * 1024
    }
}

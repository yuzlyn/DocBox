package com.yuzlyn.mdeditor.ui.preview

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.yuzlyn.mdeditor.data.preview.PreviewConfig
import com.yuzlyn.mdeditor.data.preview.PreviewPalette
import com.yuzlyn.mdeditor.ui.theme.Space
import kotlinx.coroutines.delay
import org.json.JSONObject

private const val PREVIEW_URL = "file:///android_asset/preview/preview.html"
private const val TAG = "MDEditor_Preview"

private fun jsString(value: String): String = JSONObject.quote(value)
    .replace("\u2028", "\\u2028")
    .replace("\u2029", "\\u2029")

private fun PreviewConfig.toJs(): String = JSONObject().apply {
    put("flavor", flavor.name)
    put("showToc", showToc)
    put("syntaxHighlight", syntaxHighlight)
    put("headingAnchors", headingAnchors)
    put("math", math)
}.toString()

private class PreviewBridge(private val onReady: () -> Unit) {
    var webView: WebView? = null

    @JavascriptInterface
    fun onReady() {
        webView?.post { onReady() }
    }

    @JavascriptInterface
    fun onError(message: String) {
        Log.e(TAG, message)
    }
}

private class PreviewClipboard(private val context: Context) {
    @JavascriptInterface
    fun copy(text: String) {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("Markdown code", text))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarkdownPreview(
    markdown: String,
    config: PreviewConfig,
    palette: PreviewPalette,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
    noteUri: String? = null
) {
    val context = LocalContext.current
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val readyState = remember { mutableStateOf(false) }
    val clipboard = remember { PreviewClipboard(context.applicationContext) }
    val bridge = remember { PreviewBridge(onReady = { readyState.value = true }) }
    // 相對圖片路徑（images/xxx.jpg）要回到筆記所在資料夾去撈，這裡準備好解析器。
    val imageLoader = remember(noteUri) { AttachmentWebLoader(context, noteUri) }
    val currentImageLoader = rememberUpdatedState(imageLoader)

    fun inject(webView: WebView?) {
        webView ?: return
        webView.evaluateJavascript(
            "window.applyTheme(${jsString(palette.cssVariables)})",
            null
        )
        webView.evaluateJavascript("window.applyConfig(${config.toJs()})", null)
        webView.evaluateJavascript("window.updateMarkdown(${jsString(markdown)})", null)
        // 直排要在內容渲染後才量得到寬度，交由頁面內的 setVertical 處理捲動起點。
        webView.evaluateJavascript("window.setVertical($vertical)", null)
    }

    LaunchedEffect(Unit) {
        delay(3500)
        readyState.value = true
    }

    Box(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewRef.value = this
                    bridge.webView = this
                    setBackgroundColor(palette.surfaceArgb)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = false
                        blockNetworkLoads = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        useWideViewPort = false
                        loadWithOverviewMode = false
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        mediaPlaybackRequiresUserGesture = true
                    }
                    addJavascriptInterface(bridge, "PreviewBridge")
                    addJavascriptInterface(clipboard, "AndroidClipboard")
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest
                        ): WebResourceResponse? =
                            currentImageLoader.value?.intercept(request.url)

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            val uri: Uri = request.url
                            if (uri.scheme in setOf("http", "https", "mailto")) {
                                runCatching {
                                    ctx.startActivity(
                                        Intent(Intent.ACTION_VIEW, uri)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            }
                            return true
                        }

                        override fun onPageCommitVisible(view: WebView?, url: String?) {
                            readyState.value = true
                            inject(view)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            readyState.value = true
                            inject(view)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            Log.e(TAG, "webview error: ${error?.description}")
                            readyState.value = true
                        }
                    }
                    loadUrl("$PREVIEW_URL#bg=${Uri.encode(palette.surfaceHex)}")
                }
            },
            update = { }
        )

        if (!readyState.value) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(modifier = Modifier.size(Space.s600))
            }
        }
    }

    LaunchedEffect(readyState.value, markdown, config, palette, vertical) {
        if (!readyState.value) return@LaunchedEffect
        delay(120)
        inject(webViewRef.value)
    }
}

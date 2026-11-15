package com.yuzlyn.mdeditor

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView

/**
 * 只給儀器測試用的宿主 Activity。
 *
 * 為什麼需要它：WebView 一定要掛在顯示中的 Activity 上才會真的排版，
 * 離屏的 WebView 量出來的尺寸與頁數都是 0；而主 App 的 Activity 帶了
 * 一堆資料庫與 SAF 初始化，拿來當測試載體太重也容易互相干擾。
 */
class ReaderHostActivity : Activity() {

    var webView: WebView? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /** 掛上一個 WebView 並回傳它；測試自己負責載入內容。 */
    fun mountWebView(): WebView {
        val view = WebView(this)
        webView = view
        setContentView(
            view,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        return view
    }

    override fun onDestroy() {
        // 先從畫面拆掉再 destroy，否則 WebView 會在 detached 狀態下等 renderer 收尾。
        webView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            view.stopLoading()
            view.destroy()
        }
        webView = null
        super.onDestroy()
    }
}

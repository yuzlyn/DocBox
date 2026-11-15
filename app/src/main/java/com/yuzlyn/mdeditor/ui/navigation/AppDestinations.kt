package com.yuzlyn.mdeditor.ui.navigation

import androidx.navigation3.runtime.NavKey

/**
 * 根頁：兩個平級主頁（全部筆記 / 書庫）的容器。
 *
 * 為什麼不各給一個 key：兩個主頁是**互換**的，不是層級關係。放在同一格根頁、
 * 用 [com.yuzlyn.mdeditor.data.settings.HomeTarget] 決定渲染哪一頁，抽屜切換
 * 就不會壓棧（返回鍵在兩頁都一樣是離開 App）。
 */
data object Home : NavKey

data class NoteEditor(val noteId: String) : NavKey

data object Settings : NavKey

data object Search : NavKey

data object Profile : NavKey

data object ThemeSettings : NavKey

/** 字型：匯入、刪除、編輯／閱讀各自的預設。 */
data object FontSettings : NavKey

/** 筆記背景：預設（莫奈）／純色／圖片。 */
data object NoteBackgroundSettings : NavKey

data object DarkModeSettings : NavKey

data object LanguageSettings : NavKey

data object LayoutSettings : NavKey

data object Help : NavKey

/** 統計的開關面板；從統計頁右上角或設定頁進來。 */
data object StatsSettings : NavKey

/** 作者頁（本地頁面，不跳瀏覽器）。 */
data object Author : NavKey

/** 支持作者：兩張收款碼，從作者頁那一列進來。 */
data object Support : NavKey

/** PDF 唯讀檢視；帶的是 content:// 或 file:// 的 Uri 字串。 */
data class PdfViewer(val uri: String) : NavKey

/**
 * 原生閱讀器；帶的是 content:// 或 file:// 的 Uri 字串。
 *
 * `displayName` 只拿來顯示書名（SAF 的 Uri 本身沒有檔名），解析失敗不影響開書。
 * 從書庫進來的書一定是 `file://`（書庫自己管目錄）；從主頁直接匯入的則會先被
 * 複製進書庫再開，所以下次還在。
 */
data class Reader(val uri: String, val displayName: String? = null) : NavKey

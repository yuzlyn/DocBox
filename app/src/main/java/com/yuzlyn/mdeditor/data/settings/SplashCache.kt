package com.yuzlyn.mdeditor.data.settings

import android.content.Context
import androidx.core.content.edit

/**
 * 開屏動畫用的主題快照。
 *
 * 系統的起始視窗只能用**編譯期資源**，改不到 App 內的亮暗與莫奈取色；所以真正的開屏
 * 由 Compose 畫（見 `ui/component/SplashOverlay.kt`），而畫第一幀時 DataStore 還沒讀回來。
 * 這裡在每次主題真的套用後把結果記下來，下次啟動先照這份畫，設定讀到再換成即時值。
 *
 * 只存底色與字形：標誌本身是桌面上那顆別名圖示，不必快照。
 */
class SplashCache(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** 上次套用的開屏外觀；沒存過就給預設主題（LOTUS）的值。 */
    data class Snapshot(val surface: Int, val glyph: LauncherGlyph)

    /**
     * @param night 沒有快照時的保底底色要跟系統日夜走，避免第一次安裝就閃錯色。
     */
    fun read(night: Boolean): Snapshot = Snapshot(
        surface = prefs.getInt(KEY_SURFACE, if (night) NIGHT_SURFACE else LIGHT_SURFACE),
        glyph = LauncherGlyph.fromId(prefs.getString(KEY_GLYPH, null))
    )

    /** 記下實際套用的底色；值沒變就不寫，這個方法每次重組都會被呼叫。 */
    fun writeSurface(surface: Int) {
        if (prefs.getInt(KEY_SURFACE, 0) == surface) return
        prefs.edit { putInt(KEY_SURFACE, surface) }
    }

    fun writeGlyph(glyph: LauncherGlyph) {
        if (prefs.getString(KEY_GLYPH, null) == glyph.id) return
        prefs.edit { putString(KEY_GLYPH, glyph.id) }
    }

    private companion object {
        const val FILE = "splash_snapshot"
        const val KEY_SURFACE = "surface"
        const val KEY_GLYPH = "glyph"

        /** 預設主題（LOTUS）的淺色 / 深色 surface，與 values/colors.xml 同一組值。 */
        const val LIGHT_SURFACE = 0xFFF7F9F8.toInt()
        const val NIGHT_SURFACE = 0xFF101413.toInt()
    }
}

package com.yuzlyn.mdeditor.data.export

import android.graphics.Bitmap
import android.graphics.Typeface

/**
 * 匯出時套用的視覺主題：背景、文字、輔助線與字型。
 *
 * 這些值由編輯頁算好再傳進來（見 `EditorScreen` 的 `editorColors`）。
 * 匯出的東西必須和螢幕上是同一張紙：使用者把筆記調成米紙、字調成深棕，
 * 分享出去卻是白底黑字，那就不叫「分享這一頁」，只叫「把文字倒出來」。
 *
 * [backgroundImage] 只有圖片格式吃得到：PNG 會把它鋪滿整張圖（等同編輯頁的
 * ContentScale.Crop）；PDF 不嵌圖，只用 [background] 當底色——嵌一張照片進 PDF
 * 會讓檔案大上好幾倍，而且列印時整頁噴墨。
 */
class ExportTheme(
    /** 背景色（ARGB）。 */
    val background: Int,
    /** 正文色（ARGB）。 */
    val text: Int,
    /** 輔助線（分隔線／格線）色（ARGB）。 */
    val guide: Int,
    /** 自訂字型；null 表示用系統預設（PDF 走襯線體，圖片走無襯線體）。 */
    val typeface: Typeface? = null,
    /** 背景圖片；null 表示純色背景。 */
    val backgroundImage: Bitmap? = null
) {
    companion object {
        /** 沒有主題資訊時的白紙黑字，供預設參數與測試使用。 */
        val DEFAULT = ExportTheme(
            background = 0xFFFFFFFF.toInt(),
            text = 0xFF1C1C1E.toInt(),
            guide = 0xFFCFCFCF.toInt()
        )
    }
}

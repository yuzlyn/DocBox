# DocBox 的 R8 規則。
#
# 只寫「R8 猜不到」的，其餘交給各函式庫自帶的 consumer rules：
# Room、Compose、Kotlin 協程、DataStore 都有，不需要在這裡重複宣告。

# ── WebView 的 JS 橋接 ─────────────────────────────────────────────
# JS 端是按方法名呼叫的（window.PreviewBridge.insertImage(…)），
# R8 把方法改名之後名字對不上，功能會靜默失效——不報錯、不崩潰，
# 只是按鈕點了沒反應。MarkdownPreview 與 BookReaderView 兩個橋接
# 物件都靠這條保住。
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ── 崩潰堆疊 ──────────────────────────────────────────────────────
# 保留行號，但把原始檔名換掉：堆疊看得懂，又不會洩漏目錄結構。
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── PDF 的 JPEG2000 解碼器 ────────────────────────────────────────
# PDFBox 的 JPXFilter 支援 JPEG2000 圖片，但解碼器（com.gemalto.jp2）
# 是獨立選用依賴，本專案沒有引入。R8 在 full mode 下會把它當成缺類報錯。
#
# 影響面：只有「內嵌 JPEG2000 圖片的 PDF」會走到這條路，一般 PDF 不會。
# 真的遇到時 PDFBox 會拋異常，匯入該頁會失敗——不會讓整個 App 崩潰。
-dontwarn com.gemalto.jp2.JP2Decoder

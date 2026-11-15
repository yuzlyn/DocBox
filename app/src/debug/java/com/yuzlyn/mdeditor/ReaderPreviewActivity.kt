package com.yuzlyn.mdeditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yuzlyn.mdeditor.data.book.ReaderMode
import com.yuzlyn.mdeditor.data.settings.AppSettings
import com.yuzlyn.mdeditor.ui.reader.ReaderScreen

/**
 * 只在 debug 變體存在的閱讀器預覽。
 *
 * 排版問題（分頁、直排捲動、留白）只有真的排過一次才算數，但一般要點開檔案選擇器
 * 才進得去閱讀器。這支 Activity 讓命令列直接把某一本書、某一章丟進來，
 * 方便逐項比對版面：
 *
 * ```
 * adb shell am start -n com.yuzlyn.mdeditor/.ReaderPreviewActivity \
 *   --es uri file:///data/user/0/com.yuzlyn.mdeditor/cache/preview.epub \
 *   --ei chapter 6 --ez vertical true --es mode scroll
 * ```
 */
class ReaderPreviewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent.getStringExtra("uri")
        if (uri.isNullOrBlank()) {
            finish()
            return
        }
        val vertical = intent.getBooleanExtra("vertical", false)
        val mode = ReaderMode.fromId(intent.getStringExtra("mode"))
        val margin = intent.getIntExtra("margin", 20)
        val lineHeight = intent.getIntExtra("lineHeight", 17)
        val scale = intent.getIntExtra("scale", 100)

        setContent {
            MaterialTheme {
                var settings by remember {
                    mutableStateOf(
                        AppSettings(
                            readerVertical = vertical,
                            readerMode = mode.id,
                            readerMargin = margin,
                            readerLineHeightTenths = lineHeight,
                            readerTextScale = scale
                        )
                    )
                }
                Surface(modifier = Modifier.fillMaxSize()) {
                    ReaderScreen(
                        uriString = uri,
                        displayName = intent.getStringExtra("name") ?: "preview.epub",
                        settings = settings,
                        onChange = { next -> settings = next },
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

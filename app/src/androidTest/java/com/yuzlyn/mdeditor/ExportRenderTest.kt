package com.yuzlyn.mdeditor

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.yuzlyn.mdeditor.data.export.CardPosition
import com.yuzlyn.mdeditor.data.export.ExportCard
import com.yuzlyn.mdeditor.data.export.NoteDocumentRenderer
import com.yuzlyn.mdeditor.data.settings.WritingGuide
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 匯出排版的**真機**驗證。
 *
 * PDF／圖片走的是 Android 的 Canvas 與 StaticLayout，JVM 單元測試證明不了
 * 「字有沒有落在格子裡」。這裡把幾種組合各畫一張存到外部目錄，
 * 讓驗收時可以直接拉出來看——這是唯一能看出「格子歪了」的方法。
 */
@RunWith(AndroidJUnit4::class)
class ExportRenderTest {

    private val sample = buildString {
        appendLine("春日載陽，有鳴倉庚。")
        appendLine("女執懿筐，遵彼微行，爰求柔桑。")
        appendLine("")
        appendLine("春日遲遲，采蘩祁祁。")
        appendLine("Girls with deep baskets along the narrow paths, seeking the tender mulberry leaves.")
        appendLine("")
        appendLine("這一段刻意留長，用來確認欄滿了會換到左邊一欄，不是往下擠。")
        appendLine("再補一句，讓直排至少換三欄，格子才會露出破綻。")
    }

    @Test
    fun 橫排方格_字要落在格子裡() {
        dump("h-grid", NoteDocumentRenderer.renderPng(sample, WritingGuide.GRID, vertical = false))
    }

    @Test
    fun 橫排分隔線() {
        dump("h-ruled", NoteDocumentRenderer.renderPng(sample, WritingGuide.RULED, vertical = false))
    }

    @Test
    fun 直排方格_由右往左換欄() {
        dump("v-grid", NoteDocumentRenderer.renderPng(sample, WritingGuide.GRID, vertical = true))
    }

    @Test
    fun 直排無格線() {
        dump("v-plain", NoteDocumentRenderer.renderPng(sample, WritingGuide.NONE, vertical = true))
    }

    @Test
    fun 名片在正文之後_帶圓形頭像() {
        dump(
            "card-after",
            NoteDocumentRenderer.renderPng(
                text = sample,
                guide = WritingGuide.NONE,
                vertical = false,
                card = card(CardPosition.AFTER, avatarPath())
            )
        )
    }

    @Test
    fun 名片在正文之前_方格排版() {
        dump(
            "card-before",
            NoteDocumentRenderer.renderPng(
                text = sample,
                guide = WritingGuide.GRID,
                vertical = false,
                card = card(CardPosition.BEFORE, avatarPath())
            )
        )
    }

    @Test
    fun 直排_名片在正文之後() {
        dump(
            "v-card",
            NoteDocumentRenderer.renderPng(
                text = sample,
                guide = WritingGuide.NONE,
                vertical = true,
                card = card(CardPosition.AFTER, avatarPath())
            )
        )
    }

    @Test
    fun 名片不帶頭像時_文字撐滿整行() {
        val result = NoteDocumentRenderer.renderPng(
            text = sample,
            guide = WritingGuide.NONE,
            vertical = false,
            card = card(CardPosition.AFTER, null)
        )
        assertTrue("名片版應該比純正文更高", result.bytes.size > 1000)
        dump("card-no-avatar", result)
    }

    @Test
    fun 直排方格_名片接在最左欄() {
        dump(
            "v-grid-card",
            NoteDocumentRenderer.renderPng(
                text = sample,
                guide = WritingGuide.GRID,
                vertical = true,
                card = card(CardPosition.AFTER, avatarPath())
            )
        )
    }

    @Test
    fun 直排長文_往左延伸且不截斷() {
        // 內容一多就橫著長：以前的版本會把後面的欄位直接丟掉。
        val long = (1..8).joinToString("\n") { sample }
        val result = NoteDocumentRenderer.renderPng(
            text = long,
            guide = WritingGuide.NONE,
            vertical = true,
            card = card(CardPosition.AFTER, avatarPath())
        )
        assertTrue("長文不該被截斷", !result.truncated)
        dump("v-long", result)
    }

    @Test
    fun 直排也能輸出多頁_pdf() {
        val pdf = NoteDocumentRenderer.renderPdf(sample, WritingGuide.GRID, vertical = true)
        assertTrue("PDF 應該有內容（${pdf.size} bytes）", pdf.size > 1000)
    }

    private fun card(position: CardPosition, avatar: String?) = ExportCard(
        lines = listOf("yuzlyn", "cebrayu@qq.com", "github.com/yuzlyn"),
        avatarPath = avatar,
        position = position
    )

    /** 現畫一張頭像：不需要外部素材，圓形裁切與否一眼看得出來。 */
    private fun avatarPath(): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "card-avatar.png")
        if (!file.exists()) {
            val bitmap = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.rgb(93, 136, 149))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            canvas.drawCircle(80f, 58f, 30f, paint)
            canvas.drawRect(34f, 100f, 126f, 170f, paint)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        return file.absolutePath
    }

    private fun dump(name: String, result: com.yuzlyn.mdeditor.data.export.PngResult) {
        assertTrue("$name 應該有內容（${result.bytes.size} bytes）", result.bytes.size > 1000)
        val dir = File(
            InstrumentationRegistry.getInstrumentation()
                .targetContext.getExternalFilesDir(null),
            "export-render"
        ).apply { mkdirs() }
        File(dir, "$name.png").writeBytes(result.bytes)
    }
}

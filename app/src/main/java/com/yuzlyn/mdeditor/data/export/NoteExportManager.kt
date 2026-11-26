package com.yuzlyn.mdeditor.data.export

import android.content.Context
import com.yuzlyn.mdeditor.data.settings.WritingGuide
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 匯出結果；[truncated] 只有圖片會用到（內容太長被截斷）。 */
class ExportResult(val bytes: ByteArray, val truncated: Boolean = false)

/**
 * 把當前筆記轉成各種格式，並負責「分享」與「存副本」的落地。
 *
 * 全部在 IO 執行緒產生內容：PDF 排版、圖片編碼都不該卡住輸入。
 */
class NoteExportManager(private val context: Context) {

    suspend fun build(
        title: String,
        body: String,
        format: NoteExportFormat,
        /** 名片（含位置、欄位、頭像）；null 表示不附名片。 */
        card: ExportCard?,
        /** 書寫輔助線；只有 PDF／圖片這種視覺格式吃得到，純文字格式沒有對應概念。 */
        guide: WritingGuide = WritingGuide.NONE,
        /** 直排：跟著筆記自己的排版走（見 AppSettings.verticalLayout）。 */
        vertical: Boolean = false,
        /** 背景／文字／輔助線／字型；只有 PDF 與圖片吃得到。 */
        theme: ExportTheme = ExportTheme.DEFAULT
    ): ExportResult = withContext(Dispatchers.IO) {
        when (format) {
            NoteExportFormat.MARKDOWN ->
                ExportResult(buildMarkdown(title, body, card).toByteArray(Charsets.UTF_8))

            NoteExportFormat.TEXT ->
                ExportResult(buildPlainText(title, body, card).toByteArray(Charsets.UTF_8))

            NoteExportFormat.WORD ->
                ExportResult(DocxWriter.build(buildBlocks(title, body, card)))

            // PDF 沒有「圖」這個概念，名片只以文字附上（頭像開關對它無效）。
            // 底色與字色仍然照筆記走：白底黑字的 PDF 貼給別人，和他在 App 裡看到的不一樣。
            NoteExportFormat.PDF ->
                ExportResult(
                    NoteDocumentRenderer.renderPdf(
                        text = buildPlainText(title, body, card),
                        guide = guide,
                        vertical = vertical,
                        theme = theme
                    )
                )

            NoteExportFormat.IMAGE -> {
                // 圖片走真正的名片帶：頭像、位置、逐欄開關都交給渲染器排。
                val png = NoteDocumentRenderer.renderPng(
                    text = buildPlainText(title, body),
                    guide = guide,
                    vertical = vertical,
                    card = card,
                    theme = theme
                )
                ExportResult(png.bytes, png.truncated)
            }
        }
    }

    fun fileName(title: String, format: NoteExportFormat): String =
        sanitizeFileName(title) + "." + format.extension

    /**
     * 先把內容寫進 cache，再用 FileProvider 授權給接收的 App。
     * 直接丟 `file://` 在 Android 7 以上會被 FileUriExposedException 擋掉。
     */
    fun share(
        title: String,
        result: ExportResult,
        format: NoteExportFormat,
        /** 系統分享面板的標題；由介面按語言傳進來。 */
        chooserTitle: String
    ) {
        val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        val file = File(dir, fileName(title, format))
        file.writeBytes(result.bytes)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            if (title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /** 名片放在正文之前的那一份文字；位置不是「之前」就沒有。 */
    private fun beforeText(card: ExportCard?): String? =
        card?.takeIf { it.position == CardPosition.BEFORE }?.text

    /** 名片放在正文之後的那一份文字。 */
    private fun afterText(card: ExportCard?): String? =
        card?.takeIf { it.position == CardPosition.AFTER }?.text

    private fun buildMarkdown(title: String, body: String, card: ExportCard?): String = buildString {
        if (title.isNotBlank()) append("# ").append(title).append("\n\n")
        beforeText(card)?.let { append(it).append("\n\n---\n\n") }
        append(body.trimEnd())
        afterText(card)?.let { append("\n\n---\n\n").append(it) }
        append("\n")
    }

    private fun buildPlainText(
        title: String,
        body: String,
        card: ExportCard? = null
    ): String = buildString {
        if (title.isNotBlank()) append(title).append("\n\n")
        // 名片在前：先名片、再一分隔線，正文才不會像附註。
        beforeText(card)?.let { append(it).append("\n\n———\n\n") }
        append(markdownToPlainText(body))
        afterText(card)?.let { append("\n\n———\n").append(it) }
        append("\n")
    }

    private fun buildBlocks(title: String, body: String, card: ExportCard?): List<DocxBlock> {
        val blocks = mutableListOf<DocxBlock>()
        if (title.isNotBlank()) blocks += DocxBlock(title, headingLevel = 1)
        val before = beforeText(card)
        if (before != null) {
            before.lines().forEach { blocks += DocxBlock(it) }
            blocks += DocxBlock("")
            blocks += DocxBlock("———")
        }
        blocks += markdownToDocxBlocks(body)
        val after = afterText(card)
        if (after != null) {
            blocks += DocxBlock("")
            blocks += DocxBlock("———")
            after.lines().forEach { blocks += DocxBlock(it) }
        }
        return blocks
    }

    private companion object {
        const val EXPORT_DIR = "exports"
    }
}

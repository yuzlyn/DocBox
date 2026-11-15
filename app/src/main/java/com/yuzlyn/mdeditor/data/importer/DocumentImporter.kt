package com.yuzlyn.mdeditor.data.importer

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * 匯入時認得的檔案格式，同時是「能力矩陣」的單一來源：
 * 副檔名、MIME、短標籤（筆記卡片上的徽章）都寫在這裡，UI 直接引用。
 *
 * `id` 會存進資料庫，**不要改**；顯示文字可以隨便調。
 */
enum class ImportFormat(
    val id: String,
    val label: String,
    val shortLabel: String,
    val extensions: List<String>,
    val mimeTypes: List<String>
) {
    DOCX(
        "docx",
        "Word 文件",
        "Word",
        listOf("docx"),
        listOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ),
    PDF(
        "pdf",
        "PDF 文件",
        "PDF",
        listOf("pdf"),
        listOf("application/pdf")
    ),
    MARKDOWN(
        "md",
        "Markdown",
        "MD",
        listOf("md", "markdown"),
        listOf("text/markdown", "text/x-markdown")
    ),
    TEXT(
        "txt",
        "純文字",
        "TXT",
        listOf("txt", "text", "log"),
        listOf("text/plain")
    ),
    EPUB(
        "epub",
        "EPUB 電子書",
        "EPUB",
        listOf("epub"),
        listOf("application/epub+zip")
    ),
    MOBI(
        "mobi",
        "MOBI 電子書",
        "MOBI",
        listOf("mobi", "prc"),
        listOf("application/x-mobipocket-ebook")
    ),
    AZW3(
        "azw3",
        "AZW3 電子書",
        "AZW3",
        listOf("azw3"),
        listOf("application/vnd.amazon.ebook")
    ),
    HTML(
        "html",
        "HTML 網頁",
        "HTML",
        listOf("html", "htm", "xhtml"),
        listOf("text/html", "application/xhtml+xml")
    );

    companion object {
        fun fromFileName(name: String?): ImportFormat? {
            val extension = name?.substringAfterLast('.', "")?.lowercase() ?: return null
            if (extension.isEmpty()) return null
            return entries.firstOrNull { extension in it.extensions }
        }

        fun fromId(id: String?): ImportFormat? =
            id?.takeIf { it.isNotBlank() }?.let { value -> entries.firstOrNull { it.id == value } }

        /** 檔案選擇器的 MIME 清單；最後補上「所有檔案」，避免某些 provider 把少見格式濾掉。 */
        val pickerMimeTypes: Array<String> = (entries.flatMap { it.mimeTypes } + "*/*")
            .distinct()
            .toTypedArray()
    }
}

/** 匯入結果：成功帶文字與來源格式，失敗帶可以直接給使用者看的原因。 */
sealed interface ImportResult {
    data class Success(val text: String, val format: ImportFormat? = null) : ImportResult

    data class Failure(val reason: String) : ImportResult
}

/**
 * 把各種來源檔案轉成可以放進筆記的 Markdown／純文字。
 *
 * 判斷順序：**先看副檔名，再看檔頭**。使用者的檔案常常沒有副檔名
 * （從雲端硬碟、聊天軟體轉存下來的特別容易），單靠名字會猜錯。
 *
 * 能力邊界（失敗時一律回傳看得懂的原因，不吐亂碼）：
 * | 格式 | 方式 | 限制 |
 * |---|---|---|
 * | docx | 解析 OOXML | 圖片、頁首頁尾、註解不匯入 |
 * | pdf | PDFBox-Android 取文字（15+ 再用系統 API 兜底） | 掃描檔沒有文字層 |
 * | txt / md | 解碼（UTF-8，必要時 GBK） | — |
 * | html | HTML → Markdown，保留標題／清單／連結／程式碼 | 複雜版式會退成單欄 |
 *
 * 電子書（EPUB／MOBI／AZW3／FB2）**不在這裡**：那幾種由 `data.book` 原生解析、
 * [com.yuzlyn.mdeditor.ui.reader.ReaderScreen] 直接排版，不轉成 Markdown。
 */
object DocumentImporter {

    suspend fun import(context: Context, uri: Uri, fileName: String?): ImportResult =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val byName = ImportFormat.fromFileName(fileName)
                ?: ImportFormat.fromFileName(uri.lastPathSegment)

            // PDF 需要可 seek 的檔案描述符，不能整包讀成 byte array。
            if (byName == ImportFormat.PDF) {
                return@withContext PdfTextExtractor.extract(context, uri).tagged(ImportFormat.PDF)
            }

            val bytes = readBytes(context, uri)
                ?: return@withContext ImportResult.Failure("讀不到這個檔案")

            // 沒有副檔名（或認不得）：看檔頭。
            val format = byName ?: detect(bytes)
            if (format == ImportFormat.PDF) {
                return@withContext ImportResult.Failure("這個 PDF 沒有副檔名，請確認檔案完整後再試一次")
            }
            importBytes(bytes, format)
        }

    private fun importBytes(bytes: ByteArray, format: ImportFormat?): ImportResult =
        when (format) {
            ImportFormat.DOCX -> DocxReader.toMarkdown(bytes)
                ?.let { ImportResult.Success(it, ImportFormat.DOCX) }
                ?: ImportResult.Failure("這個檔案不是可讀取的 Word（.docx）文件")

            ImportFormat.MARKDOWN, ImportFormat.TEXT -> {
                val text = decodeText(bytes)
                if (text.isBlank()) {
                    ImportResult.Failure("這個檔案沒有內容")
                } else {
                    ImportResult.Success(text, format)
                }
            }

            ImportFormat.HTML -> {
                val markdown = HtmlReader.toMarkdown(decodeText(bytes))
                if (markdown.isBlank()) {
                    ImportResult.Failure("這個 HTML 沒有可匯入的文字內容")
                } else {
                    ImportResult.Success(markdown, ImportFormat.HTML)
                }
            }

            ImportFormat.EPUB, ImportFormat.MOBI, ImportFormat.AZW3 ->
                // 電子書走原生閱讀（
                // UiViewModel.openDocument → ReaderScreen），不拆成 Markdown。
                // 這裡只是兜底：真的走到這一步就直說，不要吐一份殘缺的純文字。
                ImportResult.Failure("這是電子書，請直接開啟閱讀（不再轉成筆記）")

            // 認不出格式就當純文字；真的有 binary 內容會是空白，這裡會給明確訊息。
            null -> {
                val text = decodeText(bytes)
                if (text.isBlank()) {
                    ImportResult.Failure("不支援這個檔案格式")
                } else {
                    ImportResult.Success(text, ImportFormat.TEXT)
                }
            }

            ImportFormat.PDF -> ImportResult.Failure("這個 PDF 無法直接讀取，請確認檔案完整")
        }

    private fun readBytes(context: Context, uri: Uri): ByteArray? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull()

    /** 沒有副檔名時靠檔頭判斷；回 null 代表「當純文字處理」。 */
    internal fun detect(bytes: ByteArray): ImportFormat? {
        if (isPdf(bytes)) return ImportFormat.PDF
        if (looksLikeZip(bytes)) {
            val names = zipEntryNames(bytes)
            return when {
                names.any { it == "word/document.xml" } -> ImportFormat.DOCX
                // EPUB 也是 ZIP，但那是書不是文件：不回報格式，讓上層去走閱讀器。
                else -> null
            }
        }
        if (looksLikeHtml(bytes)) return ImportFormat.HTML
        return null
    }

    internal fun isPdf(bytes: ByteArray): Boolean =
        bytes.size > 4 && String(bytes, 0, 4, Charsets.US_ASCII) == "%PDF"

    internal fun looksLikeHtml(bytes: ByteArray): Boolean {
        val head = String(bytes, 0, minOf(bytes.size, 1024), Charsets.UTF_8)
            .trimStart('\uFEFF', ' ', '\t', '\n', '\r')
            .lowercase()
        return head.startsWith("<!doctype html") ||
                head.startsWith("<html") ||
                head.contains("<html") ||
                head.contains("<body")
    }

    private fun looksLikeZip(bytes: ByteArray): Boolean =
        bytes.size > 4 &&
                bytes[0] == 'P'.code.toByte() &&
                bytes[1] == 'K'.code.toByte()

    private fun zipEntryNames(bytes: ByteArray): Set<String> = runCatching {        val names = mutableSetOf<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                names += entry.name
            }
        }
        names
    }.getOrDefault(emptySet())

    /**
     * 純文字解碼：先當 UTF-8（去掉 BOM），出現替換字元就改用 GBK。
     * 中文使用者手上的 .txt 有不少是 GBK，直接硬解會整篇變亂碼。
     */
    internal fun decodeText(bytes: ByteArray): String {
        val offset = if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        ) 3 else 0
        val utf8 = String(bytes, offset, bytes.size - offset, Charsets.UTF_8)
        if (!utf8.contains('\uFFFD')) return utf8
        return runCatching {
            String(bytes, offset, bytes.size - offset, java.nio.charset.Charset.forName("GBK"))
        }.getOrDefault(utf8)
    }
}

/**
 * PDF 文字擷取。
 *
 * 兩條路：
 * 1. **PDFBox-Android**（Apache-2.0）——所有 Android 版本都能用，是主力。
 * 2. Android 15+ 的 `PdfRenderer.getTextContents()`——PDFBox 抽不到時再試一次，
 *    兩者失敗原因不同（例如掃描檔在兩邊都是空的，但原生訊息更精確）。
 */
object PdfTextExtractor {

    @Volatile
    private var resourceReady = false

    fun extract(context: Context, uri: Uri): ImportResult {
        val box = extractWithPdfBox(context, uri)
        if (box is ImportResult.Success) return box
        // 舊版系統沒有原生 API，PDFBox 的結果就是最終結果。
        if (Build.VERSION.SDK_INT < 35) return box
        return extractOnModernAndroid(context, uri)
    }

    private fun extractWithPdfBox(context: Context, uri: Uri): ImportResult = runCatching {
        if (!resourceReady) {
            // PDFBox 的 CMap／字型資源放在 assets，第一次使用要先載入。
            PDFBoxResourceLoader.init(context.applicationContext)
            resourceReady = true
        }
        val stream = context.contentResolver.openInputStream(uri)
            ?: return ImportResult.Failure("讀不到這個 PDF")
        stream.use {
            PDDocument.load(it).use { document ->
                val text = PDFTextStripper().getText(document)
                    .replace(Regex("""[ \t]+"""), " ")
                    .replace(Regex("""\n{3,}"""), "\n\n")
                    .trim()
                return if (text.isBlank()) {
                    ImportResult.Failure("這個 PDF 沒有文字層（多半是掃描檔），無法匯入文字")
                } else {
                    ImportResult.Success(text)
                }
            }
        }
    }.getOrElse { ImportResult.Failure("解析 PDF 失敗：${it.message ?: "未知錯誤"}") }

    @RequiresApi(35)
    private fun extractOnModernAndroid(context: Context, uri: Uri): ImportResult {
        var descriptor: ParcelFileDescriptor? = null
        return try {
            descriptor = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return ImportResult.Failure("讀不到這個 PDF")
            val renderer = PdfRenderer(descriptor)
            try {
                val builder = StringBuilder()
                for (index in 0 until renderer.pageCount) {
                    val page = renderer.openPage(index)
                    try {
                        page.textContents.forEach { content ->
                            val line = content.text.trim()
                            if (line.isNotEmpty()) builder.append(line).append('\n')
                        }
                    } finally {
                        page.close()
                    }
                    builder.append('\n')
                }
                val text = builder.toString().trim()
                if (text.isBlank()) {
                    ImportResult.Failure("這個 PDF 沒有文字層（多半是掃描檔），無法匯入文字")
                } else {
                    ImportResult.Success(text)
                }
            } finally {
                renderer.close()
            }
        } catch (e: Exception) {
            ImportResult.Failure("解析 PDF 失敗：${e.message ?: "未知錯誤"}")
        } finally {
            runCatching { descriptor?.close() }
        }
    }
}

/** 把「來源格式」補進結果；失敗原樣傳回。 */
internal fun ImportResult.tagged(format: ImportFormat?): ImportResult = when (this) {
    is ImportResult.Success -> if (this.format == null) copy(format = format) else this
    is ImportResult.Failure -> this
}

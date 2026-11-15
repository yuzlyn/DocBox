package com.yuzlyn.mdeditor.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 插入圖片失敗的種類；句子由介面按語言組。 */
enum class ImageInsertError {
    /** 筆記還沒載入完，拿不到可寫入的 Uri。 */
    NOTE_NOT_LOADED,
    PERMISSION_DENIED,
    UNKNOWN,
    /** 這篇筆記不是 SAF 文件（例如已關聯的 PDF 副本）。 */
    NOT_WRITABLE,
    UNSUPPORTED_FORMAT,
    /** 建不出 `images/` 目錄。 */
    IMAGES_DIR_FAILED,
    CREATE_FAILED,
    COPY_FAILED
}

/** 插入圖片的結果；失敗時帶錯誤種類，[detail] 只放技術細節。 */
sealed interface ImageInsertResult {
    /** [markdownPath] 是相對於筆記檔案的圖片路徑，例如 `images/img_20251113_124500.jpg`。 */
    data class Success(val markdownPath: String, val alt: String) : ImageInsertResult

    data class Failure(val error: ImageInsertError, val detail: String? = null) : ImageInsertResult
}

/**
 * 把使用者選的圖片存進筆記資料夾，並回傳可寫進 Markdown 的相對路徑。
 *
 * 為什麼不直接用相簿的 `content://` 位址：那個授權是暫時的，重開 App 就失效，
 * 而且 `.md` 換個地方放圖片就全掛。所以複製一份到 `<筆記目錄>/images/`，
 * Markdown 只寫 `images/xxx.jpg`——資料夾搬走、用別的編輯器打開都還在。
 */
class NoteAttachments(context: Context) {

    private val resolver = context.applicationContext.contentResolver

    suspend fun attachImage(noteUriString: String, source: Uri): ImageInsertResult =
        withContext(Dispatchers.IO) {
            try {
                attachImageInternal(noteUriString, source)
            } catch (e: SecurityException) {
                Log.e(TAG, "[NoteAttachments] 讀取圖片權限不足: ${e.message}", e)
                ImageInsertResult.Failure(ImageInsertError.PERMISSION_DENIED)
            } catch (e: Exception) {
                Log.e(TAG, "[NoteAttachments] 插入圖片失敗: ${e.message}", e)
                ImageInsertResult.Failure(ImageInsertError.UNKNOWN, e.message)
            }
        }

    private fun attachImageInternal(noteUriString: String, source: Uri): ImageInsertResult {
        val noteUri = Uri.parse(noteUriString)
        val noteDocId = runCatching { DocumentsContract.getDocumentId(noteUri) }.getOrNull()
            ?: return ImageInsertResult.Failure(ImageInsertError.NOT_WRITABLE)

        val sourceName = displayName(source) ?: "image"
        val mime = resolveMime(source, sourceName)
            ?: return ImageInsertResult.Failure(ImageInsertError.UNSUPPORTED_FORMAT)

        val parentId = if ('/' in noteDocId) {
            noteDocId.substringBeforeLast('/')
        } else {
            // 筆記就在資料夾根層：父目錄就是這棵樹的根。
            runCatching { DocumentsContract.getTreeDocumentId(noteUri) }
                .getOrDefault(noteDocId)
        }

        val imagesDirUri = findOrCreateImagesDir(noteUri, parentId)
            ?: return ImageInsertResult.Failure(ImageInsertError.IMAGES_DIR_FAILED)
        val imagesDirId = runCatching { DocumentsContract.getDocumentId(imagesDirUri) }
            .getOrDefault("")

        val extension = imageExtensionFor(mime, sourceName)
        val base = "img_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = uniqueName(noteUri, imagesDirId, base, extension)

        val targetUri = runCatching {
            DocumentsContract.createDocument(resolver, imagesDirUri, mime, fileName)
        }.getOrNull() ?: return ImageInsertResult.Failure(ImageInsertError.CREATE_FAILED)

        val written = runCatching {
            val input = resolver.openInputStream(source)
                ?: throw IllegalStateException("cannot read source image")
            input.use { from ->
                val output = resolver.openOutputStream(targetUri, "wt")
                    ?: throw IllegalStateException("cannot write into note folder")
                output.use { to -> from.copyTo(to) }
            }
        }.isSuccess

        if (!written) {
            runCatching { DocumentsContract.deleteDocument(resolver, targetUri) }
            return ImageInsertResult.Failure(ImageInsertError.COPY_FAILED)
        }

        // provider 可能因為同名而自動改名（例如加上 " (1)"），要用實際寫入的檔名，
        // 否則 Markdown 裡的相對路徑會指向不存在的檔案。
        val actualName = displayName(targetUri) ?: fileName

        return ImageInsertResult.Success(
            markdownPath = "$IMAGES_DIR/$actualName",
            alt = sanitizeAlt(sourceName.substringBeforeLast('.').ifBlank { "image" })
        )
    }

    /** 找 `<父目錄>/images`，沒有就建一個。 */
    private fun findOrCreateImagesDir(noteUri: Uri, parentId: String): Uri? {
        findChild(noteUri, parentId, IMAGES_DIR, DocumentsContract.Document.MIME_TYPE_DIR)
            ?.let { return it }
        val parentUri = DocumentsContract.buildDocumentUriUsingTree(noteUri, parentId)
        runCatching {
            DocumentsContract.createDocument(
                resolver,
                parentUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                IMAGES_DIR
            )
        }.getOrNull()?.let { return it }
        // 併發或 provider 行為差異：再找一次，真的沒有才放棄。
        return findChild(noteUri, parentId, IMAGES_DIR, DocumentsContract.Document.MIME_TYPE_DIR)
    }

    private fun findChild(
        noteUri: Uri,
        parentId: String,
        name: String,
        mime: String?
    ): Uri? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(noteUri, parentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        return runCatching {
            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    val display = cursor.getString(1)
                    val type = cursor.getString(2)
                    if (display == name && (mime == null || type == mime)) {
                        return@use DocumentsContract.buildDocumentUriUsingTree(noteUri, id)
                    }
                }
                null
            }
        }.getOrNull()
    }

    private fun uniqueName(noteUri: Uri, dirId: String, base: String, extension: String): String {
        if (dirId.isEmpty()) return "$base.$extension"
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(noteUri, dirId)
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val existing = runCatching {
            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                buildSet {
                    while (cursor.moveToNext()) add(cursor.getString(0))
                }
            }
        }.getOrNull().orEmpty()

        var name = "$base.$extension"
        var index = 2
        while (name in existing) {
            name = "${base}_$index.$extension"
            index++
        }
        return name
    }

    private fun displayName(uri: Uri): String? = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    private fun resolveMime(uri: Uri, name: String): String? {
        val declared = runCatching { resolver.getType(uri) }.getOrNull()
        if (declared != null && declared.startsWith("image/")) return declared
        return imageMimeForName(name)
    }

    /** 清掉會破壞 Markdown 連結語法的字元。 */
    private fun sanitizeAlt(value: String): String = sanitizeImageAlt(value)
    companion object {
        private const val TAG = "MDEditor_Debug"
        const val IMAGES_DIR = "images"
    }
}

/** 依照 MIME 決定副檔名；認不得就從原檔名借，再不行當 jpg。 */
internal fun imageExtensionFor(mime: String, sourceName: String): String {
    return when (mime.lowercase(Locale.US)) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        "image/svg+xml" -> "svg"
        "image/bmp" -> "bmp"
        "image/heic" -> "heic"
        "image/heif" -> "heif"
        "image/avif" -> "avif"
        else -> sourceName.substringAfterLast('.', "").lowercase(Locale.US)
            .takeIf { it.isNotEmpty() && it.length <= 5 } ?: "jpg"
    }
}

/** 圖片在 Markdown 裡的替代文字：不能帶括號，否則連結會斷掉。 */
internal fun sanitizeImageAlt(value: String): String =
    value.replace(Regex("""[\[\]()\\]"""), "_").trim().ifBlank { "image" }

/** 檔名 → MIME；給預覽載入圖片時回 Response 用。 */
internal fun imageMimeForName(name: String): String? =
    when (name.substringAfterLast('.', "").lowercase(Locale.US)) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "svg" -> "image/svg+xml"
        "bmp" -> "image/bmp"
        "heic" -> "image/heic"
        "heif" -> "image/heif"
        "avif" -> "image/avif"
        else -> null
    }

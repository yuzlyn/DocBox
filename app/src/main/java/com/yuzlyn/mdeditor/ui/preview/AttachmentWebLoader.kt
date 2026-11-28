package com.yuzlyn.mdeditor.ui.preview

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.webkit.WebResourceResponse
import androidx.core.net.toUri
import com.yuzlyn.mdeditor.data.NoteAttachments
import com.yuzlyn.mdeditor.data.imageMimeForName

/**
 * 讓預覽頁顯示筆記資料夾裡的圖片。
 *
 * 預覽頁是從 `file:///android_asset/preview/preview.html` 載入的，所以 Markdown 裡的
 * `images/xxx.jpg` 會被 WebView 解析成 `file:///android_asset/preview/images/xxx.jpg`——
 * 這個檔案當然不存在。這裡在請求發出時攔下來，改成去筆記所在資料夾的 `images/`
 * 目錄找同名檔案，透過 ContentResolver 讀出來回傳。
 *
 * 這樣 Markdown 只寫相對路徑（可攜、換編輯器也看得到圖），App 也不需要直接檔案權限。
 */
internal class AttachmentWebLoader(context: Context, noteUriString: String?) {

    private val resolver = context.applicationContext.contentResolver
    private val cache = HashMap<String, Uri?>()

    private val imagesChildrenUri: Uri? = noteUriString?.let { raw ->
        runCatching {
            val noteUri = raw.toUri()
            val documentId = DocumentsContract.getDocumentId(noteUri)
            val parentId = if ('/' in documentId) {
                documentId.substringBeforeLast('/')
            } else {
                DocumentsContract.getTreeDocumentId(noteUri)
            }
            DocumentsContract.buildChildDocumentsUriUsingTree(
                noteUri,
                "$parentId/${NoteAttachments.IMAGES_DIR}"
            )
        }.onFailure {
            Log.e(TAG, "[AttachmentWebLoader] 無法定位圖片目錄: ${it.message}")
        }.getOrNull()
    }

    /** 不是 `images/` 底下的請求就回 null，交還給 WebView 預設處理。 */
    fun intercept(url: Uri): WebResourceResponse? {
        if (url.scheme != "file") return null
        val path = url.path ?: return null
        if (!path.startsWith(ASSET_IMAGE_PREFIX)) return null
        val name = path.removePrefix(ASSET_IMAGE_PREFIX)
        if (name.isEmpty() || name.contains('/')) return null

        val document = lookup(name) ?: return null
        val stream = runCatching { resolver.openInputStream(document) }.getOrNull() ?: run {
            Log.e(TAG, "[AttachmentWebLoader] 讀取圖片失敗: $name")
            return null
        }
        return WebResourceResponse(
            imageMimeForName(name) ?: "application/octet-stream",
            "UTF-8",
            200,
            "OK",
            mapOf("Cache-Control" to "no-store"),
            stream
        )
    }

    private fun lookup(name: String): Uri? = synchronized(cache) {
        if (cache.containsKey(name)) return cache[name]
        val found = queryChild(name)
        cache[name] = found
        found
    }

    private fun queryChild(name: String): Uri? {
        val childrenUri = imagesChildrenUri ?: return null
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )
        return runCatching {
            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getString(1) == name) {
                        return@use DocumentsContract.buildDocumentUriUsingTree(
                            childrenUri,
                            cursor.getString(0)
                        )
                    }
                }
                null
            }
        }.getOrNull()
    }

    companion object {
        private const val TAG = "MDEditor_Preview"

        /** 預覽頁的 assets 目錄；相對路徑都會被解析到這裡。 */
        private const val ASSET_IMAGE_PREFIX = "/android_asset/preview/${NoteAttachments.IMAGES_DIR}/"
    }
}

package com.yuzlyn.mdeditor.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 一次資料夾掃描的結果。
 *
 * [accessible] 為 false 代表根目錄當下讀不到（權限被回收、卡片還沒掛載、雲端硬碟離線…）。
 * 這種情況呼叫端不能把掃描結果當成「檔案都被刪了」，否則資料庫裡還好好的內容會被清空。
 */
data class NoteScan(
    val notes: List<NoteModel>,
    val accessible: Boolean
)

class SafNoteDataSource(context: Context) {

    private val appContext = context.applicationContext

    /** 寫入序號化：同一時間只允許一個寫入，避免兩個協程同時截斷同一個檔案。 */
    private val writeMutex = Mutex()

    /** 所有 SAF 檔案 IO 都在此資料來源內切到 Dispatchers.IO。 */
    suspend fun takePersistablePermission(treeUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            appContext.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "[SafNoteDataSource] 鎖定權限失敗: ${e.message}", e)
            false
        }
    }

    suspend fun scanNotes(treeUri: Uri): NoteScan = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(appContext, treeUri)
        if (root == null || !root.exists()) {
            Log.e(TAG, "[SafNoteDataSource] 無法存取根目錄: $treeUri")
            return@withContext NoteScan(emptyList(), accessible = false)
        }

        val mdFiles = mutableListOf<Pair<DocumentFile, String>>()
        val traversed = try {
            traverse(root, mdFiles, root.name ?: DEFAULT_TAG)
            true
        } catch (e: Exception) {
            Log.e(TAG, "[SafNoteDataSource] 掃描目錄失敗: ${e.message}", e)
            false
        }
        if (!traversed) return@withContext NoteScan(emptyList(), accessible = false)

        val notes = mdFiles.map { (document, parentTag) ->
            // 任何一個欄位讀不到都只影響那一篇的中繼資料，不能讓整次掃描失敗
            // （掃描失敗會被當成「檔案都消失了」，反而害資料庫被清掉）。
            val fileName = runCatching { document.name }.getOrNull() ?: "untitled.md"
            val title = if (fileName.endsWith(".md", ignoreCase = true)) {
                fileName.dropLast(3)
            } else {
                fileName
            }
            // 讀不到（暫時性的 IO 失敗）要和「檔案本來就空」分開：
            // 前者回傳 null，匯入時會保留資料庫裡原本的內容，不會把筆記清空。
            val text = readText(document.uri)
            val modified = runCatching { document.lastModified() }.getOrDefault(0L)
            NoteModel(
                id = document.uri.toString(),
                title = title,
                uriString = document.uri.toString(),
                content = text ?: "",
                notebookTag = parentTag,
                lastModified = modified,
                createdAt = modified,
                contentUnavailable = text == null
            )
        }
        NoteScan(notes, accessible = true)
    }

    suspend fun createNote(treeUri: Uri, title: String, notebookTag: String): NoteModel? =
        withContext(Dispatchers.IO) {
            val root = DocumentFile.fromTreeUri(appContext, treeUri)
                ?: return@withContext null
            val targetDir = if (notebookTag != DEFAULT_TAG && notebookTag != (root.name ?: "")) {
                findOrCreateSubDirectory(root, notebookTag)
            } else {
                root
            }

            val timestamp = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date(timestamp))
            val safeFileName = title.replace(Regex("""[/\\:*?"<>|]"""), "_")
            val document = targetDir.createFile("text/markdown", "${safeFileName}_$dateStr.md")
                ?: return@withContext null

            NoteModel(
                id = document.uri.toString(),
                title = title,
                uriString = document.uri.toString(),
                content = "",
                notebookTag = notebookTag,
                lastModified = timestamp,
                createdAt = timestamp
            )
        }

    /**
     * 把內容寫回 .md 檔。
     *
     * 舊寫法只試一次 `"wt"`，任何一個 provider 不認這個 mode 就直接失敗，
     * 而呼叫端又把失敗吞掉，於是資料庫有新內容、檔案還是舊的，下次啟動掃描時
     * 再用檔案內容蓋掉資料庫——這就是內容消失的主因。
     *
     * 現在：逐個 mode 重試（`rwt` 帶截斷、`wt` 帶截斷），寫完比對檔案大小，
     * 任何一步不對就回 false，由呼叫端保留草稿下次再試。
     */
    suspend fun writeContent(uriString: String, content: String): Boolean =
        withContext(Dispatchers.IO) {
            writeMutex.withLock {
                val uri = Uri.parse(uriString)
                val bytes = content.toByteArray(Charsets.UTF_8)
                WRITE_MODES.any { mode -> writeWithMode(uri, bytes, mode) }
            }
        }

    private fun writeWithMode(uri: Uri, bytes: ByteArray, mode: String): Boolean {
        return try {
            val stream = appContext.contentResolver.openOutputStream(uri, mode)
            if (stream == null) {
                Log.e(TAG, "[SafNoteDataSource] 無法開啟輸出串流 mode=$mode: $uri")
                return false
            }
            stream.use { output ->
                output.write(bytes)
                output.flush()
                // 盡量把資料推到儲存裝置；部分檔案系統不支援 sync，失敗不影響已寫入的內容。
                runCatching { (output as? FileOutputStream)?.fd?.sync() }
            }
            val size = fileSize(uri)
            if (size >= 0 && size != bytes.size.toLong()) {
                Log.e(
                    TAG,
                    "[SafNoteDataSource] 寫入長度不符 mode=$mode: 預期 ${bytes.size}，實際 $size"
                )
                false
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "[SafNoteDataSource] SAF 寫回失敗 mode=$mode: ${e.message}", e)
            false
        }
    }

    private fun fileSize(uri: Uri): Long = try {
        appContext.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
    } catch (e: Exception) {
        -1L
    }

    suspend fun deleteDocument(uriString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            DocumentsContract.deleteDocument(appContext.contentResolver, Uri.parse(uriString))
        } catch (e: Exception) {
            Log.e(TAG, "[SafNoteDataSource] SAF 刪除失敗: ${e.message}", e)
            false
        }
    }

    private fun traverse(
        parent: DocumentFile,
        result: MutableList<Pair<DocumentFile, String>>,
        currentTag: String
    ) {
        parent.listFiles().forEach { child ->
            when {
                child.isDirectory -> traverse(child, result, child.name ?: currentTag)
                child.isFile && child.name?.endsWith(".md", ignoreCase = true) == true ->
                    result.add(child to currentTag)
            }
        }
    }

    /** 讀不到回傳 null；空檔案回傳空字串。兩者意義不同，不能混用。 */
    private fun readText(uri: Uri): String? = try {
        appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
        } ?: run {
            Log.e(TAG, "[SafNoteDataSource] 無法開啟輸入串流: $uri")
            null
        }
    } catch (e: Exception) {
        Log.e(TAG, "[SafNoteDataSource] 讀取 URI 文本失敗: $uri, ${e.message}")
        null
    }

    private fun findOrCreateSubDirectory(parent: DocumentFile, dirName: String): DocumentFile {
        parent.listFiles().firstOrNull { it.isDirectory && it.name == dirName }
            ?.let { return it }
        parent.createDirectory(dirName)?.let { return it }
        Log.e(TAG, "[SafNoteDataSource] 無法創建子目錄: $dirName，使用根目錄")
        return parent
    }

    companion object {
        private const val TAG = "MDEditor_Debug"
        const val DEFAULT_TAG = "默認筆記本"

        /**
         * 依序嘗試的寫入模式。
         * `rwt` 與 `wt` 都會先截斷檔案，避免新內容比舊內容短時留下殘尾；
         * 前者對部分 provider 較嚴格，所以留 `wt` 當備援。
         */
        private val WRITE_MODES = listOf("rwt", "wt")
    }
}

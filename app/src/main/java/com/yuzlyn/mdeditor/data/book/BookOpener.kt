package com.yuzlyn.mdeditor.data.book

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.security.MessageDigest

/**
 * 把 `content://` 的書變成可隨機讀取的 [Book]。
 *
 * EPUB 要 `ZipFile`、AZW3 要 `RandomAccessFile`，兩者都只認檔案系統上的路徑，
 * 而 SAF 給的是 content Uri，所以第一次開啟時得先串流複製到快取目錄。
 * 複製是一次性的：之後開啟同一本書直接命中快取。
 *
 * 注意這裡**不會**把整個檔案讀進記憶體，8KB 緩衝逐段搬，多大的書都不會爆。
 */
object BookOpener {

    private const val CACHE_DIR = "books"

    /** 快取上限；超過就從最舊的開始清，避免使用者手機被書塞滿。 */
    private const val CACHE_LIMIT_BYTES = 512L * 1024 * 1024

    suspend fun open(context: Context, uri: Uri, displayName: String? = null): Book {
        val name = displayName ?: queryDisplayName(context, uri)
        val file = materialize(context, uri, name)
        val format = BookFormat.fromFileName(name) ?: BookFormat.sniff(file)
        return when (format) {
            BookFormat.EPUB -> EpubBook.open(file)
            BookFormat.MOBI, BookFormat.AZW3 -> MobiBook.open(file)
            BookFormat.FB2 -> Fb2Book.open(file)
            null -> {
                // 檔名不可信時看檔頭；ZIP 也可能不是 EPUB。
                when {
                    EpubBook.isZip(file) -> EpubBook.open(file)
                    else -> throw BookException("無法辨識這個檔案的格式")
                }
            }
        }
    }

    fun queryDisplayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()

    /**
     * 看檔頭判斷是不是書。
     *
     * 只用來回答「要不要開閱讀器」，所以只讀前 80 個位元組、不落地。
     * 使用者手上的書常常沒有副檔名（雲端硬碟轉存、聊天軟體下載），
     * 只看檔名會整批漏掉，然後被當成純文字匯入成一篇亂碼筆記。
     */
    fun sniff(context: Context, uri: Uri): BookFormat? {
        if (uri.scheme == "file") {
            val path = uri.path ?: return null
            return BookFormat.sniff(File(path))
        }
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val head = ByteArray(80)
                var read = 0
                while (read < head.size) {
                    val count = stream.read(head, read, head.size - read)
                    if (count <= 0) break
                    read += count
                }
                BookFormat.sniffHead(head.copyOf(read))
            }
        }.getOrNull()
    }

    /** 取得可隨機讀取的本地檔案；已經在檔案系統上就原樣回傳。 */
    private fun materialize(context: Context, uri: Uri, name: String?): File {
        if (uri.scheme == "file") {
            val path = uri.path ?: throw BookException("檔案路徑無效")
            val file = File(path)
            if (file.isFile) return file
            throw BookException("找不到這個檔案")
        }
        val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
        val extension = name?.substringAfterLast('.', "")?.lowercase(java.util.Locale.ROOT)
            ?.takeIf { it.isNotEmpty() && it.length <= 5 }
            ?: "bin"
        val target = File(dir, "${digest(uri.toString())}.$extension")
        if (target.isFile && target.length() > 0) {
            target.setLastModified(System.currentTimeMillis())
            return target
        }

        val temporary = File(dir, target.name + ".part")
        val source = context.contentResolver.openInputStream(uri)
            ?: throw BookException("讀不到這個檔案（授權可能已失效）")
        source.use { input ->
            temporary.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                }
            }
        }
        if (temporary.length() == 0L) {
            temporary.delete()
            throw BookException("這個檔案是空的")
        }
        if (target.isFile) target.delete()
        if (!temporary.renameTo(target)) {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
        }
        prune(dir)
        return target
    }

    private fun prune(dir: File) {
        val files = dir.listFiles()?.filter { it.isFile } ?: return
        var total = files.sumOf { it.length() }
        if (total <= CACHE_LIMIT_BYTES) return
        files.sortedBy { it.lastModified() }.forEach { file ->
            if (total <= CACHE_LIMIT_BYTES) return@forEach
            val length = file.length()
            if (file.delete()) total -= length
        }
    }

    private fun digest(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }.take(24)
    }
}

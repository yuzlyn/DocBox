package com.yuzlyn.mdeditor.data.book

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.Locale

/**
 * 書庫裡的一本書。
 *
 * 檔案本體存在 App 自己的目錄（`filesDir/books`），而不是記著使用者當初選的
 * SAF Uri：SAF 授權會過期、雲端硬碟的檔案會被搬走、聊天軟體的快取會被清掉，
 * 只記 Uri 的話「回到主頁再點就開不起來」——那正是之前書會「不見」的原因。
 */
data class ShelfBook(
    val id: String,
    /** 書庫目錄裡的檔名（已去重）。 */
    val fileName: String,
    /** 匯入當下的原始檔名，用來顯示書名、判斷格式。 */
    val displayName: String,
    val addedAt: Long,
    val lastOpenedAt: Long = 0L,
    /** 這本書是從哪個資料夾收進來的；直接匯入的單本書為 null。 */
    val folderId: String? = null
) {
    /** 去掉副檔名的書名。 */
    val title: String get() = displayName.substringBeforeLast('.', displayName)

    /** `EPUB` / `FB2` 之類，給列表上的小標籤用。 */
    val extension: String
        get() = displayName.substringAfterLast('.', "").uppercase(Locale.ROOT)
}

/**
 * 書庫追蹤的一個資料夾（SAF 樹）。
 *
 * 記的是 **樹 Uri** 而不是裡面的書：使用者往資料夾裡丟新書，下次進書庫重新掃一次
 * 就自動收進來，不必再一本本匯入。授權用 `takePersistableUriPermission` 長期保存，
 * 重開 App 也還讀得到。
 */
data class ShelfFolder(
    val id: String,
    /** SAF 樹 Uri 字串，重開 App 後靠它重新讀取整個資料夾。 */
    val treeUri: String,
    /** 資料夾顯示名稱（取 SAF 給的名字，取不到就退回 Uri 末段）。 */
    val name: String,
    val addedAt: Long,
    /**
     * 最近一次掃描的時間。
     *
     * 掃描改成手動之後它不再是節流依據，留著是因為「這本書是什麼時候被收進來的」
     * 對照起來有用，也方便日後在界面上寫「上次掃描於 …」。
     */
    val lastScanAt: Long = 0L
) {
    val uri: Uri get() = Uri.parse(treeUri)
}

/**
 * 書庫：把匯入的電子書留在裝置上，重開 App 也還在。
 *
 * 除了「匯入一本書」，還管一批**外部資料夾**：資料夾只登記樹 Uri，
 * 每次進書庫時統一掃一遍，把裡面新出現的書收進來——這才是「一次設定、
 * 之後自動讀取」的用法。
 *
 * 進度由 [ReadingProgressStore] 另外管，兩者用「書名＋作者＋章數」的雜湊對上，
 * 所以書被重新匯入也不會掉進度。
 */
class BookLibrary(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val dir: File
        get() = File(appContext.filesDir, DIR_NAME).apply { mkdirs() }

    /** 書庫目錄，給「檔案在哪」這類提示用。 */
    fun directory(): File = dir

    fun fileOf(book: ShelfBook): File = File(dir, book.fileName)

    /** 交給閱讀器用的 `file://` Uri。 */
    fun uriOf(book: ShelfBook): Uri = Uri.fromFile(fileOf(book))

    fun books(): List<ShelfBook> {
        val raw = prefs.getString(KEY_BOOKS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val fileName = item.optString("fileName")
                    if (fileName.isBlank()) continue
                    // 檔案被使用者在系統裡刪掉了就別再列出來，否則點下去只會報錯。
                    if (!File(dir, fileName).isFile) continue
                    add(
                        ShelfBook(
                            id = item.optString("id", fileName),
                            fileName = fileName,
                            displayName = item.optString("displayName", fileName),
                            addedAt = item.optLong("addedAt", 0L),
                            lastOpenedAt = item.optLong("lastOpenedAt", 0L),
                            folderId = item.optString("folderId").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList()).sortedByDescending { maxOf(it.lastOpenedAt, it.addedAt) }
    }

    // ── 資料夾 ────────────────────────────────────────────────────────────

    fun folders(): List<ShelfFolder> {
        val raw = prefs.getString(KEY_FOLDERS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val treeUri = item.optString("treeUri")
                    if (treeUri.isBlank()) continue
                    add(
                        ShelfFolder(
                            id = item.optString("id", treeUri),
                            treeUri = treeUri,
                            // 名稱可能真的空白（舊資料）；怎麼顯示由介面決定，資料層不寫死語言。
                            name = item.optString("name", ""),
                            addedAt = item.optLong("addedAt", 0L),
                            lastScanAt = item.optLong("lastScanAt", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList()).sortedBy { it.addedAt }
    }

    fun folderOf(id: String?): ShelfFolder? = id?.let { key -> folders().firstOrNull { it.id == key } }

    fun booksIn(folderId: String): List<ShelfBook> = books().filter { it.folderId == folderId }

    /** 授權還有效嗎？失效時卡片會標成「需要重新授權」，而不是靜靜地漏掉書。 */
    fun canRead(folder: ShelfFolder): Boolean = runCatching {
        appContext.contentResolver.persistedUriPermissions.any {
            it.uri == folder.uri && it.isReadPermission
        }
    }.getOrDefault(false)

    /**
     * 登記一個資料夾並立刻掃一遍。
     *
     * 授權一定要在這裡取：`OpenDocumentTree` 給的是臨時授權，不 `take` 的話
     * 程序一重啟就讀不到了——那正是「資料夾存不住」的原因。
     */
    suspend fun addFolder(
        treeUri: Uri,
        /** 拿不到資料夾名稱時用的名字；由介面按語言傳進來。 */
        fallbackName: String = ""
    ): ShelfFolder = withContext(Dispatchers.IO) {
        persistPermission(treeUri)
        val document = DocumentFile.fromTreeUri(appContext, treeUri)
        val name = document?.name?.takeIf { it.isNotBlank() }
            ?: treeUri.lastPathSegment?.substringAfterLast(':')?.takeIf { it.isNotBlank() }
            ?: fallbackName
        val folder = ShelfFolder(
            id = digest(treeUri.toString()),
            treeUri = treeUri.toString(),
            name = name,
            addedAt = System.currentTimeMillis()
        )
        writeFolders(folders().filterNot { it.id == folder.id } + folder)
        scanFolder(folder)
        folder
    }

    fun removeFolder(folder: ShelfFolder, removeBooks: Boolean = false) {
        if (removeBooks) {
            books().filter { it.folderId == folder.id }.forEach { remove(it) }
        } else {
            // 書留著，只是不再是「這個資料夾來的」。
            write(books().map { if (it.folderId == folder.id) it.copy(folderId = null) else it })
        }
        runCatching {
            appContext.contentResolver.releasePersistableUriPermission(
                folder.uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        writeFolders(folders().filterNot { it.id == folder.id })
    }

    /** 掃一個資料夾（遞迴、限深度與數量），回傳新收進來的本數。 */
    suspend fun scanFolder(folder: ShelfFolder): Int = withContext(Dispatchers.IO) {
        val root = runCatching { DocumentFile.fromTreeUri(appContext, folder.uri) }.getOrNull()
            ?: return@withContext 0
        if (!root.isDirectory) return@withContext 0
        val found = mutableListOf<DocumentFile>()
        collect(root, found)
        val ignored = ignoredNames()
        var added = 0
        found.forEach { file ->
            val name = file.name ?: return@forEach
            if (sanitize(name) in ignored) return@forEach
            runCatching { import(file.uri, name, folder.id) }.onSuccess { added++ }
        }
        writeFolders(
            folders().map {
                if (it.id == folder.id) it.copy(lastScanAt = System.currentTimeMillis()) else it
            }
        )
        added
    }

    /**
     * 完整重新讀取：本機書庫目錄 + 所有登記過的資料夾。
     *
     * 這是**手動**動作，入口在設定頁。以前它掛在書庫頁的進場路徑上，每次進去都要
     * 向每個 SAF 資料夾（可能是雲端硬碟）列一遍目錄，慢的時候整頁卡著等；
     * 而書目本來就存在裝置上，掃與不掃看到的幾乎一樣。
     */
    suspend fun rescan(): Int = withContext(Dispatchers.IO) {
        var added = rescanLocal()
        folders().forEach { folder ->
            added += runCatching { scanFolder(folder) }.getOrDefault(0)
        }
        added
    }

    /**
     * 只重讀本機書庫目錄，不碰外部資料夾。
     *
     * 進書庫時走這一個：`filesDir/books` 列一次目錄是毫秒級的事，使用者自己用
     * 檔案管理器丟進來的書照樣會出現；而連結的 SAF 資料夾（尤其雲端硬碟）
     * 列一次要幾百毫秒、還可能卡在網路上，那件事改成設定頁裡的手動動作，見 [rescan]。
     */
    suspend fun refreshLocal(): Int = withContext(Dispatchers.IO) { rescanLocal() }

    /** 目錄裡有、但還沒登記的檔案（使用者自己用檔案管理器丟進來的）。 */
    private fun rescanLocal(): Int {
        val known = books().map { it.fileName }.toSet()
        var added = 0
        dir.listFiles()?.forEach { file ->
            if (!file.isFile) return@forEach
            if (file.name in known) return@forEach
            if (BookFormat.fromFileName(file.name) == null) return@forEach
            runCatching { register(file, file.name, null) }.onSuccess { added++ }
        }
        return added
    }

    // ── 匯入 ──────────────────────────────────────────────────────────────

    /**
     * 把一本書複製進書庫。
     *
     * 同名檔案已經在書庫裡就直接沿用，不重複佔空間——使用者常常會對同一本書
     * 按好幾次「匯入」。若這本書原本沒有來源資訊，這次會補上 [folderId]。
     */
    suspend fun import(
        uri: Uri,
        displayName: String? = null,
        folderId: String? = null,
        /** 檔案連顯示名稱都問不到時用的名字；由介面按語言傳進來。 */
        fallbackName: String = "untitled.epub"
    ): ShelfBook = withContext(Dispatchers.IO) {
        val raw = displayName?.takeIf { it.isNotBlank() }
            ?: queryDisplayName(uri)
            ?: fallbackName
        val name = sanitize(raw)
        if (folderId == null) {
            // 使用者主動匯入，代表他要這本書：把之前「已刪除」的記號清掉。
            val ignored = ignoredNames()
            if (name in ignored) writeIgnored(ignored - name)
        }

        books().firstOrNull { it.displayName == name }?.let { existing ->
            return@withContext if (folderId != null && existing.folderId != folderId) {
                // 同一本書從資料夾再收到一次：補上來源，不動檔案。
                val merged = existing.copy(folderId = folderId)
                write(books().map { if (it.id == existing.id) merged else it })
                merged
            } else {
                existing
            }
        }

        File(dir, name).takeIf { it.isFile && it.length() > 0 }?.let { existing ->
            return@withContext register(existing, name, folderId)
        }

        val target = uniqueFile(name)
        val input = appContext.contentResolver.openInputStream(uri)
            ?: throw BookException(BookError.FILE_UNREADABLE)
        input.use { source ->
            target.outputStream().use { sink -> source.copyTo(sink, 64 * 1024) }
        }
        if (target.length() <= 0L) {
            target.delete()
            throw BookException(BookError.FILE_EMPTY)
        }
        register(target, name, folderId)
    }

    /** 相容舊呼叫：從一個資料夾（SAF 樹）把書全部收進來並登記資料夾。 */
    suspend fun importFolder(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        val folder = addFolder(treeUri)
        booksIn(folder.id).size
    }

    // ── 書目維護 ──────────────────────────────────────────────────────────

    /**
     * 被使用者刪掉的書名（只記資料夾來的書）。
     *
     * 沒有這份名單，刪掉的書下次掃描資料夾又會被收進來——看起來就像「刪不掉」。
     * 使用者主動重新匯入同名檔案時會把這條記錄清掉。
     */
    private fun ignoredNames(): MutableSet<String> {
        val raw = prefs.getString(KEY_IGNORED, null) ?: return mutableSetOf()
        return runCatching {
            val array = JSONArray(raw)
            val names = mutableSetOf<String>()
            for (index in 0 until array.length()) {
                val name = array.optString(index)
                if (name.isNotBlank()) names += name
            }
            names
        }.getOrDefault(mutableSetOf())
    }

    private fun writeIgnored(names: Set<String>) {
        val array = JSONArray()
        names.forEach { array.put(it) }
        prefs.edit().putString(KEY_IGNORED, array.toString()).apply()
    }

    /** 記一次「剛剛打開過」。 */
    fun touch(book: ShelfBook) {
        val next = book.copy(lastOpenedAt = System.currentTimeMillis())
        val list = books().filterNot { it.id == next.id } + next
        write(list)
    }

    fun remove(book: ShelfBook) {
        runCatching { fileOf(book).delete() }
        // 資料夾來的書要留個「已刪除」記號，否則下次同步又會收進來。
        if (book.folderId != null) writeIgnored(ignoredNames() + book.displayName)
        write(books().filterNot { it.id == book.id })
    }

    // ── 內部 ──────────────────────────────────────────────────────────────

    private fun register(file: File, displayName: String, folderId: String?): ShelfBook {
        val id = digest(file.name)
        val entry = ShelfBook(
            id = id,
            fileName = file.name,
            displayName = displayName,
            addedAt = System.currentTimeMillis(),
            lastOpenedAt = System.currentTimeMillis(),
            folderId = folderId
        )
        val list = books().filterNot { it.id == id || it.fileName == file.name }
        write(list + entry)
        return entry
    }

    private fun write(list: List<ShelfBook>) {
        val array = JSONArray()
        list.forEach { book ->
            array.put(
                JSONObject()
                    .put("id", book.id)
                    .put("fileName", book.fileName)
                    .put("displayName", book.displayName)
                    .put("addedAt", book.addedAt)
                    .put("lastOpenedAt", book.lastOpenedAt)
                    .put("folderId", book.folderId ?: "")
            )
        }
        prefs.edit().putString(KEY_BOOKS, array.toString()).apply()
    }

    private fun writeFolders(list: List<ShelfFolder>) {
        val array = JSONArray()
        list.forEach { folder ->
            array.put(
                JSONObject()
                    .put("id", folder.id)
                    .put("treeUri", folder.treeUri)
                    .put("name", folder.name)
                    .put("addedAt", folder.addedAt)
                    .put("lastScanAt", folder.lastScanAt)
            )
        }
        prefs.edit().putString(KEY_FOLDERS, array.toString()).apply()
    }

    private fun persistPermission(uri: Uri) {
        runCatching {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    /** 遞迴收集電子書；深度與數量都封頂，雲端硬碟的根目錄也不會把整支 App 拖住。 */
    private fun collect(node: DocumentFile, out: MutableList<DocumentFile>, depth: Int = 0) {
        if (depth > MAX_DEPTH || out.size >= MAX_FILES) return
        val children = runCatching { node.listFiles() }.getOrNull() ?: return
        for (child in children) {
            if (out.size >= MAX_FILES) return
            if (child.isDirectory) {
                collect(child, out, depth + 1)
            } else {
                val name = child.name ?: continue
                if (BookFormat.fromFileName(name) == null) continue
                out += child
            }
        }
    }

    private fun uniqueFile(name: String): File {
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        var candidate = File(dir, name)
        var index = 2
        while (candidate.exists()) {
            val next = if (ext.isEmpty()) "$base ($index)" else "$base ($index).$ext"
            candidate = File(dir, next)
            index++
        }
        return candidate
    }

    /** 檔名要塞進檔案系統，路徑分隔字元與控制字元一律換掉。 */
    private fun sanitize(name: String): String {
        val cleaned = name.map { ch ->
            if (ch in "/\\:*?\"<>|" || ch.code < 32) '_' else ch
        }.joinToString("").trim().trim('.')
        val safe = cleaned.ifBlank { "未命名" }
        return if (safe.length > 96) safe.take(92) + safe.substringAfterLast('.', "") else safe
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()

    private fun digest(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }.take(20)
    }

    private companion object {
        const val PREFS = "book_library"
        const val KEY_BOOKS = "books"
        const val KEY_FOLDERS = "folders"
        const val KEY_IGNORED = "ignored"
        const val DIR_NAME = "books"

        /** 資料夾遞迴深度上限：書通常就一兩層，再深多半是誤選了整個儲存空間。 */
        const val MAX_DEPTH = 4

        /** 單次掃描的檔案數上限，避免選到雲端硬碟根目錄時卡住整個書庫。 */
        const val MAX_FILES = 3000
    }
}

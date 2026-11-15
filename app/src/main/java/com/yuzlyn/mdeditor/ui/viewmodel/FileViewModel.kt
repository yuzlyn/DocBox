package com.yuzlyn.mdeditor.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yuzlyn.mdeditor.data.ImageInsertResult
import com.yuzlyn.mdeditor.data.NoteAutoSaver
import com.yuzlyn.mdeditor.data.NoteModel
import com.yuzlyn.mdeditor.data.NoteSaveState
import com.yuzlyn.mdeditor.data.NoteAttachments
import com.yuzlyn.mdeditor.data.SafNoteDataSource
import com.yuzlyn.mdeditor.data.SettingsDataStore
import com.yuzlyn.mdeditor.data.book.BookFormat
import com.yuzlyn.mdeditor.data.book.BookOpener
import com.yuzlyn.mdeditor.data.importer.DocumentImporter
import com.yuzlyn.mdeditor.data.importer.ImportFormat
import com.yuzlyn.mdeditor.data.importer.ImportResult
import com.yuzlyn.mdeditor.data.local.AppDatabase
import com.yuzlyn.mdeditor.data.preview.PreviewConfig
import com.yuzlyn.mdeditor.data.preview.PreviewPreferences
import com.yuzlyn.mdeditor.data.profile.ProfileData
import com.yuzlyn.mdeditor.data.profile.ProfilePreferences
import com.yuzlyn.mdeditor.data.repository.NoteRepository
import com.yuzlyn.mdeditor.data.settings.WritingGuide
import com.yuzlyn.mdeditor.data.settings.AppSettings
import com.yuzlyn.mdeditor.data.settings.AppSettingsPreferences
import com.yuzlyn.mdeditor.data.settings.HomeTarget
import com.yuzlyn.mdeditor.data.stats.StatKind
import com.yuzlyn.mdeditor.data.stats.StatsSnapshot
import com.yuzlyn.mdeditor.data.stats.StatsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NoteRepository(AppDatabase.getInstance(application))
    private val safDataSource = SafNoteDataSource(application)
    private val autoSaver = NoteAutoSaver(repository, safDataSource)
    private val attachments = NoteAttachments(application)
    private val settings = SettingsDataStore(application)
    private val previewPreferences = PreviewPreferences(application)
    private val appSettingsPreferences = AppSettingsPreferences(application)
    private val profilePreferences = ProfilePreferences(application)
    private val statsStore = StatsStore(application)

    /**
     * 閱讀與編輯的統計。統計頁直接吃這一份；關掉總開關時它就不再長大，
     * 但已經記下的資料還在（要不要清掉由使用者自己決定）。
     */
    val stats: StateFlow<StatsSnapshot> = statsStore.snapshot

    val appSettings: StateFlow<AppSettings> = appSettingsPreferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    /**
     * 啟動頁：直接讀 DataStore 的第一份資料。
     *
     * 不能讀 [appSettings]——那是 `stateIn` 的 StateFlow，第一幀是預設值，
     * 用它會變成「先畫筆記頁再跳書庫」。
     */
    suspend fun startPage(): HomeTarget = appSettingsPreferences.settings.first().homeTarget

    fun updateAppSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { appSettingsPreferences.update(transform) }
    }

    /**
     * 讀一份「最新」的設定再決定要不要記。
     *
     * 不能讀 [appSettings]：那是 `stateIn`，沒有訂閱者時停在預設值，
     * 使用者明明關掉了統計還是會被記上一筆。
     */
    private fun withSettings(block: (AppSettings) -> Unit) {
        viewModelScope.launch {
            val current = runCatching { appSettingsPreferences.settings.first() }
                .getOrDefault(AppSettings())
            block(current)
        }
    }

    /** 閱讀計時：[seconds] 是這一段的長度，[key] 用書的 Uri，跨天同一個鍵才排得出榜。 */
    fun recordReading(key: String, name: String?, seconds: Long) {
        if (key.isBlank() || seconds <= 0) return
        withSettings { current ->
            if (current.statsEnabled && current.statsTrackReading) {
                statsStore.record(
                    kind = StatKind.READ,
                    key = key,
                    name = name?.takeIf { it.isNotBlank() }
                        ?: key.substringAfterLast('/').substringAfterLast(':'),
                    value = seconds
                )
            }
        }
    }

    /** 清除全部統計紀錄。 */
    fun clearStats() {
        statsStore.clear()
    }

    /** 立刻把統計寫進檔案（切到背景時呼叫）。 */
    fun flushStats() {
        statsStore.flushNow()
    }

    /**
     * 同一個「編輯段」的最短間隔。
     *
     * [onNoteEdited] 是**每打一個字**都會被叫到的，直接記就變成「打了三千次字」。
     * 用一段冷卻時間把連續輸入算成一次編輯：寫了半小時的長文大概記成十次左右，
     * 既反映「有沒有在寫」，也不會讓統計數字膨脹到沒有意義。
     */
    private val lastEditAt = mutableMapOf<String, Long>()

    private fun noteEdit(key: String, title: String) {
        val now = android.os.SystemClock.elapsedRealtime()
        val last = lastEditAt[key] ?: 0L
        if (now - last < EDIT_COOLDOWN_MS) return
        lastEditAt[key] = now
        withSettings { current ->
            if (current.statsEnabled && current.statsTrackDocuments) {
                statsStore.record(StatKind.EDIT, key, title, 1)
            }
        }
    }

    val profile: StateFlow<ProfileData> = profilePreferences.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileData())

    fun updateProfile(transform: (ProfileData) -> ProfileData) {
        viewModelScope.launch { profilePreferences.update(transform) }
    }

    val notes: StateFlow<List<NoteModel>> = repository.notes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notebookTags: StateFlow<List<String>> = notes
        .map { list -> list.map { it.notebookTag }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasSelectedFolder: StateFlow<Boolean> = settings.treeUri
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    val previewConfig: StateFlow<PreviewConfig> = previewPreferences.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreviewConfig())

    /** 各篇筆記目前的保存狀態，編輯器頂欄的「已保存」提示用。 */
    val saveStates: StateFlow<Map<String, NoteSaveState>> = autoSaver.states

    /** 啟動時從草稿還原過內容的筆記。 */
    val recoveredNotes: StateFlow<Set<String>> = autoSaver.recovered

    /** 插入圖片的結果（成功帶 Markdown 路徑，失敗帶原因）。 */
    private val _imageInsert = MutableSharedFlow<ImageInsertResult>(extraBufferCapacity = 8)
    val imageInsert: SharedFlow<ImageInsertResult> = _imageInsert.asSharedFlow()

    private var importJob: Job? = null

    init {
        viewModelScope.launch {
            val persistedUri = settings.treeUriOnce()
            if (persistedUri != null) {
                Log.d(TAG, "[FileViewModel] 檢測到持久化目錄 Uri，自動匯入")
                importFilesFromSelectedFolder(Uri.parse(persistedUri))
            } else {
                Log.d(TAG, "[FileViewModel] 無持久化目錄，等待用戶選擇資料夾")
                // 還是要把上次的草稿補回資料庫，使用者才看得到自己打過的字。
                autoSaver.recoverPendingDrafts()
            }
        }
        // 每次啟動自檢一次：來源檔可能已被刪除、搬走，或使用者在系統設定裡撤銷授權。
        // 這種情況下頂欄那顆「開啟原始檔」按了只會失敗，先把它清掉。
        viewModelScope.launch { pruneStaleSourceUris() }
    }

    /**
     * 只保留「現在還讀得到」的來源 Uri。
     *
     * 注意：拿不到授權清單時直接跳過，不能當成「一個都沒有」——那會把所有筆記的
     * 原始檔關聯一次清空。
     */
    private suspend fun pruneStaleSourceUris() {
        val valid = runCatching {
            getApplication<Application>().contentResolver.persistedUriPermissions
                .asSequence()
                .filter { it.isReadPermission }
                .mapTo(mutableSetOf()) { it.uri.toString() }
        }.getOrNull() ?: return
        runCatching { repository.pruneSourceUris(valid) }
            .onFailure { Log.e(TAG, "[FileViewModel] 清理失效的來源 Uri 失敗", it) }
    }

    fun updatePreviewConfig(transform: (PreviewConfig) -> PreviewConfig) {
        viewModelScope.launch { previewPreferences.update(transform) }
    }

    fun onFolderSelected(treeUri: Uri) {
        viewModelScope.launch {
            if (!safDataSource.takePersistablePermission(treeUri)) return@launch
            settings.setTreeUri(treeUri.toString())
            importFilesFromSelectedFolder(treeUri)
        }
    }

    fun importFilesFromSelectedFolder(treeUri: Uri) {
        importJob?.cancel()
        importJob = viewModelScope.launch {
            _isImporting.value = true
            try {
                val scan = safDataSource.scanNotes(treeUri)
                if (scan.accessible) {
                    repository.applyScan(scan.notes)
                    Log.d(TAG, "[FileViewModel] SAF 匯入完成: 共 ${scan.notes.size} 條筆記")
                } else {
                    // 目錄當下讀不到（權限被回收、卡片沒掛上）：保留資料庫現有內容，不清空。
                    Log.e(TAG, "[FileViewModel] 目錄無法存取，跳過本次匯入以保護既有內容")
                }
                // 上次沒寫完的草稿一律在這裡補寫回檔案，順便把內容補進資料庫。
                autoSaver.recoverPendingDrafts()
            } catch (e: Exception) {
                Log.e(TAG, "[FileViewModel] SAF 匯入失敗: ${e.message}", e)
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun createNewNote(title: String, notebookTag: String = SafNoteDataSource.DEFAULT_TAG) {
        viewModelScope.launch {
            val treeUri = settings.treeUriOnce()
            if (treeUri == null) {
                Log.e(TAG, "[FileViewModel] 無法新建文件：尚未選擇 SAF 資料夾")
                return@launch
            }
            val note = safDataSource.createNote(Uri.parse(treeUri), title, notebookTag)
            if (note != null) {
                repository.insert(note)
            } else {
                Log.e(TAG, "[FileViewModel] SAF 新建文件失敗")
            }
        }
    }

    private val _importError = MutableStateFlow<String?>(null)

    /** 匯入失敗的原因；主頁收集後用 Snackbar 顯示，顯示完呼叫 [clearImportError]。 */
    val importError: StateFlow<String?> = _importError.asStateFlow()

    fun clearImportError() {
        _importError.value = null
    }

    /** 匯入結果：成功回新筆記 id，失敗回可以直接顯示的原因。 */
    sealed interface ImportNoteResult {
        data class Success(val noteId: String) : ImportNoteResult
        data class Failure(val message: String) : ImportNoteResult
    }

    /**
     * 匯入任意支援的檔案（Word／PDF／Markdown／TXT）。
     * 失敗時把原因放進 [importError]，由主頁的 Snackbar 顯示。
     */
    fun importDocument(source: Uri, onImported: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = importToNote(source)) {
                is ImportNoteResult.Success -> onImported(result.noteId)
                is ImportNoteResult.Failure -> _importError.value = result.message
            }
        }
    }

    /**
     * 打開使用者挑的檔案：電子書走**原生閱讀**，其餘照舊匯入成筆記。
     *
     * 這道分岔就是「不把書轉成 Markdown」的落點。EPUB／MOBI／AZW3 由閱讀器
     * 按章節讀，排版、目錄、圖片原樣保留；轉成筆記等於把書拆成純文字，
     * 既慢又丟內容，所以那條路已經移除。
     */
    fun openDocument(
        source: Uri,
        onBook: (uri: String, displayName: String?) -> Unit,
        onNote: (String) -> Unit
    ) {
        viewModelScope.launch {
            val displayName = withContext(Dispatchers.IO) { queryDisplayName(source) }
            // 先看檔名，再問檔頭：沒有副檔名的書也要進閱讀器，不能被當純文字匯入。
            val isBook = BookFormat.fromFileName(displayName) != null ||
                    withContext(Dispatchers.IO) { BookOpener.sniff(getApplication(), source) } != null
            if (isBook) {
                onBook(source.toString(), displayName)
                return@launch
            }
            when (val result = importToNote(source)) {
                is ImportNoteResult.Success -> onNote(result.noteId)
                is ImportNoteResult.Failure -> _importError.value = result.message
            }
        }
    }

    /** 給 PDF 檢視頁直接呼叫：結果自己顯示，不經過全域錯誤訊息。 */
    suspend fun importToNote(source: Uri): ImportNoteResult {
        val displayName = queryDisplayName(source)
        val parsed = DocumentImporter.import(getApplication(), source, displayName)
        return when (parsed) {
            is ImportResult.Failure -> ImportNoteResult.Failure(parsed.reason)
            is ImportResult.Success -> {
                // PDF 之後要能在編輯頁頂欄重新打開，所以先要一份常駐讀取權限。
                // 拿不到就別記 Uri：否則頂欄會出現一顆按了必然失敗的按鈕。
                val sourceUri = if (parsed.format == ImportFormat.PDF && takePersistableRead(source)) {
                    source.toString()
                } else {
                    ""
                }
                createImportedNote(displayName, parsed.text, parsed.format, sourceUri)
            }
        }
    }

    /** 建檔 → 寫內容 → 進資料庫。 */
    /** 向 provider 要一份跨啟動的讀取權限；不支援常駐授權的 provider 會丟例外。 */
    private fun takePersistableRead(uri: Uri): Boolean = runCatching {
        getApplication<Application>().contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }.isSuccess

    private suspend fun createImportedNote(
        displayName: String?,
        content: String,
        format: ImportFormat?,
        sourceUri: String = ""
    ): ImportNoteResult {
        val treeUri = settings.treeUriOnce()
            ?: return ImportNoteResult.Failure("尚未選擇資料夾，無法建立筆記")
        val note = withContext(Dispatchers.IO) {
            safDataSource.createNote(
                Uri.parse(treeUri),
                titleFor(displayName, content),
                SafNoteDataSource.DEFAULT_TAG
            )
        } ?: return ImportNoteResult.Failure("無法在資料夾裡建立筆記")
        if (!safDataSource.writeContent(note.uriString, content)) {
            return ImportNoteResult.Failure("內容寫入失敗，請確認資料夾權限")
        }
        repository.insert(
            note.copy(
                content = content,
                lastModified = System.currentTimeMillis(),
                // 檔名一律是 .md，來源類型只留在資料庫；首頁卡片靠它顯示徽章。
                sourceFormat = format?.id ?: "",
                sourceUri = sourceUri
            )
        )
        return ImportNoteResult.Success(note.id)
    }

    private fun queryDisplayName(source: Uri): String? = runCatching {
        getApplication<Application>().contentResolver
            .query(source, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
    }.getOrNull()

    /** 標題優先取原檔名；取不到才退回首個標題。 */
    private fun titleFor(displayName: String?, content: String): String {
        val fromFileName = displayName
            ?.substringBeforeLast('.', displayName)
            ?.trim()
        if (!fromFileName.isNullOrBlank()) return fromFileName
        return content.lineSequence()
            .firstOrNull { it.startsWith("# ") }
            ?.removePrefix("# ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "匯入的文件"
    }

    /**
     * 把使用者選的圖片複製進筆記資料夾的 `images/`，並回傳要寫進 Markdown 的相對路徑。
     * 失敗時透過 [imageInsert] 帶回可以直接顯示的原因。
     */
    fun attachImage(noteId: String, source: Uri) {
        viewModelScope.launch {
            val note = repository.getNote(noteId)
            if (note == null) {
                _imageInsert.emit(
                    ImageInsertResult.Failure("插入圖片失敗：這篇筆記還沒載入完成，請稍後再試")
                )
                return@launch
            }
            _imageInsert.emit(attachments.attachImage(note.uriString, source))
        }
    }

    /** 關聯原始檔的結果訊息；成功與失敗都給一句話，由編輯頁的 Snackbar 顯示。 */
    private val _sourceLinkMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val sourceLinkMessage: SharedFlow<String> = _sourceLinkMessage.asSharedFlow()

    /**
     * 把一個原始檔（目前只支援 PDF）關聯到既有筆記。
     *
     * 這是「補救」路徑：早期匯入的 PDF 筆記沒有記下原始檔的位置，而那個位置
     * 從來沒被保存過，任何自動遷移都變不回來——只能讓使用者指一次。
     * 取得常駐讀取權限後才寫入，否則寧可不記，免得頂欄多一顆按了會失敗的按鈕。
     */
    fun linkSourceDocument(noteId: String, source: Uri) {
        viewModelScope.launch {
            val resolver = getApplication<Application>().contentResolver
            val granted = runCatching {
                resolver.takePersistableUriPermission(
                    source,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }.isSuccess
            if (!granted) {
                _sourceLinkMessage.emit("這個位置不支援長期授權，請換一個資料夾再試一次")
                return@launch
            }
            val readable = runCatching {
                resolver.openInputStream(source)?.use { it.read() } != null
            }.getOrDefault(false)
            if (!readable) {
                _sourceLinkMessage.emit("讀不到這個檔案")
                return@launch
            }
            repository.setSourceDocument(noteId, ImportFormat.PDF.id, source.toString())
            _sourceLinkMessage.emit("已關聯原始 PDF")
        }
    }

    /**
     * 編輯器每次改動都往這裡送；實際落盤節奏由 [NoteAutoSaver] 控制
     * （防抖寫草稿 + 空閒／定時寫檔 + 離開時強制寫）。
     */
    fun onNoteEdited(noteId: String, title: String, content: String) {
        autoSaver.onEdit(noteId, title, content)
        noteEdit(noteId, title)
    }

    /** 編輯器載入完成：記下起點內容，滑動游標之類的偽改動就不會觸發保存。 */
    fun onNoteLoaded(noteId: String, title: String, content: String) {
        autoSaver.markLoaded(noteId, title, content)
        // 打開就算「看過」，不論有沒有改。
        withSettings { current ->
            if (current.statsEnabled && current.statsTrackDocuments) {
                statsStore.record(StatKind.OPEN, noteId, title, 1)
            }
        }
    }

    /** 立即落盤一次（返回、切背景、頁面銷毀時呼叫）。 */
    fun flushNote(noteId: String) {
        autoSaver.flush(noteId)
    }

    fun acknowledgeRecoveredDraft(noteId: String) {
        autoSaver.acknowledgeRecovery(noteId)
    }

    /** 一次寫入同時更新標題與內容，並立刻落盤；給需要同步語意的場合用。 */
    fun saveNote(noteId: String, title: String, content: String) {
        autoSaver.onEdit(noteId, title, content)
        autoSaver.flush(noteId)
    }

    /** 只改這一篇筆記的排版：直排 / 橫排，會立刻寫進資料庫，下次打開仍是這個排版。 */
    fun setNoteVertical(noteId: String, vertical: Boolean) {
        viewModelScope.launch { repository.setVerticalLayout(noteId, vertical) }
    }

    /** 單篇的書寫輔助線；傳 null 表示恢復成跟隨預設設定。 */
    fun setNoteWritingGuide(noteId: String, guide: WritingGuide?) {
        viewModelScope.launch { repository.setWritingGuide(noteId, guide?.id) }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            autoSaver.discard(noteId)
            val note = repository.getNote(noteId) ?: return@launch
            repository.delete(noteId)
            repository.deleteDraft(noteId)
            safDataSource.deleteDocument(note.uriString)
        }
    }

    companion object {
        private const val TAG = "MDEditor_Debug"
    }
}

/** 連續輸入算成一次編輯的冷卻時間。 */
private const val EDIT_COOLDOWN_MS = 3 * 60 * 1000L

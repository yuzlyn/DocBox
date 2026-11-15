package com.yuzlyn.mdeditor.data

import android.util.Log
import com.yuzlyn.mdeditor.data.repository.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 單篇筆記目前的保存狀態，給編輯器顯示用。 */
enum class NoteSaveState {
    /** 沒有未保存的改動。 */
    IDLE,

    /** 剛改完，還在防抖視窗內。 */
    PENDING,

    /** 正在寫入檔案。 */
    SAVING,

    /** 已寫回 `.md` 檔。 */
    SAVED,

    /** 寫檔失敗（草稿已保留，下次會重試）。 */
    FAILED
}

/**
 * 筆記自動保存引擎。
 *
 * 三條防線，任何一條成立內容都不會掉：
 * 1. **防抖寫草稿**：停止輸入 400ms 就把整篇內容寫進 Room 的草稿表，成本極低、不會漏。
 * 2. **空閒／定時落盤**：停手 1.2s 或持續輸入每 5s，把草稿寫回 `.md` 檔並更新資料庫。
 * 3. **強制落盤**：切到背景、返回、離開編輯器、頁面銷毀時立即寫一次。
 *
 * 寫檔一律「先寫資料庫、再寫檔案」：檔案寫失敗時資料庫與草稿都還有內容，
 * 下次啟動會重試，不會出現「檔案舊、資料庫新、掃描時被舊檔蓋掉」。
 */
class NoteAutoSaver(
    private val repository: NoteRepository,
    private val files: SafNoteDataSource
) {

    private data class PendingEdit(
        val title: String,
        val content: String,
        val revision: Long
    )

    private val lock = Any()
    private val writeMutex = Mutex()

    private val pending = mutableMapOf<String, PendingEdit>()
    private val draftJobs = mutableMapOf<String, Job>()
    private val flushJobs = mutableMapOf<String, Job>()
    private val fileDirty = mutableSetOf<String>()
    private val failures = mutableMapOf<String, Int>()

    /** 每篇筆記最後一次確認寫入的 (標題, 內容)，用來濾掉「只是移動游標」的偽改動。 */
    private val lastCommitted = mutableMapOf<String, Pair<String, String>>()

    /** 每篇筆記最後一次寫草稿的時間，用來做草稿節流。 */
    private val lastDraftWrittenAt = mutableMapOf<String, Long>()

    /** 每篇筆記最後一次嘗試落盤的時間，持續輸入時靠它做定時兜底。 */
    private val lastFlushedAt = mutableMapOf<String, Long>()

    private var revisionCounter = 0L
    private var tickerRunning = false

    private val _states = MutableStateFlow<Map<String, NoteSaveState>>(emptyMap())
    val states: StateFlow<Map<String, NoteSaveState>> = _states.asStateFlow()

    private val _recovered = MutableStateFlow<Set<String>>(emptySet())
    val recovered: StateFlow<Set<String>> = _recovered.asStateFlow()

    /** 每次輸入都呼叫；內部只做狀態記錄與排程，不會同步寫盤。 */
    fun onEdit(noteId: String, title: String, content: String) {
        val snapshot = synchronized(lock) {
            // TextFieldValue 連游標一起回報，選取範圍變動也會走到這裡；
            // 文字真的沒變就不要排程，免得只是點一下就把筆記重寫一遍。
            if (lastCommitted[noteId] == title to content) return
            val current = pending[noteId]
            if (current != null && current.title == title && current.content == content) return
            val next = PendingEdit(
                title = title,
                content = content,
                revision = ++revisionCounter
            )
            pending[noteId] = next
            next
        }
        publish(noteId, NoteSaveState.PENDING)
        scheduleDraft(noteId, snapshot.revision)
        scheduleFlush(noteId)
        ensureTicker()
    }

    /** 編輯器載入完成時登記目前內容；之後只有文字真的變了才會排程保存。 */
    fun markLoaded(noteId: String, title: String, content: String) {
        synchronized(lock) {
            if (pending[noteId] == null) lastCommitted[noteId] = title to content
        }
        // 重新打開一篇筆記時先回到「沒有改動」，讓標題先露出來；
        // 只有真的存過／存失敗，頂欄才換成狀態字樣。失敗狀態保留，不要蓋掉警告。
        _states.update { current ->
            if (current[noteId] == NoteSaveState.SAVED) {
                current + (noteId to NoteSaveState.IDLE)
            } else {
                current
            }
        }
    }

    /** 立即排一次落盤（不等待結果），給返回、切背景、頁面銷毀用。 */
    fun flush(noteId: String) {
        val hasPending = synchronized(lock) { pending.containsKey(noteId) }
        if (!hasPending) return
        scope.launch { flushInternal(noteId) }
    }

    /** 等待落盤完成；活動結束前想確保寫完時用。 */
    suspend fun flushAndWait(noteId: String) = flushInternal(noteId)

    /** 筆記被刪除時呼叫：丟掉未保存內容，避免已刪的筆記又被寫回來。 */
    fun discard(noteId: String) {
        synchronized(lock) {
            pending.remove(noteId)
            draftJobs.remove(noteId)?.cancel()
            flushJobs.remove(noteId)?.cancel()
            fileDirty.remove(noteId)
            failures.remove(noteId)
            lastCommitted.remove(noteId)
            lastDraftWrittenAt.remove(noteId)
            lastFlushedAt.remove(noteId)
        }
        _states.update { it - noteId }
        _recovered.update { it - noteId }
    }

    /** 使用者已看到「草稿已還原」提示。 */
    fun acknowledgeRecovery(noteId: String) {
        _recovered.update { it - noteId }
    }

    /**
     * 啟動時把上次沒寫完的草稿補寫回檔案。
     *
     * 草稿存在代表「上次離開前沒能成功寫檔」，所以這裡無條件再寫一次，
     * 即使資料庫內容看起來一樣（很可能只是資料庫寫成功了、檔案沒寫進去）。
     */
    suspend fun recoverPendingDrafts() {
        val drafts = runCatching { repository.allDrafts() }
            .onFailure { Log.e(TAG, "[NoteAutoSaver] 讀取草稿失敗", it) }
            .getOrDefault(emptyList())
        if (drafts.isEmpty()) return

        val now = System.currentTimeMillis()
        runCatching { repository.pruneDrafts(now - DRAFT_TTL_MS) }

        val restored = mutableSetOf<String>()
        val failed = mutableSetOf<String>()
        drafts.forEach { draft ->
            if (draft.updatedAt < now - DRAFT_TTL_MS) return@forEach
            writeMutex.withLock {
                val note = runCatching { repository.getNote(draft.noteId) }.getOrNull()
                if (note == null) {
                    // 筆記已經不在清單裡（可能被外部刪除）；草稿留著，不主動清掉內容。
                    return@withLock
                }
                // 資料庫可能比草稿還新（寫完資料庫就斷電），那就以資料庫為準；
                // 反過來若草稿比較新，代表檔案還沒寫成功，草稿才是最新內容。
                val dbIsNewer = note.lastModified > draft.updatedAt
                val title = if (dbIsNewer) note.title else draft.title
                val content = if (dbIsNewer) note.content else draft.content

                val wrote = runCatching { files.writeContent(note.uriString, content) }
                    .getOrDefault(false)
                if (wrote) {
                    runCatching { repository.deleteDraft(draft.noteId) }
                } else {
                    failed.add(draft.noteId)
                }
                // 不論檔案寫不寫得回去，資料庫都要有這份內容，編輯器打開時才看得到。
                runCatching {
                    repository.update(
                        draft.noteId,
                        title,
                        content,
                        if (wrote) System.currentTimeMillis() else draft.updatedAt
                    )
                }
                restored.add(draft.noteId)
                synchronized(lock) { lastCommitted[draft.noteId] = title to content }
            }
        }

        if (restored.isNotEmpty()) {
            Log.d(TAG, "[NoteAutoSaver] 已還原 ${restored.size} 篇草稿")
            _recovered.update { it + restored }
            _states.update { current ->
                current + restored.associateWith { noteId ->
                    if (noteId in failed) NoteSaveState.FAILED else NoteSaveState.SAVED
                }
            }
        }
    }

    // ---- 內部 -----------------------------------------------------------

    /**
     * 草稿節流：兩次寫入至少間隔 [DRAFT_DEBOUNCE_MS]。
     *
     * 單純的防抖在「一直打字停不下來」時永遠等不到寫入時機，所以這裡改成
     * 「距離上次寫入滿 400ms 就補一次」，最壞情況也只丟掉最後 400ms 的字。
     */
    private fun scheduleDraft(noteId: String, revision: Long) {
        synchronized(lock) { draftJobs.remove(noteId)?.cancel() }
        val wait = synchronized(lock) {
            val dueAt = (lastDraftWrittenAt[noteId] ?: 0L) + DRAFT_DEBOUNCE_MS
            (dueAt - System.currentTimeMillis()).coerceAtLeast(0L)
        }
        val job = scope.launch {
            if (wait > 0) delay(wait)
            val snapshot = synchronized(lock) { pending[noteId] } ?: return@launch
            if (snapshot.revision != revision) return@launch
            runCatching { repository.writeDraft(noteId, snapshot.title, snapshot.content) }
                .onFailure { Log.e(TAG, "[NoteAutoSaver] 草稿寫入失敗: $noteId", it) }
            synchronized(lock) { lastDraftWrittenAt[noteId] = System.currentTimeMillis() }
        }
        synchronized(lock) { draftJobs[noteId] = job }
    }

    private fun scheduleFlush(noteId: String) {
        synchronized(lock) { flushJobs.remove(noteId)?.cancel() }
        val job = scope.launch {
            delay(IDLE_FLUSH_MS)
            flushInternal(noteId)
        }
        synchronized(lock) { flushJobs[noteId] = job }
    }

    /**
     * 持續輸入時（防抖永遠等不到）由這個迴圈每 [PERIODIC_FLUSH_MS] 落盤一次；
     * 沒有待寫內容就自動結束，不會常駐。
     */
    private fun ensureTicker() {
        synchronized(lock) {
            if (tickerRunning) return
            tickerRunning = true
        }
        scope.launch {
            try {
                while (true) {
                    delay(PERIODIC_FLUSH_MS)
                    val due = synchronized(lock) {
                        if (pending.isEmpty()) {
                            null
                        } else {
                            // 用「上次落盤時間」而不是「最後編輯時間」判斷：
                            // 一直打字時最後編輯時間會不停往後跑，永遠等不到落盤時機。
                            val now = System.currentTimeMillis()
                            pending.keys
                                .filter { noteId ->
                                    now - (lastFlushedAt[noteId] ?: 0L) >= PERIODIC_FLUSH_MS
                                }
                        }
                    } ?: return@launch
                    due.forEach { flushInternal(it) }
                }
            } finally {
                synchronized(lock) { tickerRunning = false }
            }
        }
    }

    private suspend fun flushInternal(noteId: String) {
        if (synchronized(lock) { pending[noteId] } == null) return

        writeMutex.withLock {
            val snapshot = synchronized(lock) { pending[noteId] } ?: return@withLock
            publish(noteId, NoteSaveState.SAVING)

            val note = runCatching { repository.getNote(noteId) }.getOrNull()
            if (note == null) {
                // 筆記已被刪除或還沒匯入：丟掉這份內容，避免寫回已刪的檔案。
                synchronized(lock) { pending.remove(noteId) }
                publish(noteId, NoteSaveState.IDLE)
                return@withLock
            }

            val now = System.currentTimeMillis()
            synchronized(lock) { lastFlushedAt[noteId] = now }
            // 資料庫先寫：即使接著檔案寫失敗，內容也還在，重啟後掃描不會拿舊檔蓋掉。
            runCatching { repository.update(noteId, snapshot.title, snapshot.content, now) }
                .onFailure { Log.e(TAG, "[NoteAutoSaver] 資料庫寫入失敗: $noteId", it) }

            val needsFileWrite = synchronized(lock) { noteId in fileDirty } ||
                note.content != snapshot.content
            val fileOk = if (needsFileWrite) {
                runCatching { files.writeContent(note.uriString, snapshot.content) }
                    .getOrDefault(false)
            } else {
                true
            }

            if (fileOk) {
                synchronized(lock) {
                    fileDirty.remove(noteId)
                    failures.remove(noteId)
                    lastCommitted[noteId] = snapshot.title to snapshot.content
                }
                runCatching { repository.deleteDraft(noteId) }
                    .onFailure { Log.e(TAG, "[NoteAutoSaver] 刪除草稿失敗: $noteId", it) }
                val stillDirty = synchronized(lock) {
                    if (pending[noteId]?.revision == snapshot.revision) {
                        pending.remove(noteId)
                        false
                    } else {
                        // 寫盤期間又打了字：這批算存好了，但畫面上還有更新的內容待寫。
                        pending.containsKey(noteId)
                    }
                }
                publish(noteId, if (stillDirty) NoteSaveState.PENDING else NoteSaveState.SAVED)
            } else {
                // 檔案寫不回去：草稿留著，資料庫也已經是新的，等下一輪或下次啟動重試。
                runCatching { repository.writeDraft(noteId, snapshot.title, snapshot.content) }
                val attempts = synchronized(lock) {
                    fileDirty.add(noteId)
                    val next = (failures[noteId] ?: 0) + 1
                    failures[noteId] = next
                    if (next >= MAX_FILE_RETRIES) pending.remove(noteId)
                    next
                }
                Log.e(
                    TAG,
                    "[NoteAutoSaver] 檔案寫入失敗 ($attempts/$MAX_FILE_RETRIES): ${note.uriString}"
                )
                publish(noteId, NoteSaveState.FAILED)
            }
        }
    }

    private fun publish(noteId: String, state: NoteSaveState) {
        _states.update { current ->
            if (current[noteId] == state) current else current + (noteId to state)
        }
    }

    companion object {
        private const val TAG = "MDEditor_Debug"

        /** 停止輸入多久後把內容寫進草稿表。 */
        private const val DRAFT_DEBOUNCE_MS = 400L

        /** 停止輸入多久後把草稿落盤成 `.md` 檔。 */
        private const val IDLE_FLUSH_MS = 1_200L

        /** 持續輸入時，最久每隔這麼久落盤一次。 */
        private const val PERIODIC_FLUSH_MS = 5_000L

        /** 檔案連續寫失敗幾次後不再自動重試（草稿仍保留，下次啟動再試）。 */
        private const val MAX_FILE_RETRIES = 5

        /** 草稿保留天數；更舊的草稿在啟動時清掉。 */
        private const val DRAFT_TTL_MS = 7L * 24 * 60 * 60 * 1000

        /**
         * 寫入用的 Process 級 Scope：離開編輯器或 Activity 被回收後，已經排隊的
         * 落盤仍會完成，不會因為 viewModelScope 被取消而寫到一半斷掉。
         */
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

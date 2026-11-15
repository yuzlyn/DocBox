package com.yuzlyn.mdeditor.ui.component

/** 一次編輯快照：還原時連游標位置一起回去，使用者才不會失去焦點感。 */
internal data class EditSnapshot(
    val title: String,
    val body: String,
    val selectionStart: Int,
    val selectionEnd: Int
)

/**
 * 編輯歷史（撤銷／重做）。
 *
 * 規則刻意做得像一般編輯器：
 * - 連續打字合併成一步（停手超過 [coalesceMs] 才切新的一步），
 *   否則每打一個字都算一步，撤銷要按幾十次才回得去。
 * - 一次連續輸入最長 [maxBurstMs]，避免一路打十分鐘只留下一個步驟。
 * - 只移動游標不算編輯。
 * - 撤銷後再編輯，重做堆疊立即失效（跟桌面編輯器一致）。
 *
 * 這個類別不碰 Compose，方便直接用單元測試鎖行為。
 */
internal class EditHistory(
    private val limit: Int = DEFAULT_LIMIT,
    private val coalesceMs: Long = DEFAULT_COALESCE_MS,
    private val maxBurstMs: Long = DEFAULT_MAX_BURST_MS
) {
    private val undoStack = ArrayDeque<EditSnapshot>()
    private val redoStack = ArrayDeque<EditSnapshot>()
    private var current: EditSnapshot? = null
    private var lastEditAt = 0L
    private var burstStartedAt = 0L

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /** 載入筆記時建立基準點：此時還沒有任何可撤銷的步驟。 */
    fun reset(snapshot: EditSnapshot) {
        undoStack.clear()
        redoStack.clear()
        current = snapshot
        lastEditAt = 0L
        burstStartedAt = 0L
    }

    /** 每次內容變動後呼叫；同一次連續輸入只留一個步驟。 */
    fun record(snapshot: EditSnapshot, now: Long) {
        val previous = current
        if (previous == null) {
            current = snapshot
            lastEditAt = now
            burstStartedAt = now
            return
        }
        // 文字沒變（只是移動游標／改變選取）：更新位置就好，不產生步驟。
        if (previous.title == snapshot.title && previous.body == snapshot.body) {
            current = snapshot
            return
        }
        val sameBurst = now - lastEditAt <= coalesceMs && now - burstStartedAt <= maxBurstMs
        if (!sameBurst) {
            pushUndo(previous)
            redoStack.clear()
            burstStartedAt = now
        }
        current = snapshot
        lastEditAt = now
    }

    /** @return 要還原到的狀態；沒得撤銷時回傳 null。 */
    fun undo(): EditSnapshot? {
        val target = undoStack.removeLastOrNull() ?: return null
        current?.let { redoStack.addLast(it) }
        current = target
        // 讓下一次編輯一定開新的一步，不會和撤銷前的節奏黏在一起。
        lastEditAt = 0L
        burstStartedAt = 0L
        return target
    }

    /** @return 要重做到的狀態；沒得重做時回傳 null。 */
    fun redo(): EditSnapshot? {
        val target = redoStack.removeLastOrNull() ?: return null
        current?.let { pushUndo(it) }
        current = target
        lastEditAt = 0L
        burstStartedAt = 0L
        return target
    }

    private fun pushUndo(snapshot: EditSnapshot) {
        if (limit > 0 && undoStack.size >= limit) undoStack.removeFirst()
        undoStack.addLast(snapshot)
    }

    companion object {
        /** 最多保留幾步；長文件每步都是整篇字串，不宜無限長。 */
        const val DEFAULT_LIMIT = 40
        /** 停手多久之後算新的一步。 */
        const val DEFAULT_COALESCE_MS = 900L
        /** 一次連續輸入最多合併這麼久，避免長篇寫作只留一步。 */
        const val DEFAULT_MAX_BURST_MS = 5_000L
    }
}

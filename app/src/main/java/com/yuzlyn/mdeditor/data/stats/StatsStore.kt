package com.yuzlyn.mdeditor.data.stats

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * 統計紀錄的落盤端：`files/reading_stats.log`。
 *
 * 三個設計取捨：
 * 1. **追加而不是改寫**。記一筆就 append 一行，2 秒後才真的寫；關掉 app 前再補一次。
 *    只改尾端的話，寫到一半被殺最多掉最後一行，不會像 JSON 那樣整份壞掉讀不回來。
 * 2. **開機時壓實（compact）**。同一天同一本書會有很多筆「+5 秒」，壓成一行之後
 *    檔案大小跟「讀了幾本書 × 用了幾天」成正比，而不是跟翻頁次數成正比。
 * 3. **全部改動走同一把鎖**。讀檔是非同步的，如果它還在讀、使用者已經在看書，
 *    後寫的那一份就會把前一份蓋掉——統計少了幾分鐘，使用者只會覺得「它在亂算」。
 */
class StatsStore(context: Context) {

    private val file = File(context.filesDir, FILE_NAME)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _snapshot = MutableStateFlow(StatsSnapshot())
    val snapshot: StateFlow<StatsSnapshot> = _snapshot.asStateFlow()

    /** 還沒寫進檔案的增量。每筆都是「這次多了多少」，重讀時再累加一次就是對的。 */
    private val pending = mutableListOf<String>()
    private var flushJob: Job? = null

    init {
        scope.launch { mutex.withLock { loadLocked() } }
    }

    /**
     * 記一筆。[key] 要跨天穩定（書用 Uri、文件用 note id），排行榜才合得起來。
     * 呼叫端負責先看設定裡有沒有允許記錄。
     */
    fun record(kind: StatKind, key: String, name: String, value: Long = 1) {
        if (key.isBlank() || value <= 0) return
        val row = StatRow(StatsDates.today(), kind, key, name, value)
        scope.launch {
            mutex.withLock {
                _snapshot.value = _snapshot.value.with(row)
                pending += row.encode()
                scheduleFlushLocked()
            }
        }
    }

    /** 立刻落盤（切到背景、離開閱讀器時呼叫）。 */
    fun flushNow() {
        scope.launch { mutex.withLock { flushLocked() } }
    }

    /** 清空全部紀錄。設定頁的「清除統計」用。 */
    fun clear() {
        scope.launch {
            mutex.withLock {
                flushJob?.cancel()
                flushJob = null
                pending.clear()
                runCatching { file.delete() }
                    .onFailure { Log.w(TAG, "清除統計檔失敗", it) }
                _snapshot.value = StatsSnapshot()
            }
        }
    }

    private fun scheduleFlushLocked() {
        if (flushJob?.isActive == true) return
        flushJob = scope.launch {
            delay(FLUSH_DELAY_MS)
            mutex.withLock { flushLocked() }
        }
    }

    private fun flushLocked() {
        if (pending.isEmpty()) return
        val text = pending.joinToString(separator = "\n", postfix = "\n")
        pending.clear()
        runCatching {
            file.parentFile?.mkdirs()
            file.appendText(text)
        }.onFailure {
            Log.e(TAG, "寫入統計檔失敗", it)
            // 寫不進去就留在記憶體裡，下一輪再試——統計掉了比寫壞檔案好。
            pending.add(0, text.trimEnd('\n'))
        }
    }

    private fun loadLocked() {
        val text = runCatching { if (file.exists()) file.readText() else "" }.getOrElse {
            Log.e(TAG, "讀取統計檔失敗", it)
            ""
        }
        if (text.isBlank()) return
        var snapshot = StatsSnapshot()
        text.lineSequence().forEach { line ->
            StatRow.decode(line)?.let { snapshot = snapshot.with(it) }
        }
        _snapshot.value = snapshot
        compactLocked(snapshot)
    }

    /**
     * 把「很多筆增量」壓成「一天一格」。
     *
     * 行數少於門檻就不動：每次啟動都重寫一次檔案，反而多一次寫入風險。
     */
    private fun compactLocked(snapshot: StatsSnapshot) {
        if (snapshot.rows.size < COMPACT_THRESHOLD) return
        runCatching {
            val temp = File(file.parentFile, "$FILE_NAME.tmp")
            temp.writeText(
                snapshot.rows.values
                    .sortedBy { it.day }
                    .joinToString(separator = "\n", postfix = "\n") { it.encode() }
            )
            if (file.exists()) file.delete()
            temp.renameTo(file)
            Log.i(TAG, "統計壓實完成：${snapshot.rows.size} 格")
        }.onFailure { Log.w(TAG, "統計壓實失敗，保留原檔", it) }
    }

    private companion object {
        const val TAG = "ReadingStats"
        const val FILE_NAME = "reading_stats.log"
        const val FLUSH_DELAY_MS = 2_000L
        const val COMPACT_THRESHOLD = 240
    }
}

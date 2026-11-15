package com.yuzlyn.mdeditor.data.book

import android.content.Context
import org.json.JSONObject
import java.security.MessageDigest

/** 上次讀到哪裡。 */
data class ReadingProgress(
    /** 章節序號（0 基）。 */
    val chapter: Int,
    /** 章內錨點；沒有就靠 [fraction] 換算。 */
    val anchor: String?,
    /** 章內比例 0..1。翻頁模式是「第幾頁 ÷ 總頁數」，滾動模式是捲動距離。 */
    val fraction: Float,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 閱讀進度的落地儲存。
 *
 * 用 SharedPreferences 而不是資料庫：一本書一筆、讀寫都在主執行緒附近發生，
 * 開資料庫反而要處理執行緒與交易。鍵不是檔案路徑——`content://` 每次給的
 * URI 可能不同，同一本書換個入口就該接上進度，所以用「書名 + 作者 + 章數」摘要。
 */
class ReadingProgressStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun keyOf(book: Book): String {
        val meta = book.metadata
        val raw = listOf(
            meta.title,
            meta.author.orEmpty(),
            book.chapterCount.toString()
        ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-1").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(20)
    }

    fun load(key: String): ReadingProgress? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            ReadingProgress(
                chapter = json.optInt("chapter", 0).coerceAtLeast(0),
                anchor = json.optString("anchor").takeIf { it.isNotBlank() },
                fraction = json.optDouble("fraction", 0.0).toFloat().coerceIn(0f, 1f),
                updatedAt = json.optLong("updatedAt", 0L)
            )
        }.getOrNull()
    }

    fun save(key: String, progress: ReadingProgress) {
        if (progress.chapter < 0) return
        val json = JSONObject()
            .put("chapter", progress.chapter)
            .put("anchor", progress.anchor.orEmpty())
            .put("fraction", progress.fraction.toDouble())
            .put("updatedAt", progress.updatedAt)
        prefs.edit().putString(key, json.toString()).apply()
        prune()
    }

    fun clear(key: String) {
        prefs.edit().remove(key).apply()
    }

    /** 只留最近 [MAX_ENTRIES] 本，免得讀過幾百本就開始長灰。 */
    private fun prune() {
        val all = prefs.all
        if (all.size <= MAX_ENTRIES) return
        val editor = prefs.edit()
        all.entries
            .mapNotNull { entry ->
                runCatching {
                    entry.key to JSONObject(entry.value as String).optLong("updatedAt", 0L)
                }.getOrNull()
            }
            .sortedByDescending { it.second }
            .drop(MAX_ENTRIES)
            .forEach { editor.remove(it.first) }
        editor.apply()
    }

    private companion object {
        const val PREFS_NAME = "reading_progress"
        const val MAX_ENTRIES = 80
    }
}

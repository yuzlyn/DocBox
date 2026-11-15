package com.yuzlyn.mdeditor.data.stats

import java.time.LocalDate
import java.util.Base64

/**
 * 閱讀／編輯紀錄的**純資料層**。
 *
 * 這裡只有字串與數字，不碰 Android：整份統計能不能算對，靠 JVM 單測守著，
 * 不必上真機翻頁才能驗。
 *
 * 儲存形式是「一天一格」的追加式文字檔（見 [StatRow.encode]）：同一本書同一天
 * 只會有一行，重複紀錄就累加。之所有不用 JSON：
 * - 追加不必整檔重寫，寫到一半被殺也不會壞掉（最多掉最後一行）；
 * - 舊版讀不懂新欄位時可以整行跳過，不會把整份統計弄成空白。
 */

/** 統計紀錄的三種事件。 */
enum class StatKind(val id: String) {
    /** 閱讀電子書；`value` 是秒數。 */
    READ("r"),

    /** 打開一份文件（筆記）；`value` 是次數。 */
    OPEN("o"),

    /** 編輯一份文件；`value` 是存檔次數。 */
    EDIT("e");

    companion object {
        fun fromId(id: String): StatKind? = entries.firstOrNull { it.id == id }
    }
}

/**
 * 一行紀錄：某一天、某一本書或某一份文件、某一種事件。
 *
 * [key] 是書的 Uri 或筆記的 id（同一個物件跨天要不變，排行榜才合得起來）；
 * [name] 是顯示用的名字，**取最新一次**——書名或標題改過之後，排行榜要跟著改。
 */
data class StatRow(
    val day: String,
    val kind: StatKind,
    val key: String,
    val name: String,
    val value: Long
) {
    /** 合併用的鍵：同一天、同一種事件、同一個物件就是要疊在一起。 */
    val slot: Slot get() = Slot(day, kind, key)

    data class Slot(val day: String, val kind: StatKind, val key: String)

    /**
     * 一行文字：`日期 \t 種類 \t 數值 \t key \t 名字`。
     *
     * key 與名字走 base64url：書名裡有 tab、換行、逗號都很正常，
     * 直接塞進分隔格式遲早會把一整行拆錯。
     */
    fun encode(): String = buildString {
        append(day).append('\t')
        append(kind.id).append('\t')
        append(value).append('\t')
        append(encodeText(key)).append('\t')
        append(encodeText(name))
    }

    companion object {
        fun decode(line: String): StatRow? {
            if (line.isBlank()) return null
            val parts = line.split('\t')
            if (parts.size < 5) return null
            val kind = StatKind.fromId(parts[1]) ?: return null
            val value = parts[2].toLongOrNull() ?: return null
            // 0 或負數沒有意義（舊版可能寫過 0 秒的閱讀），直接丟掉。
            if (value <= 0) return null
            if (!StatsDates.isDay(parts[0])) return null
            return StatRow(
                day = parts[0],
                kind = kind,
                key = decodeText(parts[3]),
                name = decodeText(parts[4]),
                value = value
            )
        }
    }
}

private fun encodeText(text: String): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(text.toByteArray(Charsets.UTF_8))

private fun decodeText(text: String): String = runCatching {
    if (text.isEmpty()) {
        ""
    } else {
        String(Base64.getUrlDecoder().decode(text), Charsets.UTF_8)
    }
}.getOrDefault("")

/** 一段期間的總量。 */
data class StatsTotals(
    val readSeconds: Long = 0,
    /** 期間內讀過的書（不重複）。 */
    val booksRead: Int = 0,
    /** 打開過的文件的次數。 */
    val docsOpened: Int = 0,
    /** 編輯過的文件份數（不重複）。 */
    val docsEdited: Int = 0,
    /** 存檔次數。 */
    val editCount: Int = 0
) {
    val isEmpty: Boolean
        get() = readSeconds == 0L && docsOpened == 0 && editCount == 0
}

/** 排行榜的一列：一本書或一份文件。 */
data class StatsItem(
    val key: String,
    val name: String,
    /** 書是閱讀秒數；文件是編輯次數。 */
    val value: Long,
    val firstDay: String,
    val lastDay: String,
    val opens: Int = 0
)

/** 某一天的活躍度（熱力圖一格）。 */
data class StatsDay(val day: String, val score: Long, val readSeconds: Long)

/**
 * 統計快照：一份不可變的聚合結果。
 *
 * 不可變是刻意的——統計頁直接吃它，換一份就是換一份，不會出現「列表讀到一半
 * 資料被改掉」這種只有真機偶爾才重現的問題。
 */
data class StatsSnapshot(
    val rows: Map<StatRow.Slot, StatRow> = emptyMap()
) {
    /** 併入一行紀錄；同一天同一物件就累加。 */
    fun with(row: StatRow): StatsSnapshot {
        val old = rows[row.slot]
        val merged = row.copy(
            value = (old?.value ?: 0L) + row.value,
            name = row.name.ifBlank { old?.name.orEmpty() }
        )
        return StatsSnapshot(rows + (row.slot to merged))
    }

    fun merge(other: StatsSnapshot): StatsSnapshot {
        var next = this
        other.rows.values.forEach { next = next.with(it) }
        return next
    }

    val isEmpty: Boolean get() = rows.isEmpty()

    /** 期間內（含頭尾）的所有紀錄。日期是 ISO 格式，字串比較就等於時間比較。 */
    fun between(from: String, to: String): List<StatRow> =
        rows.values.filter { it.day >= from && it.day <= to }

    fun totals(from: String, to: String): StatsTotals {
        val period = between(from, to)
        return StatsTotals(
            readSeconds = period.filter { it.kind == StatKind.READ }.sumOf { it.value },
            booksRead = period.filter { it.kind == StatKind.READ }.mapTo(mutableSetOf()) { it.key }.size,
            docsOpened = period.filter { it.kind == StatKind.OPEN }.sumOf { it.value.toInt() },
            docsEdited = period.filter { it.kind == StatKind.EDIT }.mapTo(mutableSetOf()) { it.key }.size,
            editCount = period.filter { it.kind == StatKind.EDIT }.sumOf { it.value.toInt() }
        )
    }

    /** 排行榜：[kind] 是 READ 就是書，EDIT 就是文件。 */
    fun ranking(from: String, to: String, kind: StatKind, limit: Int = 20): List<StatsItem> =
        between(from, to)
            .filter { it.kind == kind }
            .groupBy { it.key }
            .map { (key, list) ->
                StatsItem(
                    key = key,
                    // 名字取最後一天那一筆：改過標題要顯示新的。
                    name = list.maxByOrNull { it.day }?.name.orEmpty().ifBlank { key },
                    value = list.sumOf { it.value },
                    firstDay = list.minOf { it.day },
                    lastDay = list.maxOf { it.day },
                    opens = list.sumOf { it.value.toInt() }
                )
            }
            .sortedWith(compareByDescending<StatsItem> { it.value }.thenByDescending { it.lastDay })
            .take(limit)

    /**
     * 熱力圖用的每日活躍度：把「讀多久」與「打了幾次字」折算成一條可比的量。
     *
     * 一分鐘閱讀 = 一次存檔：兩者都算「有在動」。純看時長的話，只寫筆記的那天
     * 會被算成空白；純看次數的話，靜靜讀兩小時反而輸給隨手改五個字。
     */
    fun activity(from: String, to: String): Map<String, StatsDay> {
        val map = mutableMapOf<String, StatsDay>()
        between(from, to).forEach { row ->
            val score = when (row.kind) {
                StatKind.READ -> row.value / 60
                else -> row.value
            }
            val old = map[row.day]
            map[row.day] = StatsDay(
                day = row.day,
                score = (old?.score ?: 0) + score,
                readSeconds = (old?.readSeconds ?: 0) + if (row.kind == StatKind.READ) row.value else 0
            )
        }
        return map
    }
}

/** 日期工具：全部走 ISO 字串，跨時區問題留給 [today]。 */
object StatsDates {

    fun today(): String = LocalDate.now().toString()

    fun daysAgo(days: Long): String = LocalDate.now().minusDays(days).toString()

    fun isDay(text: String): Boolean = runCatching { LocalDate.parse(text) }.isSuccess

    fun plusDays(day: String, days: Long): String =
        runCatching { LocalDate.parse(day).plusDays(days).toString() }.getOrDefault(day)

    /** 星期幾，0 = 週一。熱力圖的列就是它。 */
    fun weekday(day: String): Int =
        runCatching { LocalDate.parse(day).dayOfWeek.value - 1 }.getOrDefault(0)

    /** 從 [day] 往回推到那一週的週一。 */
    fun startOfWeek(day: String): String = plusDays(day, -weekday(day).toLong())

    /** 兩個日期之間的天數（含頭不含尾）；用來配版面。 */
    fun daysBetween(from: String, to: String): Int = runCatching {
        java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(from), LocalDate.parse(to)).toInt()
    }.getOrDefault(0)

    /** `2026-11-15` → `11/15`。 */
    fun shortLabel(day: String): String = runCatching {
        val date = LocalDate.parse(day)
        "%02d/%02d".format(date.monthValue, date.dayOfMonth)
    }.getOrDefault(day)

    /**
     * `2026-11-15` → `11月`（中文）／`Nov`（英文）／`nov.`（法文）。
     *
     * 用 [java.time.format.DateTimeFormatter] 的 `MMM` 而不是自己拼「${month}月」：
     * 這個字串會直接印在熱力圖上，語言一換就得跟著換。
     */
    fun monthLabel(day: String, locale: java.util.Locale = java.util.Locale.getDefault()): String =
        runCatching {
            val date = LocalDate.parse(day)
            java.time.format.DateTimeFormatter.ofPattern("MMM", locale).format(date)
        }.getOrDefault(day)
}

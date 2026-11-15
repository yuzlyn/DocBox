package com.yuzlyn.mdeditor.data.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 統計的資料層單測。
 *
 * 這一層全部是純函數，所以在 JVM 上就能把「跨天、跨區間、壞行」這些
 * 只有放到真機跑幾個月才會冒出來的問題先釘住。
 */
class StatsLogTest {

    private fun read(day: String, key: String, seconds: Long, name: String = "書 $key") =
        StatRow(day, StatKind.READ, key, name, seconds)

    private fun open(day: String, key: String, name: String = "筆記 $key") =
        StatRow(day, StatKind.OPEN, key, name, 1)

    private fun edit(day: String, key: String, times: Long = 1, name: String = "筆記 $key") =
        StatRow(day, StatKind.EDIT, key, name, times)

    // ── 檔案格式 ──────────────────────────────────────────────────────

    @Test
    fun `一行紀錄可以原樣讀回來`() {
        // 名字裡有 tab、換行、逗號、引號都是常態，分隔式格式最容易死在這裡。
        val row = StatRow(
            day = "2026-11-15",
            kind = StatKind.READ,
            key = "content://book/1?a=b",
            name = "第一行\t第二行\n第三行, \"引號\"",
            value = 3725
        )
        assertEquals(row, StatRow.decode(row.encode()))
    }

    @Test
    fun `壞掉的行直接跳過 不會讓整份統計消失`() {
        assertNull("欄位不足", StatRow.decode("2026-11-15\tr\t60"))
        assertNull("未知種類", StatRow.decode("2026-11-15\tz\t60\tYQ\tYg"))
        assertNull("數值不是數字", StatRow.decode("2026-11-15\tr\tabc\tYQ\tYg"))
        assertNull("零值", StatRow.decode("2026-11-15\tr\t0\tYQ\tYg"))
        assertNull("日期不合法", StatRow.decode("2026-13-99\tr\t60\tYQ\tYg"))
        assertNull("空行", StatRow.decode(""))
    }

    @Test
    fun `同一本書同一天累加 而不是覆蓋`() {
        val snapshot = StatsSnapshot()
            .with(read("2026-11-15", "book-a", 60))
            .with(read("2026-11-15", "book-a", 30))
        assertEquals(1, snapshot.rows.size)
        assertEquals(90L, snapshot.totals("2026-11-15", "2026-11-15").readSeconds)
    }

    // ── 區間統計 ──────────────────────────────────────────────────────

    private val sample = StatsSnapshot()
        .with(read("2026-11-10", "book-a", 600))
        .with(read("2026-11-12", "book-a", 1200))
        .with(read("2026-11-14", "book-b", 300))
        .with(open("2026-11-14", "note-1"))
        .with(open("2026-11-15", "note-1"))
        .with(edit("2026-11-15", "note-1", 4))
        .with(edit("2026-11-15", "note-2", 2))

    @Test
    fun `區間含頭含尾 區間外的紀錄不算`() {
        val totals = sample.totals("2026-11-11", "2026-11-14")
        assertEquals(1200L + 300L, totals.readSeconds)
        assertEquals(2, totals.booksRead) // book-a、book-b
        assertEquals(1, totals.docsOpened) // note-1 打開一次
        assertEquals(0, totals.docsEdited)
    }

    @Test
    fun `文件看過幾次 和 編輯過幾份 是兩件事`() {
        val totals = sample.totals("2026-11-01", "2026-11-30")
        // note-1 打開兩次（同一天一次、另一天一次）
        assertEquals(2, totals.docsOpened)
        // 編輯過 note-1、note-2 兩份，但一共存了 6 次
        assertEquals(2, totals.docsEdited)
        assertEquals(6, totals.editCount)
    }

    @Test
    fun `排行榜依量排序 並記下起訖日`() {
        val books = sample.ranking("2026-11-01", "2026-11-30", StatKind.READ)
        assertEquals(listOf("book-a", "book-b"), books.map { it.key })
        assertEquals(1800L, books[0].value)
        assertEquals("2026-11-10", books[0].firstDay)
        assertEquals("2026-11-12", books[0].lastDay)
    }

    @Test
    fun `改過名字之後排行榜顯示新的`() {
        val snapshot = StatsSnapshot()
            .with(read("2026-11-14", "book-a", 60, name = "舊書名"))
            .with(read("2026-11-15", "book-a", 60, name = "新書名"))
        assertEquals("新書名", snapshot.ranking("2026-11-01", "2026-11-30", StatKind.READ)[0].name)
    }

    @Test
    fun `熱力圖把閱讀與寫作折成同一個量`() {
        val snapshot = StatsSnapshot()
            .with(read("2026-11-15", "book-a", 600)) // 10 分鐘 → 10
            .with(edit("2026-11-15", "note-1", 3)) // 3 次 → 3
        val day = snapshot.activity("2026-11-15", "2026-11-15")["2026-11-15"]
        assertEquals(13L, day?.score)
        assertEquals(600L, day?.readSeconds)
    }

    // ── 日期 ─────────────────────────────────────────────────────────

    @Test
    fun `週一起算的週首`() {
        // 2026-11-15 是週日，同週的週一是 11-09。
        assertEquals(6, StatsDates.weekday("2026-11-15"))
        assertEquals("2026-11-09", StatsDates.startOfWeek("2026-11-15"))
        assertEquals(0, StatsDates.weekday("2026-11-09"))
    }

    @Test
    fun `日期加減與天數`() {
        assertEquals("2026-11-16", StatsDates.plusDays("2026-11-15", 1))
        assertEquals("2026-11-08", StatsDates.plusDays("2026-11-15", -7))
        assertEquals(7, StatsDates.daysBetween("2026-11-08", "2026-11-15"))
    }

    @Test
    fun `壓實前後結果不變`() {
        // 模擬「寫了很多筆增量 → 開機時壓成一天一格 → 重新載入」。
        var snapshot = StatsSnapshot()
        repeat(20) { snapshot = snapshot.with(read("2026-11-15", "book-a", 5)) }
        val compacted = snapshot.rows.values.joinToString("\n") { it.encode() }
        var reloaded = StatsSnapshot()
        compacted.lineSequence().forEach { line ->
            StatRow.decode(line)?.let { reloaded = reloaded.with(it) }
        }
        assertEquals(snapshot.totals("2026-11-15", "2026-11-15"), reloaded.totals("2026-11-15", "2026-11-15"))
        assertEquals(1, reloaded.rows.size)
        assertTrue(reloaded.totals("2026-11-15", "2026-11-15").readSeconds == 100L)
    }
}

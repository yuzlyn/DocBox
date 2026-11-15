package com.yuzlyn.mdeditor.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 撤銷／重做的行為鎖定。
 *
 * 重點不是「能回上一版」，而是**步數的切法**：連續打字要合成一步，
 * 停手後要能切成新的一步，只移動游標不能製造空步驟。
 */
class EditHistoryTest {

    private fun snap(body: String, caret: Int = body.length) =
        EditSnapshot(title = "t", body = body, selectionStart = caret, selectionEnd = caret)

    @Test
    fun `載入後沒有可撤銷的步驟`() {
        val history = EditHistory()
        history.reset(snap(""))

        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
        assertNull(history.undo())
    }

    @Test
    fun `連續打字合併成一步`() {
        val history = EditHistory()
        history.reset(snap(""))
        history.record(snap("a"), now = 1_000)
        history.record(snap("ab"), now = 1_200)
        history.record(snap("abc"), now = 1_400)

        assertTrue(history.canUndo)
        // 一次撤銷要回到「開始打這串字之前」，而不是只刪掉最後一個字。
        assertEquals("", history.undo()?.body)
        assertFalse(history.canUndo)
    }

    @Test
    fun `停手超過合併視窗後切成新的一步`() {
        val history = EditHistory()
        history.reset(snap(""))
        history.record(snap("abc"), now = 1_000)
        history.record(snap("abc def"), now = 5_000)

        assertEquals("abc", history.undo()?.body)
        assertEquals("", history.undo()?.body)
    }

    @Test
    fun `只移動游標不產生步驟`() {
        val history = EditHistory()
        history.reset(snap("abc"))
        history.record(snap("abc", caret = 1), now = 2_000)

        assertFalse(history.canUndo)
    }

    @Test
    fun `撤銷之後可以重做`() {
        val history = EditHistory()
        history.reset(snap("one"))
        history.record(snap("one two"), now = 2_000)

        assertEquals("one", history.undo()?.body)
        assertTrue(history.canRedo)
        assertEquals("one two", history.redo()?.body)
        assertFalse(history.canRedo)
    }

    @Test
    fun `撤銷後再編輯會清掉重做`() {
        val history = EditHistory()
        history.reset(snap("one"))
        history.record(snap("one two"), now = 2_000)
        history.undo()

        history.record(snap("one three"), now = 10_000)

        assertFalse(history.canRedo)
        assertEquals("one", history.undo()?.body)
    }

    @Test
    fun `撤銷後立刻打字會開新的一步`() {
        val history = EditHistory()
        history.reset(snap(""))
        history.record(snap("abc"), now = 1_000)
        history.undo()
        // 撤銷後 100ms 就打字：若沿用合併視窗，這步會被吃掉。
        history.record(snap("x"), now = 1_100)

        assertEquals("", history.undo()?.body)
    }

    @Test
    fun `連續輸入超過上限會自動分段`() {
        val history = EditHistory()
        history.reset(snap(""))
        var now = 1_000L
        // 每 100ms 打一個字，持續 12 秒：必須留下多個步驟。
        for (i in 1..120) {
            history.record(snap("a".repeat(i)), now = now)
            now += 100
        }

        var steps = 0
        while (history.canUndo) {
            history.undo()
            steps++
        }
        assertTrue("長篇輸入應該切成多段，實際 $steps 步", steps >= 2)
    }

    @Test
    fun `步驟數量不會超過上限`() {
        val history = EditHistory(limit = 3)
        history.reset(snap(""))
        var now = 1_000L
        for (i in 1..10) {
            history.record(snap("a".repeat(i)), now = now)
            now += 5_000
        }

        var steps = 0
        while (history.canUndo) {
            history.undo()
            steps++
        }
        assertEquals(3, steps)
    }

    @Test
    fun `重做到底之後不能再重做`() {
        val history = EditHistory()
        history.reset(snap(""))
        history.record(snap("a"), now = 2_000)
        history.undo()

        assertEquals("a", history.redo()?.body)
        assertNull(history.redo())
    }
}

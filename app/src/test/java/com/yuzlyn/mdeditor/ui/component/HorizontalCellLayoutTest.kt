package com.yuzlyn.mdeditor.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 橫排「一格一字」版面的切格規則。
 *
 * 這裡只驗證純幾何（哪個字落在第幾列第幾格、游標換算），不涉及繪製。
 * [buildHCellLayout] 的量測用「一個字 10px」的假量測器代入，結果才可預期。
 */
class HorizontalCellLayoutTest {

    private val cell = 10f
    private val fakeWidth: (String) -> Float = { it.length * cell }

    private fun layout(text: String, columns: Int) =
        buildHCellLayout(text, columns, cell, fakeWidth)

    @Test
    fun `中文每字一格並依欄數換列`() {
        val grid = layout("一二三四五六", columns = 3)

        assertEquals(2, grid.rows)
        assertEquals(3, grid.columns)
        assertEquals(listOf(0, 0, 0, 1, 1, 1), grid.atoms.map { it.row })
        assertEquals(listOf(0, 1, 2, 0, 1, 2), grid.atoms.map { it.col })
        assertTrue(grid.atoms.all { it.cells == 1 })
    }

    @Test
    fun `英文單詞整體佔整數格不被拆開`() {
        // "markdown" 8 個字母 → 80px → 8 格
        val grid = layout("a markdown b", columns = 20)
        val word = grid.atoms.first { it.text == "markdown" }

        assertEquals(8, word.cells)
        assertEquals(2, word.col)
        assertEquals(1, grid.rows)
    }

    @Test
    fun `英文單詞遇到列尾會整段換列`() {
        // 每列 4 格：一個字 1 格、空格 1 格，"abc" 佔 3 格 → 在第 0 列放得下，
        // 下一個詞放不下就整段換列，不會被切成兩半。
        val grid = layout("a abc abcd", columns = 8)
        val second = grid.atoms.first { it.text == "abc" }
        val third = grid.atoms.first { it.text == "abcd" }

        assertEquals(0, second.row)
        assertEquals(1, third.row)
        assertEquals(0, third.col)
    }

    @Test
    fun `換行符強制換列`() {
        val grid = layout("一二\n三四", columns = 10)

        assertEquals(2, grid.rows)
        assertEquals(listOf(0, 0, 1, 1), grid.atoms.map { it.row })
        assertEquals(listOf(0, 1, 0, 1), grid.atoms.map { it.col })
    }

    @Test
    fun `游標位置落在格線上`() {
        val grid = layout("一二三四", columns = 4)

        assertEquals(0f, grid.caretPosition(0, cell).x)
        assertEquals(10f, grid.caretPosition(1, cell).x)
        assertEquals(30f, grid.caretPosition(3, cell).x)
    }

    @Test
    fun `換行處的游標落在下一列開頭`() {
        val grid = layout("一二\n三四", columns = 10)
        // offset 2 是換行符，游標應該出現在第二列行首。
        val caret = grid.caretPosition(2, cell)

        assertEquals(10f, caret.y)
        assertEquals(0f, caret.x)
    }

    @Test
    fun `點擊換算回字元位移`() {
        val grid = layout("一二三四", columns = 4)

        assertEquals(0, grid.offsetAt(2f, 5f, cell))
        assertEquals(1, grid.offsetAt(12f, 5f, cell))
        assertEquals(2, grid.offsetAt(18f, 5f, cell))
        assertEquals(3, grid.offsetAt(32f, 5f, cell))
        // 點在最後一格的右半邊，游標落在字之後。
        assertEquals(4, grid.offsetAt(38f, 5f, cell))
        // 點在字的下半格也還是同一個字（每格一個原子）。
        assertEquals(1, grid.offsetAt(12f, 9f, cell))
    }

    @Test
    fun `空白列也有游標位置`() {
        val grid = layout("一\n\n二", columns = 10)

        assertEquals(3, grid.rows)
        val caret = grid.caretPosition(2, cell)
        assertEquals(10f, caret.y)
    }

    @Test
    fun `空字串只有一列`() {
        val grid = layout("", columns = 4)

        assertEquals(1, grid.rows)
        assertTrue(grid.atoms.isEmpty())
        assertEquals(0f, grid.caretPosition(0, cell).x)
    }
}

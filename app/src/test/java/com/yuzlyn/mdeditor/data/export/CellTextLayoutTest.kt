package com.yuzlyn.mdeditor.data.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 稿紙裝箱：只有「一列裝幾格、什麼時候換列」這一件事，不需要 Android 繪圖。 */
class CellTextLayoutTest {

    private fun atom(text: String, cells: Int = 1) = CellAtom(text, cells)

    @Test
    fun `一列裝得下就只會有一列`() {
        val lines = packCellLines(listOf(atom("春"), atom("眠"), atom("不")), cellsPerLine = 10)

        assertEquals(1, lines.size)
        assertEquals(listOf("春", "眠", "不"), lines[0].map { it.text })
    }

    @Test
    fun `裝滿一列就換到下一列`() {
        val lines = packCellLines(
            listOf(atom("一"), atom("二"), atom("三"), atom("四"), atom("五")),
            cellsPerLine = 2
        )

        assertEquals(3, lines.size)
        assertEquals(listOf("一", "二"), lines[0].map { it.text })
        assertEquals(listOf("三", "四"), lines[1].map { it.text })
        assertEquals(listOf("五"), lines[2].map { it.text })
    }

    @Test
    fun `單詞按格數佔位`() {
        // 一個四格的英文單詞＋兩個漢字，一列五格：單詞先佔四格，第二個漢字就得換列。
        val lines = packCellLines(
            listOf(atom("word", cells = 4), atom("甲"), atom("乙")),
            cellsPerLine = 5
        )

        assertEquals(2, lines.size)
        assertEquals(listOf("word", "甲"), lines[0].map { it.text })
        assertEquals(listOf("乙"), lines[1].map { it.text })
    }

    @Test
    fun `換段一定換列`() {
        val lines = packCellLines(
            listOf(atom("上"), CellAtom.Break, atom("下")),
            cellsPerLine = 10
        )

        assertEquals(2, lines.size)
        assertEquals(listOf("上"), lines[0].map { it.text })
        assertEquals(listOf("下"), lines[1].map { it.text })
    }

    @Test
    fun `比一列還長的單詞不切開`() {
        // 寧可讓它溢出，也不要把一個單詞劈成兩半——那是讀不下去，不是不好看。
        val lines = packCellLines(
            listOf(atom("extraordinarily", cells = 9), atom("字")),
            cellsPerLine = 6
        )

        assertEquals(2, lines.size)
        assertEquals("extraordinarily", lines[0].single().text)
        assertEquals("字", lines[1].single().text)
    }

    @Test
    fun `用掉幾格是每個原子格數的總和`() {
        val line = listOf(atom("春"), atom("word", cells = 4), atom("。"))
        assertEquals(6, line.usedCells())
    }

    @Test
    fun `列長為零不會出事`() {
        assertTrue(packCellLines(listOf(atom("字")), cellsPerLine = 0).isEmpty())
    }
}

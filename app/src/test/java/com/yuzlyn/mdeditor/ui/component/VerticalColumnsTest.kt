package com.yuzlyn.mdeditor.ui.component

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 直排的欄切分：除了「填滿就換欄」，還要守住避頭尾——
 * 標點不能落在欄首、前引號不能留在欄尾，而且任何情況都不能切出空欄。
 */
class VerticalColumnsTest {

    private fun ranges(columns: VerticalColumns): List<IntRange> =
        columns.starts.indices.map { columns.starts[it] until columns.ends[it] }

    @Test
    fun `欄滿就換欄`() {
        val columns = buildVerticalColumns("一二三四五六七八九十", rows = 4)

        assertEquals(listOf(0 until 4, 4 until 8, 8 until 10), ranges(columns))
        assertEquals(listOf(1, 0, 0), columns.lineOf.toList())
    }

    @Test
    fun `句號不會落在欄首`() {
        val text = "一二三四。五六"
        val columns = buildVerticalColumns(text, rows = 4)

        // 照字數會斷在「四」之後，但下一欄開頭就變成「。」→ 往回退一個字。
        assertEquals(0 until 3, ranges(columns).first())
        assertEquals('四', text[columns.starts[1]])
        assertEquals('。', text[columns.starts[1] + 1])
    }

    @Test
    fun `前括號不會留在欄尾`() {
        val text = "一二三（四五"
        val columns = buildVerticalColumns(text, rows = 4)

        assertEquals(0 until 3, ranges(columns).first())
        assertEquals('（', text[columns.starts[1]])
    }

    @Test
    fun `逗號頓號引號都不落欄首`() {
        val text = "一二三，四五六、七八九」十"
        val columns = buildVerticalColumns(text, rows = 3)

        columns.starts.drop(1).forEach { start ->
            val first = text[start]
            assertTrue("第 $start 個字元是禁則字元：$first", first !in "。，、．,；：？！）］｝」』】")
        }
    }

    @Test
    fun `換行會換欄並遞增行號`() {
        val columns = buildVerticalColumns("一二三四\n五六", rows = 2)

        assertEquals(listOf(0 until 2, 2 until 4, 5 until 7), ranges(columns))
        // 第一欄是第 1 行、折行續欄不編號、換行後是第 2 行。
        assertEquals(listOf(1, 0, 2), columns.lineOf.toList())
    }

    @Test
    fun `連續標點也不會切出空欄`() {
        val columns = buildVerticalColumns("。。。一二", rows = 2)

        ranges(columns).forEach { range ->
            assertTrue("出現空欄：$range", range.first <= range.last)
        }
        assertEquals(0 until 1, ranges(columns).first())
    }

    @Test
    fun `每個字元都落在自己那一欄的範圍內`() {
        val text = "直排測試，含標點。「引號」與（括號）還有換行\n第二行文字。"
        val rows = 5
        val columns = buildVerticalColumns(text, rows)

        assertEquals(text.length, columns.columnOf.size - 1)
        for (index in text.indices) {
            val column = columns.columnOf[index]
            assertTrue("字元 $index 沒有被分欄", column in columns.starts.indices)
            if (index < columns.ends[column]) {
                assertTrue(index >= columns.starts[column])
                assertTrue("列號超出範圍：${columns.rowOf[index]}", columns.rowOf[index] < rows)
            } else {
                // 換行符掛在該欄結尾，游標停在換行後時才畫得出來。
                assertEquals(columns.ends[column], index)
                assertEquals('\n', text[index])
            }
        }
    }

    @Test
    fun `空文字也有一欄可以放游標`() {
        val columns = buildVerticalColumns("", rows = 10)

        assertEquals(1, columns.count)
        assertEquals(0, columns.columnOf[0])
    }
    @Test
    fun `emoji 只佔一格`() {
        // 😀 在 UTF-16 是兩個 char；照 char 切會變成上下兩個方塊。
        val text = "😀😀😀"
        val columns = buildVerticalColumns(text, rows = 2)

        assertEquals(2, columns.count)
        assertEquals(2, columns.rowsOf(0))
        assertEquals(1, columns.rowsOf(1))
        assertEquals("😀\n😀", columns.columnText(text, 0))
        assertEquals("😀", columns.columnText(text, 1))
    }

    @Test
    fun `游標落在代理對中間也指同一格`() {
        val text = "a😀b"
        val columns = buildVerticalColumns(text, rows = 4)

        // 0='a'、1..2='😀'（代理對）、3='b'：中間那個 offset 不該被算成新的列。
        assertEquals(columns.rowOf[0], 0)
        assertEquals(columns.rowOf[1], 1)
        assertEquals(columns.rowOf[2], 1)
        assertEquals(columns.rowOf[3], 2)
        assertEquals(columns.columnOf[1], columns.columnOf[2])
    }

    @Test
    fun `grapheme 邊界不會切開代理對`() {
        assertEquals(listOf(0, 1, 3, 4), graphemeStarts("a😀b").toList())
        assertEquals(listOf(0), graphemeStarts("").toList())
    }

    @Test
    fun `emoji 與中文混排時列數正確`() {
        val text = "一😀二😀三"
        val columns = buildVerticalColumns(text, rows = 3)

        assertEquals(2, columns.count)
        assertEquals("一\n😀\n二", columns.columnText(text, 0))
        assertEquals("😀\n三", columns.columnText(text, 1))
    }

    @Test
    fun `外部传入字格边界与自行计算的结果一致`() {
        // 键盘升降只换列数、文字没变，字格边界可以重用；这条守住「重用不会改变分栏」。
        val text = "一二😀三\n四五"
        val auto = buildVerticalColumns(text, rows = 3)
        val reused = buildVerticalColumns(text, rows = 3, cells = graphemeStarts(text))

        assertEquals(auto.starts.toList(), reused.starts.toList())
        assertEquals(auto.ends.toList(), reused.ends.toList())
        assertEquals(auto.cellStarts.toList(), reused.cellStarts.toList())
        assertEquals(auto.cellEnds.toList(), reused.cellEnds.toList())
        assertEquals(auto.lineOf.toList(), reused.lineOf.toList())
        assertEquals(auto.columnText(text, 0), reused.columnText(text, 0))
    }

    @Test
    fun `同一段文字换列数后分栏会不同`() {
        // 重排的本质：文字没动，列数一变，每一栏装的内容就变了。
        val text = "一二三四五六"
        val wide = buildVerticalColumns(text, rows = 3)
        val narrow = buildVerticalColumns(text, rows = 2)

        assertEquals(2, wide.count)
        assertEquals(3, narrow.count)
        assertEquals("一\n二\n三", wide.columnText(text, 0))
        assertEquals("一\n二", narrow.columnText(text, 0))
    }

    @Test
    fun `點字格下半部會放到該字之後`() {
        val text = "一二三"
        val columns = buildVerticalColumns(text, rows = 3)
        val height = 100f

        // 上半部＝放在這個字前面；下半部＝放在它後面（最後一個字才點得到）。
        assertEquals(0, caretOffsetForTap(columns, 0, 10f, height, text.length))
        assertEquals(1, caretOffsetForTap(columns, 0, 60f, height, text.length))
        // 第二格：上半部→「二」之前(1)，下半部→「二」之後(2)。
        assertEquals(1, caretOffsetForTap(columns, 0, 110f, height, text.length))
        assertEquals(2, caretOffsetForTap(columns, 0, 160f, height, text.length))
        // 最後一格的下半部→整欄尾端(3)，也就是「點得到最後一個字」。
        assertEquals(3, caretOffsetForTap(columns, 0, 260f, height, text.length))
    }

    @Test
    fun `含 emoji 時不會把游標放進代理對中間`() {
        val text = "😀好"
        val columns = buildVerticalColumns(text, rows = 2)
        val height = 100f

        assertEquals(0, caretOffsetForTap(columns, 0, 10f, height, text.length))
        // 點 emoji 的下半部：必須跳到「好」的起點（2），不是 1（代理對中間）。
        assertEquals(2, caretOffsetForTap(columns, 0, 60f, height, text.length))
        assertEquals(3, caretOffsetForTap(columns, 0, 160f, height, text.length))
    }

    @Test
    fun `點到欄底以下會落在欄尾`() {
        val text = "一二三四五六"
        val columns = buildVerticalColumns(text, rows = 3)

        assertEquals(3, caretOffsetForTap(columns, 0, 5000f, 100f, text.length))
    }

    @Test
    fun `點在文字下方的空白也只到欄尾`() {
        // 空白處的 row 遠大於這一欄的字數，落點不能被 +1 推出欄尾。
        val text = "一二三四五六"
        val columns = buildVerticalColumns(text, rows = 3)

        assertEquals(3, caretOffsetForTap(columns, 0, 5050f, 100f, text.length))
        assertEquals(6, caretOffsetForTap(columns, 1, 7050f, 100f, text.length))
    }

    @Test
    fun `點在最後一格的下半部算是點在行尾`() {
        val text = "一二三四五六"
        val columns = buildVerticalColumns(text, rows = 3)
        val height = 100f

        // 第三格是這一欄的最後一格：下半部＝行尾。
        val tail = caretForTap(columns, 0, 260f, height, text.length)
        assertEquals(3, tail.offset)
        assertTrue(tail.atTail)

        // 第二格的下半部只是字與字之間，不是行尾。
        val middle = caretForTap(columns, 0, 160f, height, text.length)
        assertEquals(2, middle.offset)
        assertTrue(!middle.atTail)

        // 點在欄底以下的空白，一樣算行尾。
        assertTrue(caretForTap(columns, 0, 5000f, height, text.length).atTail)
    }

    @Test
    fun `行尾的游標畫在欄尾而不是下一欄欄首`() {
        val text = "一二三四五六"
        val columns = buildVerticalColumns(text, rows = 3)
        val tap = caretForTap(columns, 0, 260f, 100f, text.length)

        val tail = tailCaretColumn(columns, tap.offset, tap.offset)
        assertEquals(0, tail)
        // 欄尾：畫在第三列（最後一格的下方）。
        assertEquals(3, caretRowInColumn(columns, 0, tap.offset, tail))
        // 下一欄這一帧不畫，否則同一個位置會同時出現兩個游標。
        assertEquals(-1, caretRowInColumn(columns, 1, tap.offset, tail))
    }

    @Test
    fun `沒有行尾提示時畫在下一欄欄首`() {
        val text = "一二三四五六"
        val columns = buildVerticalColumns(text, rows = 3)

        // offset 3 同時是第一欄的欄尾與第二欄的欄首。
        assertEquals(-1, caretRowInColumn(columns, 0, 3, -1))
        assertEquals(0, caretRowInColumn(columns, 1, 3, -1))
    }

    @Test
    fun `換行前的位置也畫得出游標`() {
        val text = "一二\n三四"
        val columns = buildVerticalColumns(text, rows = 3)

        // '\n' 掛在第一欄欄尾、不屬於第二欄；沒有人接手時要畫在第一欄的欄尾。
        assertEquals(2, columns.ends[0])
        assertEquals(-1, caretRowInColumn(columns, 1, 2, -1))
        assertEquals(2, caretRowInColumn(columns, 0, 2, -1))
    }

    @Test
    fun `行尾提示在游標移開後失效`() {
        val text = "一二三四五六"
        val columns = buildVerticalColumns(text, rows = 3)

        assertEquals(0, tailCaretColumn(columns, 3, 3))
        // 游標被輸入法搬到別的位置。
        assertEquals(-1, tailCaretColumn(columns, 4, 3))
        // 沒有提示時（點的是行首、或剛打開檔案）也畫在下一欄。
        assertEquals(-1, tailCaretColumn(columns, 3, -1))
    }

    @Test
    fun `重新分欄後行尾提示改用新的欄`() {
        val text = "一二三四五六"
        // 鍵盤收起時列數變多，同一個 offset 會落到另一欄的欄尾，甚至不再是欄尾。
        val taller = buildVerticalColumns(text, rows = 4)

        assertEquals(4, taller.ends[0])
        // 3 已經不是任何一欄的欄尾（第一欄到 4、第二欄到 6）。
        assertEquals(-1, tailCaretColumn(taller, 3, 3))
        // 4 變成第一欄的欄尾，提示跟著指到第一欄，而不是原本記下的欄號。
        assertEquals(0, tailCaretColumn(taller, 4, 4))
    }

    @Test
    fun `游標停在代理對中間會被推到字格邊界`() {
        val text = "a😀b"
        // offset 2 落在高位與低位代理之間，是非法的插入點。
        val snapped = snapSelection(TextFieldValue(text, TextRange(2)))

        assertEquals(1, snapped.selection.start)
    }

    @Test
    fun `合法游標位置不會被改動`() {
        val text = "a😀b"
        val value = TextFieldValue(text, TextRange(3))

        assertEquals(3, snapSelection(value).selection.start)
    }

    @Test
    fun `長按只選中按住的那一行`() {
        val text = "一二三四五六七八"
        val columns = buildVerticalColumns(text, rows = 4)

        // 一欄就是一行：選取範圍是欄首到欄尾，不含換行符。
        assertEquals(0 to 4, anchoredLineRange(columns, 0, 0))
        assertEquals(4 to 8, anchoredLineRange(columns, 1, 1))
    }

    @Test
    fun `拖到別的欄會把中間的行一起圈進來`() {
        val text = "一二三四五六七八"
        val columns = buildVerticalColumns(text, rows = 4)

        // 往後拖（左邊）與往前拖（右邊）都應該得到同一個由小到大的範圍。
        assertEquals(0 to 8, anchoredLineRange(columns, 0, 1))
        assertEquals(0 to 8, anchoredLineRange(columns, 1, 0))
    }

    @Test
    fun `空內容沒有可選的行`() {
        val columns = buildVerticalColumns("", rows = 4)

        assertEquals(null, anchoredLineRange(columns, 0, 0))
    }
}

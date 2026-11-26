package com.yuzlyn.mdeditor.data.note

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskLinesTest {

    @Test
    fun `认得出未完成与已完成的待办`() {
        val text = "- [ ] 買牛奶\n- [x] 回信\n- [X] 繳費"
        val tasks = TaskLines.parse(text)
        assertEquals(3, tasks.size)
        assertEquals(listOf(false, true, true), tasks.map { it.checked })
        assertEquals("買牛奶", text.substring(tasks[0].contentStart, tasks[0].lineEnd))
        assertEquals("回信", text.substring(tasks[1].contentStart, tasks[1].lineEnd))
    }

    @Test
    fun `缩排与星号也算待办`() {
        val text = "  * [ ] 子項"
        val task = TaskLines.parse(text).single()
        assertEquals(0, task.lineStart)
        assertEquals(4, task.boxStart)
        assertEquals(7, task.boxEnd)
        assertEquals("子項", text.substring(task.contentStart, task.lineEnd))
    }

    @Test
    fun `不是待办的行不误判`() {
        assertTrue(TaskLines.parse("- [] 缺空格").isEmpty())
        assertTrue(TaskLines.parse("- [ ]没有空格").isEmpty())
        assertTrue(TaskLines.parse("- [y] 不是勾").isEmpty())
        assertTrue(TaskLines.parse("普通文字\n- 項目").isEmpty())
        assertTrue(TaskLines.parse("").isEmpty())
    }

    @Test
    fun `切换只改方框里的一个字元`() {
        val text = "前言\n- [ ] 買牛奶\n後記"
        val task = TaskLines.parse(text).single()
        val checked = TaskLines.toggle(text, task)
        assertEquals("前言\n- [x] 買牛奶\n後記", checked)
        val back = TaskLines.toggle(checked, TaskLines.parse(checked).single())
        assertEquals(text, back)
    }

    @Test
    fun `按位置找得到所在的那一行`() {
        val text = "- [ ] 一\n- [x] 二"
        val second = TaskLines.at(text, text.indexOf("二"))
        assertEquals("二", text.substring(second!!.contentStart, second.lineEnd))
        assertNull(TaskLines.at("普通\n文字", 1))
    }
}

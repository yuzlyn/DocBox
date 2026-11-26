package com.yuzlyn.mdeditor.data.note

/**
 * 一行待辦：Markdown 的 `- [ ] 文字`（未完成）或 `- [x] 文字`（已完成）。
 *
 * 只描述位置，不改字串——編輯器靠這些 offset 把方框畫在文字上、把已完成那行變灰，
 * 真正翻狀態是 [TaskLines.toggle] 換掉方框裡的那一個字元。
 */
data class TaskLine(
    /** 行首（含縮排）在整篇文字裡的 offset。 */
    val lineStart: Int,
    /** `[` 在整篇文字裡的 offset。 */
    val boxStart: Int,
    /** `]` 之後的 offset（不含）。 */
    val boxEnd: Int,
    /** 文字內容的起點：跳過 `- [ ]` 與其後的空格。 */
    val contentStart: Int,
    /** 行尾，不含換行符。 */
    val lineEnd: Int,
    val checked: Boolean
)

/**
 * 待辦清單的解析與翻狀態。
 *
 * 為什麼不另存一份清單：筆記本體就是 `.md`，`- [ ]` 是標準寫法，預覽、匯出、
 * 別的編輯器都認得。勾選狀態只存在文字裡，不引入第二份會走樣的資料。
 */
object TaskLines {

    /** `- [ ]` / `* [x]`，前面可以有縮排；方框後面至少要有一個空白才算待辦。 */
    private val PATTERN = Regex("""^([ \t]*[-*][ \t]+)(\[)([ xX])(\])([ \t]+)""")

    fun parse(text: String): List<TaskLine> {
        if (text.isEmpty()) return emptyList()
        val result = mutableListOf<TaskLine>()
        var lineStart = 0
        while (lineStart <= text.length) {
            val newline = text.indexOf('\n', lineStart)
            val lineEnd = if (newline < 0) text.length else newline
            val match = PATTERN.find(text.substring(lineStart, lineEnd))?.takeIf {
                it.range.first == 0
            }
            if (match != null) {
                val boxStart = lineStart + match.groups[2]!!.range.first
                val boxEnd = lineStart + match.groups[4]!!.range.last + 1
                result += TaskLine(
                    lineStart = lineStart,
                    boxStart = boxStart,
                    boxEnd = boxEnd,
                    contentStart = lineStart + match.range.last + 1,
                    lineEnd = lineEnd,
                    checked = match.groups[3]!!.value != " "
                )
            }
            if (newline < 0) break
            lineStart = newline + 1
        }
        return result
    }

    /**
     * 翻轉 [task] 的勾選狀態，只改方框裡那一個字元。
     *
     * 只動一個字元是刻意的：游標、選取、復原堆疊都只是一個正常的文字編輯步驟，
     * 不會因為「重排整行」把游標甩到別處。
     */
    fun toggle(text: String, task: TaskLine): String {
        val index = task.boxStart + 1
        if (index !in text.indices) return text
        val next = if (task.checked) ' ' else 'x'
        return text.replaceRange(index, index + 1, next.toString())
    }

    /** 這一行是不是待辦；不是就回 null。給「整行切換」之類的入口用。 */
    fun at(text: String, offset: Int): TaskLine? =
        parse(text).firstOrNull { offset in it.lineStart..it.lineEnd }
}

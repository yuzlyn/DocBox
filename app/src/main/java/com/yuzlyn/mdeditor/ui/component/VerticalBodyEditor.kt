package com.yuzlyn.mdeditor.ui.component

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuzlyn.mdeditor.data.note.TaskLine
import com.yuzlyn.mdeditor.data.note.TaskLines
import com.yuzlyn.mdeditor.data.settings.WritingGuide
import com.yuzlyn.mdeditor.ui.i18n.LocalAppStrings
import com.yuzlyn.mdeditor.ui.theme.Space
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * 每一欄的字元範圍，以及「字元索引 → (欄, 列)」的反查表。
 *
 * 直排的換行規則是：一欄由上往下填滿 [rows] 個字就換到左邊下一欄；
 * 原文的 '\n' 也直接換欄，等同段落另起一行。
 *
 * 「字」的單位是 grapheme cluster（見 [graphemeStarts]），不是 UTF-16 字元：
 * emoji 佔兩個 char，若照 char 逐個排，一個 😀 會被拆成上下兩格、變成兩個方塊。
 */
internal class VerticalColumns(
    val starts: IntArray,
    val ends: IntArray,
    val columnOf: IntArray,
    val rowOf: IntArray,
    /** 每一欄要顯示的行號（1 起算）；0 表示這一欄是上一行折過來的續欄，不標號。 */
    val lineOf: IntArray,
    /** 每一欄從第幾個「字」開始、到第幾個結束（格索引，不是 char 索引）。 */
    val cellStarts: IntArray = IntArray(starts.size),
    val cellEnds: IntArray = IntArray(starts.size),
    /** 全篇的「字」邊界（char offset）；最後一個等於 text.length。 */
    val cells: IntArray = intArrayOf(0)
) {
    val count: Int get() = starts.size

    /** 這一欄有幾個字格（＝要畫幾列）。 */
    fun rowsOf(index: Int): Int =
        (cellEnds.getOrElse(index) { 0 } - cellStarts.getOrElse(index) { 0 }).coerceAtLeast(0)

    /**
     * 這一欄帶待辦樣式的文字。
     *
     * 與 [columnText] 同樣是「一字一行」，只是把待辦語法（`- ` 與 `[ ]`）標成透明、
     * 把已勾選那行標成灰字加刪除線。字數與換行都沒變，所以字格與分欄完全不受影響。
     */
    fun columnAnnotated(
        text: String,
        index: Int,
        tasks: List<TaskLine>,
        checkedInk: Color
    ): AnnotatedString {
        val from = cellStarts.getOrElse(index) { 0 }
        val to = cellEnds.getOrElse(index) { 0 }
        if (to <= from || tasks.isEmpty()) return AnnotatedString(columnText(text, index))
        return buildAnnotatedString {
            for (cell in from until to) {
                if (cell > from) append('\n')
                val start = cells.getOrElse(cell) { text.length }.coerceIn(0, text.length)
                val end = cells.getOrElse(cell + 1) { text.length }.coerceIn(0, text.length)
                val at = length
                if (end > start) append(text, start, end)
                val task = tasks.firstOrNull { start >= it.lineStart && start <= it.lineEnd }
                if (task != null) {
                    when {
                        start < task.boxEnd -> addStyle(
                            SpanStyle(color = Color.Transparent),
                            at,
                            length
                        )
                        task.checked -> addStyle(
                            SpanStyle(
                                color = checkedInk,
                                textDecoration = TextDecoration.LineThrough
                            ),
                            at,
                            length
                        )
                    }
                }
            }
        }
    }

    /** 這一欄裡有方框就回（列、那一行待辦、是否已勾選），沒有回 null。 */
    fun markerIn(index: Int, tasks: List<TaskLine>): Triple<Int, TaskLine, Boolean>? {
        if (tasks.isEmpty()) return null
        val from = cellStarts.getOrElse(index) { 0 }
        val to = cellEnds.getOrElse(index) { 0 }
        if (to <= from) return null
        for (task in tasks) {
            val cell = cells.indexOf(task.boxStart)
            if (cell in from until to) return Triple(cell - from, task, task.checked)
        }
        return null
    }

    /** 把第 [index] 欄的內容組成「每字一行」的字串；一個 emoji 就是一格。 */
    fun columnText(text: String, index: Int): String {
        val from = cellStarts.getOrElse(index) { 0 }
        val to = cellEnds.getOrElse(index) { 0 }
        if (to <= from) return ""
        return buildString {
            for (cell in from until to) {
                if (cell > from) append('\n')
                val cellStart = cells.getOrElse(cell) { text.length }.coerceIn(0, text.length)
                val cellEnd = cells.getOrElse(cell + 1) { text.length }.coerceIn(0, text.length)
                if (cellEnd > cellStart) append(text, cellStart, cellEnd)
            }
        }
    }
}

/**
 * 直排字距（字送り），以字級為單位。
 * 1.2em 只留一點呼吸感；再大就會鬆散、像逐字拆開。
 */
internal const val CHAR_ADVANCE_EM = 1.2f

/**
 * 直排列距（行送り），以字級為單位。
 * 一定要明顯大於字距，欄才不會黏在一起；1.72em 約等於 0.7 個字的欄間。
 */
internal const val COLUMN_PITCH_EM = 1.72f

/** 行首禁則：不能出現在一欄開頭的字元（收尾標點、後引號、後括號等）。 */
private const val LINE_START_FORBIDDEN =
    "。，、．,；：？！!?）］｝〕〉》」』】〙〗〛ーヽヾゝゞ々～…‥・%‰℃"

/** 行末禁則：不能留在欄尾的字元（前引號、前括號等）。 */
private const val LINE_END_FORBIDDEN = "（［｛〔〈《「『【〘〖〚([{"

/** 在 [index] 前面斷行是否可以；用來做避頭尾，不讓標點落在欄首。 */
internal fun canBreakBefore(text: String, index: Int): Boolean {
    if (index <= 0 || index >= text.length) return true
    if (text[index] in LINE_START_FORBIDDEN) return false
    if (text[index - 1] in LINE_END_FORBIDDEN) return false
    return true
}

/**
 * 把文字切成「字」的邊界（char offset），最後一個元素固定是 text.length。
 *
 * 用 [java.text.BreakIterator] 的字元模式：它認得代理對（emoji）、組合字、
 * 膚色修飾與 ZWJ 串接，切出來的每一格都能獨立畫成一個字。
 * 沒有這一步，emoji 在直排裡會被拆成兩個半個代理，顯示成兩個方塊。
 */
internal fun graphemeStarts(text: String): IntArray {
    if (text.isEmpty()) return intArrayOf(0)
    val breaker = java.text.BreakIterator.getCharacterInstance()
    breaker.setText(text)
    val bounds = ArrayList<Int>(text.length)
    var index = breaker.first()
    while (index != java.text.BreakIterator.DONE) {
        if (index >= 0 && index <= text.length) bounds.add(index)
        index = breaker.next()
    }
    if (bounds.isEmpty() || bounds[0] != 0) bounds.add(0, 0)
    if (bounds.last() != text.length) bounds.add(text.length)
    return bounds.toIntArray()
}

/**
 * 量一次真正的行距（兩行的基線差），也就是字格真正的高度。
 *
 * Compose 的 `lineHeight` 只是「建議值」：字型的 ascent/descent 比它大時，每一行照字型算，
 * 行框就會比字格高一點。格線若照要求值畫，偏差會隨行數累積——直排看起來就是
 * 游標越往下越貼字、最後壓在字身上。用同一套 style 量兩行取基線差，
 * 畫的跟量的才會永遠一致。
 */
internal fun measuredLineAdvance(measurer: TextMeasurer, style: TextStyle): Float? {
    val layout = measurer.measure("字\n字", style = style, softWrap = false, maxLines = 2)
    if (layout.lineCount < 2) return null
    val advance = layout.getLineBaseline(1) - layout.getLineBaseline(0)
    return advance.takeIf { it.isFinite() && it > 0f }
}

/**
 * 量出「一行字裡基線在第幾 px」。
 *
 * 直排的格子要對齊字，靠「行高的一半」去猜是猜不準的：字的基線由字型的 ascent 決定，
 * 中文/日文字型的上伸往往比下伸大很多（字會偏下），不同字型還不一樣。
 * 直接量一行的基線，格子就知道該落在哪裡。
 */
internal fun measuredBaseline(measurer: TextMeasurer, style: TextStyle): Float? {
    val layout = measurer.measure("字", style = style, softWrap = false, maxLines = 1)
    if (layout.lineCount < 1) return null
    return layout.getLineBaseline(0).takeIf { it > 0f }
}

/**
 * 點擊落在哪：文字位移，以及這次點擊是不是指在「行尾」。
 *
 * 直排的插入點只有一套：某一欄欄尾與下一欄欄首是同一個 offset，
 * 但畫面上有兩個可以擺游標的位置。只有點擊的當下知道使用者指的是哪一個，
 * 所以 [atTail] 要跟著 offset 一起回傳，由呼叫端決定游標畫在哪一側。
 */
internal data class TapCaret(val offset: Int, val atTail: Boolean)

/** 點擊當下指在行尾的那個 offset；-1 表示沒有這個提示。 */
private const val NO_TAIL_CARET = -1

/** 重排前游標所在欄的螢幕位置；重排後用它把畫面滑回原處。 */
private data class ReflowAnchor(
    /** 游標那一欄在視窗內的起始 x；那一欄不在畫面上時是 null。 */
    val caretScreenX: Int?,
    /** 重排前的列數；列數沒變就不必還原。 */
    val rows: Int
)

/**
 * 把 [index] 這一欄放到「距離視窗起始邊 [screenX]」的位置。
 *
 * `scrollToItem` 的 scrollOffset 是「往版面起點推」的量，跟 `visibleItemsInfo.offset` 反向；
 * 反向版面（reverseLayout）下這個公約很容易搞反，所以放完之後量一次實際位置再補正，
 * 公約相反時改用反號再放一次——寧可多量一次，也不要畫面跳到別處。
 */
private suspend fun alignColumn(
    listState: LazyListState,
    index: Int,
    screenX: Int?,
    columnWidthPx: Float
) {
    if (screenX == null) {
        listState.animateScrollToItem(index)
        return
    }
    listState.scrollToItem(index, -screenX)
    var now = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }?.offset
    if (now == null) {
        listState.scrollToItem(index, screenX)
        now = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }?.offset
    }
    when {
        now == null -> listState.scrollToItem(index, 0)
        abs(now - screenX) > columnWidthPx -> listState.scrollBy((now - screenX).toFloat())
    }
}

/**
 * 點在某一欄的 [y]（相對於文字頂端）→ 游標應該落在哪個 char offset、是不是指在行尾。
 *
 * 這裡有三個都不能省的細節：
 * 1. **用字格換算，不是用 char**。舊版是 `start + row`，在含 emoji 的欄裡
 *    row 是列號、卻被當成字元位移，游標會正好落進代理對中間；
 *    接下來打一個字就把那個 emoji 拆成兩個半字，畫面上就是兩個問號。
 * 2. **點在字格下半部要放到該字之後**。否則點最後一個字時游標只會跑到它前面，
 *    看起來就是「點不到最後一個字，只選到前一個」。
 * 3. **落點要夾在這一欄的字格範圍內**。點在文字下方的空白時 row 會遠大於這一欄的
 *    字數，若只夾住字格卻沒夾住 +1，游標會衝過欄尾、跑到下一欄中間去。
 */
internal fun caretForTap(
    columns: VerticalColumns,
    column: Int,
    y: Float,
    cellHeight: Float,
    textLength: Int
): TapCaret {
    if (columns.count == 0) return TapCaret(0, false)
    val index = column.coerceIn(0, columns.count - 1)
    val safeHeight = if (cellHeight > 0f) cellHeight else 1f
    val row = floor(y / safeHeight).toInt().coerceAtLeast(0)
    val within = y - row * safeHeight
    val startCell = columns.cellStarts.getOrElse(index) { 0 }
    val endCell = columns.cellEnds.getOrElse(index) { startCell }
    val cell = (startCell + row).coerceIn(startCell, endCell)
    val pastHalf = within > safeHeight / 2f
    val target = (cell + if (pastHalf) 1 else 0).coerceIn(startCell, endCell)
    val offset = columns.cells.getOrElse(target) { textLength }.coerceIn(0, textLength)
    // 落點在欄尾＝指在行尾（含「點在最後一個字的下半部」與「點在整欄文字下方」）。
    val atTail = endCell > startCell && target >= endCell
    return TapCaret(offset, atTail)
}

internal fun caretOffsetForTap(
    columns: VerticalColumns,
    column: Int,
    y: Float,
    cellHeight: Float,
    textLength: Int
): Int = caretForTap(columns, column, y, cellHeight, textLength).offset

/**
 * 游標這一帧要畫在 [index] 這一欄的第幾列；-1 表示這一欄不畫游標。
 *
 * 直排的一個插入點有兩個落點：第 j 欄的欄尾（第 rowsOf(j) 列）與第 j+1 欄的欄首（第 0 列）。
 * 平常一律畫在下一欄的欄首，只有使用者真的點在行尾時（[tailColumn] == j）才畫在欄尾。
 * 換行符掛在欄尾、不屬於下一欄，所以在沒有行尾提示時也要畫在欄尾——否則游標會整個消失。
 */
internal fun caretRowInColumn(
    columns: VerticalColumns,
    index: Int,
    caretIndex: Int,
    tailColumn: Int
): Int {
    if (index !in 0 until columns.count) return -1
    val columnRows = columns.rowsOf(index)
    if (index == tailColumn) return columnRows
    if (tailColumn >= 0) return -1
    val start = columns.starts[index]
    val end = columns.ends[index]
    val sharedWithNext =
        index + 1 < columns.count && columns.starts[index + 1] == caretIndex
    val here = caretIndex in start until end || (caretIndex == end && !sharedWithNext)
    if (!here) return -1
    return columns.rowOf.getOrElse(caretIndex) { 0 }.coerceIn(0, columnRows)
}

/**
 * 「行尾提示」還算不算數，算的話是哪一欄。
 *
 * 提示只是點擊當下的意圖，所以只記住那個 offset：游標一被輸入法搬走就過期。
 * 欄號刻意不記——鍵盤進出會重新分欄，舊欄號可能指到別的地方，
 * 每次都用 offset 現查「哪一欄的結尾正好是它」才不會畫錯。
 */
internal fun tailCaretColumn(
    columns: VerticalColumns,
    caretIndex: Int,
    tailOffset: Int
): Int {
    if (tailOffset < 0 || tailOffset != caretIndex) return -1
    for (index in 0 until columns.count) {
        if (columns.cellEnds[index] > columns.cellStarts[index] &&
            columns.ends[index] == caretIndex
        ) {
            return index
        }
    }
    return -1
}

internal fun buildVerticalColumns(
    text: String,
    rows: Int,
    // 字格邊界可以外部算好後傳進來：鍵盤升降只換列數、文字沒變，
    // 重跑一次 BreakIterator 是純浪費（那是整個分欄裡最貴的一步）。
    cells: IntArray = graphemeStarts(text)
): VerticalColumns {
    val length = text.length

    if (length == 0) {
        return VerticalColumns(
            starts = intArrayOf(0),
            ends = intArrayOf(0),
            columnOf = IntArray(1),
            rowOf = IntArray(1),
            lineOf = intArrayOf(1),
            cellStarts = intArrayOf(0),
            cellEnds = intArrayOf(0),
            cells = intArrayOf(0)
        )
    }

    val cellCount = cells.size - 1
    val capacity = cellCount / rows.coerceAtLeast(1) + 2
    val starts = ArrayList<Int>(capacity)
    val ends = ArrayList<Int>(capacity)
    val lineOf = ArrayList<Int>(capacity)
    val cellStarts = ArrayList<Int>(capacity)
    val cellEnds = ArrayList<Int>(capacity)

    val columnOf = IntArray(length + 1)
    val rowOf = IntArray(length + 1)

    var line = 1
    var startCell = 0
    var continuation = false
    var lastStartCell = 0
    var lastEndCell = 0

    while (true) {
        val startChar = cells[startCell]
        starts.add(startChar)
        cellStarts.add(startCell)
        // 只有真的換行（'\n'）才進一行；一欄填滿自動折到下一欄時仍算同一行，續欄不標號。
        lineOf.add(if (continuation) 0 else line)

        var probe = startCell
        var count = 0
        var newlineCell = -1
        while (probe < cellCount) {
            if (text[cells[probe]] == '\n') {
                newlineCell = probe
                break
            }
            if (count == rows) break
            probe++
            count++
        }

        val endedByNewline = newlineCell >= 0
        var endCell: Int
        if (endedByNewline) {
            endCell = newlineCell
        } else {
            endCell = probe
            if (endCell < cellCount) {
                // 欄滿必須軟斷：往回推到「可以斷」的位置，讓標點不落在下一欄開頭、
                // 前括號不會被留在欄尾。至少保留一個字，免得推出空欄。
                var candidate = endCell
                while (candidate > startCell + 1 && !canBreakBefore(text, cells[candidate])) candidate--
                endCell = candidate
            }
        }
        val endChar = cells[endCell]
        ends.add(endChar)
        cellEnds.add(endCell)

        val column = starts.lastIndex
        for (cell in startCell until endCell) {
            val from = cells[cell]
            val to = cells[cell + 1]
            val row = cell - startCell
            for (position in from until to) {
                columnOf[position] = column
                rowOf[position] = row
            }
        }
        // 換行符本身掛在這一欄結尾，游標停在換行後時才畫得出來。
        if (endCell < cellCount && text[cells[endCell]] == '\n') {
            val from = cells[endCell]
            val to = cells[endCell + 1]
            for (position in from until to) {
                columnOf[position] = column
                rowOf[position] = endCell - startCell
            }
        }

        lastStartCell = startCell
        lastEndCell = endCell

        val nextCell = if (endedByNewline) newlineCell + 1 else endCell
        if (nextCell >= cellCount) break
        if (endedByNewline) line++
        continuation = !endedByNewline
        startCell = nextCell
    }

    columnOf[length] = starts.lastIndex
    rowOf[length] = (lastEndCell - lastStartCell).coerceAtLeast(0)

    return VerticalColumns(
        starts = starts.toIntArray(),
        ends = ends.toIntArray(),
        columnOf = columnOf,
        rowOf = rowOf,
        lineOf = lineOf.toIntArray(),
        cellStarts = cellStarts.toIntArray(),
        cellEnds = cellEnds.toIntArray(),
        cells = cells
    )
}

/**
 * 直排（縦書き）內文編輯區。
 *
 * Compose 沒有直排的輸入欄位，所以這裡自己把文字切成一欄一欄，
 * 每欄用「一個字一行」的 [Text] 疊起來（字身保持正立），再以反轉的
 * [LazyRow] 由右往左排列；輸入則靠一個透明的最小 [BasicTextField] 收鍵盤事件。
 *
 * 版面用「字送り／行送り」兩個比例控制（以字級為單位）：
 * - [CHAR_ADVANCE_EM]：同一欄裡字與字的間距。太大會鬆散、像逐字散開。
 * - [COLUMN_PITCH_EM]：欄與欄的間距。必須比字距寬，否則欄會擠成一團。
 * 舊版是字距 1.5em、列距 1.3em——正好把兩者寫反，所以讀起來「字太散、行太擠」。
 */
/**
 * 長按選取：以「整行（整欄）」為單位，回傳 [firstColumn] 到 [secondColumn] 的 char 範圍。
 *
 * 直排的一欄就是一行，長按選一行最接近使用者的直覺：複製出來的是一整句話，
 * 不會斷在半個詞上。往後拖（左邊）或往前拖（右邊）都只是把比較遠的那一欄算進來。
 * 空欄（沒有任何字格）回傳 null，呼叫端就當成沒選到東西。
 */
internal fun anchoredLineRange(
    columns: VerticalColumns,
    firstColumn: Int,
    secondColumn: Int
): Pair<Int, Int>? {
    if (columns.count == 0) return null
    val lo = min(firstColumn, secondColumn).coerceIn(0, columns.count - 1)
    val hi = max(firstColumn, secondColumn).coerceIn(0, columns.count - 1)
    val fromCell = columns.cellStarts[lo]
    val toCell = columns.cellEnds[hi]
    if (toCell <= fromCell) return null
    val from = columns.cells.getOrElse(fromCell) { 0 }
    val to = columns.cells.getOrElse(toCell) { from }
    if (to <= from) return null
    return from to to
}

/**
 * 吞掉系統選取工具列的替身。
 *
 * 真正接收輸入的 [BasicTextField] 只有 1dp、完全透明，它的選取工具列卻會照著
 * 1dp 的框定位——不論選到哪一行都擠在畫面角落，和我們自己的工具列打架。
 * 直排的選取由 [VerticalBodyEditor] 自己處理，這裡讓它閉嘴就好。
 */
private object SilentTextToolbar : TextToolbar {
    override val status: TextToolbarStatus get() = TextToolbarStatus.Hidden
    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) = Unit

    override fun hide() = Unit
}

/**
 * 游標停在代理對中間時，往外推到最近的合法邊界。
 *
 * 組字（IME composition）期間不動：那時的字串本來就可能是半成品，
 * 插手只會讓輸入法把已經組好的字退回。
 */
internal fun snapSelection(value: TextFieldValue): TextFieldValue {
    if (value.composition != null) return value
    val text = value.text
    val start = snapOffset(text, value.selection.start)
    val end = snapOffset(text, value.selection.end)
    return if (start == value.selection.start && end == value.selection.end) {
        value
    } else {
        value.copy(selection = TextRange(start, end))
    }
}

private fun snapOffset(text: String, offset: Int): Int {
    var index = offset.coerceIn(0, text.length)
    // 游標落在高位代理與低位代理之間：兩邊都不合法，退到這個字格的前面。
    while (index > 0 && index < text.length &&
        Character.isLowSurrogate(text[index]) && Character.isHighSurrogate(text[index - 1])
    ) {
        index--
    }
    return index
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VerticalBodyEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    textStyle: TextStyle,
    focusRequester: FocusRequester,
    bottomReserve: Dp,
    modifier: Modifier = Modifier,
    showLineNumbers: Boolean = true,
    /** 書寫輔助線；直排的分隔線是豎線、格子按 1em 字身對齊。 */
    writingGuide: WritingGuide = WritingGuide.NONE,
    placeholder: String = "",
    accentColor: Color = MaterialTheme.colorScheme.primary,
    variantColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val strings = LocalAppStrings.current
    val clipboard = LocalClipboardManager.current
    // 複製成功後工具列上的字樣短暫換成「已複製」，一秒多之後換回來。
    var copiedHint by remember { mutableStateOf(false) }
    LaunchedEffect(copiedHint) {
        if (copiedHint) {
            delay(1_200)
            copiedHint = false
        }
    }
    val density = LocalDensity.current
    val fontSize = if (textStyle.fontSize.isSp) textStyle.fontSize else 16.sp
    val fontSizePx = with(density) { fontSize.toPx() }
    val cellWidthPx = fontSizePx * COLUMN_PITCH_EM
    // 這是「要求」的字格高度：lineHeight 只是給 Compose 的建議值，真正的行距要看字型。
    val requestedCellHeightPx = fontSizePx * CHAR_ADVANCE_EM
    val cellWidth = with(density) { cellWidthPx.toDp() }
    val lineHeight = with(density) { requestedCellHeightPx.toSp() }
    val keyboardController = LocalSoftwareKeyboardController.current
    // 焦點一直留在隱藏的輸入欄位上，收起鍵盤後再 requestFocus() 不會重新叫出 IME，
    // 所以每次點擊都遞增這個計數，由 LaunchedEffect 重新聚焦並明確把鍵盤叫出來。
    var focusTick by remember { mutableIntStateOf(0) }
    val caretColor = accentColor
    val selectionColor = accentColor.copy(alpha = 0.22f)
    val textColor = if (textStyle.color != Color.Unspecified) textStyle.color
    else MaterialTheme.colorScheme.onSurface

    val measurer = rememberTextMeasurer()
    val columnStyle = textStyle.copy(
        color = textColor,
        textAlign = TextAlign.Center,
        lineHeight = lineHeight,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        ),
        // 字型自帶的上下留白不關掉的話，行框會比 lineHeight 高，
        // 每一行都比字格多一點，累積起來游標就壓到字身上。
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        // 直排的標點要換成豎排字形：直角引號「」、括號、書名號在橫排是左右開口的，
        // 疊成直書後若不轉九十度，開口會朝著上下而不是引用的文字，看起來就是「躺錯方向」。
        // `vert`／`vrt2` 是 OpenType 的豎排替換，Noto／思源等 CJK 字型都帶；
        // 自訂字型沒帶也不會壞，只是維持原本的字形。
        fontFeatureSettings = "'vert' 1, 'vrt2' 1"
    )

    // 格線、游標、點擊換算全部用「量出來的行距」：只要有一邊用要求值，兩者就會越差越遠。
    // 不用 remember：字型是資產檔時是非同步載入的，載入完成後要重新量一次才不會沿用替代字型。
    // 樣本字串固定，TextMeasurer 自己有快取，重算只是一次查表。
    val cellHeightPx = measuredLineAdvance(measurer, columnStyle) ?: requestedCellHeightPx
    // 格子要用「真正的基線」定位；量不到才退回 0.88em 的通用值。
    val baselinePx = measuredBaseline(measurer, columnStyle) ?: (fontSizePx * 0.88f)
    val placeholderStyle = columnStyle.copy(
        color = variantColor.copy(alpha = 0.6f)
    )
    val numberStyle = MaterialTheme.typography.labelSmall.copy(
        color = variantColor.copy(alpha = 0.55f),
        fontSize = 9.sp,
        lineHeight = 11.sp
    )

    BoxWithConstraints(modifier = modifier) {
        // 直排的斷行取決於「每一欄放得下幾個字」，所以列數絕對不能跟著鍵盤變：
        // 一變就是整篇重新分欄，游標明明還停在同一個字，畫面卻跳到別的欄、別的列，
        // 看起來就是「點在一處、鍵盤一升一降游標跑去三個地方」。
        // 直排的斷行取決於「每一欄放得下幾個字」，所以鍵盤一升起、視窗變矮，
        // 整篇就得重新分欄，這是豎排的性質（相當於橫排時把視窗拉窄）。
        // 重排不可怕，可怕的是重排後畫面沒有跟著游標走——所以改列數之前先記下
        // 游標那一欄在螢幕上的位置，重排後再滑回去，眼睛才跟得上。
        var settledHeight by remember { mutableStateOf(maxHeight) }
        val gutter = 14.dp
        // 文字區離欄頂的距離＝原本的 LazyRow 上內距 + 行號帶。
        // 欄現在撐滿整個編輯區高（分隔線才鋪得到頂與底），這兩段要由欄自己留。
        val textTop = Space.s100 + gutter
        val textTopPx = with(density) { textTop.toPx() }
        val rows = remember(settledHeight, bottomReserve, cellHeightPx) {
            // 扣掉 LazyRow 的 contentPadding 與欄頂的行號帶，列數才不會超出可視高度。
            val available = with(density) {
                (settledHeight - bottomReserve - Space.s100 - gutter).toPx()
            }
            max(1, floor(available / cellHeightPx).toInt())
        }
        val text = value.text
        // 字格邊界只跟文字有關，跟列數無關：記住它，鍵盤升降重排時就不必再跑一次
        // BreakIterator（emoji/組合字要逐字查 ICU，是分欄裡最貴的一步）。
        val graphemes = remember(text) { graphemeStarts(text) }
        // 待辦清單：狀態只存在文字裡，這裡只推導位置與樣式。
        val tasks = remember(text) { TaskLines.parse(text) }
        val checkedInk = textColor.copy(alpha = 0.55f)
        val columns = remember(text, rows, graphemes) {
            buildVerticalColumns(text, rows, graphemes)
        }
        val listState = rememberLazyListState()
        // 格子的節拍（行距、基線）由「真的渲染出文字的那一欄」量出來之後**全欄共用**。
        // 原本每欄自己量、空白欄用全域量測值，兩套數字一有出入，
        // 有字的欄和沒字的欄就會畫出兩種節奏——往下幾十列就明顯錯開。
        var guideAdvancePx by remember { mutableFloatStateOf(0f) }
        var guideBaselinePx by remember { mutableFloatStateOf(0f) }
        // 字型或字級換了就重新量：沿用舊節拍，格子會跟新的字完全對不上。
        LaunchedEffect(cellHeightPx, baselinePx) {
            guideAdvancePx = 0f
            guideBaselinePx = 0f
        }
        val caretIndex = value.selection.start.coerceIn(0, text.length)
        // 手勢處理器存活在 pointerInput 裡，key 沒變就不會重建；用這兩個轉發
        // 讓它每次都讀到最新的內容與分欄，否則「同長度的改字」之後點擊會用舊字串回寫。
        val latestValue = rememberUpdatedState(value)
        val latestColumns = rememberUpdatedState(columns)

        // 游標要畫在「行尾」還是「行首」。同一個插入點在直排裡有兩個落點
        // （上一欄的欄尾＝下一欄的欄首），只有點擊的那一刻知道使用者指的是哪一側。
        // 這裡只在 offset 仍對得上那一欄結尾時才成立：打字、刪字、重新分欄之後
        // 提示自動過期，游標不會卡在舊位置。
        var tailCaret by remember { mutableIntStateOf(NO_TAIL_CARET) }
        val tailColumn = tailCaretColumn(columns, caretIndex, tailCaret)

        // 重排前先記下「游標那一欄在哪個螢幕位置」，重排後才滑得回同一個位置。
        var reflowAnchor by remember { mutableStateOf<ReflowAnchor?>(null) }
        fun captureReflowAnchor() {
            val caretColumn = if (tailColumn >= 0) {
                tailColumn
            } else {
                columns.columnOf.getOrElse(caretIndex.coerceIn(0, columns.columnOf.lastIndex)) { 0 }
            }
            reflowAnchor = ReflowAnchor(
                caretScreenX = listState.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == caretColumn }?.offset,
                rows = rows
            )
        }

        // 重排後短暫出現的「光標柄」：淡入 → 停一下 → 淡出。
        // 只有重排（鍵盤升降、旋轉）才觸發，打字時不跳出來干擾。
        val motion = MaterialTheme.motionScheme
        var hintTick by remember { mutableIntStateOf(0) }
        val hintAlpha = remember { Animatable(0f) }
        LaunchedEffect(hintTick) {
            if (hintTick == 0) return@LaunchedEffect
            hintAlpha.snapTo(0f)
            hintAlpha.animateTo(
                targetValue = 1f,
                animationSpec = motion.fastEffectsSpec()
            )
            delay(1100)
            hintAlpha.animateTo(
                targetValue = 0f,
                animationSpec = motion.fastEffectsSpec()
            )
        }

        // 鍵盤升降時「排版高度」怎麼切換——決定了重排看起來是自然還是突然。
        //
        // 高度在動畫期間是一帧一帧變的。若等它停下來再重排，使用者會先看到文字被鍵盤
        // 切掉、停住，過一會兒才「啪」地跳一次，那就是延遲感。所以兩端的高度都記下來，
        // 動畫一開始就直接切到對應那一端。
        //
        // 判定只看「鍵盤可不可見」和這兩個記下來的常數，**絕不看動畫中的高度**：
        // 拿 maxHeight 跟排版高度比大小會在升起動畫中途來回誤判（排版已經切到矮的，
        // 而動畫中的高度還比它高 → 被當成在收起 → 又切回高的），一帧之内反覆切換，
        // 畫面就會一長一短地閃。
        val imeVisible = WindowInsets.isImeVisible
        var expandedHeight by remember { mutableStateOf(maxHeight) }
        var collapsedHeight by remember { mutableStateOf(maxHeight) }
        LaunchedEffect(maxHeight, imeVisible, expandedHeight, collapsedHeight) {
            val target = when {
                imeVisible && collapsedHeight < expandedHeight -> collapsedHeight
                !imeVisible && expandedHeight > collapsedHeight -> expandedHeight
                // 兩端還沒量到（第一次升降）：等動畫停下來再切。
                else -> {
                    delay(40)
                    maxHeight
                }
            }
            if (target != settledHeight) {
                captureReflowAnchor()
                settledHeight = target
                hintTick++
            }
        }
        // 動畫期間不去記，停下來 2 帧之後才把兩端的高度更新成實際值。
        LaunchedEffect(maxHeight, imeVisible) {
            delay(40)
            if (imeVisible) collapsedHeight = maxHeight else expandedHeight = maxHeight
        }

        var caretVisible by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(520)
                caretVisible = !caretVisible
            }
        }
        LaunchedEffect(value.selection) { caretVisible = true }

        // 每次點擊直排內容：重新聚焦隱藏輸入框，並明確要求顯示鍵盤。
        LaunchedEffect(focusTick) {
            if (focusTick == 0) return@LaunchedEffect
            focusRequester.requestFocus()
            // 等焦點真的落到輸入框後再叫鍵盤，否則系統會忽略這次請求。
            delay(60)
            keyboardController?.show()
        }

        // 重排後先把游標那一欄滑回原來的螢幕位置，再看它有沒有跑出可視範圍。
        LaunchedEffect(caretIndex, columns, tailColumn) {
            val anchor = reflowAnchor
            if (anchor != null) {
                reflowAnchor = null
                if (anchor.rows != rows) {
                    val target = if (tailColumn >= 0) tailColumn else columns.columnOf[caretIndex]
                    alignColumn(listState, target, anchor.caretScreenX, cellWidthPx)
                    return@LaunchedEffect
                }
            }
            // 使用者正在拖動時不插手，免得手指還在滑、程式又把畫面拉回去。
            if (listState.isScrollInProgress) return@LaunchedEffect
            // 行尾提示生效時，游標畫在上一欄的欄尾，而不是 offset 所屬的下一欄；
            // 這時要滑進來的是上一欄，否則畫面會為了一個看不見的游標往左跳一欄。
            val target = if (tailColumn >= 0) tailColumn else columns.columnOf[caretIndex]
            val visible = listState.layoutInfo.visibleItemsInfo
            if (visible.isNotEmpty() && visible.none { it.index == target }) {
                listState.animateScrollToItem(target)
            }
        }

        // 直排從**右邊**起排：內容不滿一屏時貼著右緣，只留一個正常邊距，
        // 多出來的空白全部留在左邊——那才是「還沒寫到」的方向。
        // （置中會讓第一欄浮在畫面中間、右邊空一大片，看起來就像排版壞了。）
        val baseWidth = cellWidth * columns.count + Space.s200 * 2
        val fillsScreen = baseWidth > maxWidth
        // 最後一欄要能滑到畫面正中央，而不只是貼在左邊界：
        // 末端多留「半個視口減半個字欄」，滑到底時最後一欄正好落在中線上。
        // 這只在內容已經長到需要捲動時才有意義；短內容不必為它留白。
        val tailSpace = if (fillsScreen) {
            ((maxWidth - cellWidth) / 2).coerceAtLeast(Space.s200)
        } else if (writingGuide != WritingGuide.NONE) {
            // 內容不滿一屏：整張紙都要有線，字從右邊起排，左邊用空欄鋪到畫面左緣。
            // 分隔線與作文格走同一條規則——只鋪文字區的話，內容短時左半邊就是一片空白。
            (maxWidth - Space.s200 - cellWidth * columns.count).coerceAtLeast(Space.s200)
        } else {
            Space.s200
        }
        val rowWidth = (cellWidth * columns.count + Space.s200 + tailSpace)
            .coerceAtMost(maxWidth)
        // 左邊那段留白也鋪成線：用「空欄」把線補到畫面左緣。
        // 分隔線與作文格都要填，否則內容短的時候左半邊會是一整片空白，
        // 看起來就是「紙只鋪了一半」。
        val fillerCount = if (writingGuide != WritingGuide.NONE) {
            floor(with(density) { tailSpace.toPx() } / cellWidthPx).toInt().coerceAtLeast(0)
        } else {
            0
        }
        val startPadding = tailSpace -
            with(density) { (cellWidthPx * fillerCount).toDp() }

        Box(
            modifier = Modifier.fillMaxSize(),
            // 貼右緣而不是置中：直排的第一欄就該在右邊。
            contentAlignment = Alignment.CenterEnd
        ) {
            LazyRow(
                state = listState,
                reverseLayout = true,
                // reverseLayout 下 Compose 會把「end」當成捲動起點那一側（畫面右邊）、
                // 「start」當成末端（畫面左邊）。直排的「最後面」在左邊，
                // 所以那段留白一定要放在 start，放 end 會變成第一欄前面多出一片空白。
                // 上下不留 padding：欄要撐到整個編輯區高，分隔線才能從頂畫到底；
                // 原本那兩段內距改由欄內自己留（見下面的內層 Box）。
                contentPadding = PaddingValues(start = startPadding, end = Space.s200),
                modifier = Modifier
                    .width(rowWidth)
                    .fillMaxHeight()
            ) {
                items(count = columns.count) { index ->
                    val start = columns.starts[index]
                    val end = columns.ends[index]
                    val columnRows = columns.rowsOf(index)
                    // 這一欄畫游標、換算點擊，用的都是同一個節拍。
                    val rowHeightPx = if (guideAdvancePx > 0f) guideAdvancePx else cellHeightPx
                    val rowBaselinePx = if (guideBaselinePx > 0f) guideBaselinePx else baselinePx
                    // 欄尾與下一欄欄首是同一個 offset：預設畫在下一欄的欄首，
                    // 只有真的點在這一欄的行尾時，游標才沿著這一欄畫在欄尾。
                    val caretRow = caretRowInColumn(columns, index, caretIndex, tailColumn)
                    val selection = value.selection
                    val fromRow = if (selection.min <= start) {
                        0
                    } else {
                        columns.rowOf.getOrElse(selection.min) { 0 }.coerceIn(0, columnRows)
                    }
                    val toRow = if (selection.max >= end) {
                        columnRows
                    } else {
                        columns.rowOf.getOrElse(selection.max) { 0 }.coerceIn(0, columnRows)
                    }
                    val selectedRows = if (selection.max > selection.min && toRow > fromRow) {
                        fromRow until toRow
                    } else {
                        null
                    }

                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .fillMaxHeight()
                            .pointerInput(index, start, end, text.length, textTopPx, rowHeightPx) {
                                // 不用 detectTapGestures：它在 down 就消費事件，會擋住 LazyRow 的拖曳捲動。
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val timeout = viewConfiguration.longPressTimeoutMillis
                                    // 先看這段時間內會不會放開：放開＝點擊，被拖曳取消＝交還 LazyRow。
                                    val released = withTimeoutOrNull(timeout) {
                                        waitForUpOrCancellation()
                                    }
                                    if (released != null) {
                                        val current = latestValue.value
                                        val tap = caretForTap(
                                            columns = latestColumns.value,
                                            column = index,
                                            y = down.position.y - textTopPx,
                                            cellHeight = rowHeightPx,
                                            textLength = current.text.length
                                        )
                                        // 記住「這次是點在行尾」：同一欄的欄尾與下一欄的欄首是
                                        // 同一個插入點，沒有這個提示就永遠只能看到行首的游標。
                                        // 點在方框（`[ ]` 那三格）上就是勾選／取消，不是放游標。
                                        val hit = TaskLines.parse(current.text)
                                            .firstOrNull { tap.offset in it.boxStart..it.boxEnd }
                                        if (hit != null) {
                                            val next = TaskLines.toggle(current.text, hit)
                                            onValueChange(
                                                current.copy(
                                                    text = next,
                                                    selection = TextRange(
                                                        current.selection.start
                                                            .coerceIn(0, next.length)
                                                    ),
                                                    composition = null
                                                )
                                            )
                                            focusTick++
                                            return@awaitEachGesture
                                        }
                                        tailCaret = if (tap.atTail) tap.offset else NO_TAIL_CARET
                                        onValueChange(
                                            current.copy(
                                                selection = TextRange(tap.offset),
                                                composition = null
                                            )
                                        )
                                        focusTick++
                                        // 點一下就冒出一次手柄，跟橫排一致（原本只有重排時才彈）。
                                        hintTick++
                                        return@awaitEachGesture
                                    }
                                    // 沒有放開、而且時間真的過去了才算是長按。
                                    // 不到門檻就結束＝LazyRow 已經接手這次拖曳，這裡不能插手——
                                    // 否則每一次滑動都會順手選一行、還把捲動卡住。
                                    if (SystemClock.uptimeMillis() - down.uptimeMillis < timeout) {
                                        return@awaitEachGesture
                                    }

                                    // 長按：先選這一整行（欄首到欄尾，不含換行符），
                                    // 再跟著手指跨欄延伸；拖到哪一欄就把那一行一起圈進來。
                                    val range = anchoredLineRange(latestColumns.value, index, index)
                                        ?: return@awaitEachGesture
                                    tailCaret = NO_TAIL_CARET
                                    onValueChange(
                                        latestValue.value.copy(
                                            selection = TextRange(range.first, range.second),
                                            composition = null
                                        )
                                    )
                                    // 選取期間由這裡獨佔手勢：LazyRow 不能同時把畫面拖走，
                                    // 否則手指停在原處、欄位卻已經滑開，選到的字跟眼睛看到的對不上。
                                    var itemOffset = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.index == index }?.offset ?: 0
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes
                                            .firstOrNull { it.id == down.id } ?: break
                                        val pressed = change.pressed
                                        val position = change.position
                                        change.consume()
                                        if (!pressed) break
                                        // 這一欄可能被捲走過，每次都用它現在的偏移換算手指的位置。
                                        itemOffset = listState.layoutInfo.visibleItemsInfo
                                            .firstOrNull { it.index == index }?.offset ?: itemOffset
                                        val listX = itemOffset + position.x
                                        val target = listState.layoutInfo.visibleItemsInfo
                                            .firstOrNull {
                                                listX >= it.offset && listX < it.offset + it.size
                                            }?.index ?: continue
                                        val next = anchoredLineRange(
                                            columns = latestColumns.value,
                                            firstColumn = index,
                                            secondColumn = target
                                        ) ?: continue
                                        val current = latestValue.value
                                        if (current.selection.start != next.first ||
                                            current.selection.end != next.second
                                        ) {
                                            onValueChange(
                                                current.copy(
                                                    selection = TextRange(next.first, next.second),
                                                    composition = null
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                    ) {
                        // 分隔線要鋪滿整個編輯區高（含頂端行號帶與底部工具列預留），
                        // 所以畫在欄的最外層；它跟著欄一起捲動，不會和文字錯位。
                        if (writingGuide == WritingGuide.RULED) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawVerticalGuide(
                                    guide = writingGuide,
                                    cellWidthPx = fontSizePx,
                                    rowHeightPx = rowHeightPx,
                                    baselinePx = rowBaselinePx,
                                    color = variantColor
                                )
                            }
                        }
                        // 行號固定在欄頂；折行續欄（0）不標號，避免把同一行算成好幾行。
                        val lineNumber = columns.lineOf[index]
                        if (showLineNumbers && lineNumber > 0) {
                            Text(
                                text = lineNumber.toString(),
                                style = numberStyle,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = Space.s100)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = textTop, bottom = bottomReserve)
                        ) {
                            // 作文格只鋪在文字區裡：格子要跟字對齊，多畫到行號帶或
                            // 工具列底下反而多出一截沒有字的線。
                            if (writingGuide == WritingGuide.GRID) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawVerticalGuide(
                                        guide = writingGuide,
                                        cellWidthPx = fontSizePx,
                                        rowHeightPx = rowHeightPx,
                                        baselinePx = rowBaselinePx,
                                        color = variantColor
                                    )
                                }
                            }
                            if (selectedRows != null) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    // 選取底色對齊格子的寬（1.3em）而不是整欄（1.72em）：
                                    // 整欄塗滿會把欄與欄之間的空白也染色，看不出字被選了幾格。
                                    val highlightWidth = fontSizePx * 1.3f
                                    val highlightLeft = (size.width - highlightWidth) / 2f
                                    for (row in selectedRows) {
                                        drawRoundRect(
                                            color = selectionColor,
                                            topLeft = Offset(highlightLeft, row * rowHeightPx),
                                            size = Size(highlightWidth, rowHeightPx),
                                            cornerRadius = CornerRadius(rowHeightPx * 0.25f)
                                        )
                                    }
                                }
                            }

                            // 一個「字」一格：emoji 這種代理對整格放進同一行，
                            // 逐 char 排會把它拆成上下兩個半個字、顯示成兩個方塊。
                            // columns 一定要放進 key：鍵盤升降只換列數、不換文字，
                            // 少了它就會沿用舊分欄的字串——版面重排了、字卻還是舊的。
                            val columnText = remember(columns, text, index) {
                                columns.columnText(text, index)
                            }
                            // 待辦語法（`- ` 與 `[ ]`）隱形，勾選那行變灰加刪除線；
                            // 只加樣式、字數不變，分欄與字格位置完全不受影響。
                            val columnAnnotated = remember(columns, text, index, tasks, checkedInk) {
                                columns.columnAnnotated(text, index, tasks, checkedInk)
                            }
                            // 這一欄是否含有某個待辦的方框；有就記下列位置與狀態。
                            val marker = remember(columns, index, tasks) {
                                columns.markerIn(index, tasks)
                            }
                            if (text.isEmpty()) {
                                Text(
                                    text = placeholder.toCharArray().joinToString("\n"),
                                    style = placeholderStyle,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = columnAnnotated,
                                    style = columnStyle,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible,
                                    modifier = Modifier.fillMaxSize(),
                                    // 用這一欄「自己的」排版結果來畫格子：行距與基線都以實際渲染為準。
                                    // 拿全域量到的值去套，只要差一點點，往下十幾行就會累積成明顯的偏移。
                                    onTextLayout = { result ->
                                        val b = result.getLineBaseline(0)
                                        val a = if (result.lineCount > 1) {
                                            result.getLineBaseline(1) - b
                                        } else {
                                            0f
                                        }
                                        // 只採納「第一個量得到的值」，之後固定住：
                                        // 每欄都回寫的話，兩欄浮點尾數差一點點就會互相覆蓋、
                                        // 每一帧都重組，格子反而跟著抖。
                                        if (guideBaselinePx <= 0f && b > 0f) guideBaselinePx = b
                                        if (guideAdvancePx <= 0f && a > 0f) guideAdvancePx = a
                                    }
                                )
                            }

                            if (marker != null) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val box = minOf(rowHeightPx, cellWidthPx) * 0.62f
                                    val top = marker.first * rowHeightPx + (rowHeightPx - box) / 2f
                                    val left = (size.width - box) / 2f
                                    val radius = CornerRadius(box * 0.22f)
                                    val stroke = box * 0.12f
                                    if (marker.third) {
                                        drawRoundRect(
                                            color = accentColor,
                                            topLeft = Offset(left, top),
                                            size = Size(box, box),
                                            cornerRadius = radius
                                        )
                                    }
                                    drawRoundRect(
                                        color = if (marker.third) accentColor else variantColor,
                                        topLeft = Offset(left + stroke / 2f, top + stroke / 2f),
                                        size = Size(box - stroke, box - stroke),
                                        cornerRadius = radius,
                                        style = Stroke(stroke)
                                    )
                                    if (marker.third) {
                                        val w = box
                                        drawLine(
                                            color = Color.White,
                                            start = Offset(left + w * 0.27f, top + w * 0.52f),
                                            end = Offset(left + w * 0.44f, top + w * 0.69f),
                                            strokeWidth = stroke * 1.2f,
                                            cap = StrokeCap.Round
                                        )
                                        drawLine(
                                            color = Color.White,
                                            start = Offset(left + w * 0.44f, top + w * 0.69f),
                                            end = Offset(left + w * 0.75f, top + w * 0.32f),
                                            strokeWidth = stroke * 1.2f,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                            }

                            if (caretRow >= 0) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    // 1.5dp：字格 1.2em 時，字與字之間只剩約 0.2em 的空隙，
                                    // 2dp 的線會把空隙填滿、看起來就像貼在字上。
                                    val stroke = 1.5.dp.toPx()
                                    // 游標要落在「字與字之間」的格線上，而不是字格中央。
                                    val y = (caretRow * rowHeightPx)
                                        .coerceIn(stroke / 2f, size.height - stroke / 2f)
                                    if (caretVisible) {
                                        drawLine(
                                            color = caretColor,
                                            start = Offset(size.width * 0.2f, y),
                                            end = Offset(size.width * 0.8f, y),
                                            strokeWidth = stroke,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                    // 重排後短暫出現的「光標柄」（原生那種水滴氣泡）：
                                    // 豎排的光標是一條橫線，重排時它會換到別的列去，
                                    // 光靠閃爍很難一眼找到；這顆氣泡就指著它，淡入、停一下、淡出。
                                    val alpha = hintAlpha.value
                                    if (alpha > 0.01f) {
                                        // 同一滴水，轉 90 度：尖端抵住光標線右端，肚子往右。
                                        // （用 teardropPath 的切線+圓弧，邊緣才順。）
                                        val radius = 8.dp.toPx()
                                        val tipX = size.width * 0.8f
                                        val centerY = y.coerceIn(
                                            radius * 2f,
                                            size.height - radius * 2f
                                        )
                                        drawPath(
                                            path = teardropPath(
                                                tip = Offset(tipX, centerY),
                                                radius = radius,
                                                tipAngleDeg = 180f
                                            ),
                                            color = caretColor.copy(alpha = alpha)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                items(count = fillerCount) {
                    // 左邊沒寫到的空白也要有線，否則整張紙會缺一角。
                    // 參數與文字欄完全一致，接縫處才不會錯位。
                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .fillMaxHeight()
                    ) {
                        if (writingGuide == WritingGuide.RULED) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawVerticalGuide(
                                    guide = writingGuide,
                                    cellWidthPx = fontSizePx,
                                    rowHeightPx = 1f,
                                    baselinePx = 0f,
                                    color = variantColor
                                )
                            }
                        }
                        if (writingGuide == WritingGuide.GRID) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = textTop, bottom = bottomReserve)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawVerticalGuide(
                                        guide = writingGuide,
                                        cellWidthPx = fontSizePx,
                                        rowHeightPx = if (guideAdvancePx > 0f) {
                                            guideAdvancePx
                                        } else {
                                            cellHeightPx
                                        },
                                        baselinePx = if (guideBaselinePx > 0f) {
                                            guideBaselinePx
                                        } else {
                                            baselinePx
                                        },
                                        color = variantColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 選取工具列。固定貼在頂部：直排的選取常常橫跨好幾欄，
            // 跟著選取位置左右跑反而一直跳，找按鈕比找字還費力。
            if (!value.selection.collapsed) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = Space.s100),
                    shape = RoundedCornerShape(percent = 50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 3.dp,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        // 五個動作在窄螢幕會超出寬度；工具列自己可以橫向捲，
                        // 不要讓最後一顆（貼上）被切在螢幕外。
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = Space.s100),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            val body = value.text
                            val from = value.selection.min.coerceIn(0, body.length)
                            val to = value.selection.max.coerceIn(from, body.length)
                            clipboard.setText(AnnotatedString(body.substring(from, to)))
                            copiedHint = true
                        }) {
                            Text(if (copiedHint) strings.copied else strings.copySelection)
                        }
                        TextButton(onClick = {
                            val raw = value.text.toString()
                            val from = value.selection.min.coerceIn(0, raw.length)
                            val to = value.selection.max.coerceIn(from, raw.length)
                            // 先寫剪貼簿再刪；順序反過來就只剩空字串可複製。
                            clipboard.setText(AnnotatedString(raw.substring(from, to)))
                            onValueChange(
                                value.copy(
                                    text = raw.removeRange(from, to),
                                    selection = TextRange(from)
                                )
                            )
                        }) {
                            Text(strings.cutSelection)
                        }
                        TextButton(onClick = {
                            val clip = clipboard.getText() ?: return@TextButton
                            val raw = value.text.toString()
                            val from = value.selection.min.coerceIn(0, raw.length)
                            val to = value.selection.max.coerceIn(from, raw.length)
                            val next = raw.removeRange(from, to).replaceRange(from, from, clip.text)
                            onValueChange(
                                value.copy(
                                    text = next,
                                    selection = TextRange(from + clip.length)
                                )
                            )
                        }) {
                            Text(strings.pasteSelection)
                        }
                        TextButton(onClick = {
                            onValueChange(
                                value.copy(selection = TextRange(0, value.text.length))
                            )
                        }) {
                            Text(strings.selectAll)
                        }
                        TextButton(onClick = {
                            // 取消＝把插入點收在選取末端，接著打字就從那裡繼續。
                            onValueChange(
                                value.copy(selection = TextRange(value.selection.max))
                            )
                        }) {
                            Text(strings.cancel)
                        }
                    }
                }
            }
        }

        // 真正接收 IME 的欄位：尺寸趨近 0、完全透明，只負責送字與選取狀態。
        // 它的系統選取工具列要關掉：那個框只有 1dp，工具列不管選到哪一行都擠在角落，
        // 和上面那條自訂工具列重複。直排的選取由這個檔案自己處理。
        CompositionLocalProvider(LocalTextToolbar provides SilentTextToolbar) {
            BasicTextField(
                value = value,
                // IME 也會給出「停在代理對中間」的游標位置（例如快速輸入）。
                // 這裡先把它推到字格邊界，否則下一個字元會把 emoji 劈成兩個半字。
                onValueChange = { next ->
                    val snapped = snapSelection(next)
                    // 游標一旦被輸入法搬走（打字、選字），點擊當下的「行尾」提示就過期了；
                    // 留著會讓之後刪字回到同一個 offset 時，游標莫名又跑回欄尾。
                    if (snapped.selection != value.selection) tailCaret = NO_TAIL_CARET
                    onValueChange(snapped)
                },
                textStyle = textStyle,
                cursorBrush = SolidColor(Color.Transparent),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(1.dp)
                    .alpha(0f)
                    .focusRequester(focusRequester)
            )
        }
    }
}

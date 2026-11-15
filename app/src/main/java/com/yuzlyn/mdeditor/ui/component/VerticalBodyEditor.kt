package com.yuzlyn.mdeditor.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.yuzlyn.mdeditor.data.settings.WritingGuide
import com.yuzlyn.mdeditor.ui.theme.Space
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

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

@OptIn(ExperimentalLayoutApi::class)
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
    var focusTick by remember { mutableStateOf(0) }
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
        platformStyle = PlatformTextStyle(includeFontPadding = false)
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
        val gutterPx = with(density) { gutter.toPx() }
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
        val columns = remember(text, rows, graphemes) {
            buildVerticalColumns(text, rows, graphemes)
        }
        val listState = rememberLazyListState()
        // 格子的節拍（行距、基線）由「真的渲染出文字的那一欄」量出來之後**全欄共用**。
        // 原本每欄自己量、空白欄用全域量測值，兩套數字一有出入，
        // 有字的欄和沒字的欄就會畫出兩種節奏——往下幾十列就明顯錯開。
        var guideAdvancePx by remember { mutableStateOf(0f) }
        var guideBaselinePx by remember { mutableStateOf(0f) }
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
        var tailCaret by remember { mutableStateOf(NO_TAIL_CARET) }
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
        var hintTick by remember { mutableIntStateOf(0) }
        val hintAlpha = remember { Animatable(0f) }
        LaunchedEffect(hintTick) {
            if (hintTick == 0) return@LaunchedEffect
            hintAlpha.snapTo(0f)
            hintAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(160, easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f))
            )
            delay(1100)
            hintAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(280, easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f))
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
        } else if (writingGuide == WritingGuide.GRID) {
            // 作文格時內容不滿一屏，也把左邊填滿：整張紙都是格子，字在右邊起排。
            (maxWidth - Space.s200 - cellWidth * columns.count).coerceAtLeast(Space.s200)
        } else {
            Space.s200
        }
        val rowWidth = (cellWidth * columns.count + Space.s200 + tailSpace)
            .coerceAtMost(maxWidth)
        // 左邊那段留白也鋪成格子：用「空欄」把格子補到畫面左緣。
        // 空欄和文字欄用同一套參數畫格子，所以接縫處不會錯位。
        val fillerCount = if (writingGuide == WritingGuide.GRID) {
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
                contentPadding = PaddingValues(
                    start = startPadding,
                    end = Space.s200,
                    top = Space.s100,
                    bottom = bottomReserve
                ),
                modifier = Modifier
                    .width(rowWidth)
                    .fillMaxHeight()
            ) {
                items(count = columns.count) { index ->
                    val start = columns.starts[index]
                    val end = columns.ends[index]
                    val columnRows = columns.rowsOf(index)
                    // 這一欄畫格子、畫游標、換算點擊，用的都是同一個節拍。
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
                            .pointerInput(index, start, end, text.length, gutterPx, rowHeightPx) {
                                // 不用 detectTapGestures：它在 down 就消費事件，會擋住 LazyRow 的拖曳捲動。
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                                    // 長按要留給「選取／複製」這類手勢。若當成點擊處理，會把游標搬走、
                                    // 重新叫出鍵盤，整篇跟著重新分欄——看起來就是長按時畫面亂跳。
                                    if (up.uptimeMillis - down.uptimeMillis >
                                        viewConfiguration.longPressTimeoutMillis
                                    ) {
                                        return@awaitEachGesture
                                    }
                                    val current = latestValue.value
                                    val tap = caretForTap(
                                        columns = latestColumns.value,
                                        column = index,
                                        y = down.position.y - gutterPx,
                                        cellHeight = rowHeightPx,
                                        textLength = current.text.length
                                    )
                                    // 記住「這次是點在行尾」：同一欄的欄尾與下一欄的欄首是
                                    // 同一個插入點，沒有這個提示就永遠只能看到行首的游標。
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
                                }
                            }
                    ) {
                        // 行號固定在欄頂；折行續欄（0）不標號，避免把同一行算成好幾行。
                        val lineNumber = columns.lineOf[index]
                        if (showLineNumbers && lineNumber > 0) {
                            Text(
                                text = lineNumber.toString(),
                                style = numberStyle,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = gutter)
                        ) {
                            // 輔助線鋪在最底層：文字、選取與游標都壓在它上面。
                            if (writingGuide != WritingGuide.NONE) {
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
                                    for (row in selectedRows) {
                                        drawRoundRect(
                                            color = selectionColor,
                                            topLeft = Offset(0f, row * rowHeightPx),
                                            size = Size(size.width, rowHeightPx),
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
                                    text = columnText,
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
                    // 空白欄也要吃同一段上內距（行號帶）：文字欄的格子畫在 padding 之內，
                    // 空白欄若從頂端起畫，兩邊格子就差這 14dp，一欄一欄看過去就是
                    // 「有字的地方和沒字的地方沒對齊」。
                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .fillMaxHeight()
                            .padding(top = gutter)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawVerticalGuide(
                                guide = writingGuide,
                                cellWidthPx = fontSizePx,
                                rowHeightPx = if (guideAdvancePx > 0f) guideAdvancePx else cellHeightPx,
                                baselinePx = if (guideBaselinePx > 0f) guideBaselinePx else baselinePx,
                                color = variantColor
                            )
                        }
                    }
                }
            }
        }

        // 真正接收 IME 的欄位：尺寸趨近 0、完全透明，只負責送字與選取狀態。
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

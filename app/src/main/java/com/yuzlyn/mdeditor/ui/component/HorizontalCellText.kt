package com.yuzlyn.mdeditor.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * 橫排的「一格一字」版面。
 *
 * 這套版面**不交給文字引擎決定位置**：文字引擎是按比例排的（中文 1em、英文 0.4em、
 * 標點還不一定滿格），格子卻是等寬的；兩套座標系放一起，誤差一定逐字累積。
 * 直排之所以準，就是因為它自己算每一格。這裡把同一套做法搬到橫排：
 *
 * 1. 把文字切成「原子」：一個漢字/標點/emoji 一格，一個英文單詞佔整數格；
 * 2. 自己決定每個原子落在第幾列第幾格；
 * 3. 畫的時候直接把它放在 `格號 × 格寬` 的位置，並在它佔的格子裡居中。
 *
 * 於是「字在格子中間」是擺上去的結果，不是算出來的。
 */
data class HAtom(
    val text: String,
    /** 在原始字串裡的 char 範圍。 */
    val start: Int,
    val end: Int,
    val row: Int,
    /** 這一列的第幾格（從 0 起）。 */
    val col: Int,
    /** 佔幾格（英文單詞可能佔多格）。 */
    val cells: Int
)

class HCellLayout(
    val atoms: List<HAtom>,
    val rows: Int,
    val columns: Int,
    /** 每一列從哪個 char offset 開始；空行也記得住，游標才停得進去。 */
    val rowStarts: List<Int> = emptyList()
) {
    /** 某個 char offset 的游標位置（px，相對於版面左上角）。 */
    fun caretPosition(offset: Int, cellPx: Float, rowPitch: Float = cellPx): Offset {
        if (atoms.isEmpty()) return Offset(0f, 0f)
        // 先找「包含這個 offset 的原子」；找不到（例如 offset 正好是空白列的開頭、
        // 或是換行符）就退到「下一列的列首」，再不行才用下一個原子／最後一個。
        val atom = atoms.firstOrNull { offset >= it.start && offset < it.end }
        if (atom == null) {
            val emptyRow = rowStarts.indexOf(offset)
            if (emptyRow >= 0) return Offset(0f, emptyRow * rowPitch)
        }
        val target = atom
            ?: atoms.firstOrNull { it.start >= offset }
            ?: atoms.last()
        val fraction = if (target.end > target.start && offset >= target.start) {
            ((offset - target.start).toFloat() / (target.end - target.start)).coerceIn(0f, 1f)
        } else {
            0f
        }
        val x = (target.col + fraction * target.cells) * cellPx
        return Offset(x, target.row * rowPitch)
    }

    /** 點在 (x, y) 時應該落在哪個 char offset。 */
    fun offsetAt(x: Float, y: Float, cellPx: Float, rowPitch: Float = cellPx): Int {
        if (atoms.isEmpty()) return 0
        val row = floor(y / rowPitch).toInt().coerceIn(0, rows - 1)
        val inRow = atoms.filter { it.row == row }
        if (inRow.isEmpty()) {
            // 這一列沒有字（空行）：落在這一列的列首；沒有就退到最近的原子開頭。
            return rowStarts.getOrNull(row)
                ?: atoms.firstOrNull { it.row > row }?.start
                ?: atoms.last().end
        }
        val col = x / cellPx
        val atom = inRow.firstOrNull { col < it.col + it.cells } ?: inRow.last()
        val fraction = ((col - atom.col) / atom.cells).coerceIn(0f, 1f)
        val within = (fraction * (atom.end - atom.start)).roundToInt()
            .coerceIn(0, atom.end - atom.start)
        return atom.start + within
    }
}

/**
 * 切格並排成列。
 *
 * [columns] 是每一列能放幾格（由可用寬度決定）。換行只發生在原子邊界上：
 * 中文一格一個原子，英文單詞整體換行，不會被劈成兩半。
 */
internal fun buildHCellLayout(
    text: String,
    columns: Int,
    cellPx: Float,
    widthOf: (String) -> Float
): HCellLayout {
    val safeColumns = columns.coerceAtLeast(1)
    val starts = graphemeStarts(text)
    val atoms = ArrayList<HAtom>(starts.size)
    val rowStarts = ArrayList<Int>()
    rowStarts.add(0)
    var row = 0
    var col = 0
    var i = 0
    while (i < starts.size - 1) {
        val from = starts[i]
        val to = starts[i + 1]
        val piece = text.substring(from, to)
        if (piece == "\n") {
            row++
            col = 0
            rowStarts.add(to)
            i++
            continue
        }
        var last = i
        if (piece.length == 1 && piece[0].isAsciiWordChar()) {
            while (last + 1 < starts.size - 1) {
                val next = text.substring(starts[last + 1], starts[last + 2])
                if (next.length == 1 && next[0].isAsciiWordChar()) last++ else break
            }
        }
        val end = starts[last + 1]
        val whole = text.substring(from, end)
        val cells = if (last > i && cellPx > 0f) {
            ceil(widthOf(whole) / cellPx).toInt().coerceAtLeast(1)
        } else {
            1
        }.coerceAtMost(safeColumns)
        if (col + cells > safeColumns) {
            row++
            col = 0
        }
        atoms.add(HAtom(whole, from, end, row, col, cells))
        col += cells
        i = last + 1
    }
    return HCellLayout(atoms, row + 1, safeColumns, rowStarts)
}

/**
 * 畫格線、字、選取與游標。
 *
 * 三個尺寸是分開的，不要混用：
 * - [cellPx]：**列距**（橫向一格多寬，比字身寬，字四周才有空隙）；
 * - [fontPx]：字身（1em），也是格子的高；
 * - [rowPitchPx]：**行距**，比格高多一點，行與行之間才有縫。
 *
 * 游標與點擊一律用「這個字實際排版出來的位置」反推，不做平均分配：
 * 英文單詞佔好幾格時，字母是連著寫的，按格數平均算，游標就會落在字母縫外。
 */
@Composable
fun HorizontalCellText(
    layout: HCellLayout,
    cellPx: Float,
    fontPx: Float,
    style: TextStyle,
    textColor: Color,
    caretColor: Color,
    selectionColor: Color,
    guideColor: Color?,
    measurer: TextMeasurer,
    selection: TextRange,
    caretVisible: Boolean,
    rowPitchPx: Float,
    modifier: Modifier = Modifier,
    /** 外層的垂直捲動：只畫看得見的那幾列，長文捲動時不會逐格重畫。 */
    scrollState: ScrollState? = null,
    viewportHeightPx: Float = 0f,
    onTap: ((Int) -> Unit)? = null
) {
    // 點一下會短暫冒出「游標手柄」：淡入 → 停一下 → 淡出。
    // 曲線用 M3 的進出場（emphasized decelerate / accelerate），和直排那邊一致。
    var handleTick by remember { mutableIntStateOf(0) }
    val handleAlpha = remember { Animatable(0f) }
    LaunchedEffect(handleTick) {
        if (handleTick == 0) return@LaunchedEffect
        handleAlpha.snapTo(0f)
        handleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(150, easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f))
        )
        delay(900)
        handleAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(260, easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f))
        )
    }

    // 字的排版結果按字串快取：打字只會新增一兩個字，其餘都是查表。
    val cache = remember(style, textColor) { HashMap<String, TextLayoutResult>() }
    val drawStyle = remember(style, textColor) { style.copy(color = textColor) }

    fun resultOf(atom: HAtom): TextLayoutResult = cache.getOrPut(atom.text) {
        measurer.measure(
            text = AnnotatedString(atom.text),
            style = drawStyle,
            softWrap = false,
            maxLines = 1
        )
    }

    /** 這個原子畫在哪個 x（多格時在格子範圍內居中）。 */
    fun originX(atom: HAtom, result: TextLayoutResult): Float =
        atom.col * cellPx + (atom.cells * cellPx - result.size.width) / 2f

    /** 原子內第 [local] 個字元的 x —— 走真實排版，英文詞裡才落得準。 */
    fun caretX(atom: HAtom, local: Int): Float {
        val result = resultOf(atom)
        return originX(atom, result) +
            result.getHorizontalPosition(local.coerceIn(0, atom.text.length), true)
    }

    fun caretFor(offset: Int): Offset {
        val atom = layout.atoms.firstOrNull { offset >= it.start && offset < it.end }
            ?: layout.atoms.firstOrNull { it.start >= offset }
            ?: layout.atoms.lastOrNull()
            ?: return Offset(0f, 0f)
        return Offset(caretX(atom, offset - atom.start), atom.row * rowPitchPx)
    }

    fun offsetAtPosition(position: Offset): Int {
        if (layout.atoms.isEmpty()) return 0
        val row = floor(position.y / rowPitchPx).toInt().coerceIn(0, layout.rows - 1)
        val inRow = layout.atoms.filter { it.row == row }
        if (inRow.isEmpty()) {
            return layout.rowStarts.getOrNull(row)
                ?: layout.atoms.firstOrNull { it.row > row }?.start
                ?: layout.atoms.last().end
        }
        val atom = inRow.firstOrNull { position.x < (it.col + it.cells) * cellPx } ?: inRow.last()
        val result = resultOf(atom)
        val local = result.getOffsetForPosition(
            Offset(
                (position.x - originX(atom, result)).coerceIn(0f, result.size.width.toFloat()),
                (position.y - atom.row * rowPitchPx)
                    .coerceIn(0f, (result.size.height - 1).toFloat())
            )
        )
        return atom.start + local.coerceIn(0, atom.end - atom.start)
    }

    Canvas(
        modifier = modifier.then(
            if (onTap != null) {
                Modifier.pointerInput(layout, cellPx, rowPitchPx) {
                    detectTapGestures { position ->
                        handleTick++
                        onTap(offsetAtPosition(position))
                    }
                }
            } else {
                Modifier
            }
        )
    ) {
        val height = size.height
        // 只畫可見的列：讀 ScrollState 的值是在繪製階段，捲動只會重繪、不會重組。
        val firstRow = if (scrollState != null && viewportHeightPx > 0f) {
            (floor(scrollState.value / rowPitchPx).toInt() - 1).coerceAtLeast(0)
        } else {
            0
        }
        val lastRow = if (scrollState != null && viewportHeightPx > 0f) {
            (ceil((scrollState.value + viewportHeightPx) / rowPitchPx).toInt() + 1)
                .coerceAtMost(layout.rows - 1)
        } else {
            layout.rows - 1
        }

        // 格線：豎線貫通到底，橫線是每一格自己的上下邊（所以行與行之間有縫）。
        if (guideColor != null) {
            val path = Path()
            var x = 0f
            while (x <= size.width) {
                path.moveTo(x, 0f)
                path.lineTo(x, height)
                x += cellPx
            }
            var row = firstRow
            while (row <= lastRow && row * rowPitchPx < height) {
                val top = row * rowPitchPx
                val bottom = top + fontPx
                path.moveTo(0f, top)
                path.lineTo(size.width, top)
                if (bottom < height) {
                    path.moveTo(0f, bottom)
                    path.lineTo(size.width, bottom)
                }
                row++
            }
            drawPath(path, guideColor, style = Stroke(width = 1.dp.toPx() * 0.8f))
        }

        // 選取：逐字取真實位置，不會和字錯開。
        if (selection.max > selection.min) {
            for (atom in layout.atoms) {
                if (atom.row !in firstRow..lastRow) continue
                if (atom.end <= selection.min || atom.start >= selection.max) continue
                val from = (selection.min - atom.start).coerceIn(0, atom.end - atom.start)
                val to = (selection.max - atom.start).coerceIn(0, atom.end - atom.start)
                if (to <= from) continue
                val left = caretX(atom, from)
                val right = caretX(atom, to)
                drawRect(
                    color = selectionColor,
                    topLeft = Offset(left, atom.row * rowPitchPx),
                    size = Size(right - left, fontPx)
                )
            }
        }

        // 字：按基線擺，不受行框的 leading 影響。
        for (atom in layout.atoms) {
            if (atom.row !in firstRow..lastRow) continue
            val result = resultOf(atom)
            val x = originX(atom, result)
            val y = atom.row * rowPitchPx + fontPx * BASELINE_IN_CELL -
                result.getLineBaseline(0)
            drawText(result, topLeft = Offset(x, y))
        }

        // 游標：兩端圓角，像原生編輯器那樣收成半圓。
        val caret = caretFor(selection.start)
        val caretTop = caret.y + fontPx * 0.12f
        val caretBottom = caret.y + fontPx * 0.88f
        if (caretVisible) {
            drawLine(
                color = caretColor,
                start = Offset(caret.x, caretTop),
                end = Offset(caret.x, caretBottom),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 游標手柄：一滴完整的水滴——尖端收在游標下端，往下是整圓的肚子。
        // 用一條連續的曲線畫（切線 + 圓弧），不是三角拼膠囊，邊緣才是順的。
        val handle = handleAlpha.value
        if (handle > 0.01f) {
            drawPath(
                path = teardropPath(
                    tip = Offset(caret.x, caretBottom + 1.dp.toPx()),
                    radius = 8.dp.toPx(),
                    tipAngleDeg = -90f
                ),
                color = caretColor.copy(alpha = caretColor.alpha * handle)
            )
        }
    }
}

/**
 * 一滴水的形狀：尖端在 [tip]，肚子是一個半徑 [radius] 的圓。
 *
 * 尖端用「從外部點到圓的兩條切線」收邊（夾角由 asin(r/h) 決定），
 * 再沿圓弧繞過遠端——這樣尖端和圓身是連續的，不會像三角拼起來那樣有折角。
 * [tipAngleDeg] 是「圓心 → 尖端」的方向：-90 表示尖端朝上（尖在水滴頂端），180 表示尖端朝左。
 */
internal fun teardropPath(tip: Offset, radius: Float, tipAngleDeg: Float): Path {
    val distance = radius * 1.35f
    val phi = Math.toRadians(tipAngleDeg.toDouble())
    val theta = Math.toRadians(Math.toDegrees(Math.asin((radius / distance).toDouble())).toDouble())
    val centerX = tip.x - (Math.cos(phi) * distance).toFloat()
    val centerY = tip.y - (Math.sin(phi) * distance).toFloat()
    val rect = androidx.compose.ui.geometry.Rect(
        centerX - radius,
        centerY - radius,
        centerX + radius,
        centerY + radius
    )
    val startAngle = Math.toDegrees(phi + theta).toFloat()
    val sweep = 360f - Math.toDegrees(theta * 2).toFloat()
    return Path().apply {
        moveTo(tip.x, tip.y)
        arcTo(rect, startAngle, sweep, forceMoveTo = false)
        close()
    }
}

/**
 * 基線落在字身高度的第幾倍處。0.92 是量出來的：字面高約 0.9em，
 * 留一點上下空隙時字才在格子正中。只影響字在格內的上下位置。
 */
private const val BASELINE_IN_CELL = 0.92f

/**
 * 列距（以字身為 1）：橫向一格比字身寬，字左右才有空隙。
 * 對齊直排的欄寬感覺——直排是 1.72em 欄距，這裡取 1.3em 已經明顯鬆開，
 * 再寬一行人放不下幾個字。
 */
const val CELL_PITCH_EM = 1.3f

/** 行距（以字身為 1）：格高仍是 1em，多出來的 0.4em 就是每行下方的空隙。 */
const val ROW_PITCH_EM = 1.4f

/** 只把 ASCII 字母數字當成「可以連寫的單詞」；全角英文/中文一律各自佔一格。 */
private fun Char.isAsciiWordChar(): Boolean =
    this.code < 0x2E80 && (isLetterOrDigit() || this == '\'' || this == '-')

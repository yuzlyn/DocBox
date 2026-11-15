package com.yuzlyn.mdeditor.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuzlyn.mdeditor.data.settings.WritingGuide
import kotlin.math.abs
import kotlin.math.ceil

/**
 * 書寫輔助線的繪製。
 *
 * 兩個方向分開畫，因為「線」在兩個方向上是不同東西：
 * - 橫排：分隔線是**橫線**，分隔的是行。
 * - 直排：分隔線是**豎線**，分隔的是欄。
 * 作文格兩邊都是格子，但直排的格子要按欄內的 1em 字身對齊，不能用欄距（1.72em）。
 *
 * 顏色一律由呼叫端給（通常是 `onBackgroundVariant`），只調透明度——
 * 深色背景與六種紙色預設都能自適應，不需要為每種背景配一套線色。
 */
private const val LINE_ALPHA = 0.20f
private const val GRID_ALPHA = 0.16f

/**
 * 方格線的座標：每一行有自己的一組豎線，橫線則是整份共用的行邊界。
 *
 * 為什麼不畫「等距線」：格子的週期是量出來的，文字卻是由排版引擎按字型自己決定的，
 * 兩者只要差 1%，一行二十字就會偏出四分之一格。改成直接取每個字的實際位置，
 * 對齊就是定義本身，不存在「算不準」這回事。
 */
data class GuideLines(
    val rows: List<Float> = emptyList(),
    val colsByRow: List<List<Float>> = emptyList()
)

/**
 * 從排版結果算出方格線。
 *
 * - 橫線：每行的上下邊，再整體位移，讓字落在格子中間
 *   （字在行框裡不是垂直居中的，位置由字型的 ascent 決定）。
 * - 豎線：每行自己的一組 x。取「間距至少 0.7 格」的字邊界——補過格的字邊界剛好一格，
 *   英文單詞內部只有零點幾個字寬，於是不會被豎線切開。
 */
internal fun buildGridLines(
    layout: TextLayoutResult,
    cellPx: Float,
    emPx: Float
): GuideLines {
    if (cellPx <= 0f || layout.lineCount == 0) return GuideLines()
    // 橫線用「基線」推，不用 getLineTop：行高裁剪會讓首行的 top 多出一段 leading，
    // 直接拿 top 排格線，第二行就會跨在線上。基線是等距的，逐行都準。
    val inset = emPx * BASELINE_TO_TOP + (cellPx - emPx) / 2f
    val rows = ArrayList<Float>(layout.lineCount + 1)
    for (line in 0 until layout.lineCount) {
        rows.add(layout.getLineBaseline(line) - inset)
    }
    rows.add(rows.last() + cellPx)

    val threshold = cellPx * 0.7f
    val colsByRow = ArrayList<List<Float>>(layout.lineCount)
    for (line in 0 until layout.lineCount) {
        val start = layout.getLineStart(line)
        val end = layout.getLineEnd(line, visibleEnd = true)
        val xs = ArrayList<Float>(64)
        var last = layout.getHorizontalPosition(start, true)
        xs.add(last)
        var cursor = start + 1
        while (cursor <= end) {
            val x = layout.getHorizontalPosition(cursor, true)
            if (x - last >= threshold) {
                xs.add(x)
                last = x
            }
            cursor++
        }
        colsByRow.add(xs)
    }
    return GuideLines(rows, colsByRow)
}

/**
 * 橫排：鋪在文字底下，隨內容一起捲動。
 *
 * 作文格與分隔線的幾何**完全不同**，不要用同一套參數：
 *
 * - **作文格**：格線由 [buildGridLines] 從排版結果算出（見 [GuideLines]），
 *   呼叫端必須把行高也設成格寬，兩個方向才是同一套節拍。
 * - **分隔線**：只畫每行底下那條線，所以要貼著字，得用實測的基線與行距
 *   （[baselinePx]、[lineAdvancePx]），不能用字號推。
 */
@Composable
fun WritingGuideBackground(
    guide: WritingGuide,
    baselinePx: Float,
    lineAdvancePx: Float,
    cellPx: Float,
    color: Color,
    modifier: Modifier = Modifier,
    gridLines: GuideLines = GuideLines()
) {
    if (guide == WritingGuide.NONE) return
    if (guide == WritingGuide.GRID) {
        if (gridLines.colsByRow.isEmpty() || gridLines.rows.isEmpty()) return
    } else if (baselinePx <= 0f || lineAdvancePx <= 0f || cellPx <= 0f) {
        return
    }
    Canvas(modifier = modifier) {
        val lineColor = color.copy(
            alpha = if (guide == WritingGuide.GRID) GRID_ALPHA else LINE_ALPHA
        )
        val stroke = 1.dp.toPx()
        if (guide == WritingGuide.GRID) {
            // 整片格子合成一條 path：上千條線也只是一個 draw call，捲動時不會掉幀。
            val path = Path()
            val rows = gridLines.rows
            gridLines.colsByRow.forEachIndexed { index, xs ->
                val top = rows.getOrNull(index) ?: return@forEachIndexed
                val bottom = rows.getOrNull(index + 1) ?: return@forEachIndexed
                xs.forEach { x ->
                    path.moveTo(x, top)
                    path.lineTo(x, bottom)
                }
            }
            rows.forEach { y ->
                path.moveTo(0f, y)
                path.lineTo(size.width, y)
            }
            drawPath(path, lineColor, style = Stroke(width = stroke * 0.8f))
            return@Canvas
        }
        // 分隔線：每行一條，貼著字的下緣。
        var baseline = baselinePx
        while (baseline - cellPx < size.height) {
            val bottom = baseline + cellPx * BASELINE_TO_BOTTOM
            if (bottom >= 0f && bottom <= size.height) {
                drawLine(lineColor, Offset(0f, bottom), Offset(size.width, bottom), stroke)
            }
            baseline += lineAdvancePx
        }
    }
}

/** 直排格子的寬度（以字身為 1em 計）；兩側各放寬 (此值 - 1) / 2 em。 */
private const val VERTICAL_CELL_WIDTH_EM = 1.3f

/** 字面在基線以上的高度（em）：CJK 字型的字面約佔 0.88em，其餘留給下沉筆畫。 */
private const val BASELINE_TO_TOP = 0.88f

/** 基線以下留給下沉筆畫的比例。 */
private const val BASELINE_TO_BOTTOM = 0.14f

/**
 * 直排：每一欄自己畫自己的線，呼叫端只負責把它鋪在文字底下。
 *
 * [cellWidthPx] 是**字身寬**（1em），不是欄距。
 * 分隔線要畫在欄的邊界上（＝兩列字正中間），不能畫在字身右緣：
 * 畫在字身右緣會變成「線貼著字的右邊、左邊空一大截」，看起來就是字沒有居中。
 */
internal fun DrawScope.drawVerticalGuide(
    guide: WritingGuide,
    cellWidthPx: Float,
    rowHeightPx: Float,
    /** 一行字裡基線落在第幾 px；格子以它為準，不是以行高的一半為準。 */
    baselinePx: Float,
    color: Color
) {
    if (guide == WritingGuide.NONE) return
    val lineColor = color.copy(
        alpha = if (guide == WritingGuide.GRID) GRID_ALPHA else LINE_ALPHA
    )
    val stroke = 1.dp.toPx()
    // 格子的**寬**比字身寬一些：1em 剛好貼著字的左右邊緣，看起來太窄；
    // 高度維持 1em（正好框住字），只把左右各放寬 0.15em。
    val cellWidth = cellWidthPx * VERTICAL_CELL_WIDTH_EM
    val left = (size.width - cellWidth) / 2f
    val right = left + cellWidth
    if (guide == WritingGuide.RULED) {
        // 畫在欄的右邊界：欄寬 1.72em、字身 1em，邊界正好是兩列字的中間，
        // 兩邊各留 0.36em，字才會落在行格的中央。
        drawLine(lineColor, Offset(size.width, 0f), Offset(size.width, size.height), stroke)
        return
    }
    if (rowHeightPx <= 0f) return
    // 高度維持字身（1em）：字身的上緣＝基線往上 0.88em，
    // 從那裡往下畫 1em，字就正好落在格子中間（實測這台機器剛好）。
    // 寬度另外放寬（見 VERTICAL_CELL_WIDTH_EM），所以格子是略寬的長方形。
    val inset = (baselinePx - cellWidthPx * 0.88f).coerceAtLeast(0f)
    var row = 0
    while (true) {
        val top = row * rowHeightPx + inset
        if (top > size.height) break
        val bottom = top + cellWidthPx
        drawLine(lineColor, Offset(left, top), Offset(right, top), stroke * 0.8f)
        if (bottom <= size.height) {
            drawLine(lineColor, Offset(left, bottom), Offset(right, bottom), stroke * 0.8f)
        }
        row++
    }
    drawLine(lineColor, Offset(left, 0f), Offset(left, size.height), stroke * 0.8f)
    drawLine(lineColor, Offset(right, 0f), Offset(right, size.height), stroke * 0.8f)
}

/**
 * 作文格：把文字排成「整數格」的稿紙模型。
 *
 * 這是橫排格子能對齊的**唯一前提**：格子是等寬的，所以每個字的步進也必須被壓成等寬。
 * 文字本身是按比例排版的（中文 1em、英文 0.4em、標點也不一定滿格），兩套座標系放一起，
 * 誤差會逐字累積，於是出現「只有行首第一字在格子裡、越往右越歪」。
 *
 * 規則：
 * - 中文、標點、emoji：各佔 1 格。字比格寬就用負字距收回來、比格窄就補出去，
 *   一律讓**下一個字正好落在格線上**（不能只補正數，否則寬字會一格一格往外推）。
 * - 英文/數字：整個單詞量一次寬，向上取整成 N 格；單詞內部字距不動（保持連寫），
 *   多出來的餘量左右各半 → 單詞在 N 格裡居中，兩邊都留白。
 *
 * 實作只調 [SpanStyle.letterSpacing]，不動文字內容，所以 [OffsetMapping.Identity] 成立，
 * 游標與選取完全不受影響。
 */
internal class CellAlignedTransformation(
    private val cellPx: Float,
    private val style: TextStyle,
    private val measurer: TextMeasurer,
    private val density: Density,
    /**
     * 量測寬度 → 實際渲染寬度的比例。
     *
     * [TextMeasurer] 是在組字當下量的，和真正畫出來的字寬可能差 1% 上下
     * （字型替換、字距換算、快取拿到舊字型都會造成）。這 1% 會逐字累積，
     * 一整行二十字就是四分之一格——看起來就是「越寫越歪」。
     * 呼叫端用「實際排版量到的漢字步進 ÷ 量測到的步進」算出這個比例。
     */
    private val widthScale: Float = 1f
) : VisualTransformation {

    /** 每個字元（grapheme）以基準樣式量到的寬度，key 是那一段字串。 */
    private val widths = HashMap<String, Float>()

    /** 基準字距（bodyLarge 是 0.5sp）：span 會「覆蓋」而不是累加，換算時要扣掉。 */
    private val baseSpacingPx = with(density) {
        if (style.letterSpacing.isSp) style.letterSpacing.toPx() else 0f
    }

    /**
     * 這一段字在**基準字距下**的真實步進。
     *
     * 不能用 `layout.size.width`：它有沒有把最後一個字的字距算進去，各家實作不同，
     * 差半個字距就會逐字累積成「越寫越歪」。這裡改用「起點 → 下一字起點」的位置差，
     * 與 [cellPx] 的取法（`getHorizontalPosition(1)`）完全同一個定義，兩者必然一致。
     */
    private fun widthOf(piece: String): Float = widths.getOrPut(piece) {
        val probe = measurer.measure(
            text = piece + "国",
            style = style,
            softWrap = false,
            maxLines = 1
        )
        val advance = probe.getHorizontalPosition(piece.length, true) -
            probe.getHorizontalPosition(0, true)
        (advance.takeIf { it > 0f } ?: probe.size.width.toFloat()) * widthScale
    }

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty() || cellPx <= 0f) return TransformedText(text, OffsetMapping.Identity)
        val starts = graphemeStarts(raw)
        if (starts.size < 2) return TransformedText(text, OffsetMapping.Identity)

        // 先切出所有「字」（grapheme）的 char 範圍，後面分配間距時一律動整個字，
        // 不能只動代理對的一半，否則 emoji 會被劈開。
        val graphemes = ArrayList<IntRange>(starts.size - 1)
        for (k in 0 until starts.size - 1) {
            if (starts[k] < starts[k + 1]) graphemes.add(starts[k] until starts[k + 1])
        }

        // 1) 切成「原子」：一個中文/標點/emoji，或一整個英文單詞。
        val atoms = ArrayList<Atom>(graphemes.size)
        var lineStart = true
        var g = 0
        while (g < graphemes.size) {
            val range = graphemes[g]
            val piece = raw.substring(range.first, range.last + 1)
            if (piece == "\n") {
                lineStart = true
                g++
                continue
            }
            var last = g
            if (piece.length == 1 && piece[0].isAsciiWordChar()) {
                while (last + 1 < graphemes.size) {
                    val following = graphemes[last + 1]
                    val p = raw.substring(following.first, following.last + 1)
                    if (p.length == 1 && p[0].isAsciiWordChar()) last++ else break
                }
            }
            val from = range.first
            val to = graphemes[last].last + 1
            val natural = widthOf(raw.substring(from, to))
            val isWord = last > g
            val extra = if (isWord) {
                val cells = ceil(natural / cellPx - 0.001f).toInt().coerceAtLeast(1)
                // 補到整數格；餘量一定小於一格（剛好整除時為 0）。
                (cells * cellPx - natural).coerceIn(0f, cellPx * 0.99f)
            } else {
                // 單字：差額可以是負的（該字比格寬），用負字距收回來，
                // 否則寬字會把後面整排推出去，一格一格累積成「越寫越歪」。
                (cellPx - natural).coerceIn(-cellPx * 0.5f, cellPx * 0.99f)
            }
            atoms.add(Atom(from, to, extra, lineStart, g, last, isWord))
            lineStart = false
            g = last + 1
        }

        // 2) 分配餘量。
        //   單字：整份補在自己後面 → 下一個字正好落在格線上。
        //   單詞：半份補在前一個字的後面、半份補在詞尾 → 單詞在 N 格裡居中。
        val extraAt = LinkedHashMap<IntRange, Float>()
        fun add(range: IntRange, value: Float) {
            extraAt[range] = (extraAt[range] ?: 0f) + value
        }
        atoms.forEach { atom ->
            if (abs(atom.extra) <= 0.01f) return@forEach
            val previous = atom.firstGrapheme - 1
            val canSplit = atom.isWord && !atom.firstOnLine && previous >= 0 &&
                raw.substring(graphemes[previous].first, graphemes[previous].last + 1) != "\n"
            if (canSplit) {
                add(graphemes[previous], atom.extra / 2f)
                add(graphemes[atom.lastGrapheme], atom.extra / 2f)
            } else {
                add(graphemes[atom.lastGrapheme], atom.extra)
            }
        }
        if (extraAt.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        // 3) 逐字套上 span（值＝基準字距 + 這一格要多留的量）。
        val builder = AnnotatedString.Builder(text)
        extraAt.forEach { (range, extra) ->
            val spacing = with(density) { (baseSpacingPx + extra).toSp() }
            builder.addStyle(SpanStyle(letterSpacing = spacing), range.first, range.last + 1)
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    private data class Atom(
        val from: Int,
        val to: Int,
        /** 這個原子佔滿整數格之後還剩多少 px 要分配（可為負）。 */
        val extra: Float,
        val firstOnLine: Boolean,
        val firstGrapheme: Int,
        val lastGrapheme: Int,
        val isWord: Boolean
    )
}

/** 只把 ASCII 字母數字當成「可以連寫的單詞」；全角英文/中文一律各自佔一格。 */
private fun Char.isAsciiWordChar(): Boolean =
    this.code < 0x2E80 && (isLetterOrDigit() || this == '\'' || this == '-')

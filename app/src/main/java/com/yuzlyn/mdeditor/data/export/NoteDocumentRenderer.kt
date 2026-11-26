package com.yuzlyn.mdeditor.data.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import com.yuzlyn.mdeditor.data.settings.WritingGuide
import java.io.ByteArrayOutputStream
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/** 圖片匯出的結果；[truncated] 表示內容太長，只截了前面一段。 */
internal data class PngResult(val bytes: ByteArray, val truncated: Boolean)

/**
 * 把純文字排版成 PDF／PNG，支援橫排與直排。
 *
 * 兩條路徑：
 * - **有格線或直排**走稿紙模型（[CellAtom]／[packCellLines]）：一個字一格，自己排。
 *   方格必須是正方形、字必須落在格子裡，這件事只有「自己排」才保證得了——
 *   排版引擎按比例排字，格子按字號鋪線，兩套座標系一定會愈寫愈歪。
 * - **無格線／分隔線的橫排**走 [StaticLayout]：那是 Android 的排版引擎，
 *   換行、標點避頭尾都幫你做好，散文用它是最好看的。
 *
 * 直排不是「把橫排轉九十度」：漢字直立、英數整串旋轉九十度、換行是換到左邊一欄。
 * StaticLayout 沒有直書模式，所以直排一律走稿紙模型。
 */
internal object NoteDocumentRenderer {

    private const val PAGE_WIDTH = 595   // A4 @72dpi
    private const val PAGE_HEIGHT = 842
    private const val PAGE_MARGIN = 48f

    private const val IMAGE_WIDTH = 720
    private const val IMAGE_PADDING = 48
    private const val IMAGE_TEXT_SIZE = 30f
    private const val IMAGE_MAX_HEIGHT = 9000

    /**
     * 直排圖片橫著長，寬度才是它的「長度」。
     *
     * 8000px 約 200 欄（6000 字），遠超一般筆記；上限設在這裡是因為整張圖要放在
     * 記憶體裡壓成 PNG（8000×1000 就已經 32MB），再寬就有 OOM 風險。
     * 超過就截在欄與欄之間，並回報 truncated。
     */
    private const val IMAGE_MAX_WIDTH = 8000

    /** 直排圖片每欄幾格：30 格 × 30px＝900px 高，貼進聊天室剛好。 */
    private const val VERTICAL_IMAGE_ROWS = 30

    /** 直排名片與正文之間留的空隙，以格寬為單位。 */
    private const val VERTICAL_CARD_GAP_EM = 1.0f

    /** 直排名片：字縮到正文的這個比例，一欄一行的縱深才不會壓過正文。 */
    private const val VERTICAL_CARD_TEXT_SCALE = 0.72f

    /** 直排名片的字距（每個字往下走多少）。 */
    private const val VERTICAL_CARD_STEP_EM = 1.05f

    /** 直排名片的欄寬（一行資料一欄）。 */
    private const val VERTICAL_CARD_COLUMN_EM = 1.3f

    /** 直排名片的頭像大小：疊在資料上方，比一欄略大一點才像一枚章。 */
    private const val VERTICAL_CARD_AVATAR_EM = 2.2f

    /** 直排名片頭像與下方資料之間的空隙。 */
    private const val VERTICAL_CARD_AVATAR_GAP_EM = 0.9f

    // 格子紙的節拍，全部以「字身（1em）」為單位，數值與編輯器同源：
    // ui/component/HorizontalCellText.kt 的 CELL_PITCH_EM／ROW_PITCH_EM，
    // ui/component/VerticalBodyEditor.kt 的 COLUMN_PITCH_EM／CHAR_ADVANCE_EM。
    // 少對一個，導出的圖就不是編輯頁那張紙了。

    /** 橫排格子寬：比字身寬一點，字才不會貼著豎線。 */
    private const val CELL_PITCH_EM = 1.3f

    /** 橫排行距：格高仍是 1em，多出來的 0.4em 就是行與行之間的空白。 */
    private const val ROW_PITCH_EM = 1.4f

    /** 直排列距（欄與欄之間）；要比字距明顯寬，否則欄會擠成一團。 */
    private const val COLUMN_PITCH_EM = 1.72f

    /** 直排格子寬（在欄內居中）。 */
    private const val VERTICAL_CELL_WIDTH_EM = 1.3f

    /** 直排字距：同一欄裡字往下走的距離，比格高多一點就是行間隔。 */
    private const val CHAR_ADVANCE_EM = 1.2f

    /** 基線以下留給下沉筆畫的比例；與編輯器 `BASELINE_TO_BOTTOM` 同源。 */
    private const val BASELINE_TO_BOTTOM = 0.14f

    /**
     * 直排時要跟著轉九十度的中文標點。
     *
     * 橫排的「」是左右開口的，疊成直書後開口會朝著上下——那是「躺著」的引號，
     * 不是直排的引號。中文直排的慣例是把這一整類標點轉九十度（字型裡的 `vert`
     * 特性做的也是同一件事），引號的開口才會朝向被引用的那句話。
     * 編輯器走字型的 `vert`，這裡沒有字型特性可用，就自己轉。
     */
    private const val VERTICAL_ROTATED_PUNCT = "「」『』（）《》〈〉【】〔〕［］｛｝"

    /** 這個字在直排裡要不要轉九十度。 */
    private fun rotatesInVertical(codePoint: Int): Boolean =
        codePoint < 0x2E80 || VERTICAL_ROTATED_PUNCT.indexOf(codePoint.toChar()) >= 0

    // ---------------------------------------------------------------- PDF

    fun renderPdf(
        text: String,
        guide: WritingGuide = WritingGuide.NONE,
        vertical: Boolean = false,
        /** 背景／文字／輔助線的顏色；由編輯頁傳進來，輸出才跟螢幕一致。 */
        theme: ExportTheme = ExportTheme.DEFAULT
    ): ByteArray {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.text
            textSize = 11f
            typeface = theme.typeface ?: Typeface.SERIF
        }
        return if (vertical || guide == WritingGuide.GRID) {
            renderPdfCells(text, guide, paint, vertical, theme)
        } else {
            renderPdfFlow(text, guide, paint, theme)
        }
    }

    /** 稿紙模型：方格與直排都走這裡，差別只在裝箱方向。 */
    private fun renderPdfCells(
        text: String,
        guide: WritingGuide,
        paint: TextPaint,
        vertical: Boolean,
        theme: ExportTheme
    ): ByteArray {
        val document = PdfDocument()
        try {
            val contentWidth = PAGE_WIDTH - PAGE_MARGIN * 2
            val contentHeight = PAGE_HEIGHT - PAGE_MARGIN * 2
            val cell = cellWidth(paint)
            val rowPitch = cell * ROW_PITCH_EM
            val charStep = cell * CHAR_ADVANCE_EM
            val columnPitch = cell * COLUMN_PITCH_EM
            // 直排：一欄的高度＝頁高；橫排：一行的寬度＝格子紙能排幾格。
            val cellsPerLine = if (vertical) {
                floor(contentHeight / charStep).toInt()
            } else {
                floor(contentWidth / (cell * CELL_PITCH_EM)).toInt()
            }.coerceAtLeast(1)
            val lines = packCellLines(
                cellAtoms(text.ifEmpty { " " }, paint, cell, cellsPerLine),
                cellsPerLine
            )
            if (lines.isEmpty()) return emptyPdf(document)

            val linesPerPage = when {
                vertical -> floor(contentWidth / columnPitch).toInt()
                else -> floor(contentHeight / rowPitch).toInt()
            }.coerceAtLeast(1)

            var pageNumber = 1
            var index = 0
            while (index < lines.size) {
                val slice = lines.subList(index, minOf(index + linesPerPage, lines.size))
                val page = document.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                )
                val canvas = page.canvas
                // 整頁底色先鋪：PDF 預設是白紙，深色主題不鋪這一層就會變回白底配淺字。
                canvas.drawColor(theme.background)
                canvas.save()
                canvas.translate(PAGE_MARGIN, PAGE_MARGIN)
                canvas.clipRect(0f, 0f, contentWidth, contentHeight)
                if (vertical) {
                    // 直排由右往左讀：這一頁的欄從右邊界開始往左排。
                    drawColumns(
                        canvas = canvas,
                        paint = paint,
                        columns = slice,
                        areaRight = contentWidth.toFloat(),
                        top = 0f,
                        cell = cell,
                        guide = guide,
                        color = theme.guide,
                        maxRows = cellsPerLine
                    )
                } else {
                    drawRows(
                        canvas = canvas,
                        paint = paint,
                        rows = slice,
                        top = 0f,
                        cell = cell,
                        cellsPerLine = cellsPerLine,
                        guide = guide,
                        color = theme.guide
                    )
                }
                canvas.restore()
                document.finishPage(page)
                index += linesPerPage
                pageNumber++
            }

            return ByteArrayOutputStream().use { out ->
                document.writeTo(out)
                out.toByteArray()
            }
        } finally {
            document.close()
        }
    }

    /** 散文：交給 StaticLayout 排，只有分隔線需要自己畫。 */
    private fun renderPdfFlow(
        text: String,
        guide: WritingGuide,
        paint: TextPaint,
        theme: ExportTheme
    ): ByteArray {
        val document = PdfDocument()
        try {
            val contentWidth = (PAGE_WIDTH - PAGE_MARGIN * 2).toInt()
            val contentHeight = (PAGE_HEIGHT - PAGE_MARGIN * 2).toInt()
            val body = text.ifEmpty { " " }
            val cell = cellWidth(paint)
            var start = 0
            var pageNumber = 1

            while (start < body.length) {
                val layout = newLayout(body, start, body.length, paint, contentWidth, 1.15f, 2f)

                // 這頁最多放得下幾行：用行底位置判斷，不猜字數。
                var lastLine = -1
                for (line in 0 until layout.lineCount) {
                    if (layout.getLineBottom(line) <= contentHeight) lastLine = line else break
                }
                if (lastLine < 0) lastLine = 0

                val page = document.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                )
                val canvas = page.canvas
                canvas.drawColor(theme.background)
                canvas.save()
                canvas.translate(PAGE_MARGIN, PAGE_MARGIN)
                canvas.clipRect(0f, 0f, contentWidth.toFloat(), contentHeight.toFloat())
                drawFlowGuides(
                    canvas = canvas,
                    paint = paint,
                    layout = layout,
                    guide = guide,
                    width = contentWidth.toFloat(),
                    height = contentHeight.toFloat(),
                    cell = cell,
                    color = theme.guide
                )
                layout.draw(canvas)
                canvas.restore()
                document.finishPage(page)

                if (lastLine + 1 >= layout.lineCount) break
                val next = layout.getLineStart(lastLine + 1)
                // 保險：一行比整頁還高時 next 可能不再前進，強制往前推一個字。
                start = if (next > start) next else start + 1
                pageNumber++
            }

            return ByteArrayOutputStream().use { out ->
                document.writeTo(out)
                out.toByteArray()
            }
        } finally {
            document.close()
        }
    }

    /** 空內容也要給一個合法 PDF，不能回傳 0 byte（分享時會變成壞檔）。 */
    private fun emptyPdf(document: PdfDocument): ByteArray {
        val page = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        )
        document.finishPage(page)
        return ByteArrayOutputStream().use { out ->
            document.writeTo(out)
            out.toByteArray()
        }
    }

    // ---------------------------------------------------------------- PNG

    /**
     * 圖片匯出。與 PDF 的差別不只是「換一個輸出容器」：圖片會貼進聊天室，
     * 名片要長得像一張名片——圓形頭像、逐欄資料、一條細線與正文分開。
     *
     * [card] 只影響圖片：Markdown／純文字／PDF 沒有「圖」這個概念，
     * 它們的名片是純文字（見 [NoteExportManager]）。
     */
    fun renderPng(
        text: String,
        guide: WritingGuide = WritingGuide.NONE,
        vertical: Boolean = false,
        card: ExportCard? = null,
        /** 背景／文字／輔助線／字型；圖片連背景圖一起帶。 */
        theme: ExportTheme = ExportTheme.DEFAULT
    ): PngResult {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.text
            textSize = IMAGE_TEXT_SIZE
            typeface = theme.typeface ?: Typeface.DEFAULT
        }
        return if (vertical || guide == WritingGuide.GRID) {
            renderPngCells(text, guide, paint, vertical, card, theme)
        } else {
            renderPngFlow(text, guide, paint, card, theme)
        }
    }

    private fun renderPngCells(
        text: String,
        guide: WritingGuide,
        paint: TextPaint,
        vertical: Boolean,
        card: ExportCard?,
        theme: ExportTheme
    ): PngResult {
        val cell = cellWidth(paint)
        return if (vertical) {
            renderVerticalCells(text, guide, paint, cell, card, theme)
        } else {
            renderHorizontalCells(text, guide, paint, cell, card, theme)
        }
    }

    /** 橫排稿紙：一頁到底，只有超過畫布高度才截。 */
    private fun renderHorizontalCells(
        text: String,
        guide: WritingGuide,
        paint: TextPaint,
        cell: Float,
        card: ExportCard?,
        theme: ExportTheme
    ): PngResult {
        val contentWidth = IMAGE_WIDTH - IMAGE_PADDING * 2
        val band = card?.let { measureCardBand(it, paint, contentWidth) }
        val bandSpace = band?.let { cardGap(paint) + it.height } ?: 0f
        val before = band != null && card?.position == CardPosition.BEFORE

        // 格子比字身寬（1.3em）、行距比格高（1.4em）：字在格子中間，行與行之間留白。
        val rowPitch = cell * ROW_PITCH_EM
        val cellsPerLine = floor(contentWidth / (cell * CELL_PITCH_EM)).toInt().coerceAtLeast(1)
        val lines = packCellLines(
            cellAtoms(text.ifEmpty { " " }, paint, cell, cellsPerLine),
            cellsPerLine
        )
        val maxRows = ((IMAGE_MAX_HEIGHT - IMAGE_PADDING * 2 - bandSpace) / rowPitch)
            .toInt().coerceAtLeast(1)
        val truncated = lines.size > maxRows
        val rows = lines.size.coerceAtMost(maxRows).coerceAtLeast(1)
        // 最後一行的下緣：不是「行數 × 行距」，那會多出一段沒格子的空白。
        val bodyHeight = rows * rowPitch - (rowPitch - cell)
        val height = (bodyHeight + bandSpace).toInt() + IMAGE_PADDING * 2
        val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            drawPageBackground(canvas, theme, IMAGE_WIDTH, height)
            canvas.save()
            canvas.translate(IMAGE_PADDING.toFloat(), IMAGE_PADDING.toFloat())
            if (before && band != null && card != null) {
                drawCardBand(canvas, band, card, contentWidth.toFloat(), 0f, theme.guide)
            }
            canvas.save()
            canvas.translate(0f, if (before) bandSpace else 0f)
            canvas.clipRect(0f, 0f, contentWidth.toFloat(), bodyHeight)
            drawRows(
                canvas = canvas,
                paint = paint,
                rows = lines.take(rows),
                top = 0f,
                cell = cell,
                cellsPerLine = cellsPerLine,
                guide = guide,
                color = theme.guide
            )
            canvas.restore()
            if (!before && band != null && card != null) {
                drawCardBand(
                    canvas = canvas,
                    band = band,
                    card = card,
                    width = contentWidth.toFloat(),
                    top = bodyHeight + cardGap(paint),
                    guideColor = theme.guide
                )
            }
            canvas.restore()
            return PngResult(compress(bitmap), truncated)
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * 直排稿紙：一列一列往**左**長，不再向下換頁。
     *
     * 直排本來就是由右往左讀，「下一頁」接在左邊才順——往下堆疊會讓人以為是另一段。
     * 所以圖片是一條長卷：欄位愈多就愈寬，寬到 [IMAGE_MAX_WIDTH] 才截。
     * 名片是落款，接在最後一欄的左邊、同一條頂線上，跟著往左收尾。
     */
    private fun renderVerticalCells(
        text: String,
        guide: WritingGuide,
        paint: TextPaint,
        cell: Float,
        card: ExportCard?,
        theme: ExportTheme
    ): PngResult {
        val rows = VERTICAL_IMAGE_ROWS
        val all = packCellLines(
            cellAtoms(text.ifEmpty { " " }, paint, cell, rows),
            rows
        )
        val columnWidth = cell * COLUMN_PITCH_EM
        val charStep = cell * CHAR_ADVANCE_EM
        val band = card?.let { measureVerticalCardBand(it, paint) }
        val cardGap = if (band != null) cell * VERTICAL_CARD_GAP_EM else 0f
        val bandWidth = if (band != null) cardGap + band.width else 0f

        // 寬度上限：正文與名片都要放得下，放不下就先捨掉最左邊（也就是最後）的欄。
        val limitWidth = IMAGE_MAX_WIDTH - IMAGE_PADDING * 2
        var columns = all
        var truncated = false
        if (columns.size.coerceAtLeast(1) * columnWidth + bandWidth > limitWidth) {
            val keep = floor((limitWidth - bandWidth) / columnWidth).toInt().coerceAtLeast(1)
            if (columns.size > keep) {
                columns = columns.take(keep)
                truncated = true
            }
        }

        val bodyWidth = columns.size.coerceAtLeast(1) * columnWidth
        // 名片接在正文「左邊」，所以是相加，不是取大：取大會把落款畫到圖外。
        val contentW = (bodyWidth + bandWidth).toInt().coerceAtLeast(1)
        val width = contentW + IMAGE_PADDING * 2

        // 正文與名片在同一條頂線上，圖高就看誰高。
        val usedRows = (columns.maxOfOrNull { it.usedCells() } ?: 0).coerceIn(1, rows)
        // 每格高一格字身、字往下走 1.2em，所以最後一格的下緣要再收回 0.2em。
        val bodyHeight = usedRows * charStep - (charStep - cell)
        val height = maxOf(bodyHeight, band?.height ?: 0f).toInt() + IMAGE_PADDING * 2

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            drawPageBackground(canvas, theme, width, height)
            canvas.save()
            canvas.translate(IMAGE_PADDING.toFloat(), IMAGE_PADDING.toFloat())
            // 正文靠右：contentW 可能被名片撐寬，右邊界仍然是同一條。
            drawColumns(
                canvas = canvas,
                paint = paint,
                columns = columns,
                areaRight = contentW.toFloat(),
                top = 0f,
                cell = cell,
                guide = guide,
                color = theme.guide,
                maxRows = rows
            )
            if (band != null && card != null) {
                val bandRight = contentW - bodyWidth - cardGap
                // 一條細豎線把落款與正文分開；直排的分隔是豎的，和閱讀方向一致。
                canvas.drawLine(
                    bandRight + cardGap / 2f,
                    0f,
                    bandRight + cardGap / 2f,
                    height - IMAGE_PADDING * 2f,
                    guidePaint(theme.guide)
                )
                drawVerticalCardBand(
                    canvas = canvas,
                    band = band,
                    card = card,
                    left = bandRight - band.width,
                    top = 0f
                )
            }
            canvas.restore()
            return PngResult(compress(bitmap), truncated)
        } finally {
            bitmap.recycle()
        }
    }

    private fun renderPngFlow(
        text: String,
        guide: WritingGuide,
        paint: TextPaint,
        card: ExportCard?,
        theme: ExportTheme
    ): PngResult {
        val contentWidth = IMAGE_WIDTH - IMAGE_PADDING * 2
        val body = text.ifEmpty { " " }
        val layout = newLayout(body, 0, body.length, paint, contentWidth, 1.2f, 6f)
        val band = card?.let { measureCardBand(it, paint, contentWidth) }
        val bandSpace = band?.let { cardGap(paint) + it.height } ?: 0f
        val before = band != null && card?.position == CardPosition.BEFORE

        // 名片一定要留得下：先扣掉它的高度，再決定正文能放多少（正文被截才算 truncated）。
        val bodyLimit = (IMAGE_MAX_HEIGHT - IMAGE_PADDING * 2 - bandSpace).toFloat()
        val bodyHeight = layout.height.toFloat().coerceAtMost(bodyLimit)
        val truncated = layout.height > bodyHeight
        val height = (bodyHeight + bandSpace).toInt() + IMAGE_PADDING * 2
        val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            drawPageBackground(canvas, theme, IMAGE_WIDTH, height)
            canvas.save()
            canvas.translate(IMAGE_PADDING.toFloat(), IMAGE_PADDING.toFloat())
            if (before && band != null && card != null) {
                drawCardBand(canvas, band, card, contentWidth.toFloat(), 0f, theme.guide)
            }
            canvas.save()
            canvas.translate(0f, if (before) bandSpace else 0f)
            canvas.clipRect(0f, 0f, contentWidth.toFloat(), bodyHeight)
            drawFlowGuides(
                canvas = canvas,
                paint = paint,
                layout = layout,
                guide = guide,
                width = contentWidth.toFloat(),
                height = bodyHeight,
                cell = cellWidth(paint),
                color = theme.guide
            )
            layout.draw(canvas)
            canvas.restore()
            if (!before && band != null && card != null) {
                drawCardBand(
                    canvas = canvas,
                    band = band,
                    card = card,
                    width = contentWidth.toFloat(),
                    top = bodyHeight + cardGap(paint),
                    guideColor = theme.guide
                )
            }
            canvas.restore()
            return PngResult(compress(bitmap), truncated)
        } finally {
            bitmap.recycle()
        }
    }

    // ------------------------------------------------------- 名片帶

    /**
     * 量好的名片帶：左邊圓形頭像、右邊三行資料。
     *
     * 兩邊不出現在同一條基線上——頭像 2.4 字高、三行資料 3.5 字高，本來就不等高，
     * 所以各自在自己的高度裡置中，共用同一條**水平中線**。整組靠左貼齊正文左邊界，
     * 名片是落款，不置中也不撐滿整行。
     *
     * [height] 已經包含分隔線上方那一小段留白，畫的時候直接從 `top` 開始即可。
     */
    private class CardBand(
        val height: Float,
        val avatarSize: Float,
        val avatarGap: Float,
        val layout: StaticLayout,
        val paint: TextPaint
    )

    /** 名片帶與正文之間的空隙；用字號的比例，換字號時節奏不變。 */
    private fun cardGap(paint: TextPaint): Float = paint.textSize * 0.8f

    /** 名片字級：比正文小一級，它附在正文旁邊，不該搶正文的份量。 */
    private const val CARD_TEXT_SCALE = 0.78f

    /** 橫排頭像大小（相對名片字級）：比三行資料矮一頭，中線才對得起來又不會喧賓奪主。 */
    private const val CARD_AVATAR_EM = 2.4f

    private fun measureCardBand(card: ExportCard, paint: TextPaint, width: Int): CardBand? {
        if (card.isEmpty) return null
        val cardPaint = TextPaint(paint).apply { textSize = paint.textSize * CARD_TEXT_SCALE }
        val hasAvatar = card.avatarPath != null
        val avatarSize = if (hasAvatar) cardPaint.textSize * CARD_AVATAR_EM else 0f
        val avatarGap = if (hasAvatar) cardPaint.textSize * 0.7f else 0f
        val textWidth = (width - avatarSize - avatarGap).toInt().coerceAtLeast(1)
        // 一項一行：暱稱、信箱、GitHub 各佔一行，靠折行位置去湊反而會把網址斷開。
        val content = card.text
        val layout = newLayout(content, 0, content.length, cardPaint, textWidth, 1.15f, 3f)
        val lead = cardPaint.textSize * 1.1f
        val contentHeight = maxOf(avatarSize, layout.height.toFloat())
        return CardBand(lead + contentHeight, avatarSize, avatarGap, layout, cardPaint)
    }

    private fun drawCardBand(
        canvas: Canvas,
        band: CardBand,
        card: ExportCard,
        width: Float,
        top: Float,
        guideColor: Int
    ) {
        // 一條髮絲線把名片與正文分開；用的是稿紙格線同一個顏色，整張圖才是一套。
        val lead = band.paint.textSize * 1.1f
        canvas.drawLine(0f, top + lead * 0.2f, width, top + lead * 0.2f, guidePaint(guideColor))
        val contentTop = top + lead
        val contentHeight = band.height - lead
        // 頭像與右邊那三行資料各自在自己的高度裡置中：一邊貼頂就成了錯位。
        if (band.avatarSize > 0f && card.avatarPath != null) {
            drawAvatar(
                canvas = canvas,
                path = card.avatarPath,
                left = 0f,
                top = contentTop + (contentHeight - band.avatarSize) / 2f,
                size = band.avatarSize
            )
        }
        canvas.save()
        canvas.translate(
            band.avatarSize + band.avatarGap,
            contentTop + (contentHeight - band.layout.height) / 2f
        )
        band.layout.draw(canvas)
        canvas.restore()
    }

    // ------------------------------------------------ 名片帶（直排）

    /**
     * 直排用的名片帶：**上面頭像、下面三欄資料**，資料一行變一欄，從右往左讀。
     *
     * 是橫排名片的直排版：橫排是「左頭像、右三行」，轉九十度就成了「上頭像、下三欄」。
     * 頭像與資料塊共用同一條**豎中線**，整組貼著頂線，不隨正文長短上下浮動。
     */
    private class VerticalCardBand(
        val width: Float,
        val height: Float,
        val avatarSize: Float,
        val avatarGap: Float,
        val lines: List<String>,
        val paint: TextPaint,
        /** 每個字的步進（直排沒有行距，只有字距）。 */
        val step: Float,
        val columnWidth: Float
    )

    private fun measureVerticalCardBand(card: ExportCard, paint: TextPaint): VerticalCardBand? {
        val lines = card.lines.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return null
        val cardPaint = TextPaint(paint).apply {
            textSize = paint.textSize * VERTICAL_CARD_TEXT_SCALE
        }
        val step = cardPaint.textSize * VERTICAL_CARD_STEP_EM
        val columnWidth = cardPaint.textSize * VERTICAL_CARD_COLUMN_EM
        val longest = lines.maxOf { it.codePointCount(0, it.length) }
        val textHeight = longest * step
        val textWidth = lines.size * columnWidth
        val avatarSize = if (card.avatarPath != null) {
            cardPaint.textSize * VERTICAL_CARD_AVATAR_EM
        } else {
            0f
        }
        val avatarGap = if (avatarSize > 0f) {
            cardPaint.textSize * VERTICAL_CARD_AVATAR_GAP_EM
        } else {
            0f
        }
        // 名片寬度取兩者較寬的一邊：頭像比三欄窄時，由資料決定寬度，兩邊再各自置中。
        return VerticalCardBand(
            width = maxOf(textWidth, avatarSize),
            height = avatarSize + avatarGap + textHeight,
            avatarSize = avatarSize,
            avatarGap = avatarGap,
            lines = lines,
            paint = cardPaint,
            step = step,
            columnWidth = columnWidth
        )
    }

    /**
     * 直排落款：頭像在上、一行資料一欄在下面從右往左排，共用一條豎中線。
     * 呼叫端只負責給左上角與那條分隔豎線。
     */
    private fun drawVerticalCardBand(
        canvas: Canvas,
        band: VerticalCardBand,
        card: ExportCard,
        left: Float,
        top: Float
    ) {
        val paint = band.paint
        val metrics = paint.fontMetrics
        val glyphHeight = metrics.descent - metrics.ascent
        val align = paint.textAlign
        val textWidth = band.lines.size * band.columnWidth

        if (band.avatarSize > 0f && card.avatarPath != null) {
            drawAvatar(
                canvas = canvas,
                path = card.avatarPath,
                left = left + (band.width - band.avatarSize) / 2f,
                top = top,
                size = band.avatarSize
            )
        }

        // 資料塊在頭像下方，自己也在名片寬度裡置中：頭像比三欄窄時，中線才會是同一條。
        val textTop = top + band.avatarSize + band.avatarGap
        var columnRight = left + (band.width + textWidth) / 2f
        band.lines.forEach { line ->
            val columnLeft = columnRight - band.columnWidth
            val center = (columnLeft + columnRight) / 2f
            var y = textTop
            var index = 0
            while (index < line.length) {
                val codePoint = line.codePointAt(index)
                val glyph = String(Character.toChars(codePoint))
                index += Character.charCount(codePoint)
                if (rotatesInVertical(codePoint)) {
                    // 英數與直排標點同一個規則：轉九十度，字頂朝右。
                    canvas.save()
                    canvas.translate(
                        center + (metrics.ascent + metrics.descent) / 2f,
                        y
                    )
                    canvas.rotate(90f)
                    paint.textAlign = Paint.Align.LEFT
                    canvas.drawText(glyph, 0f, 0f, paint)
                    canvas.restore()
                } else {
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(
                        glyph,
                        center,
                        y + (band.step - glyphHeight) / 2f - metrics.ascent,
                        paint
                    )
                }
                y += band.step
            }
            columnRight = columnLeft
        }
        paint.textAlign = align
    }

    /** 圓形頭像：置中裁成正方形再裁圓，避免人像被拉扁。 */
    private fun drawAvatar(canvas: Canvas, path: String, left: Float, top: Float, size: Float) {
        val bitmap = runCatching { BitmapFactory.decodeFile(path) }.getOrNull() ?: return
        try {
            val side = minOf(bitmap.width, bitmap.height)
            val src = Rect(
                (bitmap.width - side) / 2,
                (bitmap.height - side) / 2,
                (bitmap.width + side) / 2,
                (bitmap.height + side) / 2
            )
            val dst = RectF(left, top, left + size, top + size)
            canvas.save()
            canvas.clipPath(Path().apply { addOval(dst, Path.Direction.CW) })
            canvas.drawBitmap(bitmap, src, dst, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            canvas.restore()
        } finally {
            bitmap.recycle()
        }
    }


    private fun compress(bitmap: Bitmap): ByteArray = ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.toByteArray()
    }

    /**
     * 鋪一張圖的底：先填底色，有背景圖就等比裁切鋪滿。
     *
     * 裁切方式與編輯頁的 `ContentScale.Crop` 一致：取短邊放大到蓋滿整張圖，
     * 多出來的部分左右（或上下）對稱切掉，所以匯出的圖不會出現變形的臉或拉長的紙紋。
     */
    private fun drawPageBackground(canvas: Canvas, theme: ExportTheme, width: Int, height: Int) {
        canvas.drawColor(theme.background)
        val image = theme.backgroundImage ?: return
        if (image.isRecycled || image.width <= 0 || image.height <= 0) return
        val scale = max(
            width.toFloat() / image.width,
            height.toFloat() / image.height
        )
        val drawWidth = image.width * scale
        val drawHeight = image.height * scale
        val left = (width - drawWidth) / 2f
        val top = (height - drawHeight) / 2f
        val src = Rect(0, 0, image.width, image.height)
        val dst = RectF(left, top, left + drawWidth, top + drawHeight)
        canvas.drawBitmap(
            image,
            src,
            dst,
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        )
    }

    // ------------------------------------------------------- 稿紙模型：畫

    /**
     * 橫排的一行一行。
     *
     * 格線畫在字底下：橫線整份共用（貫穿整行），豎線只畫到有字的格數——
     * 這是編輯器的畫法（見 `buildGridLines`），兩邊才會長得一樣。
     * 英文單詞在自己的 N 格裡居中，不會被豎線切開。
     */
    private fun drawRows(
        canvas: Canvas,
        paint: TextPaint,
        rows: List<List<CellAtom>>,
        top: Float,
        cell: Float,
        cellsPerLine: Int,
        guide: WritingGuide,
        color: Int
    ) {
        // cell 是字身（1em）。格子比字寬（1.3em）、行距比格高（1.4em），
        // 多出來的 0.4em 就是行與行之間的空白——和編輯頁同一組數字。
        val cellWidth = cell * CELL_PITCH_EM
        val rowPitch = cell * ROW_PITCH_EM
        val metrics = paint.fontMetrics
        val glyphHeight = metrics.descent - metrics.ascent
        val linePaint = guidePaint(color)
        val lineWidth = cellsPerLine * cellWidth
        val align = paint.textAlign
        rows.forEachIndexed { index, atoms ->
            val y = top + index * rowPitch
            val baseline = y + (cell - glyphHeight) / 2f - metrics.ascent
            if (guide == WritingGuide.GRID) {
                // 作文紙整行鋪滿：行尾沒寫字的格子照樣在，不是只畫到最後一個字。
                for (boundary in 0..cellsPerLine) {
                    val x = boundary * cellWidth
                    canvas.drawLine(x, y, x, y + cell, linePaint)
                }
                canvas.drawLine(0f, y, lineWidth, y, linePaint)
                canvas.drawLine(0f, y + cell, lineWidth, y + cell, linePaint)
            } else if (guide == WritingGuide.RULED) {
                // 分隔線貼在格底：字下面是格子下緣，不是行框底。
                canvas.drawLine(0f, y + cell, lineWidth, y + cell, linePaint)
            }
            var column = 0
            atoms.forEach { atom ->
                val span = atom.cells.coerceAtLeast(1)
                paint.textAlign = Paint.Align.LEFT
                val width = paint.measureText(atom.text)
                val offset = column * cellWidth + (span * cellWidth - width) / 2f
                canvas.drawText(atom.text, offset, baseline, paint)
                column += span
            }
        }
        paint.textAlign = align
    }

    /**
     * 直排的一欄一欄，由右往左。
     *
     * 漢字原地直立、英數整串旋轉九十度——這是中文直排的慣例；
     * 全部轉九十度會變成「躺著讀」，全部直立又會把英文單詞拆散。
     * 分隔線畫在**欄的右邊界**：欄寬 1.3em、字身 1em，邊界正好是兩列字的中間。
     */
    private fun drawColumns(
        canvas: Canvas,
        paint: TextPaint,
        columns: List<List<CellAtom>>,
        areaRight: Float,
        top: Float,
        cell: Float,
        guide: WritingGuide,
        color: Int,
        /** 一欄最多幾格；格紙要鋪到這裡才算「一行」。 */
        maxRows: Int
    ) {
        if (columns.isEmpty()) return
        // cell 是字身（1em）：欄距 1.72em、格子寬 1.3em、字往下的步進 1.2em。
        // 三個數字都跟編輯頁同源，直排貼在一起才像同一張紙。
        val columnPitch = cell * COLUMN_PITCH_EM
        val cellWidth = cell * VERTICAL_CELL_WIDTH_EM
        val charStep = cell * CHAR_ADVANCE_EM
        val metrics = paint.fontMetrics
        val glyphHeight = metrics.descent - metrics.ascent
        val linePaint = guidePaint(color)
        val align = paint.textAlign
        // 一頁的欄位共用同一個高度：哪一欄短了就自己縮，整片格紙會參差不齊。
        val rows = columns.maxOfOrNull { it.usedCells() }?.coerceIn(1, maxRows) ?: 1
        val height = rows * charStep - (charStep - cell)
        val blockRight = areaRight
        val blockLeft = blockRight - columns.size * columnPitch

        if (guide == WritingGuide.GRID) {
            // 格子不貼欄邊：欄寬 1.72em、格子寬 1.3em，兩側各留 0.21em，
            // 欄與欄之間因此有 0.42em 的空白——這就是直排的「行間隔」。
            val inset = (columnPitch - cellWidth) / 2f
            for (index in 0 until columns.size) {
                val right = blockRight - index * columnPitch
                val left = right - columnPitch
                val cellLeft = left + inset
                val cellRight = right - inset
                canvas.drawLine(cellLeft, top, cellLeft, top + height, linePaint)
                canvas.drawLine(cellRight, top, cellRight, top + height, linePaint)
                // 每格自己一條上緣、一條下緣：格高 1em，字與字之間留 0.2em。
                for (row in 0 until rows) {
                    val y = top + row * charStep
                    canvas.drawLine(cellLeft, y, cellRight, y, linePaint)
                    canvas.drawLine(cellLeft, y + cell, cellRight, y + cell, linePaint)
                }
            }
        } else if (guide == WritingGuide.RULED) {
            for (index in 0 until columns.size) {
                val x = blockRight - index * columnPitch
                canvas.drawLine(x, top, x, top + height, linePaint)
            }
        }

        columns.forEachIndexed { index, atoms ->
            val right = blockRight - index * columnPitch
            val left = right - columnPitch
            var row = 0
            atoms.forEach { atom ->
                val rotated = rotatesInVertical(atom.text.codePointAt(0))
                if (rotated) {
                    // 旋轉九十度後，本地 +x 就是螢幕向下、本地 +y 指向螢幕左邊；
                    // 所以要讓字身在欄中央，基準線得往右平移 (ascent + descent) / 2。
                    canvas.save()
                    canvas.translate(
                        (left + right) / 2f + (metrics.ascent + metrics.descent) / 2f,
                        top + row * charStep
                    )
                    canvas.rotate(90f)
                    paint.textAlign = Paint.Align.LEFT
                    canvas.drawText(atom.text, 0f, 0f, paint)
                    canvas.restore()
                } else {
                    paint.textAlign = Paint.Align.CENTER
                    val baseline = top + row * charStep +
                        (cell - glyphHeight) / 2f - metrics.ascent
                    canvas.drawText(atom.text, (left + right) / 2f, baseline, paint)
                }
                row += atom.cells.coerceAtLeast(1)
            }
        }
        paint.textAlign = align
    }

    // ------------------------------------------------------- 散文：分隔線

    /**
     * 散文模式只畫分隔線（方格走稿紙模型）。
     *
     * 位置取「基線往下 0.14 格」，不是行框底部：行框底部離字還有一截 leading，
     * 線會浮在字下面很遠的地方。這個比例與編輯器同源。
     */
    private fun drawFlowGuides(
        canvas: Canvas,
        paint: TextPaint,
        layout: StaticLayout,
        guide: WritingGuide,
        width: Float,
        height: Float,
        cell: Float,
        color: Int
    ) {
        if (guide != WritingGuide.RULED) return
        val linePaint = guidePaint(color)
        val gap = cell * BASELINE_TO_BOTTOM
        for (line in 0 until layout.lineCount) {
            val y = layout.getLineBaseline(line) + gap
            if (y >= 0f && y <= height) canvas.drawLine(0f, y, width, y, linePaint)
        }
    }

    // ------------------------------------------------------------- 共用零件

    private fun guidePaint(color: Int) = Paint().apply {
        this.color = color
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    /** 一個漢字的真實步進；量不到就退回字號。格寬與行距都以它為準。 */
    private fun cellWidth(paint: TextPaint): Float {
        val measured = paint.measureText("国")
        return if (measured > 0f) measured else paint.textSize
    }

    private fun newLayout(
        body: String,
        start: Int,
        end: Int,
        paint: TextPaint,
        width: Int,
        mult: Float,
        add: Float
    ): StaticLayout = StaticLayout.Builder
        .obtain(body, start, end, paint, width)
        .setLineSpacing(add, mult)
        .setIncludePad(false)
        .build()

    // ------------------------------------------------------------- 稿紙原子

    /**
     * 把純文字切成稿紙原子。
     *
     * 漢字／標點各佔一格；[isAsciiWordChar] 連續的字元合成一個單詞，
     * 佔 `ceil(單詞寬 / 格寬)` 格；超過一列長度就切成幾段，
     * 免得一個長網址把整列撐爆。
     */
    private fun cellAtoms(
        text: String,
        paint: TextPaint,
        cell: Float,
        maxCells: Int
    ): List<CellAtom> {
        val atoms = ArrayList<CellAtom>()
        var index = 0
        while (index < text.length) {
            val code = text.codePointAt(index)
            val size = Character.charCount(code)
            if (code == '\n'.code) {
                atoms.add(CellAtom.Break)
                index += size
                continue
            }
            if (isAsciiWordChar(code)) {
                val word = StringBuilder()
                var cursor = index
                while (cursor < text.length) {
                    val c = text.codePointAt(cursor)
                    if (!isAsciiWordChar(c)) break
                    word.appendCodePoint(c)
                    cursor += Character.charCount(c)
                }
                atoms.addAll(splitWord(word.toString(), paint, cell, maxCells))
                index = cursor
            } else {
                atoms.add(CellAtom(String(Character.toChars(code)), 1))
                index += size
            }
        }
        return atoms
    }

    /** 只把 ASCII 字母數字（含 `'`、`-`）當成可連寫的單詞；全角英文與中文一律各自佔一格。 */
    private fun isAsciiWordChar(code: Int): Boolean =
        code < 0x2E80 && (Character.isLetterOrDigit(code) ||
            code == '\''.code || code == '-'.code)

    private fun cellsOf(text: String, paint: TextPaint, cell: Float): Int =
        ceil(paint.measureText(text) / cell).toInt().coerceAtLeast(1)

    /** 太長的單詞按字切段：每段都塞得進一列（`maxCells` 格）。 */
    private fun splitWord(
        word: String,
        paint: TextPaint,
        cell: Float,
        maxCells: Int
    ): List<CellAtom> {
        val total = cellsOf(word, paint, cell)
        if (total <= maxCells) return listOf(CellAtom(word, total))
        val pieces = ArrayList<CellAtom>()
        val buffer = StringBuilder()
        var bufferCells = 0
        word.forEach { ch ->
            val candidate = buffer.toString() + ch
            val cells = cellsOf(candidate, paint, cell)
            if (cells > maxCells && buffer.isNotEmpty()) {
                pieces.add(CellAtom(buffer.toString(), bufferCells))
                buffer.setLength(0)
                buffer.append(ch)
                bufferCells = cellsOf(buffer.toString(), paint, cell)
            } else {
                buffer.setLength(0)
                buffer.append(candidate)
                bufferCells = cells
            }
        }
        if (buffer.isNotEmpty()) pieces.add(CellAtom(buffer.toString(), bufferCells))
        return pieces
    }
}

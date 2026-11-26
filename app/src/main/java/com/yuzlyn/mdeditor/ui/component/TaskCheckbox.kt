package com.yuzlyn.mdeditor.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.yuzlyn.mdeditor.data.note.TaskLine
import kotlin.math.roundToInt

/** 方框的可視尺寸與觸控區。行內控制項搆不到 48dp，40dp 是可點又不會壓到上下行的上限。 */
private val BOX_SIZE = 18.dp
private val TOUCH_SIZE = 40.dp

/**
 * 把 `- [ ]` 這段語法藏起來，並把已勾選那行變灰、加刪除線。
 *
 * 只加 SpanStyle、不改字串長度，所以位移對映可以用 [OffsetMapping.Identity]：
 * 游標、選取、輸入法組字都還對得回原始文字，不必自己維護一張容易錯的換算表。
 *
 * 方框本身不是文字，由 [TaskCheckboxOverlay] 畫在同樣的位置上——文字藏起來但保留寬度，
 * 版面因此不會因為勾選而跳動。
 */
class TaskListTransformation(
    private val tasks: List<TaskLine>,
    /** 已勾選那行的文字色，通常是 `onSurfaceVariant` 再淡一點。 */
    private val checkedColor: Color
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        if (tasks.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val builder = AnnotatedString.Builder(text)
        for (task in tasks) {
            // 語法本體（`- ` 與 `[ ]`）隱形；後面那個空白留著，當作方框與文字的間距。
            builder.addStyle(
                SpanStyle(color = Color.Transparent),
                task.lineStart.coerceIn(0, text.length),
                task.boxEnd.coerceIn(0, text.length)
            )
            if (task.checked) {
                builder.addStyle(
                    SpanStyle(
                        color = checkedColor,
                        textDecoration = TextDecoration.LineThrough
                    ),
                    task.contentStart.coerceIn(0, text.length),
                    task.lineEnd.coerceIn(0, text.length)
                )
            }
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

/**
 * 把方框畫在每一行待辦的語法位置上。
 *
 * 位置從 [layout] 算：`[` 與 `]` 的 bounding box 取中點，正好是藏起來的 `[ ]` 的位置。
 * 點整塊（含 40dp 觸控區）就翻狀態，並帶 `Role.Checkbox`，TalkBack 會唸出勾選與否。
 */
@Composable
fun TaskCheckboxOverlay(
    layout: TextLayoutResult,
    tasks: List<TaskLine>,
    checkedColor: Color,
    onToggle: (TaskLine) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tasks.isEmpty()) return
    val density = LocalDensity.current
    val touchPx = with(density) { TOUCH_SIZE.toPx() }
    val textLength = layout.layoutInput.text.length
    val borderColor = MaterialTheme.colorScheme.onSurfaceVariant
    val fillColor = MaterialTheme.colorScheme.primary
    val markColor = MaterialTheme.colorScheme.onPrimary

    Box(modifier) {
        tasks.forEach { task ->
            if (task.boxEnd > textLength) return@forEach
            val left = layout.getBoundingBox(task.boxStart.coerceIn(0, textLength - 1))
            val right = layout.getBoundingBox((task.boxEnd - 1).coerceIn(0, textLength - 1))
            val centerX = (left.left + right.right) / 2f
            val centerY = (left.top + left.bottom) / 2f
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (centerX - touchPx / 2f).roundToInt(),
                            (centerY - touchPx / 2f).roundToInt()
                        )
                    }
                    .size(TOUCH_SIZE)
                    .toggleable(
                        value = task.checked,
                        role = Role.Checkbox,
                        onValueChange = { onToggle(task) }
                    )
                    .padding((TOUCH_SIZE - BOX_SIZE) / 2),
                contentAlignment = Alignment.Center
            ) {
                TaskCheckMark(
                    checked = task.checked,
                    borderColor = borderColor,
                    fillColor = fillColor,
                    markColor = markColor
                )
            }
        }
    }
}

/**
 * 一個小方框：未勾選是 2dp 描邊，勾選後填主色並畫出勾。
 *
 * 顏色與勾的出現都走 effects spring——這是狀態變化，不該彈跳；
 * 尺寸不變，所以同一行的文字不會因此重排。
 */
@Composable
private fun TaskCheckMark(
    checked: Boolean,
    borderColor: Color,
    fillColor: Color,
    markColor: Color
) {
    val motion = MaterialTheme.motionScheme
    val fill by animateColorAsState(
        targetValue = if (checked) fillColor else Color.Transparent,
        animationSpec = motion.defaultEffectsSpec(),
        label = "task-fill"
    )
    val stroke by animateColorAsState(
        targetValue = if (checked) fillColor else borderColor,
        animationSpec = motion.defaultEffectsSpec(),
        label = "task-stroke"
    )
    val mark by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = motion.fastEffectsSpec(),
        label = "task-mark"
    )

    Canvas(modifier = Modifier.size(BOX_SIZE)) {
        val radius = CornerRadius(size.minDimension * 0.22f)
        drawRoundRect(color = fill, cornerRadius = radius)
        drawRoundRect(
            color = stroke,
            cornerRadius = radius,
            style = Stroke(width = size.minDimension * 0.12f)
        )
        if (mark > 0.01f) {
            val w = size.width
            val h = size.height
            val pathEffect = PathEffect.cornerPathEffect(size.minDimension * 0.12f)
            drawLine(
                color = markColor.copy(alpha = mark),
                start = Offset(w * 0.27f, h * 0.52f),
                end = Offset(w * 0.44f, h * 0.69f),
                strokeWidth = size.minDimension * 0.14f,
                cap = StrokeCap.Round,
                pathEffect = pathEffect
            )
            drawLine(
                color = markColor.copy(alpha = mark),
                start = Offset(w * 0.44f, h * 0.69f),
                end = Offset(w * 0.75f, h * 0.32f),
                strokeWidth = size.minDimension * 0.14f,
                cap = StrokeCap.Round,
                pathEffect = pathEffect
            )
        }
    }
}

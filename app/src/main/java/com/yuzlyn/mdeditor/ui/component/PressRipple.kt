package com.yuzlyn.mdeditor.ui.component

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.sqrt

/**
 * 自繪按壓回縮與波紋。
 *
 * Material3 1.5 的 ripple 交給原生 RippleDrawable（RippleHostView）繪製，
 * 外層 `Modifier.clip(shape)` 裁不到它，按住卡片或底欄時四角會冒出方形遮罩。
 * 這裡改成純 Compose 繪製，並在 draw 內用 shape 明確裁切，
 * 遮罩形狀因此一定跟著元件圓角，不依賴外層 clip 是否生效。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Modifier.pressRipple(
    interactionSource: InteractionSource,
    shape: Shape,
    color: Color = MaterialTheme.colorScheme.onSurface,
    pressedAlpha: Float = 0.1f,
    /** 回縮比例；卡片沿用較含蓄的預設。 */
    pressedScale: Float = 0.98f,
    /**
     * 是否由這支 modifier 負責縮放。
     *
     * 頭像要跟頂欄／側欄圓鈕同一套「最短保持 140ms」的節奏，所以關掉這裡的縮放，
     * 改由 `pressScale` 負責；否則快速點一下同樣看不到縮。
     */
    animateScale: Boolean = true,
    /**
     * 縮放至少保持這麼久才回彈；0 表示抬手就回。
     *
     * 卡片和圓鈕共用同一段節奏（`MIN_PRESS_HOLD_MS`）：快速點按的按下→抬起
     * 只有 50–80ms，彈簧還沒走完就反向，使用者會覺得「點了沒反應」。
     */
    minPressHoldMs: Long = 0L
): Modifier {
    val state = remember { PressRippleState() }
    val motion = MaterialTheme.motionScheme
    val pressedAt = remember { mutableLongStateOf(0L) }
    val scaleReturn = remember { mutableStateOf<Job?>(null) }

    // drawWithContent 直接讀 Animatable 不一定會觸發重繪；把動畫值同步到
    // mutableFloatState，繪製階段訂閱的是明確的 snapshot state。
    LaunchedEffect(state) {
        launch { snapshotFlow { state.alpha.value }.collect { state.alphaDraw.floatValue = it } }
        launch { snapshotFlow { state.radius.value }.collect { state.radiusDraw.floatValue = it } }
        launch { snapshotFlow { state.scale.value }.collect { state.scaleDraw.floatValue = it } }
    }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressedAt.longValue = SystemClock.uptimeMillis()
                    // 連點時先取消上一次排隊的回彈，否則它會在保持時間到點時把新的按下彈回去。
                    scaleReturn.value?.cancel()
                    scaleReturn.value = null
                    state.pressPosition.value = interaction.pressPosition
                    state.radius.snapTo(0f)
                    state.alpha.snapTo(0f)
                    launch { state.alpha.animateTo(pressedAlpha, motion.fastEffectsSpec()) }
                    // 小元件以 spatial spring 回縮；透明度只走 effects，避免過衝。
                    if (animateScale) {
                        launch { state.scale.animateTo(pressedScale, motion.fastSpatialSpec()) }
                    }
                    launch {
                        state.radius.animateTo(
                            targetValue = 1f,
                            animationSpec = motion.defaultSpatialSpec()
                        )
                    }
                }

                is PressInteraction.Release -> {
                    if (animateScale) {
                        val remaining =
                            minPressHoldMs - (SystemClock.uptimeMillis() - pressedAt.longValue)
                        scaleReturn.value?.cancel()
                        scaleReturn.value = launch {
                            if (remaining > 0) delay(remaining)
                            state.scale.animateTo(1f, motion.fastSpatialSpec())
                        }
                    }
                    // 波紋照常立刻收；只有縮放需要多留一下。
                    launch {
                        state.alpha.animateTo(0f, motion.fastEffectsSpec())
                    }
                }

                is PressInteraction.Cancel -> {
                    // 手指出界或手勢被系統接手：立即回彈，別讓卡片停在縮小狀態。
                    scaleReturn.value?.cancel()
                    if (animateScale) {
                        scaleReturn.value = launch {
                            state.scale.animateTo(1f, motion.fastSpatialSpec())
                        }
                    }
                    launch {
                        state.alpha.animateTo(0f, motion.fastEffectsSpec())
                    }
                }
            }
        }
    }

    return graphicsLayer {
        val scale = if (animateScale) state.scaleDraw.floatValue else 1f
        scaleX = scale
        scaleY = scale
    }.drawWithContent {
        drawContent()
        val alpha = state.alphaDraw.floatValue
        if (alpha <= 0.001f || size.isEmpty()) return@drawWithContent
        val origin = state.pressPosition.value.takeIf { it != Offset.Unspecified } ?: center
        val dx = max(origin.x, size.width - origin.x)
        val dy = max(origin.y, size.height - origin.y)
        val reach = sqrt(dx * dx + dy * dy)
        val outline = shape.createOutline(size, layoutDirection, this)
        clipPath(Path().apply { addOutline(outline) }) {
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = reach * state.radiusDraw.floatValue,
                center = origin
            )
        }
    }
}

private class PressRippleState {
    val alpha = Animatable(0f)
    val radius = Animatable(0f)
    val scale = Animatable(1f)
    val alphaDraw = mutableFloatStateOf(0f)
    val radiusDraw = mutableFloatStateOf(0f)
    val scaleDraw = mutableFloatStateOf(1f)
    val pressPosition = mutableStateOf(Offset.Unspecified)
}

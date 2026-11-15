package com.yuzlyn.mdeditor.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.sqrt

/**
 * 自繪按壓波紋。
 *
 * Material3 1.5 的 ripple 交給原生 RippleDrawable（RippleHostView）繪製，
 * 外層 `Modifier.clip(shape)` 裁不到它，按住卡片或底欄時四角會冒出方形遮罩。
 * 這裡改成純 Compose 繪製，並在 draw 內用 shape 明確裁切，
 * 遮罩形狀因此一定跟著元件圓角，不依賴外層 clip 是否生效。
 */
@Composable
fun Modifier.pressRipple(
    interactionSource: InteractionSource,
    shape: Shape,
    color: Color = MaterialTheme.colorScheme.onSurface,
    pressedAlpha: Float = 0.1f
): Modifier {
    val state = remember { PressRippleState() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    state.pressPosition = interaction.pressPosition
                    state.radius.snapTo(0f)
                    state.alpha.snapTo(0f)
                    launch { state.alpha.animateTo(pressedAlpha, tween(durationMillis = 80)) }
                    launch {
                        state.radius.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 450,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    }
                }

                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    state.alpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing)
                    )
                }
            }
        }
    }

    return drawWithContent {
        drawContent()
        val alpha = state.alpha.value
        if (alpha <= 0.001f || size.isEmpty()) return@drawWithContent
        val origin = state.pressPosition.takeIf { it != Offset.Unspecified } ?: center
        val dx = max(origin.x, size.width - origin.x)
        val dy = max(origin.y, size.height - origin.y)
        val reach = sqrt(dx * dx + dy * dy)
        val outline = shape.createOutline(size, layoutDirection, this)
        clipPath(Path().apply { addOutline(outline) }) {
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = reach * state.radius.value,
                center = origin
            )
        }
    }
}

private class PressRippleState {
    val alpha = Animatable(0f)
    val radius = Animatable(0f)
    var pressPosition: Offset = Offset.Unspecified
}

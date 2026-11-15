package com.yuzlyn.mdeditor.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 進場動效：元素依 [index] 錯開起步，用帶回彈的 spatial spring 由下往上浮。
 *
 * 錯開的步進（預設 45ms）與上浮距離（24dp）是設計值——M3 有彈簧 token，
 * 但沒有規定列表要怎麼錯開，這兩項屬於本專案的排版決定。
 *
 * 位移與透明度共用同一個進度：透明度夾在 0..1，所以回彈只表現在位移上，
 * 不會出現「先變透明再彈回來」的閃爍。
 */
@Composable
internal fun Appear(
    index: Int,
    stepMillis: Long = 45L,
    offset: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * stepMillis)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }
    val p = progress.value
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = p.coerceIn(0f, 1f)
                translationY = (1f - p) * offset.toPx()
            }
    ) {
        content()
    }
}

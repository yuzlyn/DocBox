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

/** 進場錯開的步進；設定頁、說明頁、清單頁共用同一支。 */
internal const val APPEAR_STEP_MS = 45L

/**
 * 進場動效覆蓋的卡片數。
 *
 * 一屏的卡片量（手機 2 欄約 8 張、平板 4 欄約 16 張）都在這個範圍內；
 * 再多的話延遲會累積成「等半天才浮出來」，所以只播前這些張。
 */
internal const val APPEAR_ITEM_LIMIT = 16

/**
 * 進場動效的觀察窗。
 *
 * 超過這段時間才被組合出來的卡片（捲動、切換排序）直接顯示，不再重播。
 * 比最晚的起步時間（[APPEAR_MAX_STEPS] × [APPEAR_STEP_MS]）再多留一段彈簧時間。
 */
internal const val APPEAR_WINDOW_MS = 900L

/**
 * 錯開步數的上限。
 *
 * 卡片一多，第 30 張要等 1.3 秒才起步，看起來像壞掉。超過這個步數的一律同時起步，
 * 總時長因此封頂在 450ms + 一段彈簧。
 */
private const val APPEAR_MAX_STEPS = 10

/**
 * 進場動效：元素依 [index] 錯開起步，用帶回彈的 spatial spring 由下往上浮。
 *
 * 錯開的步進（預設 45ms）與上浮距離（24dp）是設計值——M3 有彈簧 token，
 * 但沒有規定列表要怎麼錯開，這兩項屬於本專案的排版決定。
 *
 * 位移與透明度共用同一個進度：透明度夾在 0..1，所以回彈只表現在位移上，
 * 不會出現「先變透明再彈回來」的閃爍。
 *
 * @param enabled 給延遲列表用：只有「頁面剛進來那一屏」要播。清單是懶載入的，
 *   之後捲進來的項目若也重播，每滑一段就浮一次，比沒有動效更糟。
 *   這個值只在第一次組合時讀，之後改它不會補播或中斷。
 * @param modifier 套在外層 Box 上，用來把 `animateItem` 一起掛上去。
 */
@Composable
internal fun Appear(
    index: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    stepMillis: Long = APPEAR_STEP_MS,
    offset: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!enabled) return@LaunchedEffect
        delay(index.coerceIn(0, APPEAR_MAX_STEPS) * stepMillis)
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
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = p.coerceIn(0f, 1f)
                translationY = (1f - p) * offset.toPx()
            }
    ) {
        content()
    }
}

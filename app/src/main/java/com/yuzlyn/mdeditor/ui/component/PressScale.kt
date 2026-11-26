package com.yuzlyn.mdeditor.ui.component

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * 按壓回縮的比例。
 *
 * 頂欄圓鈕、側欄圓鈕與頭像共用同一個數字：這幾顆按鈕大小不同（32／48／56dp），
 * 但按下去的「縮多少」要一致，否則同一排按鈕會有的大有的小。
 *
 * 0.88 是量出來的：32dp 的頂欄鈕縮 3.8dp、48dp 側欄鈕縮 5.8dp；
 * 之前用 0.96 時 32dp 只縮 1.3dp（每邊 0.65dp），肉眼根本看不出來。
 */
internal const val PRESSED_SCALE = 0.88f

/**
 * 按下去至少保持這麼久才開始回彈。
 *
 * 為什麼需要它：`fastSpatialSpec()`（stiffness 800）要 60–70ms 才走到 88%，
 * 而正常點按的「按下→抬起」常常只有 50–80ms——手指放開時彈簧才走到一半，
 * 立刻反向，畫面上等於沒縮。長按看得到、正常點看不到，就是這個原因。
 * 140ms 足夠讓眼睛收到一次完整的「壓下去」，又不會讓按鈕像卡住。
 */
internal const val MIN_PRESS_HOLD_MS = 140L

/**
 * 卡片的按壓比例。
 *
 * 卡片面積大，4% 已經是十幾 dp；不需要像 32dp 圓鈕那樣壓到 0.88。
 * 卡片有波紋當即時回饋，所以不套用「最短保持」——那會讓整張卡在被點開前多停一拍。
 */
internal const val CARD_PRESSED_SCALE = 0.94f

/**
 * 卡片的波紋濃度。
 *
 * M3 的 pressed state layer 是 0.10；卡片面積大、底色又接近背景，
 * 0.10 看起來像「有一點髒」而不是「按下去了」。0.12 仍在同一量級，
 * 配合 0.94 的回縮才讀得出來。
 */
internal const val CARD_RIPPLE_ALPHA = 0.12f

/**
 * 按下輕微縮小、鬆手用 spatial spring 回彈。
 *
 * 只做縮放，不畫波紋：套用的都是 Material3 原生按鈕（`FilledTonalIconButton`），
 * 它們自己已有 state layer；再疊一層自繪波紋會變成兩套反饋。
 * 彈性曲線取自 `MaterialTheme.motionScheme`，不寫死時長或 easing；
 * 最短保持時間是這支微互動自己的節奏，不屬於 M3 token。
 *
 * [interactionSource] 必須和按鈕本身共用同一個，否則按鈕按下去這個 modifier 收不到事件。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = PRESSED_SCALE
): Modifier {
    val motion = MaterialTheme.motionScheme
    val scale = remember { Animatable(1f) }
    // graphicsLayer 直接讀 Animatable.value 不一定會重繪；同步到明確的 snapshot state。
    val scaleDraw = remember { mutableFloatStateOf(1f) }
    var returnJob by remember { mutableStateOf<Job?>(null) }
    var pressedAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(scale) {
        snapshotFlow { scale.value }.collect { scaleDraw.floatValue = it }
    }
    LaunchedEffect(interactionSource, motion) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressedAt = SystemClock.uptimeMillis()
                    // 連按時先取消上一次排隊的回彈，否則它會在 140ms 後把新的按下彈回去。
                    returnJob?.cancel()
                    returnJob = null
                    launch { scale.animateTo(pressedScale, motion.fastSpatialSpec()) }
                }

                is PressInteraction.Release -> {
                    // 抬手後補足最短保持時間，快速點按才看得到壓下去的那一下。
                    val remaining = MIN_PRESS_HOLD_MS - (SystemClock.uptimeMillis() - pressedAt)
                    returnJob?.cancel()
                    returnJob = launch {
                        if (remaining > 0) delay(remaining)
                        scale.animateTo(1f, motion.fastSpatialSpec())
                    }
                }

                is PressInteraction.Cancel -> {
                    // 手指出界／被手勢打斷：立即回彈，不要讓按鈕停在縮小狀態。
                    returnJob?.cancel()
                    returnJob = launch { scale.animateTo(1f, motion.fastSpatialSpec()) }
                }
            }
        }
    }

    return this.graphicsLayer {
        scaleX = scaleDraw.floatValue
        scaleY = scaleDraw.floatValue
    }
}

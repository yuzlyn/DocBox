package com.yuzlyn.mdeditor.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import android.graphics.drawable.Drawable
import com.yuzlyn.mdeditor.data.settings.LauncherGlyph

/**
 * 開屏標誌的邊長。
 *
 * Android 12 的起始視窗把自適應圖示畫成 108dp 的 2/3，實測就是 160dp；
 * 這裡用同一個尺寸，系統視窗交棒過來的那一幀不會看到圖示突然放大或縮小。
 */
private val LOGO_SIZE = 160.dp

/** 退場時標誌微放大，和背板一起淡出。 */
private const val LOGO_GROW = 0.08f

/** 開屏退場時長。 */
private const val SPLASH_EXIT_MS = 240

/**
 * App 自繪的開屏。
 *
 * 系統起始視窗只能用編譯期資源：底色跟不上莫奈取色，圖示也只能是打包時那一顆。
 * 所以真正的開屏由這裡畫——底色是當前 `colorScheme.surface`，標誌是**桌面上那顆圖示**
 * （[LauncherIcon.currentIconBitmap]，字形與配色都跟使用者選的一致）。
 *
 * 退場沿用原本的手感：整層淡出、標誌放大 8%，240ms；期間吞掉觸控，
 * 避免使用者在淡出途中誤點到下面的頁面。
 *
 * @param icon 別名圖示的點陣；取不到時退回用 [glyph] 畫在主題 primary 上。
 * @param finish 設為 true 後開始退場，動畫結束回呼 [onFinished]。
 */
@Composable
fun SplashOverlay(
    icon: Drawable?,
    glyph: LauncherGlyph,
    surface: Color,
    finish: Boolean,
    onFinished: () -> Unit
) {
    val density = LocalDensity.current
    // 別名圖示是自適應圖示（會自帶遮罩），點陣化之後就和桌面上那顆一模一樣。
    // 換算只做一次；取不到就留 null，走下面的退回畫法。
    val bitmap = remember(icon, density) {
        icon?.let {
            runCatching {
                val px = (LOGO_SIZE.value * density.density).toInt()
                it.toBitmap(px, px).asImageBitmap()
            }.getOrNull()
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(finish) {
        if (!finish) return@LaunchedEffect
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = SPLASH_EXIT_MS, easing = FastOutSlowInEasing)
        )
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surface)
            .graphicsLayer { alpha = 1f - progress.value }
            // 開屏期間不讓點擊穿透到下層：淡出的那 240ms 也不該誤觸。
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(LOGO_SIZE)
                .graphicsLayer {
                    val grow = 1f + LOGO_GROW * progress.value
                    scaleX = grow
                    scaleY = grow
                }
                // 自適應圖示通常已自帶遮罩；再裁一次圓角是為了在沒遮罩的機型上
                // 也不會露出一塊方形底色。
                .clip(RoundedCornerShape(LOGO_SIZE * 0.22f))
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 別名圖示取不到（極少數：元件被系統停用、圖示解不開）才走這條。
                // 用主題 primary 畫底、字形來自設定，至少亮暗與字形仍是對的。
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(LOGO_SIZE * 0.22f))
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        painter = painterResource(glyph.foreground),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

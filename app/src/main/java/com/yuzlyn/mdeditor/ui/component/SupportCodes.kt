package com.yuzlyn.mdeditor.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yuzlyn.mdeditor.R
import com.yuzlyn.mdeditor.ui.i18n.AppStrings
import com.yuzlyn.mdeditor.ui.theme.Space

/** 一張收款碼的資料：名稱、圖、品牌色點。 */
internal data class SupportCode(
    val label: String,
    val drawableId: Int,
    /** 微信綠／支付寶藍，只當識別用的圓點，不參與文字對比。 */
    val accent: Color
)

/** 收款碼清單。說明頁、作者頁入口共用同一份，不在各畫面重複寫死。 */
internal fun supportCodes(strings: AppStrings): List<SupportCode> = listOf(
    SupportCode(
        label = strings.authorSupportWechat,
        drawableId = R.drawable.support_wechat,
        accent = Color(0xFF07C160)
    ),
    SupportCode(
        label = strings.authorSupportAlipay,
        drawableId = R.drawable.support_alipay,
        accent = Color(0xFF1677FF)
    )
)

/**
 * 單張碼：圖片圓角 = 外卡 28dp − 內距 16dp（M3 嵌套圓角規則）。
 * 按下縮到 0.96，用 spatial spring 回來；點擊交給呼叫端決定（放大）。
 */
@Composable
internal fun QrCard(
    code: SupportCode,
    width: Dp,
    zoomHint: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "qr-press"
    )
    val painter = painterResource(code.drawableId)
    val ratio = painter.intrinsicSize.let {
        if (it.width > 0f && it.height > 0f) it.width / it.height else 0.7f
    }
    val corner = RoundedCornerShape(Space.s150)

    Column(
        modifier = Modifier
            .width(width)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pressRipple(interactionSource = interaction, shape = corner)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = corner,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().aspectRatio(ratio)
        ) {
            Image(
                painter = painter,
                contentDescription = code.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(Space.s150))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s100)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(code.accent)
            )
            Text(
                text = code.label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = zoomHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 滿版放大：對方拿手機掃時用。
 * 遮罩淡入走 effects 軌，碼本身從 0.86 放大走 spatial 軌——兩軌不同步才是 M3 的動效語言。
 */
@Composable
internal fun QrZoomDialog(code: SupportCode, onDismiss: () -> Unit) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    scaleIn(
                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                        initialScale = 0.86f
                    ),
                exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
            ) {
                Image(
                    painter = painterResource(code.drawableId),
                    contentDescription = code.label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .border(
                            width = 2.dp,
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(Space.s300)
                        )
                        .clip(RoundedCornerShape(Space.s300))
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Space.s200)
                    .background(Color.White.copy(alpha = 0.14f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

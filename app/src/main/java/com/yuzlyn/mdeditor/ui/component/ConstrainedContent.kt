package com.yuzlyn.mdeditor.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * 將內容居中並限制閱讀版心，背景與頂欄仍由外層全寬繪製。
 *
 * 這裡刻意只讀父容器送下來的約束，不查實體螢幕或裝置類型：
 * 同一台平板在分屏時可能只剩手機寬度，手機在桌面視窗中也可能很寬。
 * 寬度直接取 `min(父容器可用寬度, maxWidth)`，不靠修飾符順序隱式猜測。
 */
@Composable
fun ConstrainedContent(
    maxWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val cap = maxWidth
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier.width(minOf(cap, this.maxWidth)).fillMaxHeight(),
            content = content
        )
    }
}

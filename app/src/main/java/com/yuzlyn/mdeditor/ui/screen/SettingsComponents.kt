package com.yuzlyn.mdeditor.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.yuzlyn.mdeditor.ui.component.pressRipple
import com.yuzlyn.mdeditor.ui.theme.Space

/** 單一設定行：前置圖標 + 標題 + 副標題 + 尾部裝飾。 */
internal data class SettingsRow(
    val icon: ImageVector,
    val title: String,
    val subtitle: String = "",
    val trailing: (@Composable () -> Unit)? = null,
    /** 開關類型的最後一行不需要「前往」的箭頭。 */
    val showChevron: Boolean = true,
    val onClick: () -> Unit
)

/**
 * M3 Segmented List 分組外角/內角：
 * 外角 = shapes.large、內角 = shapes.extraSmall。
 * 首行外（頂）角、內（底）角；末行內（頂）角、外（底）角；中間行上下皆內角。
 */
internal fun segmentedShape(
    index: Int,
    size: Int,
    outer: CornerSize,
    inner: CornerSize
): RoundedCornerShape {
    val top = if (index == 0) outer else inner
    val bottom = if (index == size - 1) outer else inner
    return RoundedCornerShape(
        topStart = top,
        topEnd = top,
        bottomEnd = bottom,
        bottomStart = bottom
    )
}

/** 一行設定項：雙行高 72dp，無陰影，水波紋貼合該行分段圓角。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SegmentedListItem(
    shape: RoundedCornerShape,
    row: SettingsRow
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            // 原生 ripple 不受外層 clip 影響，改用自繪且依分段圓角裁切的波紋。
            .pressRipple(interactionSource = interactionSource, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = row.onClick
            )
    ) {
        Row(
            modifier = Modifier
                // 高度由內容撐開：長標題／長副標題會換行，固定 72dp 會被裁掉。
                // 平常維持 M3 雙行高度的觀感（72dp），內容更多時自己長高。
                .heightIn(min = Space.s900)
                .padding(horizontal = Space.s250, vertical = Space.s150),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s200)
        ) {
            Icon(
                row.icon,
                contentDescription = null,
                modifier = Modifier.size(Space.s250),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (row.subtitle.isNotBlank()) {
                    Text(
                        text = row.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            row.trailing?.invoke()
            if (row.showChevron) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(Space.s250),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 一個分組：彩色小標籤 + Segmented List（行間距 2dp，無分割線）。 */
@Composable
internal fun SegmentedListGroup(
    title: String,
    rows: List<SettingsRow>
) {
    val outer = MaterialTheme.shapes.large.topStart
    val inner = MaterialTheme.shapes.extraSmall.topStart
    Column(verticalArrangement = Arrangement.spacedBy(Space.s100)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Space.s200)
        )
        Column(verticalArrangement = Arrangement.spacedBy(Space.s25)) {
            rows.forEachIndexed { index, row ->
                SegmentedListItem(
                    shape = segmentedShape(index, rows.size, outer, inner),
                    row = row
                )
            }
        }
    }
}

/** 子頁面裡的單選行：RadioButton + 文字，點擊即選。 */
@Composable
internal fun SettingsRadioRow(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Space.s150),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s200)
    ) {
        RadioButton(selected = selected, onClick = null)
        label()
    }
}

/** 單選文字：選中項使用較高字重（emphasized）。 */
@Composable
internal fun RadioLabel(text: String, selected: Boolean) {
    Text(
        text = text,
        style = if (selected) {
            MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        } else {
            MaterialTheme.typography.bodyLarge
        },
        color = MaterialTheme.colorScheme.onSurface
    )
}

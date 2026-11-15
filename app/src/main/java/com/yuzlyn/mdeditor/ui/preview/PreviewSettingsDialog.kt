package com.yuzlyn.mdeditor.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yuzlyn.mdeditor.data.preview.FontScaleOptions
import com.yuzlyn.mdeditor.data.preview.PreviewConfig
import com.yuzlyn.mdeditor.data.preview.PreviewFlavor
import com.yuzlyn.mdeditor.ui.theme.Space

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreviewSettingsDialog(
    config: PreviewConfig,
    onChange: (PreviewConfig) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("預覽設定", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s150)) {
                Text(
                    text = "字體大小",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ButtonGroup(
                    overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
                    horizontalArrangement = Arrangement.spacedBy(Space.s100)
                ) {
                    FontScaleOptions.forEach { (scale, label) ->
                        toggleableItem(
                            checked = config.fontScale == scale,
                            label = label,
                            onCheckedChange = { checked ->
                                if (checked) onChange(config.copy(fontScale = scale))
                            }
                        )
                    }
                }

                Text(
                    text = "語法",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ButtonGroup(
                    overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
                    horizontalArrangement = Arrangement.spacedBy(Space.s100)
                ) {
                    toggleableItem(
                        checked = config.flavor == PreviewFlavor.COMMONMARK,
                        label = "CommonMark",
                        onCheckedChange = { checked ->
                            if (checked) onChange(config.copy(flavor = PreviewFlavor.COMMONMARK))
                        }
                    )
                    toggleableItem(
                        checked = config.flavor == PreviewFlavor.GITHUB,
                        label = "GitHub",
                        onCheckedChange = { checked ->
                            if (checked) onChange(config.copy(flavor = PreviewFlavor.GITHUB))
                        }
                    )
                }

                SettingToggle("目錄", config.showToc) { onChange(config.copy(showToc = it)) }
                SettingToggle("程式碼高亮", config.syntaxHighlight) {
                    onChange(config.copy(syntaxHighlight = it))
                }
                SettingToggle("標題錨點", config.headingAnchors) {
                    onChange(config.copy(headingAnchors = it))
                }
                SettingToggle("數學公式", config.math) { onChange(config.copy(math = it)) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = Space.s0
    )
}

@Composable
private fun SettingToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

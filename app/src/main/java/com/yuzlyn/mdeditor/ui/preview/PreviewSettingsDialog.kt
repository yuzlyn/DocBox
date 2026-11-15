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
import com.yuzlyn.mdeditor.ui.i18n.AppStrings
import com.yuzlyn.mdeditor.ui.i18n.LocalAppStrings
import com.yuzlyn.mdeditor.ui.theme.Space

/** 字級檔位的名字。資料層只存倍率，叫什麼由介面語言決定。 */
private fun AppStrings.fontScaleLabel(scale: Float): String = when {
    scale < 0.95f -> fontSizeSmall
    scale < 1.1f -> fontSizeStandard
    scale < 1.25f -> fontSizeLarge
    else -> fontSizeHuge
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreviewSettingsDialog(
    config: PreviewConfig,
    onChange: (PreviewConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.previewSettings, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s150)) {
                Text(
                    text = strings.previewFontSize,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ButtonGroup(
                    overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
                    horizontalArrangement = Arrangement.spacedBy(Space.s100)
                ) {
                    FontScaleOptions.forEach { (scale, _) ->
                        toggleableItem(
                            checked = config.fontScale == scale,
                            label = strings.fontScaleLabel(scale),
                            onCheckedChange = { checked ->
                                if (checked) onChange(config.copy(fontScale = scale))
                            }
                        )
                    }
                }

                Text(
                    text = strings.previewSyntax,
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

                SettingToggle(strings.previewToc, config.showToc) { onChange(config.copy(showToc = it)) }
                SettingToggle(strings.previewCodeHighlight, config.syntaxHighlight) {
                    onChange(config.copy(syntaxHighlight = it))
                }
                SettingToggle(strings.previewHeadingAnchors, config.headingAnchors) {
                    onChange(config.copy(headingAnchors = it))
                }
                SettingToggle(strings.previewMath, config.math) { onChange(config.copy(math = it)) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.done) } },
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

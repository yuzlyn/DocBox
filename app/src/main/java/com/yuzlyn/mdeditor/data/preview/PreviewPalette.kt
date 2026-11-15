package com.yuzlyn.mdeditor.data.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yuzlyn.mdeditor.ui.theme.Space
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 所有 CSS 变量都由 MaterialTheme 的 color role 在运行时推导，
 * 不写死任何 hex；spacing 沿用 M3 spacing scale。
 * WebView 的 viewport width=device-width 会把 1 CSS px 映射到 1dp，
 * 因此这里直接使用 dp 数值，不再乘像素密度。
 *
 * surfaceArgb / surfaceHex 用于 WebView 首帧背景，避免预览白闪。
 */
data class PreviewPalette(
    val cssVariables: String,
    val surfaceArgb: Int,
    val surfaceHex: String
)

@Composable
fun rememberPreviewPalette(config: PreviewConfig): PreviewPalette {
    val scheme = MaterialTheme.colorScheme

    return remember(config, scheme) {
        fun rgb(color: Color) = String.format(Locale.ROOT, "#%06X", 0xFFFFFF and color.toArgb())
        fun px(value: Dp) = "${value.value.roundToInt()}px"

        val spacing = listOf(
            "25" to Space.s25, "50" to Space.s50, "75" to Space.s75, "100" to Space.s100,
            "125" to Space.s125, "150" to Space.s150, "175" to Space.s175, "200" to Space.s200,
            "250" to Space.s250, "300" to Space.s300, "400" to Space.s400, "450" to Space.s450,
            "500" to Space.s500, "600" to Space.s600, "700" to Space.s700, "800" to Space.s800,
            "900" to Space.s900
        )

        val vars = buildString {
            append("--md-surface:").append(rgb(scheme.surface)).append(';')
            append("--md-on-surface:").append(rgb(scheme.onSurface)).append(';')
            append("--md-on-surface-variant:").append(rgb(scheme.onSurfaceVariant)).append(';')
            append("--md-primary:").append(rgb(scheme.primary)).append(';')
            append("--md-on-primary:").append(rgb(scheme.onPrimary)).append(';')
            append("--md-primary-container:").append(rgb(scheme.primaryContainer)).append(';')
            append("--md-on-primary-container:").append(rgb(scheme.onPrimaryContainer)).append(';')
            append("--md-secondary-container:").append(rgb(scheme.secondaryContainer)).append(';')
            append("--md-on-secondary-container:").append(rgb(scheme.onSecondaryContainer)).append(';')
            append("--md-tertiary:").append(rgb(scheme.tertiary)).append(';')
            append("--md-tertiary-container:").append(rgb(scheme.tertiaryContainer)).append(';')
            append("--md-on-tertiary-container:").append(rgb(scheme.onTertiaryContainer)).append(';')
            append("--md-outline:").append(rgb(scheme.outline)).append(';')
            append("--md-outline-variant:").append(rgb(scheme.outlineVariant)).append(';')
            append("--md-surface-container-low:").append(rgb(scheme.surfaceContainerLow)).append(';')
            append("--md-surface-container-high:").append(rgb(scheme.surfaceContainerHigh)).append(';')
            append("--md-error:").append(rgb(scheme.error)).append(';')
            append("--md-on-error:").append(rgb(scheme.onError)).append(';')
            append("--md-font-size:").append(px((16f * config.fontScale).dp)).append(';')
            append("--md-max-width:").append(px(720.dp)).append(';')
            append("--md-line-height:1.65;")
            spacing.forEach { (name, dp) -> append("--sp-").append(name).append(':').append(px(dp)).append(';') }
        }

        PreviewPalette(
            cssVariables = vars,
            surfaceArgb = scheme.surface.toArgb(),
            surfaceHex = rgb(scheme.surface)
        )
    }
}

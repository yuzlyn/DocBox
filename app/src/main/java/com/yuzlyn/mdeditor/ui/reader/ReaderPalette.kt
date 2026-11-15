package com.yuzlyn.mdeditor.ui.reader

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.yuzlyn.mdeditor.data.book.ReadingColors
import com.yuzlyn.mdeditor.data.book.ReadingTheme

/**
 * 閱讀主題的莫奈配色表。
 *
 * 這些紙張**不寫死顏色**：全部由當下的莫奈配色派生。換壁紙、換種子色，
 * 閱讀器會跟著一起換——不會出現「介面是藍的、書是米黃的」這種兩個 App 的錯覺。
 *
 * 派生方式：以目前的主色為種子，各套主題用不同的 [PaletteStyle] 重算一份配色，
 * 再從裡面取三個角色：
 * - 底紙 `surfaceContainerLow`：比 `surface` 低一階。純白在夜裡太刺眼，
 *   而 M3 的容器色階本來就屬於同一家族，正文壓上去對比度是現成的。
 * - 正文 `onSurface`：與底紙配對的角色，不用自己算對比。
 * - 連結 `primary`：全書唯一的彩色，用來認路。
 *
 * [ReadingTheme.DEFAULT] 例外：它直接用 App 的 `surface` / `onSurface`，
 * 也就是「現在的莫奈種子色」本人。翻開書就是 App 的一頁，而不是另一套紙。
 */
@Composable
fun rememberReadingPalettes(): Map<ReadingTheme, ReadingColors> {
    val scheme = MaterialTheme.colorScheme
    // 「預設」跟著 App：深淺、底紙、正文全部照抄，連 App 對 surface 的替換一起繼承。
    val appDark = scheme.surface.luminance() < 0.5f
    // 種子色取主色：MONET 模式下它來自壁紙，種子色模式下就是使用者挑的那個色。
    val seed = scheme.primary

    return remember(seed, appDark, scheme.surface, scheme.onSurface) {
        readingPalettes(
            seed = seed,
            appDark = appDark,
            appSurface = scheme.surface,
            appOnSurface = scheme.onSurface,
            appPrimary = scheme.primary
        )
    }
}

/**
 * 配色表的純函數版本：只吃 [Color]、只吐 [ReadingColors]。
 *
 * 抽出來是為了能進 JVM 單元測試——六套主題的正文對比度可以一次全驗，
 * 不必開模擬器、也不必截图比對。
 */
fun readingPalettes(
    seed: Color,
    appDark: Boolean,
    appSurface: Color,
    appOnSurface: Color,
    appPrimary: Color
): Map<ReadingTheme, ReadingColors> {
    val palettes = LinkedHashMap<ReadingTheme, ReadingColors>(ReadingTheme.entries.size)
    palettes[ReadingTheme.DEFAULT] = ReadingColors(
        background = appSurface.toRgb(),
        foreground = appOnSurface.toRgb(),
        link = appPrimary.toRgb(),
        dark = appDark
    )
    ReadingTheme.entries
        .filter { it != ReadingTheme.DEFAULT }
        .forEach { theme ->
            val derived = dynamicColorScheme(
                seedColor = seed,
                isDark = theme.prefersDark,
                style = theme.paletteStyle
            )
            palettes[theme] = ReadingColors(
                // 素白要「最白」、墨黑要「最黑」：兩端都再走一階，中間兩套留在
                // surfaceContainerLow，才有一條看得出來的深淺階梯。
                //
                // 這裡**不靠換 PaletteStyle 拉開差距**：實測 Neutral/TonalSpot/Content
                // 三種風格算出來的 container 色只差 1~2 個色階，擺成圓點根本分不出來；
                // 色階（tone）才是肉眼看得見的那一維。
                background = when (theme) {
                    ReadingTheme.PAPER, ReadingTheme.INK ->
                        derived.surfaceContainerLowest.toRgb()

                    else -> derived.surfaceContainerLow.toRgb()
                },
                foreground = derived.onSurface.toRgb(),
                link = derived.primary.toRgb(),
                dark = theme.prefersDark
            )
        }
    return palettes
}

/** 每套主題用哪一種莫奈調色盤。差別主要在底紙的彩度，正文與連結都走同一家族。 */
private val ReadingTheme.paletteStyle: PaletteStyle
    get() = when (this) {
        // 素白：幾乎無彩度的紙，最接近「白紙黑字」。
        ReadingTheme.PAPER -> PaletteStyle.Neutral
        // 淡彩：M3 標準的中性色，紙面浮一層很淡的主色。
        ReadingTheme.CREAM -> PaletteStyle.TonalSpot
        // 夜色：同樣的中性色，但走深色方案。
        ReadingTheme.NIGHT -> PaletteStyle.TonalSpot
        // 墨黑：純灰階，深色方案。
        ReadingTheme.INK -> PaletteStyle.Monochrome
        // 預設不走派生，用不到；補一個值讓 when 完整。
        ReadingTheme.DEFAULT -> PaletteStyle.TonalSpot
    }

/** Compose 的 Color → `0xRRGGBB`。`ReadingColors` 只存 RGB，透明度由呼叫端補。 */
private fun Color.toRgb(): Long = toArgb().toLong() and 0xFFFFFFL

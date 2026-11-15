package com.yuzlyn.mdeditor.ui.theme

import android.app.Activity
import android.os.Build
import androidx.core.view.WindowCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.materialkolor.rememberDynamicColorScheme
import com.yuzlyn.mdeditor.data.fonts.FontStore
import com.yuzlyn.mdeditor.data.settings.AppSettings
import com.yuzlyn.mdeditor.data.settings.DarkModeOption
import com.yuzlyn.mdeditor.data.settings.ThemeMode
import com.yuzlyn.mdeditor.ui.i18n.LocalAppStrings
import com.yuzlyn.mdeditor.ui.i18n.appStrings
import androidx.compose.ui.platform.LocalConfiguration

/** M3 Expressive 圓角（0/4/8/12/16/20/28/32/48/full），整體取較大一檔。 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * 依 noteFont 設定值取得筆記內容字體；"system" 或空代表使用系統字體（回傳 null）。
 *
 * 字型檔已經從 `assets/fonts` 搬到 `filesDir/fonts`（使用者可以自己匯入、刪除），
 * 所以這裡不再走 AssetManager，而是直接讀檔案。檔案被刪掉時回傳 null，
 * 而不是拋例外——設定值留著舊檔名是正常的，不該讓整頁打不開。
 */
@Composable
fun noteFontFamily(noteFont: String): FontFamily? {
    val context = LocalContext.current
    return remember(noteFont) {
        if (noteFont == FontStore.SYSTEM || noteFont.isBlank()) {
            null
        } else {
            FontStore.fileOf(context, noteFont)?.let { file ->
                runCatching { FontFamily(Font(file)) }.getOrNull()
            }
        }
    }
}

@Composable
fun MDEditorTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (settings.darkMode) {
        DarkModeOption.FOLLOW_SYSTEM -> systemDark
        DarkModeOption.DARK -> true
        DarkModeOption.LIGHT -> false
    }

    val view = LocalView.current

    // MONET：Android 12+ 用系統壁紙動態取色；取色失敗時退回種子色派生（參考 PixelPlayer）。
    // 取色是普通函數（讀系統資源 + 建整套 ColorScheme），不 remember 的話每次 settings
    // 變動都會重算，改個筆記背景色也付這筆成本，正好和彈窗收合動畫搶同一幀。
    val monetScheme = if (
        settings.themeMode == ThemeMode.MONET &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
        remember(context, darkTheme) {
            runCatching {
                if (darkTheme) {
                    dynamicDarkColorScheme(context)
                } else {
                    dynamicLightColorScheme(context)
                }
            }.getOrNull()
        }
    } else {
        null
    }

    // CUSTOM_SEED / 低版本 / MONET 取色失敗：標準 M3 TonalSpot（中性色表面），默認對比度。
    val colorScheme = if (monetScheme != null) {
        monetScheme
    } else {
        rememberDynamicColorScheme(
            seedColor = Color(settings.seedColor.argb),
            isDark = darkTheme
        )
    }

    val finalScheme = remember(colorScheme, darkTheme) {
        if (darkTheme) {
            // 深色：背景改用卡片色，卡片/容器一律提到最淺（surfaceContainerHighest）。
            colorScheme.copy(
                surface = colorScheme.surfaceContainerLow,
                surfaceContainerLow = colorScheme.surfaceContainerHighest,
                surfaceContainer = colorScheme.surfaceContainerHighest,
                surfaceContainerHigh = colorScheme.surfaceContainerHighest
            )
        } else {
            // 淺色：背景與卡片調換（背景改用卡片色，卡片/搜索框改用背景色）。
            colorScheme.copy(
                surface = colorScheme.surfaceContainerLow,
                surfaceContainerLow = colorScheme.surface,
                surfaceContainer = colorScheme.surfaceContainerHigh,
                surfaceContainerHigh = colorScheme.surfaceContainer
            )
        }
    }

    // 設定裡存的可能是「跟隨系統」：那一刻才換算成實際語種，換了系統語言就跟著換。
    // 除了語言設定，系統語系本身也要當 key——設定沒動、只是系統語言變了，畫面也得重組。
    val systemLocale = LocalConfiguration.current.locales[0]
    val strings = remember(settings.language, systemLocale) {
        settings.language.resolved().appStrings()
    }

    // 依 App 實際深色模式設定狀態列/導航列圖標明暗（參考 PixelPlayer 的 status bar style）。
    SideEffect {
        val activity = view.context as? Activity ?: return@SideEffect
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAppStrings provides strings) {
        MaterialExpressiveTheme(
            colorScheme = finalScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = AppShapes,
            typography = AppTypography,
            content = content
        )
    }
}

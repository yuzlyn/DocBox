package com.yuzlyn.mdeditor.ui.screen

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yuzlyn.mdeditor.data.settings.AppSettings
import com.yuzlyn.mdeditor.data.settings.AppLanguage
import com.yuzlyn.mdeditor.data.settings.CardShape
import com.yuzlyn.mdeditor.data.settings.DarkModeOption
import com.yuzlyn.mdeditor.data.settings.LauncherGlyph
import com.yuzlyn.mdeditor.data.settings.SeedColor
import com.yuzlyn.mdeditor.data.settings.ThemeMode
import com.yuzlyn.mdeditor.ui.component.ConstrainedContent
import com.yuzlyn.mdeditor.ui.component.pressRipple
import com.yuzlyn.mdeditor.ui.i18n.LocalAppStrings
import com.yuzlyn.mdeditor.ui.component.Appear
import com.yuzlyn.mdeditor.ui.theme.Space

/**
 * 設定子頁的「第幾段」計數器。
 *
 * 每個 [DetailSection] 依組合順序領一個序號，進場時錯開上浮——M3 有彈簧 token，
 * 但沒有規定列表怎麼錯開，所以步進是設計值（見 [Appear]）。用 CompositionLocal
 * 傳序號，是因為 content 是呼叫端寫的一串 DetailSection，外面沒辦法逐個包。
 */
private val LocalDetailSectionIndex = compositionLocalOf { intArrayOf(0) }

/** 子頁面通用骨架：返回頂欄 + 可滾動內容。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsDetailScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        val counter = remember { intArrayOf(0) }
        CompositionLocalProvider(LocalDetailSectionIndex provides counter) {
            ConstrainedContent(
                maxWidth = 720.dp,
                modifier = Modifier.padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Space.s200, vertical = Space.s200),
                    verticalArrangement = Arrangement.spacedBy(Space.s300)
                ) {
                    content()
                }
            }
        }
    }
}

/** 子頁面裡的一個分組：彩色小標籤 + 內容。 */
@Composable
internal fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val counter = LocalDetailSectionIndex.current
    val index = remember { counter[0]++ }
    Appear(index = index) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s100)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = Space.s200)
            )
            Column(content = content)
        }
    }
}

private fun CardShape.label(strings: com.yuzlyn.mdeditor.ui.i18n.AppStrings): String = when (this) {
    CardShape.DEFAULT -> strings.cardShapeDefault
    CardShape.ROUNDED -> strings.cardShapeRounded
    CardShape.EXTRA_ROUNDED -> strings.cardShapeExtraRounded
    CardShape.SQUARE -> strings.cardShapeSquare
}

private fun LauncherGlyph.label(strings: com.yuzlyn.mdeditor.ui.i18n.AppStrings): String = when (this) {
    LauncherGlyph.XIA -> strings.glyphXia
    LauncherGlyph.WEIBO -> strings.glyphWeibo
    LauncherGlyph.ZENGZHONG -> strings.glyphZengzhong
    LauncherGlyph.LUSITU -> strings.glyphLusitu
    LauncherGlyph.SHUOWENGU -> strings.glyphShuowengu
    LauncherGlyph.SHUOWENZHU -> strings.glyphShuowenzhu
}

/**
 * 一個字形選項：圓形預覽 + 名稱。
 *
 * 圓用 **48dp**，和桌面圖標真實尺寸一致：字形在 108dp 畫布裡只佔 54dp，
 * 換算到 48dp 的圓裡就是 24dp。再小就分辨不出結構，比真實尺寸大又會讓人
 * 誤以為桌面上也是那麼大。選中時在外圈套一道 2dp 的主色圓環（容器 56dp）。
 */
@Composable
private fun GlyphChoice(
    glyph: LauncherGlyph,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pill = RoundedCornerShape(Space.s150)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.s75),
        modifier = Modifier
            .clip(pill)
            .pressRipple(interactionSource = interaction, shape = pill)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(Space.s75)
    ) {
        Box(
            modifier = Modifier
                .size(Space.s700)
                .then(
                    if (selected) {
                        Modifier
                            .border(Space.s25, MaterialTheme.colorScheme.primary, CircleShape)
                            .padding(Space.s50)
                    } else {
                        Modifier.padding(Space.s50)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(glyph.foreground),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1
        )
    }
}

/** 主題子頁面：主題模式、種子色、桌面圖標、卡片形狀（字型與筆記背景各自獨立成頁）。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThemeSettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    SettingsDetailScaffold(title = strings.theme, onBack = onBack) {
        DetailSection(strings.theme) {
            listOf(
                ThemeMode.MONET to strings.monet,
                ThemeMode.CUSTOM_SEED to strings.customMonet
            ).forEach { (mode, label) ->
                val selected = settings.themeMode == mode
                SettingsRadioRow(
                    selected = selected,
                    onClick = {
                        if (!selected) onChange(settings.copy(themeMode = mode))
                    }
                ) {
                    RadioLabel(label, selected)
                }
            }
        }

        DetailSection(strings.seedColors) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.s200),
                verticalArrangement = Arrangement.spacedBy(Space.s200)
            ) {
                SeedColor.entries.forEach { seed ->
                    val selected = settings.seedColor == seed
                    val seedInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(Space.s600)
                            .background(Color(seed.argb), CircleShape)
                            // 波紋自繪並裁成圓形，按住時才不會出現方形遮罩。
                            .pressRipple(interactionSource = seedInteraction, shape = CircleShape)
                            .clickable(
                                interactionSource = seedInteraction,
                                indication = null
                            ) { onChange(settings.copy(seedColor = seed)) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Text("✓", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }

        // 字形与配色是两个独立的自由度：配色换的是圆底颜色，这里换的是字本身。
        // 底下那排种子色和上面的取色方式同时生效，所以预览一律用当前主题的 primary 画圆底。
        DetailSection(strings.launcherIcon) {
            FlowRow(
                // 6 个 48dp 圆在 360dp 宽的屏上放不进一行，会折成两行；
                // 每行居中排，4 + 2 的分布看着才不歪。
                horizontalArrangement = Arrangement.spacedBy(Space.s150, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(Space.s150),
                modifier = Modifier.fillMaxWidth()
            ) {
                LauncherGlyph.entries.forEach { glyph ->
                    GlyphChoice(
                        glyph = glyph,
                        label = glyph.label(strings),
                        selected = settings.launcherGlyph == glyph,
                        onClick = { onChange(settings.copy(launcherGlyph = glyph)) }
                    )
                }
            }
        }

        DetailSection(strings.cardShape) {
            CardShape.entries.forEach { shape ->
                val selected = settings.cardShape == shape
                SettingsRadioRow(
                    selected = selected,
                    onClick = { onChange(settings.copy(cardShape = shape)) }
                ) {
                    RadioLabel(shape.label(strings), selected)
                }
            }
        }

    }
}

/** 深色模式子頁面。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkModeSettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    SettingsDetailScaffold(title = strings.darkMode, onBack = onBack) {
        DetailSection(strings.darkMode) {
            listOf(
                DarkModeOption.FOLLOW_SYSTEM to strings.followSystem,
                DarkModeOption.DARK to strings.dark,
                DarkModeOption.LIGHT to strings.light
            ).forEach { (mode, label) ->
                val selected = settings.darkMode == mode
                SettingsRadioRow(
                    selected = selected,
                    onClick = { onChange(settings.copy(darkMode = mode)) }
                ) {
                    RadioLabel(label, selected)
                }
            }
        }
    }
}

/** 預設排版子頁面：新筆記與還沒指定排版的筆記都套用這裡的選擇。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutSettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    SettingsDetailScaffold(title = strings.defaultLayout, onBack = onBack) {
        DetailSection(strings.defaultLayout) {
            listOf(
                false to strings.horizontalLayout,
                true to strings.verticalLayoutLabel
            ).forEach { (vertical, label) ->
                val selected = settings.verticalLayout == vertical
                SettingsRadioRow(
                    selected = selected,
                    onClick = { onChange(settings.copy(verticalLayout = vertical)) }
                ) {
                    RadioLabel(label, selected)
                }
            }
        }
        Text(
            text = strings.layoutHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Space.s200)
        )
    }
}

/** 語言子頁面。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    SettingsDetailScaffold(title = strings.language, onBack = onBack) {
        DetailSection(strings.language) {
            // 第一列是「跟隨系統」，底下用小字說明系統現在是哪一語——
            // 不然切到這一項之後清單上沒有任何一列被勾，看起來像設定壞了。
            AppLanguage.selectable.forEach { language ->
                val selected = settings.language == language
                SettingsRadioRow(
                    selected = selected,
                    onClick = { onChange(settings.copy(language = language)) }
                ) {
                    if (language == AppLanguage.SYSTEM) {
                        Column {
                            RadioLabel(strings.languageFollowSystem, selected)
                            Text(
                                text = strings.languageFollowSystemHint(
                                    AppLanguage.systemDefault().nativeName
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        RadioLabel(language.nativeName, selected)
                    }
                }
            }
        }
    }
}

package com.yuzlyn.mdeditor.ui.screen

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yuzlyn.mdeditor.data.settings.AppLanguage
import com.yuzlyn.mdeditor.data.settings.AppSettings
import com.yuzlyn.mdeditor.data.settings.DarkModeOption
import com.yuzlyn.mdeditor.data.settings.HomeTarget
import com.yuzlyn.mdeditor.data.settings.ThemeMode
import com.yuzlyn.mdeditor.data.fonts.FontStore
import com.yuzlyn.mdeditor.ui.component.Appear
import com.yuzlyn.mdeditor.data.settings.WritingGuide
import com.yuzlyn.mdeditor.ui.i18n.LocalAppStrings
import com.yuzlyn.mdeditor.ui.i18n.label
import com.yuzlyn.mdeditor.ui.theme.Space

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onOpenTheme: () -> Unit,
    onOpenDarkMode: () -> Unit,
    onOpenFont: () -> Unit,
    onOpenNoteBackground: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenLayout: () -> Unit,
    onOpenStats: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val systemDark = isSystemInDarkTheme()

    val themeSubtitle = when (settings.themeMode) {
        ThemeMode.MONET -> strings.monet
        ThemeMode.CUSTOM_SEED -> strings.customMonet
    }
    // 副標題直接報「現在編輯用的是哪一種」，比寫一句「字型設定」有用。
    val fontSubtitle = listOf(settings.noteFont, settings.readerFont)
        .firstOrNull { it.isNotBlank() && it != FontStore.SYSTEM }
        ?.substringBeforeLast('.')
        ?: strings.systemFont
    val backgroundSubtitle = when {
        settings.noteBackground.isNotBlank() ->
            settings.noteBackground.substringAfterLast('/')
        else -> settings.background.label(strings)
    }
    // 深色模式副標題：跟隨系統時顯示實際跟隨到的深/淺狀態。
    val darkSubtitle = when (settings.darkMode) {
        DarkModeOption.FOLLOW_SYSTEM ->
            strings.followSystem + " · " + if (systemDark) strings.dark else strings.light
        DarkModeOption.DARK -> strings.dark
        DarkModeOption.LIGHT -> strings.light
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = { Text(strings.settingsTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.s200, vertical = Space.s200),
            verticalArrangement = Arrangement.spacedBy(Space.s400)
        ) {
            Appear(index = 0) {
SegmentedListGroup(
                    title = strings.theme,
                    rows = listOf(
                        SettingsRow(
                            icon = Icons.Default.Palette,
                            title = strings.theme,
                            subtitle = themeSubtitle,
                            onClick = onOpenTheme
                        ),
                        SettingsRow(
                            icon = Icons.Default.DarkMode,
                            title = strings.darkMode,
                            subtitle = darkSubtitle,
                            onClick = onOpenDarkMode
                        ),
                        // 字型與筆記背景各有自己的子頁：一個管「字長什麼樣」，
                        // 一個管「紙長什麼樣」，都不適合擠在主題頁裡當一段選項。
                        SettingsRow(
                            icon = Icons.Default.TextFields,
                            title = strings.font,
                            subtitle = fontSubtitle,
                            onClick = onOpenFont
                        ),
                        SettingsRow(
                            icon = Icons.Default.Wallpaper,
                            title = strings.noteBackgroundPage,
                            subtitle = backgroundSubtitle,
                            onClick = onOpenNoteBackground
                        )
                    )
                )
            }

            Appear(index = 1) {
SegmentedListGroup(
                    title = strings.layout,
                    rows = listOf(
                        SettingsRow(
                            icon = Icons.Default.SwapVert,
                            title = strings.defaultLayout,
                            subtitle = if (settings.verticalLayout) {
                                strings.verticalLayoutLabel
                            } else {
                                strings.horizontalLayout
                            },
                            onClick = onOpenLayout
                        ),
                        // 行號是直接切換的開關，不像排版那樣再開一層子頁面。
                        SettingsRow(
                            icon = Icons.Default.FormatListNumbered,
                            title = strings.lineNumbersLabel,
                            subtitle = strings.lineNumbersSummary,
                            showChevron = false,
                            trailing = {
                                Switch(
                                    checked = settings.showLineNumbers,
                                    onCheckedChange = {
                                        onChange(settings.copy(showLineNumbers = it))
                                    }
                                )
                            },
                            onClick = {
                                onChange(settings.copy(showLineNumbers = !settings.showLineNumbers))
                            }
                        ),
                    )
                )
            }

            // 啟動頁：兩個主頁（筆記、書庫）平級，這裡只是決定開 app 時落在哪一個。
            // 同樣是兩個值，直接列出來比再開一層子頁面快。
            Appear(index = 2) {
SegmentedListGroup(
                    title = strings.startPage,
                    rows = listOf(
                        HomeTarget.NOTES to strings.startPageNotes,
                        HomeTarget.LIBRARY to strings.startPageLibrary
                    ).map { (target, label) ->
                        val selected = settings.homeTarget == target
                        SettingsRow(
                            icon = when (target) {
                                HomeTarget.NOTES -> Icons.AutoMirrored.Filled.Note
                                else -> Icons.AutoMirrored.Filled.MenuBook
                            },
                            title = label,
                            subtitle = if (selected) strings.startPageHint else "",
                            showChevron = false,
                            // 互斥的開關：打開一個，另一個自己關掉。
                            // 關掉「正在用的那一個」不做任何事——啟動頁總得有一個，
                            // 讓它真的可以關成空的，下次開 app 就不知道該去哪了。
                            trailing = {
                                Switch(
                                    checked = selected,
                                    onCheckedChange = { on ->
                                        if (on && !selected) onChange(settings.copy(homeTarget = target))
                                    }
                                )
                            },
                            onClick = { if (!selected) onChange(settings.copy(homeTarget = target)) }
                        )
                    }
                )
            }

            // 書寫輔助線只有三個值，直接列出來比再開一層子頁面快；
            // 這裡是「新筆記的預設」，單篇要改在編輯頁的「更多」裡。
            Appear(index = 3) {
SegmentedListGroup(
                    title = strings.writingGuide,
                    rows = listOf(
                        WritingGuide.NONE to strings.guideNone,
                        WritingGuide.RULED to strings.guideRuled,
                        WritingGuide.GRID to strings.guideGrid
                    ).map { (guide, label) ->
                        val selected = settings.guide == guide
                        SettingsRow(
                            icon = when (guide) {
                                WritingGuide.NONE -> Icons.Default.CheckBoxOutlineBlank
                                WritingGuide.RULED -> Icons.Default.FormatLineSpacing
                                WritingGuide.GRID -> Icons.Default.GridOn
                            },
                            title = label,
                            subtitle = if (selected) strings.guideHint else "",
                            showChevron = false,
                            // 三個值同樣互斥；「無」也是其中一檔，所以每個開關都能被關掉，
                            // 關掉任何一個就等於回到「無」。
                            trailing = {
                                Switch(
                                    checked = selected,
                                    onCheckedChange = { on ->
                                        onChange(
                                            settings.copy(
                                                writingGuide = when {
                                                    on -> guide.id
                                                    else -> WritingGuide.NONE.id
                                                }
                                            )
                                        )
                                    }
                                )
                            },
                            onClick = {
                                onChange(
                                    settings.copy(
                                        writingGuide = when {
                                            selected -> WritingGuide.NONE.id
                                            else -> guide.id
                                        }
                                    )
                                )
                            }
                        )
                    }
                )
            }

            // 統計：主開關在子頁面裡，這裡只顯示狀態，不重複一個開關——
            // 兩個地方都能關的東西，遲早會出現「這裡是開、那裡是關」。
            Appear(index = 4) {
SegmentedListGroup(
                    title = strings.stats,
                    rows = listOf(
                        SettingsRow(
                            icon = Icons.Default.Insights,
                            title = strings.statsTitle,
                            subtitle = if (settings.statsEnabled) {
                                strings.statsEnabledLabel
                            } else {
                                strings.statsDisabled
                            },
                            onClick = onOpenStats
                        )
                    )
                )
            }

            Appear(index = 5) {
SegmentedListGroup(
                    title = strings.language,
                    rows = listOf(
                        SettingsRow(
                            icon = Icons.Default.Translate,
                            title = strings.language,
                            // 跟隨系統時直接寫出系統目前是哪一語，不必再點進去確認。
                            subtitle = if (settings.language == AppLanguage.SYSTEM) {
                                strings.languageFollowSystemHint(
                                    AppLanguage.systemDefault().nativeName
                                )
                            } else {
                                settings.language.nativeName
                            },
                            onClick = onOpenLanguage
                        )
                    )
                )
            }

            Text(
                text = strings.colorGuide,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

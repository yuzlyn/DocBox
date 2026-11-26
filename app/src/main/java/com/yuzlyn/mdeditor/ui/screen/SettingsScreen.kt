package com.yuzlyn.mdeditor.ui.screen

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yuzlyn.mdeditor.data.book.BookLibrary
import com.yuzlyn.mdeditor.data.settings.AppLanguage
import com.yuzlyn.mdeditor.data.settings.AppSettings
import com.yuzlyn.mdeditor.data.settings.DarkModeOption
import com.yuzlyn.mdeditor.data.settings.HomeTarget
import com.yuzlyn.mdeditor.data.settings.ThemeMode
import com.yuzlyn.mdeditor.data.fonts.FontStore
import com.yuzlyn.mdeditor.ui.component.Appear
import com.yuzlyn.mdeditor.ui.component.ConstrainedContent
import com.yuzlyn.mdeditor.data.settings.WritingGuide
import com.yuzlyn.mdeditor.ui.i18n.LocalAppStrings
import com.yuzlyn.mdeditor.ui.i18n.label
import com.yuzlyn.mdeditor.ui.theme.Space
import kotlinx.coroutines.launch

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

    // 書庫掃描只在這裡跑。每一次掃描都要向每個連結的資料夾（可能在雲端硬碟）
    // 列一遍目錄，放進書庫頁面的進場路徑裡太慢——那一頁現在只讀已經存好的書目。
    val context = LocalContext.current
    val bookLibrary = remember { BookLibrary(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var scanning by remember { mutableStateOf(false) }
    // 掃描進行中把右邊的箭頭換成轉圈：雲端資料夾可能要等好幾秒，
    // 沒有回饋的話使用者會以為按了沒反應，然後再按一次。
    val scanTrailing: (@Composable () -> Unit)? = if (scanning) {
        { CircularProgressIndicator(modifier = Modifier.size(Space.s250), strokeWidth = 2.dp) }
    } else {
        null
    }

    fun scanLibrary() {
        if (scanning) return
        scope.launch {
            scanning = true
            val result = runCatching { bookLibrary.rescan() }
            scanning = false
            snackbar.showSnackbar(
                result.fold(
                    onSuccess = { added ->
                        if (added > 0) strings.librarySyncAdded(added) else strings.libraryScanNothing
                    },
                    onFailure = { strings.libraryScanFailed }
                )
            )
        }
    }

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
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        ConstrainedContent(
            maxWidth = 720.dp,
            modifier = Modifier.padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                                    // 整列是唯一操作入口，避免 Switch 與整列各播一次反饋。
                                    onCheckedChange = null
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
                            enabled = !selected,
                            dimWhenDisabled = false,
                            // 互斥的開關：打開一個，另一個自己關掉。
                            // 關掉「正在用的那一個」不做任何事——啟動頁總得有一個，
                            // 讓它真的可以關成空的，下次開 app 就不知道該去哪了。
                            trailing = {
                                Switch(
                                    checked = selected,
                                    onCheckedChange = null
                                )
                            },
                            onClick = { if (!selected) onChange(settings.copy(homeTarget = target)) }
                        )
                    }
                )
            }

            // 書庫掃描：連結的資料夾只在這裡掃。書庫頁每次開啟都重掃雲端資料夾太慢，
            // 而且書目本來就存著，掃與不掃看到的幾乎一樣。
            Appear(index = 3) {
SegmentedListGroup(
                    title = strings.library,
                    rows = listOf(
                        SettingsRow(
                            icon = Icons.Default.Refresh,
                            title = strings.libraryScan,
                            subtitle = strings.libraryScanHint,
                            showChevron = false,
                            trailing = scanTrailing,
                            onClick = { scanLibrary() }
                        )
                    )
                )
            }

            // 書寫輔助線只有三個值，直接列出來比再開一層子頁面快；
            // 這裡是「新筆記的預設」，單篇要改在編輯頁的「更多」裡。
            Appear(index = 4) {
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
                                    onCheckedChange = null
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
            Appear(index = 5) {
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

            Appear(index = 6) {
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
            }
        }
    }

}

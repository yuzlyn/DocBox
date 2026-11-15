package com.yuzlyn.mdeditor.ui.reader

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.yuzlyn.mdeditor.data.book.Book
import com.yuzlyn.mdeditor.data.book.BookOpener
import com.yuzlyn.mdeditor.data.book.ReaderFontCss
import com.yuzlyn.mdeditor.data.book.ReaderMode
import com.yuzlyn.mdeditor.data.book.ReadingProgress
import com.yuzlyn.mdeditor.data.book.ReadingProgressStore
import com.yuzlyn.mdeditor.data.book.ReadingColors
import com.yuzlyn.mdeditor.data.book.ReadingTheme
import com.yuzlyn.mdeditor.data.book.TocEntry
import com.yuzlyn.mdeditor.data.settings.AppLanguage
import com.yuzlyn.mdeditor.data.settings.AppSettings
import com.yuzlyn.mdeditor.data.settings.AppSettingsPreferences
import com.yuzlyn.mdeditor.ui.i18n.AppStrings
import com.yuzlyn.mdeditor.ui.i18n.LocalAppStrings
import com.yuzlyn.mdeditor.data.fonts.FontStore
import com.yuzlyn.mdeditor.ui.theme.Space
import com.yuzlyn.mdeditor.ui.theme.noteFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * 原生閱讀器。
 *
 * 這裡**不是**編輯器：打開的是書，底欄就只有閱讀需要的東西——目錄、主題、
 * 排版（翻頁方式／行距／頁邊留白）、直排橫排、字級。沒有存檔、沒有 Markdown、
 * 沒有工具列，書的內容也不會被改寫。
 *
 * 版面是「一整頁書 + 兩層浮動控制列」，而且**控制列預設是收起來的**：
 * 打開就是書頁，點一下中間才叫出上下的控制列。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    uriString: String,
    displayName: String?,
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onBack: () -> Unit,
    /** 每看了一小段就回報秒數；統計頁的「讀了多久」全靠它。 */
    onReading: (Long) -> Unit = {},
    /** 面板裡的「管理字型」跳到設定頁去匯入或刪除。 */
    onOpenFontSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val chinese = settings.language == AppLanguage.ZH_CN || settings.language == AppLanguage.ZH_TW
    val progressStore = remember { ReadingProgressStore(context) }
    // 統計計時用：只在畫面真的在前景時累加。若只開一個 while + delay，
    // 切到背景、鎖屏的時間也會被算成閱讀——睡一覺起來就「讀了八小時」。
    val screenLifecycle = LocalLifecycleOwner.current.lifecycle

    var book by remember { mutableStateOf<Book?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var opening by remember { mutableStateOf(true) }
    var progressKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(book == null) {
        if (book == null) return@LaunchedEffect
        screenLifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(READ_TICK_MS)
                onReading(READ_TICK_MS / 1000)
            }
        }
    }

    var chapter by remember { mutableIntStateOf(0) }
    var anchor by remember { mutableStateOf<String?>(null) }
    // 只有「還原上次進度」的那一次載入要跳到中間，翻到別的章就該從頭。
    var pendingFraction by remember { mutableFloatStateOf(0f) }

    var page by remember { mutableIntStateOf(1) }
    var pages by remember { mutableIntStateOf(1) }
    var fraction by remember { mutableFloatStateOf(0f) }

    // 跳到別章（目錄、書內連結）：章都在同一份文件裡時，這只是把長條平移過去，
    // 不重載文件，所以不會閃、也不會卡。
    var locateChapter by remember { mutableIntStateOf(-1) }
    var locateFraction by remember { mutableFloatStateOf(0f) }
    var locateAnchor by remember { mutableStateOf<String?>(null) }
    var locateToken by remember { mutableIntStateOf(0) }

    // 預設不顯示控制列：打開就是書頁。
    var chromeVisible by remember { mutableStateOf(false) }
    var showToc by remember { mutableStateOf(false) }
    var showThemes by remember { mutableStateOf(false) }
    var showLayout by remember { mutableStateOf(false) }
    var showFonts by remember { mutableStateOf(false) }

    val theme = settings.readingTheme
    val mode = settings.readingMode
    // 主題的顏色不再寫死在主題裡：由當下的莫奈配色派生，所以連它一起記進 key。
    val palettes = rememberReadingPalettes()
    val colors = palettes[theme] ?: ReadingColors.PAPER
    val fontCss = remember(settings.readerFont) { ReaderFontCss.css(settings.readerFont) }
    val style = remember(
        colors,
        settings.readerVertical,
        settings.readerTextScale,
        settings.readerMargin,
        settings.readerLineHeightTenths,
        mode,
        fontCss
    ) {
        theme.style(
            colors = colors,
            textScale = settings.readerTextScale,
            vertical = settings.readerVertical,
            margin = settings.readerMargin,
            lineHeight = settings.readingLineHeight,
            mode = mode,
            fontFamilyCss = fontCss
        )
    }

    LaunchedEffect(uriString) {
        opening = true
        error = null
        runCatching {
            withContext(Dispatchers.IO) {
                BookOpener.open(context, Uri.parse(uriString), displayName)
            }
        }.onSuccess { opened ->
            book = opened
            val key = progressStore.keyOf(opened)
            progressKey = key
            val saved = progressStore.load(key)
            if (saved != null && saved.chapter in 0 until opened.chapterCount) {
                chapter = saved.chapter
                anchor = saved.anchor
                pendingFraction = saved.fraction
            }
        }.onFailure {
            error = it.message?.takeIf { message -> message.isNotBlank() }
                ?: strings.readerOpenFailed
        }
        opening = false
    }

    // 離開這一頁就存進度、關掉書（ZipFile / RandomAccessFile 都要放掉，
    // 不然下次開啟會卡檔案鎖）。
    val latestProgress by rememberUpdatedState(
        ReadingProgress(chapter = chapter, anchor = null, fraction = fraction)
    )
    DisposableEffect(book) {
        val opened = book
        val key = progressKey
        onDispose {
            if (key != null) progressStore.save(key, latestProgress)
            opened?.let { runCatching { it.close() } }
        }
    }

    // 進度落盤：等翻頁停下來再寫，連續翻頁不會一直寫檔案。
    LaunchedEffect(progressKey, chapter, fraction) {
        val key = progressKey ?: return@LaunchedEffect
        delay(700)
        progressStore.save(key, latestProgress)
    }

    val current = book

    val background = ComposeColor(0xFF000000L or (colors.background and 0xFFFFFF))

    fun persistNow() {
        val key = progressKey ?: return
        progressStore.save(key, ReadingProgress(chapter, null, fraction))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        when {
            error != null -> ReaderMessage(
                title = strings.readerOpenFailed,
                body = error.orEmpty(),
                onBack = onBack
            )

            current == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ComposeColor(colors.foreground))
                    Spacer(Modifier.height(Space.s200))
                    Text(
                        text = strings.readerOpening,
                        color = ComposeColor(colors.foreground),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            else -> BookReaderView(
                book = current,
                chapter = chapter,
                chapterCount = current.chapterCount,
                anchor = anchor,
                locateChapter = locateChapter,
                locateFraction = locateFraction,
                locateAnchor = locateAnchor,
                locateToken = locateToken,
                // 還原進度那次用 pendingFraction；之後換方向、換翻頁方式時
                // 用當前位置，不然切一次設定就會被彈回章首。
                initialFraction = if (pendingFraction > 0f) pendingFraction else fraction,
                style = style,
                onOpenChapter = { target, fragment ->
                    if (target in 0 until current.chapterCount) {
                        // 一律走 locate：目標章不在 DOM 裡時腳本會就地換內容，
                        // 就在當前章時只是重新定位，都不會重載文件。
                        locateChapter = target
                        locateFraction = 0f
                        locateAnchor = fragment
                        locateToken++
                    }
                },
                onProgress = { currentPage, total, _ ->
                    // JS 的頁碼是 0 基；顯示用 1 基。
                    page = currentPage + 1
                    pages = total
                },
                onLocation = { currentChapter, currentFraction ->
                    // 章是頁內換的，這裡只更新標題與進度，不重載。
                    chapter = currentChapter
                    fraction = currentFraction
                },
                onCenterTap = { chromeVisible = !chromeVisible },
                onLoaded = {
                    opening = false
                    pendingFraction = 0f
                },
                // 只讓書頁避開狀態列／導覽列，左右留白交給書自己的版面——
                // 一旦在外層加水平內縮，翻頁看起來就會像「書在框裡滑」而不是整頁翻。
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            )
        }

        AnimatedVisibility(
            visible = chromeVisible && current != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            ReaderTopBar(
                title = displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                    ?: current?.metadata?.title.orEmpty(),
                subtitle = chapterTitle(current, chapter, strings.readerChapter(chapter + 1)),
                onBack = {
                    persistNow()
                    onBack()
                }
            )
        }

        AnimatedVisibility(
            visible = chromeVisible && current != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ReaderBottomBar(
                strings = strings,
                settings = settings,
                mode = mode,
                chapter = chapter,
                chapterCount = current?.chapterCount ?: 1,
                page = page,
                pages = pages,
                fraction = fraction,
                onOpenToc = { showToc = true },
                onOpenThemes = { showThemes = true },
                onOpenLayout = { showLayout = true },
                onOpenFonts = { showFonts = true },
                onToggleVertical = {
                    val toVertical = !settings.readerVertical
                    onChange(
                        settings.copy(
                            readerVertical = toVertical,
                            // 直排的長處是連續讀，切成直排時預設改成滾動；
                            // 想要直排翻頁也可以在「排版」裡改回來。
                            readerMode = if (toVertical) {
                                ReaderMode.defaultFor(true).id
                            } else {
                                settings.readerMode
                            }
                        )
                    )
                },
                onTextScale = { delta ->
                    val next = (settings.readerTextScale + delta).coerceIn(
                        AppSettingsPreferences.MIN_TEXT_SCALE,
                        AppSettingsPreferences.MAX_TEXT_SCALE
                    )
                    if (next != settings.readerTextScale) {
                        onChange(settings.copy(readerTextScale = next))
                    }
                }
            )
        }
    }

    if (showToc && current != null) {
        ModalBottomSheet(
            onDismissRequest = { showToc = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ReaderToc(
                toc = current.toc,
                chapter = chapter,
                emptyHint = strings.readerContents,
                fallback = strings.readerChapter,
                onPick = { entry ->
                    showToc = false
                    if (entry.chapter in 0 until current.chapterCount) {
                        // 一律走 locate：目標章不在 DOM 裡時腳本會就地換內容，
                        // 就在當前章時只是重新定位，都不會重載文件。
                        locateChapter = entry.chapter
                        locateFraction = 0f
                        locateAnchor = entry.anchor
                        locateToken++
                    }
                }
            )
        }
    }

    if (showThemes) {
        ModalBottomSheet(
            onDismissRequest = { showThemes = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ReaderThemePicker(
                current = theme,
                palettes = palettes,
                chinese = chinese,
                title = strings.theme,
                onPick = { picked ->
                    showThemes = false
                    onChange(settings.copy(readerTheme = picked.id))
                }
            )
        }
    }

    if (showFonts) {
        ModalBottomSheet(
            onDismissRequest = { showFonts = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ReaderFontSheet(
                title = strings.font,
                current = settings.readerFont,
                onPick = { fileName ->
                    showFonts = false
                    onChange(settings.copy(readerFont = fileName))
                },
                onOpenFontSettings = {
                    showFonts = false
                    onOpenFontSettings()
                }
            )
        }
    }

    if (showLayout) {
        ModalBottomSheet(
            onDismissRequest = { showLayout = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ReaderLayoutSheet(
                strings = strings,
                settings = settings,
                onMode = { picked -> onChange(settings.copy(readerMode = picked.id)) },
                onLineHeight = { tenths ->
                    onChange(settings.copy(readerLineHeightTenths = tenths))
                },
                onMargin = { dp -> onChange(settings.copy(readerMargin = dp)) }
            )
        }
    }
}

/** 目錄裡沒有這一章時退成「第 N 章」。 */
private fun chapterTitle(book: Book?, chapter: Int, fallback: String): String? {
    val entry = book?.toc?.firstOrNull { it.chapter == chapter && it.anchor == null }
    return entry?.title?.takeIf { it.isNotBlank() } ?: fallback
}

@Composable
private fun ReaderTopBar(title: String, subtitle: String?, onBack: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(56.dp)
                .padding(horizontal = Space.s50),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(Space.s200))
        }
    }
}

@Composable
private fun ReaderBottomBar(
    strings: AppStrings,
    settings: AppSettings,
    mode: ReaderMode,
    chapter: Int,
    chapterCount: Int,
    page: Int,
    pages: Int,
    fraction: Float,
    onOpenToc: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenLayout: () -> Unit,
    onOpenFonts: () -> Unit,
    onToggleVertical: () -> Unit,
    onTextScale: (Int) -> Unit
) {
    // 整本書的進度：第幾章加上章內比例，比只看單章更有意義。
    val total = chapterCount.coerceAtLeast(1)
    val overall = ((chapter + fraction.coerceIn(0f, 1f)) / total).coerceIn(0f, 1f)

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = Space.s200, vertical = Space.s150),
            verticalArrangement = Arrangement.spacedBy(Space.s125)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = strings.readerChapter(chapter + 1) + " / " + total,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (mode.isScroll) {
                        "${(fraction.coerceIn(0f, 1f) * 100).roundToInt()}%"
                    } else {
                        strings.readerProgress(page, pages)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { overall },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReaderAction(
                    icon = Icons.AutoMirrored.Filled.List,
                    label = strings.readerContents,
                    onClick = onOpenToc
                )
                ReaderAction(
                    icon = Icons.Default.Palette,
                    label = strings.theme,
                    onClick = onOpenThemes
                )
                ReaderAction(
                    icon = Icons.Default.Tune,
                    label = strings.readerLayout,
                    onClick = onOpenLayout
                )
                ReaderAction(
                    icon = Icons.Default.TextFields,
                    label = strings.font,
                    onClick = onOpenFonts
                )
                ReaderAction(
                    icon = Icons.Default.SwapHoriz,
                    label = if (settings.readerVertical) strings.horizontalLayout else strings.verticalLayoutLabel,
                    onClick = onToggleVertical
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.s50)
                ) {
                    ReaderTextAction(
                        text = "A−",
                        label = strings.readerFontSize,
                        enabled = settings.readerTextScale > AppSettingsPreferences.MIN_TEXT_SCALE,
                        onClick = { onTextScale(-AppSettingsPreferences.TEXT_SCALE_STEP) }
                    )
                    ReaderTextAction(
                        text = "A+",
                        label = "${settings.readerTextScale}%",
                        enabled = settings.readerTextScale < AppSettingsPreferences.MAX_TEXT_SCALE,
                        onClick = { onTextScale(AppSettingsPreferences.TEXT_SCALE_STEP) }
                    )
                }
            }
        }
    }
}

/**
 * 字型面板：系統字型 + 使用者匯入的字型，點一列就換。
 *
 * 選完直接寫回 `AppSettings.readerFont`，所以下次開任何一本書都還是這個字；
 * 匯入與刪除留在設定頁（見 FontSettingsScreen），面板只負責挑。
 */
@Composable
private fun ReaderFontSheet(
    title: String,
    current: String,
    onPick: (String) -> Unit,
    onOpenFontSettings: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val fonts = remember { FontStore.list(context) }
    Column(modifier = Modifier.navigationBarsPadding()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(
                start = Space.s300,
                end = Space.s300,
                top = Space.s100,
                bottom = Space.s150
            )
        )
        ReaderFontRow(
            label = strings.systemFont,
            selected = current == FontStore.SYSTEM || current.isBlank(),
            family = null,
            onClick = { onPick(FontStore.SYSTEM) }
        )
        fonts.forEach { font ->
            ReaderFontRow(
                label = font.displayName,
                selected = current == font.fileName,
                family = noteFontFamily(font.fileName),
                onClick = { onPick(font.fileName) }
            )
        }
        if (fonts.isEmpty()) {
            Text(
                text = strings.fontEmpty,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Space.s300, vertical = Space.s100)
            )
        }
        TextButton(
            onClick = onOpenFontSettings,
            modifier = Modifier.padding(horizontal = Space.s200, vertical = Space.s100)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(Space.s250))
            Spacer(Modifier.size(Space.s100))
            Text(strings.fontImport)
        }
        Spacer(Modifier.size(Space.s200))
    }
}

@Composable
private fun ReaderFontRow(
    label: String,
    selected: Boolean,
    family: FontFamily?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Space.s300, vertical = Space.s150),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s200)
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = family),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ReaderAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            // HIG 的可點區域下限是 44pt，M3 取 48dp。只靠圖示 + 內距，寬度會跟著
            // 一兩個字的標籤縮到 34dp 左右（「目錄」「主題」都是），所以明確給
            // 下限，而不是讓文字長度決定能不能點到。
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(Space.s150))
            .clickable(onClick = onClick)
            .padding(horizontal = Space.s50, vertical = Space.s75)
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun ReaderTextAction(
    text: String,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(Space.s150))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Space.s50, vertical = Space.s75)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/** 目錄面板。 */
@Composable
private fun ReaderToc(
    toc: List<TocEntry>,
    chapter: Int,
    emptyHint: String,
    fallback: (Int) -> String,
    onPick: (TocEntry) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = emptyHint,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(Space.s200)
        )
        if (toc.isEmpty()) {
            Text(
                text = "—",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Space.s200, vertical = Space.s100)
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = Space.s400)) {
                itemsIndexed(toc) { _, entry ->
                    val selected = entry.chapter == chapter && entry.anchor == null
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(entry) }
                            .padding(
                                start = Space.s200 + (entry.depth.coerceIn(0, 4) * 16).dp,
                                end = Space.s200,
                                top = Space.s125,
                                bottom = Space.s125
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = entry.title.takeIf { it.isNotBlank() }
                                ?: fallback(entry.chapter + 1),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (selected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderThemePicker(
    current: ReadingTheme,
    palettes: Map<ReadingTheme, ReadingColors>,
    chinese: Boolean,
    title: String,
    onPick: (ReadingTheme) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = Space.s200, vertical = Space.s200)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Space.s200))
        // 六套排成 3×2，不是一列六個。
        // 360dp 的手機扣掉邊距只剩 328dp，一列六個的話每個不到 46dp，
        // 名字會被壓成「素…」；兩列三個，圓點才留得住 52dp。
        ReadingTheme.entries.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.s150)
            ) {
                row.forEach { theme ->
                    ThemeSwatch(
                        theme = theme,
                        colors = palettes[theme] ?: ReadingColors.PAPER,
                        selected = theme == current,
                        chinese = chinese,
                        onPick = onPick,
                        modifier = Modifier.weight(1f)
                    )
                }
                // 最後一行不滿三個時補等重的空白，圓點才不會被拉寬、和上一行對不齊。
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(Space.s200))
    }
}

/**
 * 一個主題圓點。
 *
 * 底紙、正文、波紋全部跟著莫奈配色走：圓點畫的就是「選了以後書長什麼樣」，
 * 不是另一套示意色。所以換壁紙之後，這裡六顆會一起換色——它們本來就同源。
 */
@Composable
private fun ThemeSwatch(
    theme: ReadingTheme,
    colors: ReadingColors,
    selected: Boolean,
    chinese: Boolean,
    onPick: (ReadingTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    val paper = ComposeColor(0xFF000000L or (colors.background and 0xFFFFFF))
    val ink = ComposeColor(0xFF000000L or (colors.foreground and 0xFFFFFF))
    // 「預設」的底紙就是 App 的底，和面板同色，只靠邊框會看不出那是一顆圓。
    // 給它一圈主色，順便說明「這個是跟著主色走的」。
    val ring = if (selected || theme == ReadingTheme.DEFAULT) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            // 整列都是熱區（點名字也選得到），但遮罩不畫在這一層：
            // 水波紋的邊界就是節點邊界，放在這裡會變成一個整列寬、
            // 比圓點高兩倍的方框。這裡只當熱區，遮罩交給圓點自己畫。
            // 用 selectable 而不是 clickable：這是單選，TalkBack 要能
            // 唸出「已選中」。
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.RadioButton
            ) { onPick(theme) }
            .padding(vertical = Space.s50)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                // clip 在 clickable 之前：水波紋因此被裁成圓形，
                // 跟這個按鈕實際的形狀一致（Material 的圓形選項按鈕就是這樣）。
                .clip(CircleShape)
                // 語意交給外面那一列：一列只有一個可點節點，
                // 不然 TalkBack 會把同一個主題唸兩次。
                .clearAndSetSemantics { }
                .background(paper, CircleShape)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = ring,
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    // 波紋顏色跟著正文走：淺底是深波紋、墨黑底是淺波紋，
                    // 用主題色會在墨黑上完全看不見。
                    indication = ripple(color = ink.copy(alpha = 0.28f)),
                    role = Role.RadioButton
                ) { onPick(theme) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "文",
                color = ink,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.height(Space.s75))
        Text(
            text = ReadingTheme.label(theme, chinese),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 排版面板：翻頁方式、行距、頁邊留白。
 *
 * 這三項是「同一本書、同一個字級下，看起來還是差很多」的主因，
 * 所以跟主題分開——主題是配色，這裡是版面本身。
 */
@Composable
private fun ReaderLayoutSheet(
    strings: AppStrings,
    settings: AppSettings,
    onMode: (ReaderMode) -> Unit,
    onLineHeight: (Int) -> Unit,
    onMargin: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.s200)
            .padding(bottom = Space.s400),
        verticalArrangement = Arrangement.spacedBy(Space.s200)
    ) {
        Text(
            text = strings.readerLayout,
            style = MaterialTheme.typography.titleMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s100)) {
            FilterChip(
                selected = settings.readingMode == ReaderMode.PAGED,
                onClick = { onMode(ReaderMode.PAGED) },
                label = { Text(strings.readerModePaged) }
            )
            // 直排的內容在橫向上展開，豎著滾沒有可滾的餘量，所以不提供這一檔。
            if (!settings.readerVertical) {
                FilterChip(
                    selected = settings.readingMode == ReaderMode.SCROLL_VERTICAL,
                    onClick = { onMode(ReaderMode.SCROLL_VERTICAL) },
                    label = { Text(strings.readerModeScrollVertical) }
                )
            }
            FilterChip(
                selected = settings.readingMode == ReaderMode.SCROLL_HORIZONTAL,
                onClick = { onMode(ReaderMode.SCROLL_HORIZONTAL) },
                label = { Text(strings.readerModeScrollHorizontal) }
            )
        }
        ReaderStepper(
            label = strings.readerLineHeight,
            value = String.format("%.1f", settings.readingLineHeight),
            canDecrease = settings.readerLineHeightTenths > AppSettingsPreferences.MIN_LINE_HEIGHT_TENTHS,
            canIncrease = settings.readerLineHeightTenths < AppSettingsPreferences.MAX_LINE_HEIGHT_TENTHS,
            onDecrease = {
                onLineHeight(
                    (settings.readerLineHeightTenths - AppSettingsPreferences.LINE_HEIGHT_STEP)
                        .coerceAtLeast(AppSettingsPreferences.MIN_LINE_HEIGHT_TENTHS)
                )
            },
            onIncrease = {
                onLineHeight(
                    (settings.readerLineHeightTenths + AppSettingsPreferences.LINE_HEIGHT_STEP)
                        .coerceAtMost(AppSettingsPreferences.MAX_LINE_HEIGHT_TENTHS)
                )
            }
        )
        ReaderStepper(
            label = strings.readerMargin,
            value = "${settings.readerMargin}",
            canDecrease = settings.readerMargin > AppSettingsPreferences.MIN_MARGIN,
            canIncrease = settings.readerMargin < AppSettingsPreferences.MAX_MARGIN,
            onDecrease = {
                onMargin(
                    (settings.readerMargin - AppSettingsPreferences.MARGIN_STEP)
                        .coerceAtLeast(AppSettingsPreferences.MIN_MARGIN)
                )
            },
            onIncrease = {
                onMargin(
                    (settings.readerMargin + AppSettingsPreferences.MARGIN_STEP)
                        .coerceAtMost(AppSettingsPreferences.MAX_MARGIN)
                )
            }
        )
    }
}

@Composable
private fun ReaderStepper(
    label: String,
    value: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onDecrease, enabled = canDecrease) { Text("−") }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.Center
        )
        TextButton(onClick = onIncrease, enabled = canIncrease) { Text("+") }
    }
}

@Composable
private fun ReaderMessage(title: String, body: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Space.s300),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(Space.s150))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Space.s300))
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        }
    }
}

/**
 * 閱讀計時的節拍。
 *
 * 5 秒一次：夠細到「翻兩頁就走」也記得到，又不會讓統計檔一天多出幾千行
 * （同一天同一本書本來就會累加成一行，這裡只是控制寫入頻率）。
 */
private const val READ_TICK_MS = 5_000L

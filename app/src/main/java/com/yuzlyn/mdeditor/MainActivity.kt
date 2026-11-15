package com.yuzlyn.mdeditor

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.yuzlyn.mdeditor.ui.navigation.Author
import com.yuzlyn.mdeditor.ui.navigation.DarkModeSettings
import com.yuzlyn.mdeditor.ui.navigation.FontSettings
import com.yuzlyn.mdeditor.ui.navigation.Help
import com.yuzlyn.mdeditor.ui.navigation.LanguageSettings
import com.yuzlyn.mdeditor.ui.navigation.LayoutSettings
import com.yuzlyn.mdeditor.ui.navigation.NoteBackgroundSettings
import com.yuzlyn.mdeditor.ui.navigation.NoteEditor
import com.yuzlyn.mdeditor.ui.navigation.Home
import com.yuzlyn.mdeditor.ui.navigation.PdfViewer
import com.yuzlyn.mdeditor.ui.navigation.Profile
import com.yuzlyn.mdeditor.ui.navigation.Reader
import com.yuzlyn.mdeditor.ui.navigation.Search
import com.yuzlyn.mdeditor.ui.navigation.Settings
import com.yuzlyn.mdeditor.ui.navigation.StatsSettings
import com.yuzlyn.mdeditor.ui.navigation.Support
import com.yuzlyn.mdeditor.ui.navigation.ThemeSettings
import com.yuzlyn.mdeditor.ui.screen.AuthorScreen
import com.yuzlyn.mdeditor.ui.screen.DarkModeSettingsScreen
import com.yuzlyn.mdeditor.ui.screen.EditorScreen
import com.yuzlyn.mdeditor.ui.screen.FontSettingsScreen
import com.yuzlyn.mdeditor.ui.screen.HelpScreen
import com.yuzlyn.mdeditor.ui.screen.HomeSection
import com.yuzlyn.mdeditor.ui.screen.NoteBackgroundSettingsScreen
import com.yuzlyn.mdeditor.ui.screen.SupportScreen
import com.yuzlyn.mdeditor.ui.screen.LanguageSettingsScreen
import com.yuzlyn.mdeditor.ui.screen.LayoutSettingsScreen
import com.yuzlyn.mdeditor.ui.screen.MainScreen
import com.yuzlyn.mdeditor.ui.screen.PdfViewerScreen
import com.yuzlyn.mdeditor.data.book.BookLibrary
import com.yuzlyn.mdeditor.ui.component.HomeDrawer
import com.yuzlyn.mdeditor.ui.library.LibraryScreen
import com.yuzlyn.mdeditor.ui.reader.ReaderScreen
import com.yuzlyn.mdeditor.ui.screen.ProfileScreen
import com.yuzlyn.mdeditor.ui.screen.SearchScreen
import com.yuzlyn.mdeditor.ui.screen.SettingsScreen
import com.yuzlyn.mdeditor.ui.screen.ThemeSettingsScreen
import com.yuzlyn.mdeditor.ui.stats.StatsScreen
import com.yuzlyn.mdeditor.ui.stats.StatsSettingsScreen
import com.yuzlyn.mdeditor.data.settings.HomeTarget
import com.yuzlyn.mdeditor.data.settings.NoteLayout
import com.yuzlyn.mdeditor.data.settings.ThemeMode
import com.yuzlyn.mdeditor.ui.theme.LauncherIcon
import com.yuzlyn.mdeditor.ui.theme.MDEditorTheme
import com.yuzlyn.mdeditor.ui.viewmodel.FileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 啟動看門狗：真機「打開就卡住」時，`adb logcat -s MDStart` 能直接看出
        // 卡在建造 ViewModel、還是卡在讀設定——不必再靠猜。
        Log.i("MDStart", "onCreate")
        enableEdgeToEdge()
        setContent {
            LaunchedEffect(Unit) { Log.i("MDStart", "first-frame") }
            val fileViewModel: FileViewModel = viewModel()
            val appSettings by fileViewModel.appSettings.collectAsState()
            // 桌面图标跟随 App 内主色：
            // - 固定种子色 → 精确同色（七套别名里挑一套）
            // - 莫奈取色   → 主色由壁纸决定，只能取色轮上最近的一格
            // 放到后台线程是因为 dynamicLightColorScheme 要读一整套系统资源，
            // 冷启动时和首帧渲染抢主线程不划算。
            val iconContext = LocalContext.current
            LaunchedEffect(appSettings.themeMode, appSettings.seedColor, appSettings.launcherGlyph) {
                withContext(Dispatchers.Default) {
                    val monetPrimary = if (
                        appSettings.themeMode == ThemeMode.MONET &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ) {
                        // 取色失败时 Theme 会回落到种子色派生，图标也得跟着回落，否则两边又错位。
                        runCatching { dynamicLightColorScheme(iconContext).primary.toArgb() }.getOrNull()
                    } else {
                        null
                    }
                    if (monetPrimary != null) {
                        LauncherIcon.syncMonet(iconContext, monetPrimary, appSettings.launcherGlyph)
                    } else {
                        LauncherIcon.sync(iconContext, appSettings.seedColor, appSettings.launcherGlyph)
                    }
                }
            }
            MDEditorTheme(settings = appSettings) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    val notes by fileViewModel.notes.collectAsState()
                    val stats by fileViewModel.stats.collectAsState()
                    val notebookTags by fileViewModel.notebookTags.collectAsState()
                    val profile by fileViewModel.profile.collectAsState()
                    val hasFolderSelected by fileViewModel.hasSelectedFolder.collectAsState()
                    val isImporting by fileViewModel.isImporting.collectAsState()
                    val importError by fileViewModel.importError.collectAsState()

                    // 啟動頁由設定決定（全部筆記／書庫）。DataStore 是異步的，
                    // 先等第一份真實設定再建根頁——否則會先畫一幀預設主頁再跳過去。
                    var homeTarget by remember { mutableStateOf<HomeTarget?>(null) }
                    // 抽屜狀態放在兩個主頁之上。
                    //
                    // 抽屜是每個主頁各畫一份的，但「開著沒開著」不能各記一份：
                    // 在筆記頁點「書庫」時，若書庫那一頁以為抽屜是關的，它會從
                    // 畫面外再滑進來一次；而且舊的那份一被銷毀，抽屜就瞬間消失、
                    // 新頁面直接閃進來——就是「切過去沒有動畫」。
                    // 共用一份狀態，新的那份就會接著舊的進度繼續滑出去。
                    var drawerOpen by remember { mutableStateOf(false) }
                    // 筆記主頁內部的「全部筆記 / 筆記本」兩段。提到主頁之上，是因為
                    // 抽屜要能從**任何一頁**直接跳到「筆記本」；若它還留在 MainScreen
                    // 裡面，書庫那一頁就點不到。
                    var homeSection by remember { mutableStateOf(HomeSection.NOTES) }
                    // 筆記頁目前在看的筆記本；null = 全部筆記。同樣提到上面：
                    // 從抽屜點「全部筆記」就該真的回到全部，而不是還停在上一本的過濾。
                    var notesTag by remember { mutableStateOf<String?>(null) }
                    // 書庫的「資料夾」面板同理：在筆記頁按下它，要先切到書庫再掀開面板。
                    var libraryFolderPanel by remember { mutableStateOf(false) }
                    // 根頁一開始就在返回棧裡。畫面的渲染不能等 DataStore 那一次讀取：
                    // 它只要拋一次例外（檔案損壞、被別的實例佔用、磁碟忙），
                    // 就再也沒有人把根頁推進返回棧，介面就永遠停在空白——看起來就是卡死。
                    val backStack = remember { mutableStateListOf<NavKey>(Home) }
                    // 兩個主頁各自保留自己的狀態：切去書庫再回來，筆記列表的捲動位置還在。
                    val homeStateHolder = rememberSaveableStateHolder()
                    // 抽屜的每一列都走這裡：換頁、換段、關抽屜三件事一起做。
                    //
                    // 抽屜現在只畫一份、站在兩個主頁之上，關它只是把同一個節點滑出去；
                    // 和換頁發生在同一幀也不會斷，所以不必再「等新頁面組出來才敢關」。
                    // 舊版每一步都要錯開時機（先換頁、再收抽屜），正是抽屜被畫了兩份的代價。
                    fun goHome(
                        target: HomeTarget,
                        section: HomeSection = HomeSection.NOTES,
                        openFolderPanel: Boolean = false
                    ) {
                        homeTarget = target
                        // 統計頁用不到這兩個，關掉總開關時它們也不該半開著。
                        if (target.isStats) libraryFolderPanel = false
                        if (target == HomeTarget.NOTES) {
                            homeSection = section
                            // 「全部筆記」＝真的全部：連上一本的過濾一起清掉。
                            if (section == HomeSection.NOTES) notesTag = null
                        }
                        libraryFolderPanel = openFolderPanel
                        drawerOpen = false
                    }

                    LaunchedEffect(Unit) {
                        if (homeTarget == null) {
                            // 讀不到就當「全部筆記」：啟動頁是偏好設定，不該成為能不能進 App 的門檻。
                            val target = runCatching {
                                withTimeout(START_PAGE_TIMEOUT_MS) { fileViewModel.startPage() }
                            }
                            Log.i("MDStart", "startPage ok=${target.isSuccess} value=${target.getOrNull()}")
                            homeTarget = target.getOrDefault(HomeTarget.NOTES)
                        }
                    }
                    // 書庫：匯入的電子書留在 App 目錄裡，標題列／抽屜都能再打開。
                    val bookLibrary = remember { BookLibrary(this@MainActivity) }
                    val navScope = rememberCoroutineScope()
                    // 書庫抽屜裡的「更換資料夾」是筆記的儲存位置，選擇器留在這裡共用。
                    val notesFolderLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocumentTree()
                    ) { uri -> if (uri != null) fileViewModel.onFolderSelected(uri) }
                    val toggleNoteLayout = {
                        fileViewModel.updateAppSettings { current ->
                            current.copy(
                                noteLayout = if (current.noteLayout == NoteLayout.MASONRY) {
                                    NoteLayout.LIST
                                } else {
                                    NoteLayout.MASONRY
                                }
                            )
                        }
                    }

                    val motion = MaterialTheme.motionScheme
                    val slowSpatial = motion.slowSpatialSpec<IntOffset>()
                    val slowSpatialFloat = motion.slowSpatialSpec<Float>()
                    val slowEffects = motion.slowEffectsSpec<Float>()
                    val fastEffects = motion.fastEffectsSpec<Float>()

                    // 轉場只寫一份，每個 NavEntry 掛同一組，行為才會一致。
                    // 進頁：位移走 spatial、透明度走 effects，再疊一個 0.96→1 的放大。
                    // 只位移會像「換頁」，加上縮放才有「一層紙被推上來」的實體感——
                    // 這就是作者頁那種入場感的來源。
                    val pushTransition = NavDisplay.transitionSpec {
                        val enter = slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            slowSpatial
                        ) + scaleIn(
                            animationSpec = slowSpatialFloat,
                            initialScale = 0.96f
                        ) + fadeIn(slowEffects)
                        val exit = slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            slowSpatial
                        ) + fadeOut(fastEffects)
                        enter togetherWith exit
                    }
                    // 返回：**下層頁面完全不動**。
                    // 首頁是滿版容器（左側欄、頂欄都貼著螢幕邊緣），只要它跟著轉場位移，
                    // 左緣就會離開螢幕、露出一條背景——預測性返回（手指往左滑）時這條縫
                    // 會持續一整個手勢，看起來就是「左邊往右挪了一下」。
                    // 所以只讓被關掉的那一頁向右滑走，下層永遠滿版、貼齊邊緣。
                    val popTransition = NavDisplay.popTransitionSpec {
                        EnterTransition.None togetherWith (
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                slowSpatial
                            ) + scaleOut(
                                animationSpec = slowSpatialFloat,
                                targetScale = 0.98f
                            ) + fadeOut(fastEffects)
                            )
                    }
                    val predictivePopTransition = NavDisplay.predictivePopTransitionSpec {
                        EnterTransition.None togetherWith (
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                slowSpatial
                            ) + scaleOut(
                                animationSpec = slowSpatialFloat,
                                targetScale = 0.98f
                            ) + fadeOut(fastEffects)
                            )
                    }
                    val pageTransition =
                        pushTransition + popTransition + predictivePopTransition
                    // 首頁自己作為「進場方」時只淡入淡出，不位移、不縮放。
                    val homeTransition = NavDisplay.transitionSpec {
                        fadeIn(slowEffects) togetherWith fadeOut(fastEffects)
                    } + NavDisplay.popTransitionSpec {
                        fadeIn(slowEffects) togetherWith fadeOut(fastEffects)
                    } + NavDisplay.predictivePopTransitionSpec {
                        fadeIn(slowEffects) togetherWith fadeOut(fastEffects)
                    }

                    if (backStack.isNotEmpty()) NavDisplay(
                        backStack = backStack,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeLastOrNull()
                            } else {
                                // 根頁按返回就是離開 App，不要留一個空的返回棧。
                                finish()
                            }
                        },
                        entryProvider = { key ->
                            when (key) {
                                is Home -> NavEntry(
                                    key,
                                    metadata = homeTransition
                                ) {
                                    // 讀到之前先按預設渲染：DataStore 正常是幾十毫秒，
                                    // 但就算它慢或卡住，畫面也已經在，不會是一片空白。
                                    val target = homeTarget ?: HomeTarget.NOTES
                                    // 兩個主頁 + **一份**抽屜。
                                    //
                                    // 抽屜畫在主頁之上、只畫一次：內容不會隨頁面變形，
                                    // 切頁時也不會被銷毀重建（重建就等於動畫從頭播一次，
                                    // 看起來就是「切過去沒有動畫」）。
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        homeStateHolder.SaveableStateProvider(target.id) {
                                            if (target.isStats) {
                                                StatsScreen(
                                                    snapshot = stats,
                                                    settings = appSettings,
                                                    profile = profile,
                                                    onOpenProfile = { backStack.add(Profile) },
                                                    onOpenDrawer = { drawerOpen = true },
                                                    onOpenStatsSettings = {
                                                        backStack.add(StatsSettings)
                                                    },
                                                    onEnableStats = {
                                                        fileViewModel.updateAppSettings {
                                                            it.copy(statsEnabled = true)
                                                        }
                                                    },
                                                    onOpenBook = { uri, name ->
                                                        backStack.add(Reader(uri, name))
                                                    },
                                                    onOpenNote = { noteId ->
                                                        backStack.add(NoteEditor(noteId))
                                                    }
                                                )
                                            } else if (target.isLibrary) {
                                                LibraryScreen(
                                                    avatarPath = profile.avatarPath,
                                                    layout = appSettings.noteLayout,
                                                    cardShape = appSettings.cardShape,
                                                    // 面板由抽屜那一列控制：在筆記頁按「資料夾」
                                                    // 也是先切到書庫、再掀開它。
                                                    showFolderPanel = libraryFolderPanel,
                                                    onDismissFolderPanel = {
                                                        libraryFolderPanel = false
                                                    },
                                                    onOpenDrawer = { drawerOpen = true },
                                                    onToggleLayout = toggleNoteLayout,
                                                    onOpenProfile = { backStack.add(Profile) },
                                                    onOpenBook = { uri, name ->
                                                        backStack.add(Reader(uri, name))
                                                    }
                                                )
                                            } else {
                                                MainScreen(
                                                    notes = notes,
                                                    notebookTags = notebookTags,
                                                    hasFolderSelected = hasFolderSelected,
                                                    isImporting = isImporting,
                                                    settings = appSettings,
                                                    onSettingsChange = { next ->
                                                        fileViewModel.updateAppSettings { next }
                                                    },
                                                    avatarPath = profile.avatarPath,
                                                    // 段（全部筆記／筆記本）放在主頁之上，
                                                    // 抽屜才能從任何一頁切進來。
                                                    section = homeSection,
                                                    onSectionChange = { homeSection = it },
                                                    selectedTag = notesTag,
                                                    onSelectTag = { notesTag = it },
                                                    onNoteClick = { note ->
                                                        backStack.add(NoteEditor(note.id))
                                                    },
                                                    onCreateNewNote = { title, tag ->
                                                        fileViewModel.createNewNote(title, tag)
                                                    },
                                                    onFolderSelected = { uri ->
                                                        fileViewModel.onFolderSelected(uri)
                                                    },
                                                    onDeleteNotes = { ids ->
                                                        ids.forEach { id ->
                                                            fileViewModel.deleteNote(id)
                                                        }
                                                    },
                                                    onOpenSearch = { backStack.add(Search) },
                                                    onOpenProfile = { backStack.add(Profile) },
                                                    onOpenDrawer = { drawerOpen = true },
                                                    importError = importError,
                                                    onImportErrorShown = {
                                                        fileViewModel.clearImportError()
                                                    },
                                                    onImportDocument = { uri ->
                                                        fileViewModel.openDocument(
                                                            source = uri,
                                                            // 電子書：開原生閱讀器，不進編輯器、也不轉成筆記。
                                                            onBook = { bookUri, name ->
                                                                // 先收進書庫再打開：書原本只是 SAF 的一個
                                                                // 臨時授權，回到主頁就沒有入口，授權過期
                                                                // 還會打不開。複製一份到 App 目錄才是持久的。
                                                                navScope.launch {
                                                                    val entry = runCatching {
                                                                        bookLibrary.import(
                                                                            android.net.Uri.parse(bookUri),
                                                                            name
                                                                        )
                                                                    }.getOrNull()
                                                                    backStack.add(
                                                                        Reader(
                                                                            uri = entry?.let {
                                                                                bookLibrary.uriOf(it)
                                                                                    .toString()
                                                                            } ?: bookUri,
                                                                            displayName = entry?.displayName
                                                                                ?: name
                                                                        )
                                                                    )
                                                                }
                                                            },
                                                            onNote = { noteId ->
                                                                backStack.add(NoteEditor(noteId))
                                                            }
                                                        )
                                                    }
                                                )
                                            }
                                        }

                                        // 抽屜：整份只有這一個實例，七列內容固定不變。
                                        HomeDrawer(
                                            open = drawerOpen,
                                            // 用「等於」而不是「不是書庫」：多了統計這一頁之後，
                                            // `!isLibrary` 在統計頁也成立，會同時點亮兩列。
                                            notesSelected = target == HomeTarget.NOTES &&
                                                    homeSection == HomeSection.NOTES,
                                            notebookSelected = target == HomeTarget.NOTES &&
                                                    homeSection == HomeSection.NOTEBOOKS,
                                            librarySelected = target.isLibrary,
                                            statsSelected = target.isStats,
                                            showStats = appSettings.statsEnabled,
                                            onDismiss = { drawerOpen = false },
                                            onOpenNotes = {
                                                goHome(HomeTarget.NOTES, HomeSection.NOTES)
                                            },
                                            onOpenNotebooks = {
                                                goHome(HomeTarget.NOTES, HomeSection.NOTEBOOKS)
                                            },
                                            onOpenLibrary = { goHome(HomeTarget.LIBRARY) },
                                            onOpenStats = { goHome(HomeTarget.STATS) },
                                            onManageFolders = {
                                                goHome(HomeTarget.LIBRARY, openFolderPanel = true)
                                            },
                                            onChangeFolder = {
                                                drawerOpen = false
                                                notesFolderLauncher.launch(null)
                                            },
                                            onOpenSettings = {
                                                drawerOpen = false
                                                backStack.add(Settings)
                                            },
                                            onOpenHelp = {
                                                drawerOpen = false
                                                backStack.add(Help)
                                            },
                                            onOpenAuthor = {
                                                drawerOpen = false
                                                backStack.add(Author)
                                            }
                                        )
                                    }
                                }

                                is NoteEditor -> NavEntry(
                                    key,
                                    metadata = pageTransition
                                ) {
                                    EditorScreen(
                                        noteId = key.noteId,
                                        viewModel = fileViewModel,
                                        onNavigateBack = { backStack.removeLastOrNull() },
                                        // PDF 匯入的筆記可以回到原始版面看一眼。
                                        onOpenSourcePdf = { uri -> backStack.add(PdfViewer(uri)) },
                                        onOpenNoteBackground = {
                                            backStack.add(NoteBackgroundSettings)
                                        }
                                    )
                                }

                                is Settings -> NavEntry(
                                    key,
                                    metadata = pageTransition
                                ) {
                                    SettingsScreen(
                                        settings = appSettings,
                                        onChange = { next ->
                                            fileViewModel.updateAppSettings { next }
                                        },
                                        onOpenTheme = { backStack.add(ThemeSettings) },
                                        onOpenDarkMode = { backStack.add(DarkModeSettings) },
                                        onOpenFont = { backStack.add(FontSettings) },
                                        onOpenNoteBackground = {
                                            backStack.add(NoteBackgroundSettings)
                                        },
                                        onOpenLanguage = { backStack.add(LanguageSettings) },
                                        onOpenLayout = { backStack.add(LayoutSettings) },
                                        onOpenStats = { backStack.add(StatsSettings) },
                                        onBack = { backStack.removeLastOrNull() }
                                    )
                                }

                                is FontSettings -> NavEntry(
                                    key,
                                    metadata = pageTransition
                                ) {
                                    FontSettingsScreen(
                                        settings = appSettings,
                                        onChange = { next ->
                                            fileViewModel.updateAppSettings { next }
                                        },
                                        onBack = { backStack.removeLastOrNull() }
                                    )
                                }

                                is NoteBackgroundSettings -> NavEntry(
                                    key,
                                    metadata = pageTransition
                                ) {
                                    NoteBackgroundSettingsScreen(
                                        settings = appSettings,
                                        onChange = { next ->
                                            fileViewModel.updateAppSettings { next }
                                        },
                                        onBack = { backStack.removeLastOrNull() }
                                    )
                                }

                                is ThemeSettings -> NavEntry(
                                    key,
                                    metadata = pageTransition
                                ) {
                                    ThemeSettingsScreen(
                                        settings = appSettings,
                                        onChange = { next ->
                                            fileViewModel.updateAppSettings { next }
                                        },
                                        onBack = { backStack.removeLastOrNull() }
                                    )
                                }

                                is DarkModeSettings -> NavEntry(
                                    key,
                                    metadata = pageTransition
                                ) {
                                    DarkModeSettingsScreen(
                                        settings = appSettings,
                                        onChange = { next ->
                                            fileViewModel.updateAppSettings { next }
                                        },
                                        onBack = { backStack.removeLastOrNull() }
                                    )
                                }

                                is LanguageSettings -> NavEntry(
                                    key,
                                    metadata = pageTransition
                                ) {
                                    LanguageSettingsScreen(
                                        settings = appSettings,
                                        onChange = { next ->
                                            fileViewModel.updateAppSettings { next }
                                        },
                                        onBack = { backStack.removeLastOrNull() }
                                    )
                                }

                                is LayoutSettings -> NavEntry(
                                    key,
                                    metadata = pageTransition
                                ) {
                                    LayoutSettingsScreen(
                                        settings = appSettings,
                                        onChange = { next ->
                                            fileViewModel.updateAppSettings { next }
                                        },
                                        onBack = { backStack.removeLastOrNull() }
                                    )
                                }

                                is Reader -> NavEntry(
                                    key,
                                    metadata = pageTransition
                                ) {
                                    ReaderScreen(
                                        uriString = key.uri,
                                        displayName = key.displayName,
                                        settings = appSettings,
                                        onChange = { next ->
                                            fileViewModel.updateAppSettings { next }
                                        },
                                        onBack = { backStack.removeLastOrNull() },
                                        // 統計：書的 key 就是打開它用的 Uri，
                                        // 排行榜點回來時才接得上同一本。
                                        onReading = { seconds ->
                                            fileViewModel.recordReading(
                                                key = key.uri,
                                                name = key.displayName,
                                                seconds = seconds
                                            )
                                        }
                                    )
                                }

                                is PdfViewer -> NavEntry(
                                    key,
                                    metadata = pageTransition
                                ) {
                                    PdfViewerScreen(
                                        uriString = key.uri,
                                        onBack = { backStack.removeLastOrNull() }
                                    )
                                }

                                is Help -> NavEntry(
                                    key,
                                    metadata = pageTransition
                                ) {
                                    HelpScreen(
                                        language = appSettings.language.resolved(),
                                        onBack = { backStack.removeLastOrNull() },
                                        onOpenAuthor = { backStack.add(Author) }
                                    )
                                }

                                is Search -> NavEntry(
                                    key,
                                    metadata = pageTransition
                                ) {
                                    SearchScreen(
                                        notes = notes,
                                        onOpenNote = { noteId ->
                                            backStack.add(NoteEditor(noteId))
                                        },
                                        onBack = { backStack.removeLastOrNull() }
                                    )
                                }

                                is StatsSettings -> NavEntry(
                                    key,
                                    metadata = pageTransition
                                ) {
                                    StatsSettingsScreen(
                                        settings = appSettings,
                                        onChange = { next ->
                                            fileViewModel.updateAppSettings { next }
                                        },
                                        onClear = { fileViewModel.clearStats() },
                                        onBack = { backStack.removeLastOrNull() }
                                    )
                                }

                                is Author -> NavEntry(
                                    key,
                                    metadata = pageTransition
                                ) {
                                    AuthorScreen(
                                        onBack = { backStack.removeLastOrNull() },
                                        onOpenSupport = { backStack.add(Support) }
                                    )
                                }

                                is Support -> NavEntry(
                                    key,
                                    metadata = pageTransition
                                ) {
                                    SupportScreen(onBack = { backStack.removeLastOrNull() })
                                }

                                is Profile -> NavEntry(
                                    key,
                                    metadata = pageTransition
                                ) {
                                    ProfileScreen(
                                        profile = profile,
                                        onProfileChange = { transform ->
                                            fileViewModel.updateProfile(transform)
                                        },
                                        onBack = { backStack.removeLastOrNull() }
                                    )
                                }

                                else -> NavEntry(key) { }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * 讀「啟動頁」設定的上限。
 *
 * DataStore 正常是幾十毫秒，但它的檔案可能損壞、可能被別的實例佔著、磁碟也可能正忙。
 * 這種時候寧可先按預設主頁進去，也不能讓使用者盯著一片空白。
 */
private const val START_PAGE_TIMEOUT_MS = 1_500L

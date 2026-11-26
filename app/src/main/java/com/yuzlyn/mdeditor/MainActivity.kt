package com.yuzlyn.mdeditor

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
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
import com.yuzlyn.mdeditor.data.settings.LauncherGlyph
import com.yuzlyn.mdeditor.data.settings.NoteLayout
import com.yuzlyn.mdeditor.data.settings.SeedColor
import com.yuzlyn.mdeditor.data.settings.SplashCache
import com.yuzlyn.mdeditor.data.settings.ThemeMode
import com.yuzlyn.mdeditor.ui.component.SplashOverlay
import com.yuzlyn.mdeditor.ui.theme.LauncherIcon
import com.yuzlyn.mdeditor.ui.theme.MDEditorTheme
import com.yuzlyn.mdeditor.ui.viewmodel.FileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class MainActivity : ComponentActivity() {

    private val fileViewModel: FileViewModel by viewModels()

    /**
     * 開屏第一幀要用的主題快照。
     *
     * 系統的起始視窗只能用編譯期資源，跟不上 App 內的亮暗、莫奈取色與自選字形；
     * 真正的開屏由 Compose 畫（見 `ui/component/SplashOverlay.kt`），
     * 而第一幀時 DataStore 還沒讀回來，所以先照上次套用的結果畫。
     */
    private lateinit var splashCache: SplashCache
    private lateinit var splashSnapshot: SplashCache.Snapshot

    /** 只有真正的冷啟動才播開屏；轉屏、分屏、改日夜模式重建時不要再閃一次。 */
    private var playSplash = false

    /**
     * 別的 App 遞過來、還沒處理的檔案。
     *
     * 從 QQ／微信／檔案管理器「用其他應用程式開啟」時，系統只給一份**一次性的**
     * 讀取授權；這裡先把 Uri 接住，Compose 那邊一收到就複製進書庫，
     * 之後從書庫打開才不會因為授權失效而開不起來。
     */
    private val externalOpen = mutableStateOf<Uri?>(null)

    /**
     * 已經開著的時候又遞來一本書：清單裡的 `singleTop` 會把 Intent 送到這裡。
     * 直接換成那一本，不重建 Activity，正在讀的那本進度照常保存。
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewedFile(intent)?.let { externalOpen.value = it }
    }

    /** `ACTION_VIEW` 帶的那個檔案；不是「用其他應用程式開啟」就回 null。 */
    private fun viewedFile(intent: Intent?): Uri? =
        intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // 啟動看門狗：真機「打開就卡住」時，`adb logcat -s MDStart` 能直接看出
        // 卡在建造 ViewModel、還是卡在讀設定——不必再靠猜。
        Log.i("MDStart", "onCreate")
        enableEdgeToEdge()
        // 系統的起始視窗到第一幀就交棒，不再扣著等設定：它只有靜態底色與寫死的圖示，
        // 等得越久越容易看到「跟 App 不一樣的開屏」。真正的開屏由下面 Compose 畫。
        splashCache = SplashCache(this)
        splashSnapshot = splashCache.read(
            night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        )
        playSplash = savedInstanceState == null
        // 只在真正第一次建立時讀 Intent：轉屏、程序重建都還帶著同一份，
        // 每讀一次就會再打開一次同一本書。外部開啟一律走 exported 的 VIEW 篩選器，
        // 其他 Intent 不會帶 data，這裡拿到 null 就等於什麼都沒發生。
        if (savedInstanceState == null) externalOpen.value = viewedFile(intent)
        // 給 setContent 裡的 DisposableEffect 用：Activity 生命週期由它掛載圖示同步。
        val activityLifecycle = lifecycle
        setContent {
            LaunchedEffect(Unit) { Log.i("MDStart", "first-frame") }
            val fileViewModel = this@MainActivity.fileViewModel
            val settingsState by fileViewModel.settingsState.collectAsState()
            val appSettings = settingsState.settings
            val settingsLoaded = settingsState.loaded
            // Nav3 會把 entryProvider 的結果留在已建立的 entry 上；外層設定若只被 lambda 捕獲，
            // 改設定不會傳進畫面（排版切換看似完全沒反應就是這個原因）。
            // 用 State 包一層、在 entry 內容裡讀 .value，entry 才會訂閱到後續更新。
            val liveSettings = rememberUpdatedState(appSettings)
            // 桌面图标跟随 App 内主色：
            // - 固定种子色 → 精确同色（七套别名里挑一套）
            // - 莫奈取色   → 主色由壁纸决定，只能取色轮上最近的一格
            //
            // **只记下来，不在当下切换别名。** 切换别名会把「当前入口」禁用掉，
            // 系统就只能把正在用的界面收走——用户看到的就是「改个主题色，App 突然退出又重开」。
            // 图标不是急着看的东西，等退到后台（ON_STOP）再落，那时即便当前入口被禁也无人察觉。
            val iconContext = LocalContext.current
            var pendingLauncherSync by remember { mutableStateOf<LauncherSync?>(null) }
            var appliedLauncherSync by remember { mutableStateOf<LauncherSync?>(null) }
            LaunchedEffect(
                appSettings.themeMode,
                appSettings.seedColor,
                appSettings.launcherGlyph
            ) {
                // 莫奈取色要读一整套系统资源，放到后台线程算；算完只是存着，不碰别名。
                val monetPrimary = withContext(Dispatchers.Default) {
                    if (
                        appSettings.themeMode == ThemeMode.MONET &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ) {
                        // 取色失败时 Theme 会回落到种子色派生，图标也得跟着回落，否则两边又错位。
                        runCatching {
                            dynamicLightColorScheme(iconContext).primary.toArgb()
                        }.getOrNull()
                    } else {
                        null
                    }
                }
                pendingLauncherSync = LauncherSync(
                    seed = appSettings.seedColor,
                    monetPrimary = monetPrimary,
                    glyph = appSettings.launcherGlyph
                )
            }
            DisposableEffect(activityLifecycle) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event != Lifecycle.Event.ON_STOP) return@LifecycleEventObserver
                    val pending = pendingLauncherSync ?: return@LifecycleEventObserver
                    if (pending == appliedLauncherSync) return@LifecycleEventObserver
                    // 已经 ON_STOP，界面看不见了；这里改完就算进程被系统回收也无所谓。
                    if (pending.monetPrimary != null) {
                        LauncherIcon.syncMonet(iconContext, pending.monetPrimary, pending.glyph)
                    } else {
                        LauncherIcon.sync(iconContext, pending.seed, pending.glyph)
                    }
                    appliedLauncherSync = pending
                }
                activityLifecycle.addObserver(observer)
                onDispose { activityLifecycle.removeObserver(observer) }
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

                    // 開屏：第一幀先照上次套用的主題快照畫（見 SplashCache），
                    // 設定讀回來後換成即時配色，設定就緒再淡出——這樣亮暗、莫奈取色
                    // 與自選字形在開屏這一段就和 App 內完全一致。
                    var splashGone by remember { mutableStateOf(!playSplash) }
                    var splashFinish by remember { mutableStateOf(false) }
                    // 最短顯示時間從「第一幀畫出來」起算：冷啟動本身已經佔掉幾百毫秒，
                    // 若從 onCreate 起算，App 自己這一段往往只剩一瞬，等於沒播。
                    val splashShownAt = remember { SystemClock.uptimeMillis() }
                    if (settingsLoaded) {
                        val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()
                        SideEffect {
                            // 只記底色與字形：開屏標誌就是桌面上那顆別名圖示，
                            // 由系統元件管理，不必（也不能）快照顏色。
                            splashCache.writeSurface(surfaceColor)
                            splashCache.writeGlyph(appSettings.launcherGlyph)
                        }
                    }
                    LaunchedEffect(settingsLoaded) {
                        if (!settingsLoaded) return@LaunchedEffect
                        // 設定讀得比最短時間快時才需要補；慢了就立刻接上淡出。
                        val elapsed = SystemClock.uptimeMillis() - splashShownAt
                        delay((SPLASH_MIN_MS - elapsed).coerceAtLeast(0L))
                        splashFinish = true
                    }
                    // 同 liveSettings：entry 內容要自己訂閱，畫面才會跟著資料更新。
                    val liveNotes = rememberUpdatedState(notes)
                    val liveStats = rememberUpdatedState(stats)
                    val liveNotebookTags = rememberUpdatedState(notebookTags)
                    val liveProfile = rememberUpdatedState(profile)
                    val liveFolder = rememberUpdatedState(hasFolderSelected)
                    val liveImporting = rememberUpdatedState(isImporting)
                    val liveImportError = rememberUpdatedState(importError)

                    // 啟動頁由設定決定（全部筆記／書庫）。DataStore 是異步的，
                    // 先等第一份真實設定再建根頁——否則會先畫一幀預設主頁再跳過去。
                    //
                    // 用 rememberSaveable 而不是 remember：轉屏、系統日夜切換、改系統語言
                    // 都會重建 Activity。只要 current page 沒被保存，重建後就會退回「啟動頁」
                    // 設定（多半是書庫）——使用者明明在全部筆記，一回來卻站在書庫，
                    // 接著按排版切換，改的自然是書庫。這就是「切換有時失效、切到的卻是書庫」。
                    var homeTarget by rememberSaveable { mutableStateOf<HomeTarget?>(null) }
                    // 抽屜狀態放在兩個主頁之上。
                    //
                    // 抽屜是每個主頁各畫一份的，但「開著沒開著」不能各記一份：
                    // 在筆記頁點「書庫」時，若書庫那一頁以為抽屜是關的，它會從
                    // 畫面外再滑進來一次；而且舊的那份一被銷毀，抽屜就瞬間消失、
                    // 新頁面直接閃進來——就是「切過去沒有動畫」。
                    // 共用一份狀態，新的那份就會接著舊的進度繼續滑出去。
                    // 用 State 物件，而不是 `by` 委派值：讀取點移到 HomeDrawer 內部，
                    // MainActivity 的組合就不會因為開關抽屜而重組。以前 `open = drawerOpen`
                    // 在這一層讀，每次開抽屜都會重算整個主內容（NavDisplay 的 entryProvider
                    // 也會重建），動畫第一幀同時要付整頁重組的成本——看起來就是「一開就卡一下」。
                    val drawerOpen = remember { mutableStateOf(false) }
                    // 筆記主頁內部的「全部筆記 / 筆記本」兩段。提到主頁之上，是因為
                    // 抽屜要能從**任何一頁**直接跳到「筆記本」；若它還留在 MainScreen
                    // 裡面，書庫那一頁就點不到。
                    var homeSection by rememberSaveable { mutableStateOf(HomeSection.NOTES) }
                    // 筆記頁目前在看的筆記本；null = 全部筆記。同樣提到上面：
                    // 從抽屜點「全部筆記」就該真的回到全部，而不是還停在上一本的過濾。
                    var notesTag by rememberSaveable { mutableStateOf<String?>(null) }
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
                        drawerOpen.value = false
                    }

                    LaunchedEffect(settingsLoaded) {
                        if (settingsLoaded && homeTarget == null) {
                            // 直接取同一份已就緒設定，不再重讀 DataStore；否則畫面會先落在
                            // 預設筆記頁，下一次讀取完成又突然跳去書庫。
                            homeTarget = appSettings.homeTarget
                            Log.i("MDStart", "startPage=${appSettings.homeTarget}")
                        }
                    }
                    // 書庫：匯入的電子書留在 App 目錄裡，標題列／抽屜都能再打開。
                    val bookLibrary = remember { BookLibrary(this@MainActivity) }
                    val navScope = rememberCoroutineScope()
                    // 從別的 App 遞進來的檔案（QQ／微信／檔案管理器）。
                    // 走跟書庫匯入完全一樣的一條路：Intent 只給一次性授權，
                    // 不在這一刻複製一份進 App 目錄，下次啟動就再也打不開了。
                    val externalPending = externalOpen.value
                    LaunchedEffect(externalPending) {
                        val source = externalPending ?: return@LaunchedEffect
                        // 先清掉再處理：重組時不能因為同一個 Uri 再開一次。
                        externalOpen.value = null
                        fileViewModel.openDocument(
                            source = source,
                            onBook = { bookUri, name ->
                                navScope.launch {
                                    val entry = runCatching {
                                        bookLibrary.import(Uri.parse(bookUri), name)
                                    }.getOrNull()
                                    backStack.add(
                                        Reader(
                                            uri = entry?.let {
                                                bookLibrary.uriOf(it).toString()
                                            } ?: bookUri,
                                            displayName = entry?.displayName ?: name
                                        )
                                    )
                                }
                            },
                            onNote = { noteId -> backStack.add(NoteEditor(noteId)) }
                        )
                    }
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
                    // 這個版本只把規格掛在 NavEntry 的 metadata 上不會生效（實測：直接硬切）。
                    // 同樣的動作再寫一份成真正的 lambda，交給 NavDisplay 當全域預設。
                    // 這個版本只把規格掛在 NavEntry 的 metadata 上不會生效（實測：直接硬切）。
                    // 所以全域預設必須自己給；而且給「淡入淡出」——
                    // 這是所有頁面的保底轉場，有自己 metadata 的頁面照自己的規格走。
                    val pushSpec: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
                        fadeIn(slowEffects) togetherWith fadeOut(fastEffects)
                    }
                    val popSpec: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
                        fadeIn(slowEffects) togetherWith fadeOut(fastEffects)
                    }
                    val predictiveSpec:
                        AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform =
                        { _ -> fadeIn(slowEffects) togetherWith fadeOut(fastEffects) }
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
                        // 全域預設轉場。實測：只把規格掛在 NavEntry 的 metadata 上，
                        // 這個版本不吃——頁面直接硬切。NavDisplay 自己也要有一份。
                        transitionSpec = pushSpec,
                        popTransitionSpec = popSpec,
                        predictivePopTransitionSpec = predictiveSpec,
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
                                    val appSettings = liveSettings.value
                                    val notes = liveNotes.value
                                    val stats = liveStats.value
                                    val notebookTags = liveNotebookTags.value
                                    val profile = liveProfile.value
                                    val hasFolderSelected = liveFolder.value
                                    val isImporting = liveImporting.value
                                    val importError = liveImportError.value
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
                                                    onOpenDrawer = { drawerOpen.value = true },
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
                                                    onOpenDrawer = { drawerOpen.value = true },
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
                                                    // 直接把 transform 交給 ViewModel：它在 DataStore 鎖內
                                                    // 讀最新值再改，連續點擊不會互相覆蓋。
                                                    onSettingsChange = { transform ->
                                                        fileViewModel.updateAppSettings(transform)
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
                                                    onOpenDrawer = { drawerOpen.value = true },
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
                                            onDismiss = { drawerOpen.value = false },
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
                                                drawerOpen.value = false
                                                notesFolderLauncher.launch(null)
                                            },
                                            onOpenSettings = {
                                                drawerOpen.value = false
                                                backStack.add(Settings)
                                            },
                                            onOpenHelp = {
                                                drawerOpen.value = false
                                                backStack.add(Help)
                                            },
                                            onOpenAuthor = {
                                                drawerOpen.value = false
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
                                    val appSettings = liveSettings.value
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
                                    val appSettings = liveSettings.value
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
                                    val appSettings = liveSettings.value
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
                                    val appSettings = liveSettings.value
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
                                    val appSettings = liveSettings.value
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
                                    val appSettings = liveSettings.value
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
                                    val appSettings = liveSettings.value
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
                                    val appSettings = liveSettings.value
                                    ReaderScreen(
                                        uriString = key.uri,
                                        displayName = key.displayName,
                                        settings = appSettings,
                                        onChange = { next ->
                                            fileViewModel.updateAppSettings { next }
                                        },
                                        onBack = { backStack.removeLastOrNull() },
                                        onOpenFontSettings = { backStack.add(FontSettings) },
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
                                        language = liveSettings.value.language.resolved(),
                                        onBack = { backStack.removeLastOrNull() },
                                        onOpenAuthor = { backStack.add(Author) }
                                    )
                                }

                                is Search -> NavEntry(
                                    key,
                                    metadata = pageTransition
                                ) {
                                    SearchScreen(
                                        notes = liveNotes.value,
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
                                    val appSettings = liveSettings.value
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
                                        profile = liveProfile.value,
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

                    // 開屏疊在最上層：底色取當前主題（莫奈或種子色都成立），
                    // 標誌用使用者選的字形畫在 primary 上，和桌面圖示是同一套。
                    if (!splashGone) {
                        SplashOverlay(
                            // 桌面上那顆圖示：字形是使用者選的，底色是他那套配色。
                            icon = if (playSplash) {
                                remember(iconContext) {
                                    LauncherIcon.currentIconDrawable(iconContext)
                                }
                            } else {
                                null
                            },
                            glyph = if (settingsLoaded) {
                                appSettings.launcherGlyph
                            } else {
                                splashSnapshot.glyph
                            },
                            surface = if (settingsLoaded) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                Color(splashSnapshot.surface)
                            },
                            finish = splashFinish,
                            onFinished = { splashGone = true }
                        )
                    }
                }
            }
}
    }
}

/**
 * 開屏設定讀取上限。
 *
 * DataStore 正常是幾十毫秒，但它的檔案可能損壞、可能被別的實例佔著、磁碟也可能正忙。
 * 這種時候寧可先按預設主頁進去，也不能讓使用者盯著一片空白。
 */
/**
 * 開屏最短顯示時間（從 onCreate 起算）。
 *
 * 從 App 自己的第一幀起算的保底時間：設定讀得比它快時，開屏不會只閃一幀就消失；
 * 比它慢就立刻接上淡出，不會多扣時間。
 */
private const val SPLASH_MIN_MS = 300L


/**
 * 待寫入桌面的圖示狀態。
 *
 * [monetPrimary] 不為 null 表示走莫奈取色（由壁紙決定的主色，只能取色輪上最近的一格）；
 * 為 null 則用 [seed] 那七顆固定種子色之一，可以精確同色。
 *
 * 存在的理由：切換圖示必須改 `activity-alias`，而改別名會影響「當前入口」，
 * 所以不能在使用者還在用時做——先把目標狀態記在這裡，等 App 退到後台再落。
 */
private data class LauncherSync(
    val seed: SeedColor,
    val monetPrimary: Int?,
    val glyph: LauncherGlyph
)

package com.yuzlyn.mdeditor.ui.screen

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.yuzlyn.mdeditor.data.NoteModel
import com.yuzlyn.mdeditor.data.importer.ImportFormat
import com.yuzlyn.mdeditor.data.settings.AppSettings
import com.yuzlyn.mdeditor.data.settings.CardShape
import com.yuzlyn.mdeditor.data.settings.NoteLayout
import com.yuzlyn.mdeditor.data.settings.NoteSort
import com.yuzlyn.mdeditor.ui.component.CircleIconButton
import com.yuzlyn.mdeditor.ui.component.FabMenuEntry
import com.yuzlyn.mdeditor.ui.component.FabMenuRow
import com.yuzlyn.mdeditor.ui.component.RailButton
import com.yuzlyn.mdeditor.ui.component.pressRipple
import com.yuzlyn.mdeditor.ui.i18n.LocalAppStrings
import com.yuzlyn.mdeditor.ui.theme.Space
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.expandHorizontally
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.IconButtonDefaults

/**
 * 主頁的兩個平級視圖：筆記列表與筆記本列表。
 *
 * 這個「段」放在 MainScreen 之外由呼叫端持有：抽屜現在站在兩個主頁之上，
 * 從書庫那一頁也要能直接跳到「筆記本」，段就不能只活在這一頁裡面。
 */
enum class HomeSection { NOTES, NOTEBOOKS }

/** 內容區的互斥狀態。 */
private enum class HomeContent { LOADING, WELCOME, NOTEBOOKS, EMPTY_NOTEBOOKS, EMPTY_NOTES, NOTES }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    notes: List<NoteModel>,
    notebookTags: List<String>,
    hasFolderSelected: Boolean,
    isImporting: Boolean,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    avatarPath: String,
    section: HomeSection,
    onSectionChange: (HomeSection) -> Unit,
    /** 目前在看的筆記本；null 就是「全部筆記」。放在主頁之上，抽屜才能清掉它。 */
    selectedTag: String?,
    onSelectTag: (String?) -> Unit,
    onNoteClick: (NoteModel) -> Unit,
    onCreateNewNote: (String, String) -> Unit,
    onFolderSelected: (Uri) -> Unit,
    onDeleteNotes: (List<String>) -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    importError: String? = null,
    onImportErrorShown: () -> Unit = {},
    onImportDocument: (Uri) -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    // 右下角加號的展開選單：一列檔案類型，點哪個開哪個。
    var fabMenuOpen by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val motion = MaterialTheme.motionScheme
    val fabScale = motion.defaultSpatialSpec<Float>()
    val fabSlide = motion.defaultSpatialSpec<IntOffset>()
    val fabEffects = motion.defaultEffectsSpec<Float>()
    val fabIconRotation by animateFloatAsState(
        targetValue = if (fabMenuOpen) 45f else 0f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "fab-icon"
    )
    // 展開時由圓角矩形變成正圓；用動畫中的圓角值，而不是換一個 shape。
    val fabSize = Space.s700 * 1.5f
    val fabCorner by animateDpAsState(
        targetValue = if (fabMenuOpen) fabSize / 2 else 20.dp,
        animationSpec = motion.defaultSpatialSpec(),
        label = "fab-corner"
    )
    val fabScrimAlpha by animateFloatAsState(
        targetValue = if (fabMenuOpen) 1f else 0f,
        animationSpec = motion.defaultEffectsSpec<Float>(),
        label = "fab-scrim"
    )

    // 頂欄：內容 56dp + 下方 8dp 留白；捲動時整塊（含背景）上移並淡出，內容從下方頂上來。
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topBarContentHeight = Space.s700
    val topBarBottomSpace = Space.s100
    val topBarInset = statusBarHeight + topBarContentHeight + topBarBottomSpace
    val topBarCollapseLimitPx = with(density) { topBarInset.toPx().coerceAtLeast(1f) }
    var topBarOffset by remember { mutableFloatStateOf(0f) }
    val topBarScrollConnection = remember(topBarCollapseLimitPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // 往下拖時先展回頂欄，再讓內容捲動。
                if (available.y > 0f && topBarOffset < 0f) {
                    val newOffset = (topBarOffset + available.y).coerceAtMost(0f)
                    val consumed = newOffset - topBarOffset
                    topBarOffset = newOffset
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // 往上滑時把剩餘位移用來收合頂欄。
                if (available.y < 0f && topBarOffset > -topBarCollapseLimitPx) {
                    val newOffset = (topBarOffset + available.y)
                        .coerceAtLeast(-topBarCollapseLimitPx)
                    val delta = newOffset - topBarOffset
                    topBarOffset = newOffset
                    return Offset(0f, delta)
                }
                return Offset.Zero
            }
        }
    }


    var showNewNoteDialog by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) onFolderSelected(uri) }

    // 匯入只有一個入口：不論 Word／PDF／EPUB／HTML，都先用同一個選擇器挑檔，
    // 選完交給 DocumentImporter 依副檔名與檔頭判斷類型，再走各自的解析器。
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onImportDocument(uri) }

    LaunchedEffect(importError) {
        if (importError != null) {
            snackbarHostState.showSnackbar(importError)
            onImportErrorShown()
        }
    }

    val filteredNotes = remember(notes, selectedTag) {
        notes.filter { note -> selectedTag == null || note.notebookTag == selectedTag }
    }

    val filteredTags = remember(notebookTags) { notebookTags }

    val sortedNotes = remember(filteredNotes, settings.noteSort) {
        when (settings.noteSort) {
            NoteSort.CUSTOM -> filteredNotes
            NoteSort.CREATED -> filteredNotes.sortedByDescending { it.createdAt }
            NoteSort.MODIFIED -> filteredNotes.sortedByDescending { it.lastModified }
        }
    }

    val content = when {
        isImporting -> HomeContent.LOADING
        !hasFolderSelected -> HomeContent.WELCOME
        section == HomeSection.NOTEBOOKS ->
            if (filteredTags.isEmpty()) HomeContent.EMPTY_NOTEBOOKS else HomeContent.NOTEBOOKS
        filteredNotes.isEmpty() -> HomeContent.EMPTY_NOTES
        else -> HomeContent.NOTES
    }

    LaunchedEffect(selectedIds.isEmpty()) {
        if (selectedIds.isNotEmpty()) keyboardController?.hide()
    }

    BackHandler(enabled = fabMenuOpen) { fabMenuOpen = false }
    // 換區段、進入多選、或還沒選資料夾時收起來，避免只剩一列懸空的按鈕。
    LaunchedEffect(fabMenuOpen, section, selectedIds, hasFolderSelected) {
        if (fabMenuOpen &&
            (!hasFolderSelected || selectedIds.isNotEmpty() || section != HomeSection.NOTES)
        ) {
            fabMenuOpen = false
        }
    }
    BackHandler(enabled = selectedIds.isNotEmpty()) {
        selectedIds = emptySet()
    }

    if (showNewNoteDialog) {
        NewNoteDialog(
            existingTags = notebookTags,
            onDismiss = { showNewNoteDialog = false },
            onConfirm = { title, tag ->
                onCreateNewNote(title, tag)
                showNewNoteDialog = false
            }
        )
    }

    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = Space.s300)
            ) {
                Text(
                    text = strings.sortTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Space.s150))
                listOf(
                    NoteSort.CUSTOM to strings.sortCustom,
                    NoteSort.CREATED to strings.sortCreated,
                    NoteSort.MODIFIED to strings.sortModified
                ).forEach { (option, label) ->
                    val rowInteraction = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressRipple(
                                interactionSource = rowInteraction,
                                shape = RoundedCornerShape(Space.s150)
                            )
                            .clickable(
                                interactionSource = rowInteraction,
                                indication = null
                            ) {
                                onSettingsChange(settings.copy(noteSort = option))
                                showSortSheet = false
                            }
                            .padding(vertical = Space.s100),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.noteSort == option,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(Space.s150))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Space.s300))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(topBarScrollConnection)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 視窗寬度直接取自 Configuration，而不是 BoxWithConstraints：
            // 後者第一帧可能量到 0，`expanded` 會 false→true 閃一下，
            // 側欄就變成「每次進首頁都從 0 寬展開」——既露縫，又讓整頁重新測量（卡頓）。
            val windowWidth = LocalConfiguration.current.screenWidthDp.dp
            val expanded = windowWidth >= 840.dp
            // 瀑布流欄數按「每列至少 200dp」算：手機 2 欄、平板豎屏 3 欄、橫屏 5 欄。
            val gridColumns = (windowWidth / 200.dp).toInt().coerceIn(2, 6)
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = hasFolderSelected &&
                            selectedIds.isEmpty() &&
                            section == HomeSection.NOTES,
                    enter = fadeIn(fabEffects) +
                            scaleIn(animationSpec = fabScale) +
                            slideInVertically(animationSpec = fabSlide, initialOffsetY = { it / 3 }),
                    exit = fadeOut(fabEffects) +
                            scaleOut(animationSpec = fabScale) +
                            slideOutVertically(animationSpec = fabSlide, targetOffsetY = { it / 3 })
                ) {
                    // 選單和加號放在同一個容器：Scaffold 只擺一次，兩者間距與右緣就永遠對齊，
                    // 不會再出現「選單壓到加號」或兩者各算一次 inset 的錯位。
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(Space.s100),
                        modifier = Modifier.padding(end = Space.s200, bottom = Space.s200)
                    ) {
                        // 每一列自己管展開／收起：離加號最近的先出，收起時也由它先收，
                        // 整疊像被吸回加號裡。若把六列包在同一個 AnimatedVisibility 裡，
                        // 收起只剩一次整體淡出，看起來就跟「直接消失」沒兩樣。
                        Column(horizontalAlignment = Alignment.End) {
                            val entries = listOf(
                                FabMenuEntry(Icons.Default.FileOpen, "導入文件") {
                                    fabMenuOpen = false
                                    importLauncher.launch(ImportFormat.pickerMimeTypes)
                                },
                                FabMenuEntry(Icons.Default.Edit, "新筆記") {
                                    fabMenuOpen = false
                                    showNewNoteDialog = true
                                }
                            )
                            entries.forEachIndexed { index, entry ->
                                FabMenuRow(
                                    entry = entry,
                                    // 0 是最靠近加號的那一列；下面的延遲以它為基準。
                                    order = entries.lastIndex - index,
                                    total = entries.size,
                                    expanded = fabMenuOpen
                                )
                            }
                        }
                        FloatingActionButton(
                            onClick = { fabMenuOpen = !fabMenuOpen },
                            modifier = Modifier.size(fabSize),
                            shape = RoundedCornerShape(fabCorner),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = Space.s0,
                                pressedElevation = Space.s50,
                                focusedElevation = Space.s0,
                                hoveredElevation = Space.s0
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = strings.newNote,
                                // 轉 45 度就變成叉：同一個圖示交代「再點一次收起」。
                                modifier = Modifier
                                    .size(Space.s400)
                                    .rotate(fabIconRotation)
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            // 頂欄為覆蓋層：內容頂端預留頂欄高度，頂欄上移時內容同步上移填補。
            val contentTopInset = if (expanded) {
                statusBarHeight
            } else {
                with(density) {
                    (topBarInset + topBarOffset.toDp()).coerceAtLeast(statusBarHeight)
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                // 側欄用「展開／收起」進場：旋轉或視窗尺寸變化時，欄位是滑出來的，
                // 不是突然出現把內容擠走。
                // 側欄不做任何進場動畫：它一出現就佔著左邊 80dp，
                // 只要有淡入或展開，那 80dp 在動畫期間就是一片空白——看起來就是「左邊空了」。
                // 旋轉本來就是一次大幅度的視覺變化，直接切換反而最乾淨。
                if (expanded) {
                    HomeSideRail(
                        avatarPath = avatarPath,
                        noteLayout = settings.noteLayout,
                        onToggleLayout = {
                            onSettingsChange(
                                settings.copy(
                                    noteLayout = if (settings.noteLayout == NoteLayout.MASONRY) {
                                        NoteLayout.LIST
                                    } else {
                                        NoteLayout.MASONRY
                                    }
                                )
                            )
                        },
                        onOpenSort = { showSortSheet = true },
                        onOpenSearch = onOpenSearch,
                        onOpenProfile = onOpenProfile,
                        onMenu = onOpenDrawer
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                when (content) {
                    HomeContent.LOADING -> {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(top = contentTopInset),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(modifier = Modifier.size(Space.s600))
                        }
                    }

                    HomeContent.WELCOME -> {
                        WelcomeState(
                            innerPadding = PaddingValues(top = contentTopInset),
                            onChooseFolder = { folderLauncher.launch(null) }
                        )
                    }

                    HomeContent.NOTEBOOKS -> {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(gridColumns),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = Space.s150,
                                top = contentTopInset + Space.s200,
                                end = Space.s150,
                                bottom = Space.s200
                            ),
                            horizontalArrangement = Arrangement.spacedBy(Space.s100),
                            verticalItemSpacing = Space.s100
                        ) {
                            items(filteredTags, key = { it }) { tag ->
                                NotebookCard(
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = motion.defaultEffectsSpec(),
                                        placementSpec = motion.defaultSpatialSpec(),
                                        fadeOutSpec = motion.fastEffectsSpec()
                                    ),
                                    name = tag,
                                    count = notes.count { it.notebookTag == tag },
                                    cardShape = settings.cardShape,
                                    lastModified = notes.asSequence()
                                        .filter { it.notebookTag == tag }
                                        .maxOfOrNull { it.lastModified } ?: 0L,
                                    onClick = {
                                        onSectionChange(HomeSection.NOTES)
                                        onSelectTag(tag)
                                    }
                                )
                            }
                        }
                    }

                    HomeContent.EMPTY_NOTEBOOKS -> {
                        EmptyState(
                            innerPadding = PaddingValues(top = contentTopInset),
                            title = strings.noNotebooks,
                            hint = strings.notebookHint
                        )
                    }

                    HomeContent.EMPTY_NOTES -> {
                        EmptyState(
                            innerPadding = PaddingValues(top = contentTopInset),
                            title = strings.noNotes,
                            hint = strings.createFirstNote
                        )
                    }

                    HomeContent.NOTES -> {
                        LazyVerticalStaggeredGrid(
                            columns = if (settings.noteLayout == NoteLayout.MASONRY) {
                                StaggeredGridCells.Fixed(gridColumns)
                            } else {
                                StaggeredGridCells.Fixed(1)
                            },
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = Space.s150,
                                top = contentTopInset + Space.s200,
                                end = Space.s150,
                                bottom = Space.s200
                            ),
                            horizontalArrangement = Arrangement.spacedBy(Space.s100),
                            verticalItemSpacing = Space.s100
                        ) {
                            items(sortedNotes, key = { it.id }) { note ->
                                NoteCard(
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = motion.defaultEffectsSpec(),
                                        placementSpec = motion.defaultSpatialSpec(),
                                        fadeOutSpec = motion.fastEffectsSpec()
                                    ),
                                    note = note,
                                    selected = note.id in selectedIds,
                                    cardShape = settings.cardShape,
                                    showDetails = settings.noteLayout == NoteLayout.MASONRY,
                                    onClick = {
                                        if (selectedIds.isNotEmpty()) {
                                            selectedIds = if (note.id in selectedIds) {
                                                selectedIds - note.id
                                            } else {
                                                selectedIds + note.id
                                            }
                                        } else {
                                            onNoteClick(note)
                                        }
                                    },
                                    onLongClick = { selectedIds = selectedIds + note.id }
                                )
                            }
                        }
                    }
                }

                // 頂欄：覆蓋在內容之上，隨捲動整塊（含背景）上移並淡出。
                // 頂欄同一時間淡出／滑入，和側欄交接。
                // 這裡在 Box 作用域裡，必須指名頂層版本，否則會撞上 RowScope 的擴充函式。
                androidx.compose.animation.AnimatedVisibility(
                    visible = !expanded,
                    enter = fadeIn(motion.defaultEffectsSpec()) +
                            slideInVertically(animationSpec = motion.defaultSpatialSpec()) { -it / 3 },
                    exit = fadeOut(motion.fastEffectsSpec()) +
                            slideOutVertically(animationSpec = motion.fastSpatialSpec()) { -it / 3 }
                ) {
                AnimatedContent(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = topBarOffset
                            alpha = (1f + topBarOffset / topBarCollapseLimitPx).coerceIn(0f, 1f)
                        },
                    targetState = selectedIds.isNotEmpty(),
                    transitionSpec = {
                        (
                            slideInVertically(
                                animationSpec = motion.slowSpatialSpec(),
                                initialOffsetY = { -it / 3 }
                            ) + fadeIn(motion.slowEffectsSpec<Float>())
                            ).togetherWith(
                            slideOutVertically(
                                animationSpec = motion.fastSpatialSpec(),
                                targetOffsetY = { -it / 3 }
                            ) + fadeOut(motion.fastEffectsSpec<Float>())
                        )
                    },
                    label = "top-bar"
                ) { selecting ->
                    if (selecting) {
                        SelectionTopBar(
                            count = selectedIds.size,
                            onClose = { selectedIds = emptySet() },
                            onShare = {
                                scope.launch { snackbarHostState.showSnackbar(strings.share) }
                            },
                            onExport = {
                                scope.launch { snackbarHostState.showSnackbar(strings.export) }
                            },
                            onDelete = {
                                onDeleteNotes(selectedIds.toList())
                                selectedIds = emptySet()
                            }
                        )
                    } else {
                        HomeTopBar(
                            avatarPath = avatarPath,
                            noteLayout = settings.noteLayout,
                            onToggleLayout = {
                                onSettingsChange(
                                    settings.copy(
                                        noteLayout = if (settings.noteLayout == NoteLayout.MASONRY) {
                                            NoteLayout.LIST
                                        } else {
                                            NoteLayout.MASONRY
                                        }
                                    )
                                )
                            },
                            onOpenSort = { showSortSheet = true },
                            onOpenSearch = onOpenSearch,
                            onOpenProfile = onOpenProfile,
                            onMenu = onOpenDrawer
                        )
                    }
                }
                }
                }
                }

                // 遮罩比頂欄晚繪製，所以連頂欄一起壓暗；加號與選單在 FAB 槽，仍浮在最上層。
                if (fabScrimAlpha > 0.001f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = fabScrimAlpha }
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { fabMenuOpen = false }
                    )
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
/**
 * 平板／橫屏的左側欄：把「全域入口」從頂欄搬過來。
 *
 * 三個要點（都是和手機頂欄不同的地方）：
 * - 體積：80dp 寬、48dp 圓形按鈕，垂直間距 12dp——rail 的標準尺寸，不隨螢幕變大而放大。
 * - 背景：用 tonal 的 surfaceContainer 並在右緣加一條髮絲線，和內容分開；
 *   跟頁面同色會看起來「沒有側欄、只是圖示浮著」。
 * - 頭像：做成和其他按鈕一樣大的圓形，視覺重量一致（手機上它是小頭像，因為那裡沒有按鈕列）。
 */
@Composable
private fun HomeSideRail(
    avatarPath: String,
    noteLayout: NoteLayout,
    onToggleLayout: () -> Unit,
    onOpenSort: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenProfile: () -> Unit,
    onMenu: () -> Unit
) {
    val strings = LocalAppStrings.current
    Surface(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)) {
        Row(modifier = Modifier.fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(vertical = Space.s200),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.s150)
            ) {
                RailButton(
                    icon = Icons.Default.Menu,
                    description = strings.menu,
                    onClick = onMenu
                )
                // 搜尋是這一欄裡最主要的功能，用強調色容器把它提出來。
                RailButton(
                    icon = Icons.Default.Search,
                    description = strings.searchNotes,
                    onClick = onOpenSearch,
                    emphasized = true
                )
                RailButton(
                    icon = if (noteLayout == NoteLayout.MASONRY) {
                        Icons.AutoMirrored.Rounded.ViewList
                    } else {
                        Icons.Rounded.GridView
                    },
                    description = strings.toggleLayout,
                    onClick = onToggleLayout
                )
                RailButton(
                    icon = Icons.Rounded.Sort,
                    description = strings.sort,
                    onClick = onOpenSort
                )
                Spacer(modifier = Modifier.weight(1f))
                Avatar(
                    avatarPath = avatarPath,
                    // 和上面幾顆按鈕同一個尺寸，不再是小頭像。
                    size = Space.s600,
                    contentDescription = strings.profileTitle,
                    onClick = onOpenProfile
                )
            }
            VerticalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun HomeTopBar(
    avatarPath: String,
    noteLayout: NoteLayout,
    onToggleLayout: () -> Unit,
    onOpenSort: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenProfile: () -> Unit,
    onMenu: () -> Unit
) {
    val strings = LocalAppStrings.current
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Space.s700),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左右按鈕為 56dp 正方形熱區，位於搜尋欄外側；搜尋欄吃滿其餘寬度。
                Box(
                    modifier = Modifier.size(Space.s700),
                    contentAlignment = Alignment.Center
                ) {
                    // 和側欄按鈕同一種圓形遮罩，切到平板時不會覺得換了一套控制項。
                    RailButton(
                        icon = Icons.Default.Menu,
                        description = strings.menu,
                        onClick = onMenu
                    )
                }
                val searchInteraction = remember { MutableInteractionSource() }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .weight(1f)
                        // 左右各讓 4dp：膠囊和兩側圓鈕之間留一道縫，
                        // 三顆圓形遮罩才不會黏成一整條。
                        .padding(horizontal = Space.s50)
                        .height(Space.s700)
                        // 膠囊形搜尋欄同樣改用自繪波紋，遮罩才會貼著圓角收邊。
                        .pressRipple(interactionSource = searchInteraction, shape = CircleShape)
                        .clickable(
                            interactionSource = searchInteraction,
                            indication = null,
                            onClick = onOpenSearch
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = Space.s250, end = Space.s100),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.searchNotes,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        // 兩顆按鈕之間留 8dp。緊挨著時兩個圓形遮罩會連成一塊，
                        // 看起來像一個古怪的長條，也分不清是兩個獨立動作。
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Space.s100),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircleIconButton(
                                icon = if (noteLayout == NoteLayout.MASONRY) {
                                    Icons.AutoMirrored.Rounded.ViewList
                                } else {
                                    Icons.Rounded.GridView
                                },
                                description = strings.toggleLayout,
                                onClick = onToggleLayout
                            )
                            CircleIconButton(
                                icon = Icons.Rounded.Sort,
                                description = strings.sort,
                                onClick = onOpenSort
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier.size(Space.s700),
                    contentAlignment = Alignment.Center
                ) {
                    Avatar(
                        avatarPath = avatarPath,
                        // 和左側那顆 48dp 圓鈕同一個尺寸：頂欄兩端才是一對，
                        // 不會一邊大一邊小。
                        size = Space.s600,
                        contentDescription = strings.profileTitle,
                        onClick = onOpenProfile
                    )
                }
            }
            // 搜尋欄下方留白，讓頂欄下緣有呼吸空間。
            Spacer(modifier = Modifier.height(Space.s100))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SelectionTopBar(
    count: Int,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Space.s800)
                    .padding(horizontal = Space.s100),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = strings.cancel)
                }
                Text(
                    text = strings.selectedCount(count),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = strings.share)
                }
                IconButton(onClick = onExport) {
                    Icon(Icons.Default.SaveAlt, contentDescription = strings.export)
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = strings.delete,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(Space.s150))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WelcomeState(innerPadding: PaddingValues, onChooseFolder: () -> Unit) {
    val strings = LocalAppStrings.current
    Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.s200),
            modifier = Modifier.padding(Space.s400)
        ) {
            Box(
                modifier = Modifier
                    .size(Space.s900)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialShapes.Cookie9Sided.toShape()
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CreateNewFolder,
                    contentDescription = null,
                    modifier = Modifier.size(Space.s400),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = strings.welcomeTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = strings.welcomeBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onChooseFolder,
                shape = MaterialTheme.shapes.large,
                contentPadding = PaddingValues(horizontal = Space.s200, vertical = Space.s125)
            ) {
                Icon(
                    Icons.Default.CreateNewFolder,
                    contentDescription = null,
                    modifier = Modifier.size(Space.s250)
                )
                Spacer(modifier = Modifier.width(Space.s100))
                Text(strings.chooseFolder)
            }
        }
    }
}

@Composable
private fun EmptyState(innerPadding: PaddingValues, title: String, hint: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.s100),
            modifier = Modifier.padding(Space.s400)
        ) {
            Icon(
                Icons.Default.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(Space.s800),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 取卡片摘要。
 *
 * 不能直接 `take(n)`：emoji 在 UTF-16 是代理對，剛好切在中間就會留下半個字，
 * 畫出來是一個方塊。這裡退到安全的邊界，順便把收尾的組合字／變體選擇符一起收掉。
 */
private fun summaryOf(text: String, limit: Int): String {
    if (text.length <= limit) return text
    var end = limit.coerceAtLeast(0)
    if (end > 0 && Character.isHighSurrogate(text[end - 1])) end--
    while (end > 0) {
        val last = text[end - 1]
        val isModifier = last == '\u200D' || last == '\uFE0F' || last == '\uFE0E' ||
                Character.getType(last) == Character.NON_SPACING_MARK.toInt()
        if (!isModifier) break
        end--
        if (end > 0 && Character.isHighSurrogate(text[end - 1])) end--
    }
    return text.substring(0, end)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: NoteModel,
    selected: Boolean,
    cardShape: CardShape,
    showDetails: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val motion = MaterialTheme.motionScheme
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "card-press"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = motion.defaultEffectsSpec(),
        label = "card-border"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) Space.s25 else 1.dp,
        animationSpec = motion.defaultSpatialSpec(),
        label = "card-border-width"
    )

    val shape = RoundedCornerShape(cardShape.radiusDp.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            // 官方 ripple 由原生 RippleDrawable 繪製，外層 clip 裁不到它，
            // 按住時四角會冒出方形遮罩；改用自繪且依 shape 裁切的波紋。
            .pressRipple(interactionSource = interactionSource, shape = shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(Space.s200),
            verticalArrangement = Arrangement.spacedBy(Space.s100)
        ) {
            // 筆記本標籤在左、來源格式徽章在右。兩者都沒有時整行不佔高度，
            // 免得 Column 的 spacedBy 多留一段空白。
            val sourceFormat = ImportFormat.fromId(note.sourceFormat)
            if (showDetails || sourceFormat != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showDetails) {
                        Text(
                            text = note.notebookTag,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (sourceFormat != null) {
                        FormatBadge(
                            format = sourceFormat,
                            modifier = Modifier.padding(start = Space.s100)
                        )
                    }
                }
            }
            Text(
                text = note.title.ifEmpty { strings.untitled },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (showDetails && note.content.isNotEmpty()) {
                Text(
                    text = summaryOf(note.content.trimStart(), 80),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showDetails) {
                Text(
                    text = dateFormat.format(Date(note.lastModified)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 來源格式徽章。
 *
 * 用 MD3 的成對色票（容器色＋前景色），每個格式一組，深色模式與 Monet 取色都成立；
 * 同時帶圖示與短標籤，不靠顏色單獨傳達資訊（色弱也分得出來）。
 */
@Composable
private fun FormatBadge(format: ImportFormat, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (format) {
        ImportFormat.DOCX -> scheme.primaryContainer to scheme.onPrimaryContainer
        ImportFormat.PDF -> scheme.errorContainer to scheme.onErrorContainer
        ImportFormat.MARKDOWN -> scheme.secondaryContainer to scheme.onSecondaryContainer
        ImportFormat.TEXT -> scheme.surfaceContainerHighest to scheme.onSurfaceVariant
        ImportFormat.EPUB, ImportFormat.MOBI, ImportFormat.AZW3 ->
            scheme.tertiaryContainer to scheme.onTertiaryContainer
        ImportFormat.HTML -> scheme.primaryContainer to scheme.onPrimaryContainer
    }
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = container,
        contentColor = content
    ) {
        Row(
            modifier = Modifier.padding(
                start = Space.s75,
                end = Space.s100,
                top = Space.s25,
                bottom = Space.s25
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s50)
        ) {
            Icon(
                imageVector = format.icon,
                contentDescription = null,
                modifier = Modifier.size(Space.s150)
            )
            Text(text = format.shortLabel, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** 每種格式的代表圖示；搭配短標籤一起看，不只是靠顏色分辨。 */
private val ImportFormat.icon: ImageVector
    get() = when (this) {
        ImportFormat.DOCX -> Icons.Default.Description
        ImportFormat.PDF -> Icons.Default.PictureAsPdf
        ImportFormat.MARKDOWN -> Icons.Default.Code
        ImportFormat.TEXT -> Icons.Default.Notes
        ImportFormat.EPUB, ImportFormat.MOBI, ImportFormat.AZW3 -> Icons.Default.MenuBook
        ImportFormat.HTML -> Icons.Default.Html
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotebookCard(
    name: String,
    count: Int,
    lastModified: Long,
    cardShape: CardShape,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val motion = MaterialTheme.motionScheme
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "notebook-press"
    )

    val shape = RoundedCornerShape(cardShape.radiusDp.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            // 同上：波紋自繪並依 shape 裁切，遮罩才會跟著卡片圓角。
            .pressRipple(interactionSource = interactionSource, shape = shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = shape,
        // 與「全部筆記」的 NoteCard 用同一個容器色（surfaceContainerLow）：
        // 兩頁是同一個層級的內容，卡片底色不一樣會像是不同元件。
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(Space.s200),
            verticalArrangement = Arrangement.spacedBy(Space.s100)
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(Space.s250),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = strings.notesCount(count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (lastModified > 0L) {
                Text(
                    text = dateFormat.format(Date(lastModified)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewNoteDialog(
    existingTags: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, tag: String) -> Unit
) {
    val strings = LocalAppStrings.current
    var title by remember { mutableStateOf("") }
    var selectedTag by remember {
        mutableStateOf(if (existingTags.isNotEmpty()) existingTags.first() else "默認筆記本")
    }
    var showTagInput by remember { mutableStateOf(false) }
    var customTag by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.newNote, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s150)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.noteTitle) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = strings.notebookCategory,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ButtonGroup(
                    overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
                    horizontalArrangement = Arrangement.spacedBy(Space.s100)
                ) {
                    existingTags.take(3).forEach { tag ->
                        toggleableItem(
                            checked = selectedTag == tag && !showTagInput,
                            label = tag,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    selectedTag = tag
                                    showTagInput = false
                                }
                            }
                        )
                    }
                }
                if (showTagInput) {
                    OutlinedTextField(
                        value = customTag,
                        onValueChange = { customTag = it },
                        label = { Text(strings.newCategoryName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    TextButton(onClick = { showTagInput = true }) {
                        Text(strings.newCategory)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalTag =
                        if (showTagInput && customTag.isNotBlank()) customTag else selectedTag
                    onConfirm(title.ifBlank { strings.untitled }, finalTag)
                }
            ) { Text(strings.create) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = Space.s0
    )
}

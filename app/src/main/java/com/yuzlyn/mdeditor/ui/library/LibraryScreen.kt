package com.yuzlyn.mdeditor.ui.library

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yuzlyn.mdeditor.data.book.BookFormat
import com.yuzlyn.mdeditor.data.book.BookLibrary
import com.yuzlyn.mdeditor.data.book.BookOpener
import com.yuzlyn.mdeditor.data.book.ShelfBook
import com.yuzlyn.mdeditor.data.book.ShelfFolder
import com.yuzlyn.mdeditor.data.settings.CardShape
import com.yuzlyn.mdeditor.data.settings.NoteLayout
import com.yuzlyn.mdeditor.ui.component.CircleIconButton
import com.yuzlyn.mdeditor.ui.component.FabMenuEntry
import com.yuzlyn.mdeditor.ui.component.FabMenuRow
import com.yuzlyn.mdeditor.ui.component.RailButton
import com.yuzlyn.mdeditor.ui.component.pressRipple
import com.yuzlyn.mdeditor.ui.i18n.LocalAppStrings
import com.yuzlyn.mdeditor.ui.i18n.bookMessage
import com.yuzlyn.mdeditor.ui.screen.Avatar
import com.yuzlyn.mdeditor.ui.theme.Space
import kotlinx.coroutines.launch
import java.util.Locale

/** 書庫的排序方式；只存在這一頁，不進設定（書庫不需要像筆記那樣長期記住偏好）。 */
private enum class BookSort { RECENT, TITLE, ADDED }

/**
 * 書庫主頁：匯入過的電子書都在這裡。
 *
 * 版面與「全部筆記」是同一套：左邊菜單、中間搜尋膠囊（右端是排版與排序）、
 * 右邊頭像、內容是瀑布流、右下角加號展開「匯入書籍 / 新增資料夾」。
 * 兩頁平級，抽屜裡互相切換。
 *
 * 資料夾是**長期登記**的：只記 SAF 樹 Uri 並取持久授權，每次進頁面統一重新掃描，
 * 使用者往資料夾裡丟新書就會自動出現——不必再一本本匯入。
 *
 * 因為書都是這樣自動收進來的，這一頁**只有一種視圖**：全部圖書排在一起。
 * 資料夾不是瀏覽層級（那會讓同一批書出現兩次入口），只是「收書的來源」；
 * 要登記或移除資料夾，走抽屜裡的「資料夾」。
 */
// 頂欄是浮在內容上的，垂直偏移還是動畫值；Scaffold 給的 innerPadding 是靜態快照，
// 用了反而在頂欄收合時留下一條空白。內容改自己算 topInset，所以這裡刻意不用它。
@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryScreen(
    avatarPath: String,
    layout: NoteLayout,
    cardShape: CardShape,
    /** 「資料夾」面板開著沒有。由抽屜那一列控制，所以狀態放在主頁之上。 */
    showFolderPanel: Boolean,
    onDismissFolderPanel: () -> Unit,
    onOpenDrawer: () -> Unit,
    onToggleLayout: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenBook: (uri: String, displayName: String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val library = remember { BookLibrary(context) }
    val motion = MaterialTheme.motionScheme

    val windowWidth = LocalConfiguration.current.screenWidthDp.dp
    // 與「全部筆記」同一道門檻：840dp 以上用左側欄，頂欄只留搜尋。
    val expanded = windowWidth >= 840.dp
    // 與「全部筆記」同一套柵格：每列至少 200dp，寬螢幕自動加欄。
    val gridColumns = (windowWidth / 200.dp).toInt().coerceIn(2, 6)

    var books by remember { mutableStateOf<List<ShelfBook>>(emptyList()) }
    var folders by remember { mutableStateOf<List<ShelfFolder>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(BookSort.RECENT) }
    var pendingFolderRemoval by remember { mutableStateOf<ShelfFolder?>(null) }
    var fabMenuOpen by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun reload() {
        books = library.books()
        folders = library.folders()
    }

    /**
     * 進頁面（以及每次新增資料夾後）統一讀一次。
     *
     * 5 秒節流：從閱讀器返回書庫不該每次都把雲端資料夾列一遍，
     * 但使用者往資料夾丟了新書，也不該等半分鐘才看到。
     */
    suspend fun sync(force: Boolean) {
        busy = true
        val added = runCatching {
            library.rescan(minIntervalMs = if (force) 0L else 5_000L)
        }.getOrDefault(0)
        reload()
        busy = false
        if (added > 0) message = strings.librarySyncAdded(added)
    }

    LaunchedEffect(Unit) { sync(force = false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            message = null
        }
    }

    val pickFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            val entry = runCatching {
                library.import(
                    uri = uri,
                    displayName = BookOpener.queryDisplayName(context, uri),
                    fallbackName = strings.libraryUntitledBook
                )
            }.getOrElse {
                message = it.bookMessage(strings, strings.libraryEmptyHint)
                null
            }
            reload()
            busy = false
            if (entry != null) onOpenBook(library.uriOf(entry).toString(), entry.displayName)
        }
    }

    val pickFolder = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            runCatching { library.addFolder(uri, strings.libraryFolderFallback) }
                .onSuccess { folder ->
                    reload()
                    message = strings.libraryFolderBooks(library.booksIn(folder.id).size)
                }
                .onFailure { message = it.bookMessage(strings, strings.libraryEmptyHint) }
            busy = false
        }
    }

    BackHandler(enabled = fabMenuOpen) { fabMenuOpen = false }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // 頂欄（statusBar + 48dp 圓鈕 + 下方留白）——內容從它下面開始排。
    val topInset = statusBarHeight + Space.s700 + Space.s100

    val fabSize = Space.s700 * 1.5f
    val fabIconRotation by animateFloatAsState(
        targetValue = if (fabMenuOpen) 45f else 0f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "lib-fab-icon"
    )
    val fabCorner by animateDpAsState(
        targetValue = if (fabMenuOpen) fabSize / 2 else 20.dp,
        animationSpec = motion.defaultSpatialSpec(),
        label = "lib-fab-corner"
    )
    val fabScrim by animateFloatAsState(
        targetValue = if (fabMenuOpen) 1f else 0f,
        animationSpec = motion.defaultEffectsSpec<Float>(),
        label = "lib-fab-scrim"
    )

    // 搜尋 + 排序一次算完：這頁的書不會多到需要分頁。
    val visibleBooks = remember(books, query, sort) {
        val keyword = query.trim()
        books.asSequence()
            .filter {
                keyword.isEmpty() || it.title.contains(keyword, ignoreCase = true) ||
                        it.displayName.contains(keyword, ignoreCase = true)
            }
            .let { seq ->
                when (sort) {
                    BookSort.RECENT -> seq.sortedByDescending { maxOf(it.lastOpenedAt, it.addedAt) }
                    BookSort.TITLE -> seq.sortedBy { it.title.lowercase(Locale.getDefault()) }
                    BookSort.ADDED -> seq.sortedByDescending { it.addedAt }
                }
            }
            .toList()
    }

    // 資料夾的本數／授權狀態先算一次：`books()` 每次都要解析一遍 JSON，
    // `canRead` 更是跨進程查詢，都不該在重組裡反覆呼叫。
    val folderCounts = remember(books) { books.groupingBy { it.folderId }.eachCount() }
    val folderNames = remember(folders) { folders.associate { it.id to it.name } }
    val folderDenied = remember(folders) {
        folders.associate { it.id to !library.canRead(it) }
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
                    BookSort.RECENT to strings.sortRecent,
                    BookSort.TITLE to strings.sortByTitle,
                    BookSort.ADDED to strings.sortByAdded
                ).forEach { (option, label) ->
                    val interaction = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressRipple(
                                interactionSource = interaction,
                                shape = RoundedCornerShape(Space.s150)
                            )
                            .clickable(interactionSource = interaction, indication = null) {
                                sort = option
                                showSortSheet = false
                            }
                            .padding(vertical = Space.s100),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = sort == option, onClick = null)
                        Spacer(modifier = Modifier.size(Space.s150))
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

    // 資料夾管理：頂欄下面不再有篩選列，登記過哪些資料夾只有這裡看得到。
    if (showFolderPanel) {
        ModalBottomSheet(
            onDismissRequest = onDismissFolderPanel,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = Space.s300)
            ) {
                Text(
                    text = strings.libraryManageFolders,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Space.s100))
                Text(
                    text = strings.libraryFolderHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Space.s150))

                folders.forEach { folder ->
                    FolderRow(
                        name = folder.name.ifBlank { strings.libraryFolderFallback },
                        count = folderCounts[folder.id] ?: 0,
                        denied = folderDenied[folder.id] == true,
                        onRemove = { pendingFolderRemoval = folder }
                    )
                }

                if (folders.isEmpty()) {
                    Text(
                        text = strings.libraryNoFolderYet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = Space.s200)
                    )
                }

                Spacer(modifier = Modifier.height(Space.s100))
                val interaction = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressRipple(
                            interactionSource = interaction,
                            shape = CircleShape
                        )
                        .clickable(interactionSource = interaction, indication = null) {
                            onDismissFolderPanel()
                            pickFolder.launch(null)
                        }
                        .padding(vertical = Space.s150),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.s150)
                ) {
                    Icon(
                        Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        modifier = Modifier.size(Space.s250),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = strings.libraryNewFolder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(Space.s300))
            }
        }
    }

    pendingFolderRemoval?.let { folder ->
        AlertDialog(
            onDismissRequest = { pendingFolderRemoval = null },
            title = { Text(folder.name.ifBlank { strings.libraryFolderFallback }) },
            text = { Text(strings.libraryRemoveFolderHint) },
            confirmButton = {
                TextButton(
                    onClick = {
                        library.removeFolder(folder)
                        pendingFolderRemoval = null
                        reload()
                    }
                ) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { pendingFolderRemoval = null }) { Text(strings.cancel) }
            },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.surface,
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(Space.s100),
                    modifier = Modifier.padding(end = Space.s200, bottom = Space.s200)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        val entries = listOf(
                            FabMenuEntry(Icons.Default.CreateNewFolder, strings.libraryNewFolder) {
                                fabMenuOpen = false
                                pickFolder.launch(null)
                            },
                            FabMenuEntry(Icons.AutoMirrored.Filled.MenuBook, strings.libraryImport) {
                                fabMenuOpen = false
                                pickFile.launch(BookFormat.pickerMimeTypes)
                            }
                        )
                        entries.forEachIndexed { index, entry ->
                            FabMenuRow(
                                entry = entry,
                                // 0 是最靠近加號的那一列。
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
                            contentDescription = strings.libraryImport,
                            modifier = Modifier
                                .size(Space.s400)
                                .rotate(fabIconRotation)
                        )
                    }
                }
            }
        ) { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (visibleBooks.isEmpty()) {
                    LibraryEmpty(
                        title = if (query.isNotBlank()) {
                            strings.libraryNoMatch
                        } else {
                            strings.libraryEmpty
                        },
                        hint = strings.libraryEmptyHint,
                        topPadding = topInset + Space.s200,
                        startPadding = if (expanded) RAIL_WIDTH + Space.s400 else Space.s400
                    )
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = if (layout == NoteLayout.MASONRY) {
                            StaggeredGridCells.Fixed(gridColumns)
                        } else {
                            StaggeredGridCells.Fixed(1)
                        },
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            // 平板時左側欄是浮在內容上的，瀑布流要自己讓開那一條。
                            start = if (expanded) RAIL_WIDTH + Space.s150 else Space.s150,
                            top = topInset + Space.s200,
                            end = Space.s150,
                            bottom = 132.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(Space.s100),
                        verticalItemSpacing = Space.s100
                    ) {
                        items(visibleBooks, key = { it.id }) { book ->
                            BookCard(
                                book = book,
                                folderName = book.folderId?.let { folderNames[it] },
                                sizeBytes = remember(book.id) {
                                    library.fileOf(book).length()
                                },
                                cardShape = cardShape,
                                compact = layout == NoteLayout.LIST,
                                onClick = {
                                    library.touch(book)
                                    reload()
                                    onOpenBook(
                                        library.uriOf(book).toString(),
                                        book.displayName
                                    )
                                },
                                onRemove = {
                                    library.remove(book)
                                    reload()
                                }
                            )
                        }
                    }
                }

                // 覆蓋在頂欄下緣，不佔版面：掃描時版面不會跟著跳一下。
                if (busy) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = topInset)
                            .fillMaxWidth()
                    )
                }

                LibraryTopBar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(start = if (expanded) RAIL_WIDTH else Space.s0),
                    query = query,
                    onQueryChange = { query = it },
                    avatarPath = avatarPath,
                    layout = layout,
                    // 平板：換版面、排序、選單、頭像都搬到左側欄，頂欄只留搜尋。
                    railMode = expanded,
                    onToggleLayout = onToggleLayout,
                    onOpenSort = { showSortSheet = true },
                    onMenu = onOpenDrawer,
                    onOpenProfile = onOpenProfile
                )

                // 側欄最後畫，才蓋得住滑到它底下的瀑布流。
                if (expanded) {
                    LibrarySideRail(
                        modifier = Modifier.align(Alignment.CenterStart),
                        avatarPath = avatarPath,
                        layout = layout,
                        onToggleLayout = onToggleLayout,
                        onOpenSort = { showSortSheet = true },
                        onOpenProfile = onOpenProfile,
                        onMenu = onOpenDrawer
                    )
                }

                // 遮罩放在 Scaffold 內容裡（不是 Scaffold 外面）：這樣加號與展開的選單
                // 仍由 Scaffold 的 FAB 槽畫在上層。放到外面會連選單一起蓋住，
                // 點「匯入書籍」只會打到遮罩、把選單關掉——看起來就是「點了沒反應」。
                if (fabScrim > 0.001f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = fabScrim }
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

/** 平板左側欄的寬度：和「全部筆記」同一條。 */
private val RAIL_WIDTH = 80.dp

/**
 * 書庫的側欄（平板／橫屏）。
 *
 * 和「全部筆記」的側欄同一個尺寸、同一個材質，差別只在搜尋留在頂欄：
 * 書庫的搜尋是就地過濾，跳不到別頁去，硬搬進側欄反而多一次點擊。
 */
@Composable
private fun LibrarySideRail(
    modifier: Modifier = Modifier,
    avatarPath: String,
    layout: NoteLayout,
    onToggleLayout: () -> Unit,
    onOpenSort: () -> Unit,
    onOpenProfile: () -> Unit,
    onMenu: () -> Unit
) {
    val strings = LocalAppStrings.current
    Surface(modifier = modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.surface) {
        Row(modifier = Modifier.fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(RAIL_WIDTH)
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
                RailButton(
                    icon = if (layout == NoteLayout.MASONRY) {
                        Icons.AutoMirrored.Rounded.ViewList
                    } else {
                        Icons.Rounded.GridView
                    },
                    description = strings.toggleLayout,
                    onClick = onToggleLayout
                )
                RailButton(
                    icon = Icons.AutoMirrored.Rounded.Sort,
                    description = strings.sort,
                    onClick = onOpenSort
                )
                Spacer(modifier = Modifier.weight(1f))
                Avatar(
                    avatarPath = avatarPath,
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

/**
 * 書庫頂欄：和「全部筆記」同尺寸、同材質。
 *
 * 差別只有搜尋欄是真的可以打字（筆記那邊是跳去搜尋頁）——書庫的書全在記憶體裡，
 * 就地過濾比再開一頁快。
 */
@Composable
private fun LibraryTopBar(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    avatarPath: String,
    layout: NoteLayout,
    /** 平板模式：選單、版面、排序、頭像都在左側欄了，這裡只留搜尋欄。 */
    railMode: Boolean,
    onToggleLayout: () -> Unit,
    onOpenSort: () -> Unit,
    onMenu: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val strings = LocalAppStrings.current
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
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
                if (!railMode) {
                    Box(
                        modifier = Modifier.size(Space.s700),
                        contentAlignment = Alignment.Center
                    ) {
                        RailButton(
                            icon = Icons.Default.Menu,
                            description = strings.menu,
                            onClick = onMenu
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .weight(1f)
                        // 和筆記頁同一個寬度節奏：左右各讓 4dp。
                        .padding(horizontal = Space.s50)
                        .height(Space.s700)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = Space.s250, end = Space.s100),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = strings.bookSearch,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = onQueryChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterStart)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Space.s100),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (query.isNotEmpty()) {
                                CircleIconButton(
                                    icon = Icons.Default.Close,
                                    description = strings.clear,
                                    onClick = { onQueryChange("") }
                                )
                            }
                            if (!railMode) {
                                CircleIconButton(
                                    icon = if (layout == NoteLayout.MASONRY) {
                                        Icons.AutoMirrored.Rounded.ViewList
                                    } else {
                                        Icons.Rounded.GridView
                                    },
                                    description = strings.toggleLayout,
                                    onClick = onToggleLayout
                                )
                                CircleIconButton(
                                    icon = Icons.AutoMirrored.Rounded.Sort,
                                    description = strings.sort,
                                    onClick = onOpenSort
                                )
                            }
                        }
                    }
                }
                if (!railMode) {
                    Box(
                        modifier = Modifier.size(Space.s700),
                        contentAlignment = Alignment.Center
                    ) {
                        Avatar(
                            avatarPath = avatarPath,
                            // 與左側 48dp 圓鈕同尺寸，頂欄兩端才對稱。
                            size = Space.s600,
                            contentDescription = strings.profileTitle,
                            onClick = onOpenProfile
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(Space.s100))
        }
    }
}

/** 資料夾管理面板裡的一列：名稱、本數、移除。 */
@Composable
private fun FolderRow(
    name: String,
    count: Int,
    denied: Boolean,
    onRemove: () -> Unit
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Space.s75),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s150)
    ) {
        Icon(
            Icons.Default.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(Space.s250),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (denied) {
                    strings.libraryFolderDenied
                } else {
                    strings.libraryFolderBooks(count)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (denied) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 2
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = strings.delete,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookCard(
    book: ShelfBook,
    folderName: String?,
    sizeBytes: Long,
    cardShape: CardShape,
    compact: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(cardShape.radiusDp.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(
                start = Space.s200,
                top = Space.s200,
                end = Space.s200
            ),
            verticalArrangement = Arrangement.spacedBy(Space.s100)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(Space.s250),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (compact) 1 else 3,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = buildString {
                        if (book.extension.isNotBlank()) {
                            append(book.extension)
                            append(" · ")
                        }
                        append(formatSize(sizeBytes))
                        if (!folderName.isNullOrBlank()) {
                            append(" · ")
                            append(folderName)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryEmpty(
    title: String,
    hint: String,
    topPadding: Dp,
    startPadding: Dp = Space.s400
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding, start = startPadding, end = Space.s400),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.s150),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
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

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.ROOT, "%.1f MB", bytes / 1024f / 1024f)
    bytes >= 1024L -> String.format(Locale.ROOT, "%d KB", bytes / 1024L)
    else -> "$bytes B"
}

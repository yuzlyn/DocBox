package com.yuzlyn.mdeditor.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yuzlyn.mdeditor.ui.i18n.LocalAppStrings
import com.yuzlyn.mdeditor.ui.theme.Space
import kotlinx.coroutines.delay

/**
 * 兩個主頁（全部筆記、書庫）共用的外殼零件。
 *
 * 這些不是「通用元件」，而是**主頁這個層級**的固定語言：圓形淡色按鈕、
 * 左側抽屜、加號展開的膠囊選單。放在一起才不會兩頁各寫一套、久了走形。
 */

/** 側欄裡的一顆圓形按鈕：統一的 48dp 容器 + 24dp 圖示。 */
@Composable
fun RailButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    emphasized: Boolean = false
) {
    val motion = MaterialTheme.motionScheme
    // 選中／取消選中走 effects spring：顏色不能彈，換頁的那一下才不會「啪」地跳色。
    val container by animateColorAsState(
        targetValue = if (emphasized) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            // 本 App 的主題把 surface / surfaceContainer* 全部映射成同一個顏色，
            // 用 container 色階會「有遮罩但看不見」。改用 onSurface 的淡色疊加，
            // 任何背景、任何紙色下都保證有圓形底。
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        },
        animationSpec = motion.defaultEffectsSpec(),
        label = "rail-container"
    )
    val content by animateColorAsState(
        targetValue = if (emphasized) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = motion.defaultEffectsSpec(),
        label = "rail-content"
    )
    // 側欄與頂欄的圓鈕共用同一個按壓語言：縮放和按鈕自己的 state layer 各司其職。
    val interaction = remember { MutableInteractionSource() }
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier
            .size(Space.s600)
            .pressScale(interactionSource = interaction),
        interactionSource = interaction,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = container,
            contentColor = content
        )
    ) {
        AnimatedIcon(icon = icon, description = description, iconSize = Space.s300)
    }
}

/** 搜尋膠囊裡的小圓形按鈕：32dp 容器，和側欄用同一種遮罩語言。 */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    // 頂欄的排版／排序鈕與側欄同一套回縮，切到平板不會覺得換了一個元件。
    val interaction = remember { MutableInteractionSource() }
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier
            .size(Space.s400)
            .pressScale(interactionSource = interaction),
        interactionSource = interaction,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        AnimatedIcon(icon = icon, description = description, iconSize = Space.s200)
    }
}

/**
 * 圖示換相：排版鈕在「瀑布流／列表」之間切換時，兩個 Icon 交叉淡入淡出並略微縮放，
 * 取代原本瞬間換圖的硬切。
 *
 * 出場用 fast effects（先清掉），入場用 default effects + default spatial；
 * 不用 tween，速度差就是節奏。`SizeTransform(clip = false)` 讓 24dp/16dp 的
 * 圖示在縮放過程不會被按鈕的內容槽裁掉。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AnimatedIcon(icon: ImageVector, description: String, iconSize: Dp) {
    val motion = MaterialTheme.motionScheme
    AnimatedContent(
        targetState = icon,
        transitionSpec = {
            (
                fadeIn(motion.defaultEffectsSpec()) +
                    scaleIn(initialScale = 0.72f, animationSpec = motion.defaultSpatialSpec())
                ).togetherWith(
                fadeOut(motion.fastEffectsSpec()) +
                    scaleOut(targetScale = 0.72f, animationSpec = motion.fastSpatialSpec())
                ).using(SizeTransform(clip = false))
        },
        label = "rail-icon"
    ) { vector ->
        Icon(
            imageVector = vector,
            // 交叉淡出中的舊圖不再重複報讀，無障礙只留目前這一顆。
            contentDescription = if (vector == icon) description else null,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * 多選狀態的頂欄。
 *
 * 筆記頁與書庫頁共用同一個元件：左邊關閉、中間「已選 N 項」、右邊動作。
 * 兩頁都可用的動作只有刪除，所以 delete 是固定參數；分享／匯出屬於筆記頁的
 * 額外能力，放進 [secondaryActions] 插槽——書庫不傳，就不會出現按了沒用的圖示。
 */
@Composable
fun SelectionTopBar(
    count: Int,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryActions: @Composable RowScope.() -> Unit = {}
) {
    val strings = LocalAppStrings.current
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
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
                secondaryActions()
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

// ── 加號選單 ──────────────────────────────────────────────────────────────

/** 加號展開的一列：圖示 + 文字 + 動作。 */
class FabMenuEntry(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

/**
 * 加號選單的一列，帶錯開的進出場。
 *
 * 每一列自己管展開／收起：離加號最近的先出，收起時也由它先收，
 * 整疊像被吸回加號裡。若把全部列包在同一個 AnimatedVisibility 裡，
 * 收起只剩一次整體淡出，看起來就跟「直接消失」沒兩樣。
 *
 * 高度用 `expandVertically` / `shrinkVertically` 真的展開收合（不是只淡出），
 * 上面的列才會跟著一起滑動。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FabMenuRow(
    entry: FabMenuEntry,
    order: Int,
    total: Int,
    expanded: Boolean
) {
    val motion = MaterialTheme.motionScheme
    val visibility = remember { MutableTransitionState(false) }

    // 展開：order 0 先出；收起：倒過來，最後出來的那列最先收。
    val stagger = if (expanded) order else (total - 1 - order)
    LaunchedEffect(expanded) {
        // 延遲只能放在協程裡：md3e 的空間動效是彈簧，塞不進 delay。
        delay(stagger * 34L)
        visibility.targetState = expanded
    }

    AnimatedVisibility(
        visibleState = visibility,
        enter = fadeIn(motion.defaultEffectsSpec<Float>()) +
                scaleIn(
                    animationSpec = motion.defaultSpatialSpec<Float>(),
                    initialScale = 0.72f,
                    // 以右下角為支點：看起來是從加號那個方向長出來。
                    transformOrigin = TransformOrigin(0.9f, 1f)
                ) +
                expandVertically(
                    animationSpec = motion.defaultSpatialSpec<IntSize>(),
                    expandFrom = Alignment.Bottom
                ),
        // 與 enter 一一對應、規格相同，才是真正的反向播放。
        exit = fadeOut(motion.defaultEffectsSpec<Float>()) +
                scaleOut(
                    animationSpec = motion.defaultSpatialSpec<Float>(),
                    targetScale = 0.72f,
                    transformOrigin = TransformOrigin(0.9f, 1f)
                ) +
                shrinkVertically(
                    animationSpec = motion.defaultSpatialSpec<IntSize>(),
                    shrinkTowards = Alignment.Bottom
                )
    ) {
        FabMenuItem(
            icon = entry.icon,
            label = entry.label,
            onClick = entry.onClick,
            // 間距算在每一列身上：收起時連同留白一起收掉，才不會留下一段空白。
            // 放上方而不是下方，最後一列與加號之間才維持原本的 8dp。
            modifier = Modifier.padding(top = Space.s75)
        )
    }
}

/** 加號選單的一列：圓角膠囊 + 圖示 + 文字，整列貼右。 */
@Composable
private fun FabMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        // 統一最小寬度，整列右緣才會切齊，不會像階梯一樣參差。
        modifier = modifier.widthIn(min = 144.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        tonalElevation = Space.s0,
        shadowElevation = Space.s50
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Space.s250, vertical = Space.s125),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s150)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Space.s250)
            )
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// ── 抽屜 ──────────────────────────────────────────────────────────────────

/**
 * 抽屜裡的一列。
 *
 * 抽屜的內容是固定的幾列，不是依頁面拼出來的。既然內容固定，就用資料描述、
 * 一次畫完——手寫好幾段幾乎一樣的 NavigationDrawerItem，改一次順序就要動好幾個地方。
 */
private class DrawerEntry(
    val icon: ImageVector,
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

/**
 * 左側抽屜：整個 App 的主導覽。
 *
 * **內容永遠不變**：不論現在站在哪一頁，永遠是同一批列、同一個順序。
 * 前一版是兩個主頁各畫一份——筆記頁少「資料夾」、書庫頁少「筆記本」，
 * 切一次頁選單就矮一截、換一副樣子，看起來像在跳。導覽是「位置」不是「內容」，
 * 不該隨頁面變形，所以整份抽屜提到主頁之上，只畫一次。
 *
 * 只畫一次還有第二個好處：切頁時它不會被銷毀重建。舊版每切一次頁就是一個
 * 全新的節點、從頭播一次進場，動畫永遠接不上——這才是「切到書庫時側邊欄
 * 沒有動畫」的真正原因。
 *
 * @param notesSelected / notebooksSelected / librarySelected 目前所在的那一列（只亮一列）
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeDrawer(
    /**
     * 開關狀態用 State 傳進來，由這一層自己讀。
     *
     * 呼叫端（MainActivity）若直接讀值，開關抽屜就會重組整個主內容；
     * 抽屜動畫的第一幀正好和整頁重組撞在一起，看起來就是卡一下。
     */
    open: State<Boolean>,
    notesSelected: Boolean,
    notebookSelected: Boolean,
    librarySelected: Boolean,
    statsSelected: Boolean,
    /** 統計整個關掉時不收這一列：留一個點了什麼都不能做的入口，比沒有更糟。 */
    showStats: Boolean,
    onDismiss: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenNotebooks: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenStats: () -> Unit,
    onManageFolders: () -> Unit,
    onChangeFolder: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    /** 作者頁自成一列，不再只能從「說明」的頁尾連結鑽進去。 */
    onOpenAuthor: () -> Unit
) {
    val strings = LocalAppStrings.current
    val motion = MaterialTheme.motionScheme
    val isOpen = open.value

    // 初值跟著當下的 open 走，不是寫死 0：這個節點只有在 NavEntry 重建時才重新
    // 建立，那一刻若抽屜是開著的，就該直接站在開啟的位置，而不是再滑進來一次。
    val drawerProgress = remember { Animatable(if (isOpen) 1f else 0f) }

    // MD3 把動效拆成兩條軌道，抽屜正好各用一條：
    //   位置（translationX）→ defaultSpatialSpec，Expressive 的空間彈簧
    //   遮罩（alpha）      → defaultEffectsSpec，只改透明度、不帶位移
    // 兩者共用同一條曲線會彆扭：彈簧套在遮罩上，變暗的節奏會忽快忽慢。
    //
    // 用彈簧而不是固定時長曲線還有第三個好處：開到一半又改主意時，它會帶著
    // 當下的速度轉向；tween 只能從 0 重跑一趟，看起來就是「頓一下」。
    LaunchedEffect(isOpen) {
        drawerProgress.animateTo(
            targetValue = if (isOpen) 1f else 0f,
            animationSpec = motion.defaultSpatialSpec()
        )
    }
    // 遮罩用 effects spring：進場 default、退場 fast。MD3 的序列化做法是
    // 「先清場再進場」——關閉時讓遮罩先退，抽屜還在滑的那幾幀就不會壓著一層暗幕。
    val scrimAlpha = remember { Animatable(if (isOpen) 1f else 0f) }
    LaunchedEffect(isOpen) {
        scrimAlpha.animateTo(
            targetValue = if (isOpen) 1f else 0f,
            animationSpec = if (isOpen) {
                motion.defaultEffectsSpec()
            } else {
                motion.fastEffectsSpec()
            }
        )
    }

    // 模態抽屜開著時，返回鍵先收抽屜，而不是直接把整頁退掉。
    BackHandler(enabled = isOpen) { onDismiss() }

    // 這個鋪滿全屏的 Box 只在抽屜打開時才掛手勢節點。
    //
    // 之前寫成「永遠掛 pointerInput，block 裡判斷 open 再決定要不要處理」——
    // 看起來一樣，實際差很多：pointerInput 本身就是命中測試的目標，而這個 Box
    // 是全屏、又壓在內容之上的兄弟節點，Compose 命中到它就**不再往下測**。
    // block 有沒有做事是下一步的事，手勢在命中的那一刻就斷了。
    // 結果就是：抽屜關著時，整頁的列表、卡片全都拖不動。
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isOpen) {
                    Modifier.pointerInput(Unit) {
                        val threshold = Space.s600.toPx()
                        var total = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                if (dragAmount < 0f) {
                                    change.consume()
                                    total += dragAmount
                                }
                            },
                            onDragEnd = {
                                if (total < -threshold) onDismiss()
                                total = 0f
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        // 遮罩：透明度在內部讀取，開關時不會把整份抽屜內容重新組合。
        val interaction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrimAlpha.value.coerceIn(0f, 1f) }
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                .then(
                    if (isOpen) {
                        Modifier.clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = onDismiss
                        )
                    } else {
                        Modifier
                    }
                )
        )

        Surface(
            modifier = Modifier
                // 手機是 86% 寬；平板橫屏再乘下去就幾乎整屏了，
                // 所以封頂 320dp——和豎屏同一個寬度，才有「抽屜」的感覺。
                .width(
                    (LocalConfiguration.current.screenWidthDp.dp * 0.86f)
                        .coerceAtMost(320.dp)
                )
                .fillMaxHeight()
                .graphicsLayer {
                    // 鉗位：彈簧會短暫超過 1，不鉗的話整塊會被推到右邊、左緣露縫。
                    translationX = (drawerProgress.value.coerceIn(0f, 1f) - 1f) * size.width
                },
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            // MD3 模態抽屜只在**靠內容那一側**做圓角，貼著螢幕的左緣是直角。
            shape = RoundedCornerShape(
                topStart = Space.s0,
                topEnd = Space.s200,
                bottomEnd = Space.s200,
                bottomStart = Space.s0
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    // 橫屏手機只有 360dp 高，這幾列加標題放不下；
                    // 寧可整份抽屜可以捲，也不要最後兩列被切掉。
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(bottom = Space.s200)
            ) {
                Text(
                    text = strings.appName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        start = Space.s300,
                        end = Space.s300,
                        top = Space.s250,
                        bottom = Space.s200
                    )
                )

                // 固定內容。上面幾列是「去哪裡」（統計可關），下面四列是動作與設定。
                // 統計接著書庫排在導覽組裡：它是第三個主頁，不是設定項。
                val navEntries = buildList {
                    add(
                        DrawerEntry(
                            Icons.AutoMirrored.Filled.Note,
                            strings.allNotes,
                            notesSelected,
                            onOpenNotes
                        )
                    )
                    add(
                        DrawerEntry(
                            Icons.Default.Folder,
                            strings.notebooks,
                            notebookSelected,
                            onOpenNotebooks
                        )
                    )
                    add(
                        DrawerEntry(
                            Icons.AutoMirrored.Filled.MenuBook,
                            strings.library,
                            librarySelected,
                            onOpenLibrary
                        )
                    )
                    if (showStats) {
                        add(
                            DrawerEntry(
                                Icons.Default.Insights,
                                strings.stats,
                                statsSelected,
                                onOpenStats
                            )
                        )
                    }
                    add(
                        DrawerEntry(
                            Icons.Default.FolderOpen,
                            strings.libraryManageFolders,
                            false,
                            onManageFolders
                        )
                    )
                }
                val entries = navEntries + listOf(
                    DrawerEntry(Icons.Default.SdCard, strings.changeFolder, false, onChangeFolder),
                    DrawerEntry(Icons.Default.Settings, strings.settings, false, onOpenSettings),
                    DrawerEntry(
                        Icons.AutoMirrored.Filled.HelpOutline,
                        strings.help,
                        false,
                        onOpenHelp
                    ),
                    // 作者頁排在說明之後、同一組裡：兩者都是「看資料」，但作者頁
                    // 是獨立目的地，不該再從說明頁尾的一行字才找得到。
                    DrawerEntry(
                        Icons.Default.Person,
                        strings.authorTitle,
                        false,
                        onOpenAuthor
                    )
                )

                entries.forEachIndexed { index, entry ->
                    NavigationDrawerItem(
                        icon = { Icon(entry.icon, contentDescription = null) },
                        label = { Text(entry.label) },
                        selected = entry.selected,
                        onClick = entry.onClick,
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    // 導覽（去哪裡）與設定（做什麼）之間用髮絲線分開：
                    // 這兩組的性質不同，混在一起會讓「資料夾」看起來像另一個目的地。
                    if (index == navEntries.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                horizontal = Space.s300,
                                vertical = Space.s100
                            ),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

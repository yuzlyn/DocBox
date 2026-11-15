package com.yuzlyn.mdeditor.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yuzlyn.mdeditor.data.ImageInsertResult
import com.yuzlyn.mdeditor.data.NoteSaveState
import com.yuzlyn.mdeditor.data.importer.ImportFormat
import com.yuzlyn.mdeditor.data.export.NoteExportFormat
import com.yuzlyn.mdeditor.data.export.NoteExportManager
import com.yuzlyn.mdeditor.data.export.CardPosition
import com.yuzlyn.mdeditor.data.export.ExportCard
import com.yuzlyn.mdeditor.data.export.buildExportCard
import com.yuzlyn.mdeditor.data.profile.ProfileData
import com.yuzlyn.mdeditor.data.preview.rememberPreviewPalette
import com.yuzlyn.mdeditor.data.settings.AppSettings
import com.yuzlyn.mdeditor.data.settings.DarkModeOption
import com.yuzlyn.mdeditor.data.settings.NoteBackground
import com.yuzlyn.mdeditor.data.settings.NoteBackgroundInk
import com.yuzlyn.mdeditor.data.settings.WritingGuide
import com.yuzlyn.mdeditor.ui.i18n.AppStrings
import com.yuzlyn.mdeditor.ui.i18n.imageInsertError
import com.yuzlyn.mdeditor.ui.i18n.LocalAppStrings
import com.yuzlyn.mdeditor.ui.component.EditHistory
import com.yuzlyn.mdeditor.ui.component.EditorBringIntoViewSpec
import com.yuzlyn.mdeditor.ui.component.EditSnapshot
import com.yuzlyn.mdeditor.ui.component.VerticalBodyEditor
import com.yuzlyn.mdeditor.ui.component.CellAlignedTransformation
import com.yuzlyn.mdeditor.ui.component.GuideLines
import com.yuzlyn.mdeditor.ui.component.buildGridLines
import com.yuzlyn.mdeditor.ui.component.WritingGuideBackground
import com.yuzlyn.mdeditor.ui.component.caretScrollDelta
import com.yuzlyn.mdeditor.ui.component.clampScrollTarget
import com.yuzlyn.mdeditor.ui.component.pressRipple
import com.yuzlyn.mdeditor.ui.preview.MarkdownPreview
import com.yuzlyn.mdeditor.ui.preview.PreviewSettingsDialog
import com.yuzlyn.mdeditor.ui.theme.Space
import com.yuzlyn.mdeditor.data.fonts.FontStore
import com.yuzlyn.mdeditor.ui.theme.noteFontFamily
import com.yuzlyn.mdeditor.ui.viewmodel.FileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import kotlin.math.floor
import kotlin.math.max
import com.yuzlyn.mdeditor.ui.component.CELL_PITCH_EM
import com.yuzlyn.mdeditor.ui.component.HorizontalCellText
import com.yuzlyn.mdeditor.ui.component.ROW_PITCH_EM
import com.yuzlyn.mdeditor.ui.component.buildHCellLayout

private enum class FormatAction(
    val badge: String,
    val prefix: String? = null,
    val wrap: Pair<String, String>? = null,
    val snippet: String? = null
) {
    H1("H1", prefix = "# "),
    H2("H2", prefix = "## "),
    H3("H3", prefix = "### "),
    BOLD("B", wrap = "**" to "**"),
    ITALIC("I", wrap = "*" to "*"),
    STRIKE("S", wrap = "~~" to "~~"),
    HIGHLIGHT("==", wrap = "==" to "=="),
    INLINE_CODE("<>", wrap = "`" to "`"),
    QUOTE(">", prefix = "> "),
    BULLET("•", prefix = "- "),
    ORDERED("1.", prefix = "1. "),
    TASK("☑", prefix = "- [ ] "),
    MATH("∑", wrap = "\u0024" to "\u0024"),
    MATH_BLOCK("$$", snippet = "\u0024\u0024\n\\sum_{i=1}^{n} i = \\frac{n(n+1)}{2}\n\u0024\u0024")
}

/** 選單上的名字。枚舉本身不帶字，換語言時不必重建一張表。 */
private fun FormatAction.label(strings: AppStrings): String = when (this) {
    FormatAction.H1 -> strings.editorFormatH1
    FormatAction.H2 -> strings.editorFormatH2
    FormatAction.H3 -> strings.editorFormatH3
    FormatAction.BOLD -> strings.editorFormatBold
    FormatAction.ITALIC -> strings.editorFormatItalic
    FormatAction.STRIKE -> strings.editorFormatStrike
    FormatAction.HIGHLIGHT -> strings.editorFormatHighlight
    FormatAction.INLINE_CODE -> strings.editorFormatInlineCode
    FormatAction.QUOTE -> strings.editorFormatQuote
    FormatAction.BULLET -> strings.editorFormatBullet
    FormatAction.ORDERED -> strings.editorFormatOrdered
    FormatAction.TASK -> strings.editorFormatTask
    FormatAction.MATH -> strings.editorFormatMath
    FormatAction.MATH_BLOCK -> strings.editorFormatMathBlock
}

/** 沒選字時插進去的佔位文字，插入後會選中，直接打字就蓋掉。 */
private fun FormatAction.placeholder(strings: AppStrings): String = when (this) {
    FormatAction.BOLD -> strings.editorFormatBold
    FormatAction.ITALIC -> strings.editorFormatItalic
    FormatAction.STRIKE -> strings.editorFormatStrike
    FormatAction.HIGHLIGHT -> strings.editorFormatHighlight
    // 行內程式碼與數學的佔位是語法本身，不隨語言變。
    FormatAction.INLINE_CODE -> "code"
    FormatAction.MATH -> "E = mc^2"
    else -> ""
}

private enum class InsertAction(val icon: ImageVector, val snippet: String) {
    IMAGE(Icons.Default.Image, ""),
    CODE(Icons.Default.Code, "```kotlin\n// code\n```"),
    TABLE(Icons.Default.TableChart, "| Column | Column |\n| :--- | ---: |\n| Cell | Cell |"),
    LINK(Icons.Default.Link, "[文字](https://example.com)"),
    RULE(Icons.Default.HorizontalRule, "---")
}

/** 插入選單上的名字；帶進來的表格與連結範例本身是 Markdown 語法，不動。 */
private fun InsertAction.label(strings: AppStrings): String = when (this) {
    InsertAction.IMAGE -> strings.image
    InsertAction.CODE -> strings.codeBlock
    InsertAction.TABLE -> strings.table
    InsertAction.LINK -> strings.link
    InsertAction.RULE -> strings.divider
}
private enum class EditorSheet { INSERT, STYLE, FORMAT, MORE, EXPORT }

/** 匯出面板的兩種模式：直接分享，或存到使用者選的位置。 */
private enum class ExportMode { SHARE, SAVE_COPY }

/** 等使用者選完儲存位置後才寫入的內容。 */
private data class PendingCopy(val bytes: ByteArray, val mimeType: String)

/** CreateDocument 的輸入：檔名與 MIME 都由呼叫端決定，一支 launcher 就能支援多種格式。 */
private data class DocumentRequest(val fileName: String, val mimeType: String)

/** 匯出面板每一列左側的圖示。 */
private val NoteExportFormat.icon: ImageVector
    get() = when (this) {
        NoteExportFormat.MARKDOWN -> Icons.Default.Code
        NoteExportFormat.TEXT -> Icons.Default.Notes
        NoteExportFormat.WORD -> Icons.Default.Description
        NoteExportFormat.PDF -> Icons.Default.PictureAsPdf
        NoteExportFormat.IMAGE -> Icons.Default.Image
    }

/** 匯出格式在選單上的名字；Markdown 是格式名本身，不翻。 */
private fun NoteExportFormat.label(strings: AppStrings): String = when (this) {
    NoteExportFormat.MARKDOWN -> "Markdown"
    NoteExportFormat.TEXT -> strings.formatPlainText
    NoteExportFormat.WORD -> strings.formatWord
    NoteExportFormat.PDF -> strings.formatPdf
    NoteExportFormat.IMAGE -> strings.formatImage
}

private val MathSymbols = listOf(
    "\\alpha" to "α", "\\beta" to "β", "\\theta" to "θ", "\\pi" to "π",
    "\\infty" to "∞", "\\times" to "×", "\\leq" to "≤", "\\geq" to "≥",
    "\\neq" to "≠", "\\approx" to "≈", "\\rightarrow" to "→", "\\partial" to "∂",
    "\\nabla" to "∇", "\\sum_{i=1}^{n}" to "∑", "\\int_{a}^{b}" to "∫",
    "\\frac{a}{b}" to "a/b", "\\sqrt{x}" to "√x"
)

/** 標題兩端依滾動方向淡出：貼近哪一端，哪一端就不淡。 */
private fun Modifier.fadeEdges(start: Float, end: Float, tailColor: Color): Modifier =
    this.drawWithContent {
        drawContent()
        if (start > 0.01f) {
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to tailColor,
                    0.28f to Color.Transparent
                ),
                alpha = start.coerceIn(0f, 1f)
            )
        }
        if (end > 0.01f) {
            drawRect(
                brush = Brush.horizontalGradient(
                    0.72f to Color.Transparent,
                    1f to tailColor
                ),
                alpha = end.coerceIn(0f, 1f)
            )
        }
    }

@Composable
private fun SheetAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color? = null
) {
    val resolved = tint ?: MaterialTheme.colorScheme.onSurface
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 原生 ripple 不受外層 clip 影響，改用自繪且依 shape 裁切的波紋。
            .pressRipple(
                interactionSource = interactionSource,
                shape = RoundedCornerShape(Space.s150)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = Space.s300, vertical = Space.s150),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s200)
    ) {
        Icon(icon, contentDescription = null, tint = resolved, modifier = Modifier.size(Space.s300))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = resolved)
    }
}

/**
 * 匯出面板裡的「個人名片」區塊。
 *
 * 一列總開關 + 展開後的細節：位置（前／後）、頭像、三個欄位。
 * 所有勾選都直接寫回設定——名片是「我這個人長什麼樣」，不該每次分享重設一遍。
 *
 * 下面那行預覽刻意用 `·` 串起來而不是分行：面板只有一行的高度，
 * 分行會把手機上的分享面板撐成兩屏。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExportCardSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    /** 直排筆記：名片只能排在正文之後，位置選項換成一句說明。 */
    vertical: Boolean,
    profile: ProfileData,
    card: ExportCard?,
    settings: AppSettings,
    onPositionChange: (CardPosition) -> Unit,
    onShowAvatarChange: (Boolean) -> Unit,
    onShowNicknameChange: (Boolean) -> Unit,
    onShowEmailChange: (Boolean) -> Unit,
    onShowGithubChange: (Boolean) -> Unit
) {
    val motion = MaterialTheme.motionScheme
    val strings = LocalAppStrings.current
    val hasNickname = profile.nickname.isNotBlank()
    val hasEmail = profile.email.isNotBlank()
    val hasGithub = profile.githubUrl != null
    // 一個欄位都沒有就沒有名片可附：開關本身要停用，而不是讓人勾了一片空白。
    val hasAny = hasNickname || hasEmail || hasGithub

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.s300, vertical = Space.s150),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.cardSectionTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        !hasAny -> strings.cardNotSet
                        card == null -> strings.cardAllOff
                        !enabled -> strings.cardNotAttached
                        else -> card.lines.joinToString(" · ")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled && hasAny,
                onCheckedChange = onEnabledChange,
                enabled = hasAny
            )
        }

        AnimatedVisibility(
            visible = enabled && hasAny,
            enter = fadeIn(motion.defaultEffectsSpec<Float>()) +
                    expandVertically(
                        animationSpec = motion.defaultSpatialSpec<IntSize>(),
                        expandFrom = Alignment.Top
                    ),
            exit = fadeOut(motion.defaultEffectsSpec<Float>()) +
                    shrinkVertically(
                        animationSpec = motion.defaultSpatialSpec<IntSize>(),
                        shrinkTowards = Alignment.Top
                    )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Space.s300, end = Space.s300)
            ) {
                if (vertical) {
                    // 直排：名片固定是落款，位置不必選，直接說明。
                    Text(
                        text = strings.cardVerticalNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = Space.s100)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Space.s100),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.cardPositionLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        ButtonGroup(
                            overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
                            horizontalArrangement = Arrangement.spacedBy(Space.s50)
                        ) {
                            toggleableItem(
                                checked = settings.cardPlacement == CardPosition.BEFORE,
                                label = strings.cardPositionBefore,
                                onCheckedChange = { checked ->
                                    if (checked) onPositionChange(CardPosition.BEFORE)
                                }
                            )
                            toggleableItem(
                                checked = settings.cardPlacement == CardPosition.AFTER,
                                label = strings.cardPositionAfter,
                                onCheckedChange = { checked ->
                                    if (checked) onPositionChange(CardPosition.AFTER)
                                }
                            )
                        }
                    }
                }
                CardOptionRow(
                    label = strings.cardAvatar,
                    hint = strings.cardAvatarHint,
                    checked = settings.cardShowAvatar,
                    enabled = profile.avatarPath.isNotBlank(),
                    onCheckedChange = onShowAvatarChange
                )
                CardOptionRow(
                    label = strings.cardNickname,
                    hint = null,
                    checked = settings.cardShowNickname,
                    enabled = hasNickname,
                    onCheckedChange = onShowNicknameChange
                )
                CardOptionRow(
                    label = strings.cardEmail,
                    hint = null,
                    checked = settings.cardShowEmail,
                    enabled = hasEmail,
                    onCheckedChange = onShowEmailChange
                )
                CardOptionRow(
                    label = strings.cardGithub,
                    hint = null,
                    checked = settings.cardShowGithub,
                    enabled = hasGithub,
                    onCheckedChange = onShowGithubChange
                )
            }
        }
    }
}

/** 名片裡的一列開關：標籤、可選的小字說明、右邊一顆 switch。 */
@Composable
private fun CardOptionRow(
    label: String,
    hint: String?,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Space.s75),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
            )
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked && enabled, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun EditorScreen(
    noteId: String,
    viewModel: FileViewModel,
    onNavigateBack: () -> Unit,
    /** 開啟這篇筆記的原始 PDF（只有從 PDF 匯入、且拿到常駐權限時才有）。 */
    onOpenSourcePdf: (String) -> Unit = {},
    /** 筆記背景（預設／純色／圖片）搬到設定頁，編輯器只留一個入口。 */
    onOpenNoteBackground: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val notes by viewModel.notes.collectAsState()
    val note = remember(noteId, notes) { notes.find { it.id == noteId } }
    val previewConfig by viewModel.previewConfig.collectAsState()
    val palette = rememberPreviewPalette(previewConfig)
    val appSettings by viewModel.appSettings.collectAsState()
    // 這篇筆記自己的排版優先；沒設定過（null）就沿用設定裡的預設排版。
    val verticalLayout = note?.verticalLayout ?: appSettings.verticalLayout
    // 單篇沒設過就沿用設定裡的預設（null = 跟隨）。
    val writingGuide = WritingGuide.fromId(note?.writingGuide ?: appSettings.writingGuide)
    val bodyFontFamily = noteFontFamily(appSettings.noteFont)
    val backgroundBitmap = rememberAvatarBitmap(appSettings.noteBackground)
    // 背景預設先在本機生效、設定值寫回後再交還，避免點選後要等 DataStore 落盤
    // 才看到變化（那段空窗就是「點了沒反應」的頓感）。
    var pendingBackgroundPreset by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(appSettings.noteBackgroundPreset, pendingBackgroundPreset) {
        if (pendingBackgroundPreset != null &&
            pendingBackgroundPreset == appSettings.noteBackgroundPreset
        ) {
            pendingBackgroundPreset = null
        }
    }
    val backgroundPreset = NoteBackground.fromId(
        pendingBackgroundPreset ?: appSettings.noteBackgroundPreset
    )
    // 固定色系一律用預設自己的文字／頂欄／底欄色，系統切亮暗都不翻色；
    // 默認則完全交給主題（Monet 取色、深色模式）。背景圖片另外可以指定字的深淺。
    val editorColors = rememberEditorColors(
        preset = backgroundPreset,
        hasImage = backgroundBitmap != null,
        ink = appSettings.backgroundInk
    )
    val editorBackground = editorColors.background

    // 狀態列／導覽列圖標跟著「實際看到的底色」走：深底配白圖標、淺底配黑圖標。
    // 主題那邊的 SideEffect 會在其他設定變動時把圖標重設成主題值（例如剛選完背景色），
    // 所以這裡每次重繪都再套一次；離開編輯頁時還原成主題的判斷。
    val editorView = LocalView.current
    val systemDark = isSystemInDarkTheme()
    val insetsController = remember(editorView) {
        val activity = editorView.context as? Activity
        activity?.let { WindowCompat.getInsetsController(it.window, editorView) }
    }
    val themeDark = when (appSettings.darkMode) {
        DarkModeOption.FOLLOW_SYSTEM -> systemDark
        DarkModeOption.DARK -> true
        DarkModeOption.LIGHT -> false
    }
    if (editorColors.isFixed && insetsController != null) {
        SideEffect {
            insetsController.isAppearanceLightStatusBars = !editorColors.isDark
            insetsController.isAppearanceLightNavigationBars = !editorColors.isDark
        }
    }
    DisposableEffect(editorColors.isFixed, insetsController, themeDark) {
        onDispose {
            if (editorColors.isFixed && insetsController != null) {
                insetsController.isAppearanceLightStatusBars = !themeDark
                insetsController.isAppearanceLightNavigationBars = !themeDark
            }
        }
    }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    var isExiting by remember { mutableStateOf(false) }
    var initialized by remember(noteId) { mutableStateOf(false) }
    var autoFocusDone by remember(noteId) { mutableStateOf(false) }
    var isPreview by remember(noteId) { mutableStateOf(false) }
    var showPreviewSettings by remember { mutableStateOf(false) }
    var activeSheet by remember { mutableStateOf<EditorSheet?>(null) }
    var actionsEnabled by remember { mutableStateOf(true) }
    var titleOverflow by remember { mutableStateOf(false) }

    val saveStates by viewModel.saveStates.collectAsState()
    val saveState = saveStates[noteId] ?: NoteSaveState.IDLE
    val recoveredNotes by viewModel.recoveredNotes.collectAsState()
    var showRecoveredNotice by remember(noteId) { mutableStateOf(false) }
    LaunchedEffect(noteId, recoveredNotes) {
        if (noteId in recoveredNotes) {
            showRecoveredNotice = true
            delay(6_000)
            showRecoveredNotice = false
            viewModel.acknowledgeRecoveredDraft(noteId)
        }
    }

    var titleFocused by remember(noteId) { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    // 系統相片選擇器：Android 13+ 走 Photo Picker，舊版本由 Activity 函式庫自動退回文件選擇器。
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.attachImage(noteId, uri)
    }

    // 「已保存」只亮一秒就交還標題；保存失敗則一直留著，等下一次存成功才消失。
    var savedHintVisible by remember(noteId) { mutableStateOf(false) }
    LaunchedEffect(saveState) {
        if (saveState == NoteSaveState.SAVED) {
            savedHintVisible = true
            delay(1_000)
            savedHintVisible = false
        } else {
            savedHintVisible = false
        }
    }

    // 頂欄什麼時候用「狀態字樣」蓋住標題：剛存好、存失敗、剛還原草稿。
    // 正在打字（PENDING／SAVING）或正在改標題時，標題優先——點一下標題就能回到編輯。
    val showSaveHint = !isPreview && !titleFocused && (
            showRecoveredNotice ||
                    savedHintVisible ||
                    saveState == NoteSaveState.FAILED
            )
    val saveHintText = when {
        showRecoveredNotice -> strings.editorSaveRecovered
        saveState == NoteSaveState.FAILED -> strings.editorSaveFailed
        else -> strings.editorSaved
    }
    val saveHintColor = when {
        saveState == NoteSaveState.FAILED -> editorColors.error
        showRecoveredNotice -> editorColors.accent
        else -> editorColors.onBackgroundVariant
    }

    var titleValue by remember(noteId) { mutableStateOf(TextFieldValue("")) }
    var bodyFieldValue by remember(noteId) { mutableStateOf(TextFieldValue()) }
    val bodyScroll = rememberScrollState()
    var bodyViewportPx by remember { mutableStateOf(0) }
    // 接管「自動帶入畫面」的政策：手勢期間不動、大跳一律放棄（見 EditorBringIntoViewSpec）。
    val bringIntoViewSpec = remember { EditorBringIntoViewSpec() }
    // 游標矩形（相對輸入框）＋輸入框在滾動內容裡的起點－目前滾動值，
    // 才是游標真正落在螢幕上的位置。三者缺一，換算就會差一整個視口。
    var caretRect by remember { mutableStateOf(Rect.Zero) }
    var bodyTextTopPx by remember { mutableStateOf(0f) }
    // 上一次看到的內容與視口高度：只有這兩者變了才需要校正游標位置。
    var lastBodyText by remember(noteId) { mutableStateOf("") }
    var lastViewportPx by remember(noteId) { mutableStateOf(0) }
    val contentScope = rememberCoroutineScope()
    val density = LocalDensity.current
    // 底部工具列（40dp 按鈕 + 上下 12dp）佔用的高度；內文與游標可視範圍往上讓這麼多。
    val editorBottomReserve = Space.s900 + Space.s200
    val toolbarReservePx = with(density) { editorBottomReserve.toPx() }

    // 編輯歷史：連續打字合併成一步，停手後才切新的一步（規則在 EditHistory 裡）。
    val editHistory = remember(noteId) { EditHistory() }
    var canUndo by remember(noteId) { mutableStateOf(false) }
    var canRedo by remember(noteId) { mutableStateOf(false) }
    // 彈窗開啟期間不再訂閱 ime：鍵盤收合動畫的每一幀都會讓編輯區重新測量，
    // 這裡改成一次性的高度切換，避免動畫期間反覆重排。
    val editorImeInsets = if (activeSheet == null) WindowInsets.ime else WindowInsets(0)

    // 匯出：分享與存副本共用同一個格式面板；內容都在 IO 產生，不卡輸入。
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState()
    val exportManager = remember(context) { NoteExportManager(context) }
    var exportMode by remember { mutableStateOf(ExportMode.SHARE) }
    var pendingCopy by remember { mutableStateOf<PendingCopy?>(null) }
    // 名片的三個自由度（位置、頭像、各欄位）都跟著設定走：
    // 使用者調一次，之後每次分享都還是那一份，不必重勾。
    val card = remember(
        profile,
        appSettings.cardPosition,
        appSettings.cardShowAvatar,
        appSettings.cardShowNickname,
        appSettings.cardShowEmail,
        appSettings.cardShowGithub,
        verticalLayout
    ) {
        buildExportCard(
            profile = profile,
            // 直排由右往左讀，名片只可能是落款，插在正文前會擋住標題。
            position = if (verticalLayout) CardPosition.AFTER else appSettings.cardPlacement,
            showAvatar = appSettings.cardShowAvatar,
            showNickname = appSettings.cardShowNickname,
            showEmail = appSettings.cardShowEmail,
            showGithub = appSettings.cardShowGithub
        )
    }

    // 系統「另存新檔」：先挑位置，回來才把已經產好的內容寫進去。
    val saveCopyLauncher = rememberLauncherForActivityResult(
        contract = CreateDocumentContract()
    ) { uri: Uri? ->
        val pending = pendingCopy
        pendingCopy = null
        if (uri != null && pending != null) {
            contentScope.launch {
                val ok = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(pending.bytes)
                        } ?: error(strings.msgCannotOpenFile)
                    }
                }.isSuccess
                snackbarHostState.showSnackbar(
                    if (ok) strings.msgCopySaved else strings.msgCopySaveFailed
                )
            }
        }
    }

    // 游標被頂欄／底欄遮住才回正，而且只補最小距離；在可視範圍內一律不動。
    // 排版每打一個字都會重跑，這裡用「游標矩形」當鍵：位置沒變就不會反覆校正。
    LaunchedEffect(caretRect, bodyViewportPx, bodyTextTopPx) {
        if (bodyViewportPx <= 0 || caretRect == Rect.Zero) return@LaunchedEffect
        // 只有「內容變了」或「鍵盤把視口壓矮」才需要校正。
        // 長按選字、複製、點一下換游標都不動畫面：那些操作本來就發生在看得見的位置，
        // 跟著游標滾，反而會在選取結束、選單收起的那一刻跳一下。
        val textChanged = bodyFieldValue.text != lastBodyText
        val previousViewport = lastViewportPx
        val viewportShrank = previousViewport > 0 && bodyViewportPx < previousViewport
        lastBodyText = bodyFieldValue.text
        lastViewportPx = bodyViewportPx
        if (!textChanged && !viewportShrank) return@LaunchedEffect

        val safeTop = with(density) { Space.s50.toPx() }
        // 游標此刻在畫面哪裡（用的是上一次排版記下的矩形）。
        fun caretTopNow() = bodyTextTopPx + caretRect.top - bodyScroll.value.toFloat()
        fun caretBottomNow() = bodyTextTopPx + caretRect.bottom - bodyScroll.value.toFloat()

        // 鍵盤彈出、視口被壓矮時，只有「游標原本就在畫面裡」才跟著往下讓。
        // 長按選字會先讓輸入框取得焦點、鍵盤跟著彈出，而這時 selection 還停在上一次的位置
        // （常常是 0，也就是文件開頭）。照著它校正，就是使用者說的「在正文底部選字卻滑到最上面」。
        if (!textChanged && viewportShrank) {
            val oldSafeBottom = previousViewport - toolbarReservePx
            val caretWasVisible = caretBottomNow() > safeTop && caretTopNow() < oldSafeBottom
            if (!caretWasVisible) return@LaunchedEffect
        }

        // 等這一幀的尺寸（含鍵盤高度）都落定，避免拿中間態算出過大的位移。
        withFrameNanos { }
        // 使用者正在拖動、慣性滑動還沒停：這一刻不插手，等他停下來。
        if (bodyScroll.isScrollInProgress) return@LaunchedEffect
        val delta = caretScrollDelta(
            caretTop = caretTopNow(),
            caretBottom = caretBottomNow(),
            safeTop = safeTop,
            safeBottom = bodyViewportPx - toolbarReservePx
        )
        if (delta == 0f) return@LaunchedEffect
        // 一次校正不該跨過一整個視窗；算出來超過，說明座標還沒同步，寧可不動也不要大跳。
        if (abs(delta) > bodyViewportPx) return@LaunchedEffect
        // 四捨五入而不是直接截斷：少算的那不到 1px 會讓游標一直壓在邊線上，
        // 下一個字又觸發一次校正，看起來就是細碎的抖動。
        bodyScroll.scrollTo(
            clampScrollTarget(
                bodyScroll.value.toFloat(),
                delta,
                bodyScroll.maxValue.toFloat()
            ).roundToInt()
        )
    }

    val source = remember(titleValue.text, bodyFieldValue.text) {
        buildString {
            if (titleValue.text.isNotBlank()) {
                append("# ").append(titleValue.text).append("\n\n")
            }
            append(bodyFieldValue.text)
        }
    }

    val modeChangeSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
    val selection = titleValue.selection
    val fadeStart = if (titleOverflow && selection.start > 0) 1f else 0f
    val fadeEnd = if (titleOverflow && selection.end < titleValue.text.length) 1f else 0f
    val lastEdited = remember(note?.lastModified) {
        val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        formatter.format(Date(note?.lastModified ?: System.currentTimeMillis()))
    }

    fun syncHistory() {
        canUndo = editHistory.canUndo
        canRedo = editHistory.canRedo
    }

    // 每次內容真的變了就記一筆；只移動游標不會產生步驟。
    fun recordHistory() {
        if (!initialized) return
        editHistory.record(
            EditSnapshot(
                title = titleValue.text,
                body = bodyFieldValue.text,
                selectionStart = bodyFieldValue.selection.start,
                selectionEnd = bodyFieldValue.selection.end
            ),
            now = System.currentTimeMillis()
        )
        syncHistory()
    }

    LaunchedEffect(note) {
        if (!initialized && note != null) {
            titleValue = TextFieldValue(note.title, selection = TextRange(0))
            bodyFieldValue = TextFieldValue(
                text = note.content,
                selection = TextRange(0)
            )
            initialized = true
            lastBodyText = note.content
            lastViewportPx = bodyViewportPx
            // 載入完成的內容是歷史的基準點：此時「撤銷」應該是無效的。
            editHistory.reset(EditSnapshot(note.title, note.content, 0, 0))
            syncHistory()
            // 記下載入時的內容當基準，游標移動不算改動。
            viewModel.onNoteLoaded(noteId, note.title, note.content)
        }
    }

    // 焦點一直留在輸入框上時，requestFocus() 不會重新叫出鍵盤（例如按返回收起鍵盤後再點頁面），
    // 所以要再明確 show 一次；等焦點落到輸入框後再叫，系統才不會忽略。
    fun requestEditing() {
        focusRequester.requestFocus()
        contentScope.launch {
            delay(60)
            keyboardController?.show()
        }
    }

    LaunchedEffect(isPreview) {
        actionsEnabled = false
        if (isPreview) {
            keyboardController?.hide()
        } else if (autoFocusDone) {
            // 只在「預覽 → 編輯」回切時自動聚焦；首次進入不彈鍵盤，
            // 避免系統為了顯示焦點而把整個視窗往上推（頂欄鑽進狀態欄）。
            requestEditing()
        }
        autoFocusDone = true
        delay(400)
        actionsEnabled = true
    }

    // 先播完收合動畫再把彈窗移出組合，避免點按鈕後彈窗瞬間消失。
    // 直接以 sheetState 防重入：動畫途中再調 hide() 會打斷 spring，表現出來就是卡頓。
    fun closeSheet(afterClose: () -> Unit = {}) {
        if (activeSheet == null) return
        if (sheetState.currentValue == SheetValue.Hidden) {
            if (sheetState.targetValue == SheetValue.Hidden) {
                activeSheet = null
                afterClose()
            }
            return
        }
        contentScope.launch {
            sheetState.hide()
            activeSheet = null
            afterClose()
        }
    }

    // 點按鈕時鍵盤還開著，收合鍵盤會讓 ime inset 逐幀變化，編輯區跟著重排；
    // 若和彈窗首幀（建窗、組合內容、初始化錨點）撞在一起，就是「先卡一下才滑出」。
    // 這裡先把彈窗要用的 inset 切掉、讓編輯區先穩定，隔一幀再讓彈窗進場開始動畫。
    fun openSheet(sheet: EditorSheet) {
        if (activeSheet != null) return
        keyboardController?.hide()
        contentScope.launch {
            withFrameNanos { }
            activeSheet = sheet
        }
    }

    BackHandler(enabled = activeSheet != null) { closeSheet() }
    BackHandler(enabled = isPreview) { isPreview = false }

    // 每次改動都送進自動保存管線：防抖寫草稿，停手後寫回 .md，離開前再強制寫一次。
    fun noteEdited(title: String = titleValue.text, body: String = bodyFieldValue.text) {
        if (!initialized) return
        viewModel.onNoteEdited(noteId, title, body)
    }

    /**
     * 撤銷／重做：把快照寫回輸入框，游標也回到當時的位置。
     * 這裡刻意不呼叫 [recordHistory]，否則每次撤銷都會被記成新的一步，
     * 按兩下就會在兩個狀態之間來回跳。
     */
    fun applyHistory(snapshot: EditSnapshot) {
        val start = snapshot.selectionStart.coerceIn(0, snapshot.body.length)
        val end = snapshot.selectionEnd.coerceIn(start, snapshot.body.length)
        // 標題沒變就別碰它：撤銷正文時，正在改的標題不該被搶走游標。
        if (titleValue.text != snapshot.title) {
            titleValue = TextFieldValue(snapshot.title, TextRange(snapshot.title.length))
        }
        bodyFieldValue = TextFieldValue(snapshot.body, TextRange(start, end))
        // 撤銷的結果也要落盤，否則退出後檔案還是撤銷前的內容。
        noteEdited(snapshot.title, snapshot.body)
        syncHistory()
    }

    /** 依格式產生內容，再決定是丟給系統分享，還是交給「另存新檔」。 */
    fun startExport(mode: ExportMode, format: NoteExportFormat) {
        val title = titleValue.text.trim().ifBlank { strings.untitled }
        val attached = if (appSettings.cardEnabled) card else null
        contentScope.launch {
            val result = try {
                exportManager.build(
                    title = title,
                    body = bodyFieldValue.text,
                    format = format,
                    card = attached,
                    guide = writingGuide,
                    // 分享出去的圖／PDF 要跟螢幕上同一個方向：直排筆記就直排分享。
                    vertical = verticalLayout
                )
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(
                    strings.msgExportFailed(e.message ?: strings.msgUnknownError)
                )
                return@launch
            }
            if (result.truncated) {
                snackbarHostState.showSnackbar(strings.msgImageTruncated)
            }
            if (mode == ExportMode.SHARE) {
                try {
                    exportManager.share(title, result, format, strings.share)
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(
                        strings.msgShareFailed(e.message ?: strings.msgNoShareTarget)
                    )
                }
            } else {
                pendingCopy = PendingCopy(result.bytes, format.mimeType)
                saveCopyLauncher.launch(
                    DocumentRequest(exportManager.fileName(title, format), format.mimeType)
                )
            }
        }
    }

    // 切到背景、鎖屏、被系統回收前先把內容落盤；頁面銷毀（返回上一頁）也補一次。
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, noteId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> viewModel.flushNote(noteId)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!isExiting) viewModel.flushNote(noteId)
        }
    }

    fun handleExit() {
        if (isExiting) return
        isExiting = true
        viewModel.flushNote(noteId)
        onNavigateBack()
    }

    fun handleDelete() {
        if (isExiting) return
        isExiting = true
        viewModel.deleteNote(noteId)
        onNavigateBack()
    }

    fun updateBody(newText: String, selection: TextRange) {
        bodyFieldValue = TextFieldValue(text = newText, selection = selection)
        noteEdited(body = newText)
        recordHistory()
    }

    fun insertAtCursor(insert: String, selectFrom: Int? = null, selectTo: Int? = null) {
        val text = bodyFieldValue.text
        val start = bodyFieldValue.selection.start.coerceIn(0, text.length)
        val end = bodyFieldValue.selection.end.coerceIn(0, text.length)
        val newText = text.substring(0, start) + insert + text.substring(end)
        val range = if (selectFrom != null && selectTo != null) {
            TextRange(
                (start + selectFrom).coerceIn(0, newText.length),
                (start + selectTo).coerceIn(0, newText.length)
            )
        } else {
            TextRange((start + insert.length).coerceIn(0, newText.length))
        }
        updateBody(newText, range)
    }

    fun insertMathSymbol(snippet: String) {
        val insert = "\u0024$snippet\u0024"
        insertAtCursor(insert, 1, 1 + snippet.length)
    }

    /** 把圖片 Markdown 片段放成獨立一行插入游標處。 */
    fun insertImage(path: String, alt: String) {
        val text = bodyFieldValue.text
        val start = bodyFieldValue.selection.start.coerceIn(0, text.length)
        val end = bodyFieldValue.selection.end.coerceIn(0, text.length)
        // 圖片要自成一行：前面不是換行就先補一個。
        val leading = if (start == 0 || text[start - 1] == '\n') "" else "\n"
        val snippet = "$leading![$alt]($path)\n"
        val newText = text.substring(0, start) + snippet + text.substring(end)
        updateBody(newText, TextRange((start + snippet.length).coerceIn(0, newText.length)))
    }

    // 「關聯原始 PDF」：只挑 PDF，選完由 ViewModel 取得常駐權限再寫回筆記。
    val pdfLinkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.linkSourceDocument(noteId, uri) }

    fun openPdfPicker() {
        pdfLinkLauncher.launch(arrayOf("application/pdf"))
    }

    LaunchedEffect(noteId) {
        viewModel.sourceLinkMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // 選完圖片、複製進筆記資料夾後才插入；失敗一律用 Snackbar 說清楚原因。
    LaunchedEffect(noteId) {
        viewModel.imageInsert.collect { result ->
            when (result) {
                is ImageInsertResult.Success -> insertImage(result.markdownPath, result.alt)
                is ImageInsertResult.Failure -> launch {
                    snackbarHostState.showSnackbar(
                        strings.imageInsertError(result.error, result.detail)
                    )
                }
            }
        }
    }

    fun applyFormat(action: FormatAction) {
        val text = bodyFieldValue.text
        val start = bodyFieldValue.selection.start.coerceIn(0, text.length)
        val end = bodyFieldValue.selection.end.coerceIn(0, text.length)

        val prefix = action.prefix
        if (prefix != null) {
            val lineStart = text.substring(0, start).lastIndexOf('\n').let {
                if (it == -1) 0 else it + 1
            }
            val line = text.substring(lineStart)
            if (line.startsWith(prefix)) {
                val newText = text.removeRange(lineStart, lineStart + prefix.length)
                val cursor = (start - prefix.length).coerceAtLeast(lineStart)
                updateBody(newText, TextRange(cursor.coerceIn(0, newText.length)))
            } else {
                val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
                updateBody(newText, TextRange((start + prefix.length).coerceIn(0, newText.length)))
            }
            return
        }

        val wrap = action.wrap
        if (wrap != null) {
            val selected = if (end > start) text.substring(start, end) else action.placeholder(strings)
            val insert = wrap.first + selected + wrap.second
            val newText = text.substring(0, start) + insert + text.substring(end)
            val selectionStart = (start + wrap.first.length).coerceIn(0, newText.length)
            val selectionEnd = (selectionStart + selected.length).coerceIn(0, newText.length)
            updateBody(newText, TextRange(selectionStart, selectionEnd))
            return
        }

        val snippet = action.snippet
        if (snippet != null) {
            val leading = if (start > 0 && text[start - 1] != '\n') "\n" else ""
            val trailing = if (text.getOrNull(end) != '\n' && end < text.length) "\n" else ""
            insertAtCursor(leading + snippet + trailing)
        }
    }

    if (showPreviewSettings) {
        PreviewSettingsDialog(
            config = previewConfig,
            onChange = { next -> viewModel.updatePreviewConfig { next } },
            onDismiss = { showPreviewSettings = false }
        )
    }

    if (activeSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            when (activeSheet) {
                EditorSheet.INSERT -> {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        Text(
                            text = strings.insert,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(
                                start = Space.s300,
                                end = Space.s300,
                                top = Space.s100,
                                bottom = Space.s150
                            )
                        )
                        InsertAction.entries.forEach { action ->
                            SheetAction(
                                icon = action.icon,
                                label = action.label(strings),
                                onClick = {
                                    // 改正文會觸發長文本重新排版，放到收合動畫結束後再做。
                                    closeSheet {
                                        when {
                                            action == InsertAction.IMAGE -> runCatching {
                                                imagePicker.launch(
                                                    PickVisualMediaRequest(
                                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                                    )
                                                )
                                            }.onFailure { error ->
                                                // 少數精簡系統沒有相片選擇器／文件選擇器。
                                                contentScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        strings.msgImagePickerFailed(error.message ?: strings.msgSystemUnsupported)
                                                    )
                                                }
                                            }

                                            action.snippet.isNotEmpty() ->
                                                insertAtCursor(action.snippet)
                                        }
                                    }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.size(Space.s200))
                    }
                }

                EditorSheet.STYLE -> {
                    // 樣式面板原本全是寫死的中文；新加的兩組選項用字串表，四種語言都跟得上。
                    val strings = LocalAppStrings.current
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        Text(
                            text = strings.style,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(
                                start = Space.s300,
                                end = Space.s300,
                                top = Space.s100,
                                bottom = Space.s150
                            )
                        )
                        Text(
                            text = strings.backgroundColor,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Space.s300)
                        )
                        // 固定色系：選了哪一個，整個編輯頁（文字／頂欄／底欄）就跟著那一套，
                        // 系統切亮暗都不會翻色；「默認」才交還給主題（Monet／深色模式）。
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Space.s300, vertical = Space.s150),
                            horizontalArrangement = Arrangement.spacedBy(Space.s100),
                            verticalArrangement = Arrangement.spacedBy(Space.s100)
                        ) {
                            NoteBackground.entries.forEach { preset ->
                                FilterChip(
                                    selected = preset.id == backgroundPreset.id,
                                    onClick = {
                                        // 先在本地套用，背景色這一幀就變；寫盤交給背景協程。
                                        pendingBackgroundPreset = preset.id
                                        viewModel.updateAppSettings {
                                            it.copy(noteBackgroundPreset = preset.id)
                                        }
                                        // 給這次重繪留兩幀，再播收合動畫，
                                        // 避免整屏重繪和動畫搶同一批幀。
                                        contentScope.launch {
                                            withFrameNanos { }
                                            withFrameNanos { }
                                            closeSheet()
                                        }
                                    },
                                    label = { Text(preset.label) },
                                    leadingIcon = { BackgroundSwatch(preset) }
                                )
                            }
                        }
                        // 背景是圖片時，字色要當場能調：照片偏亮就得換深字，
                        // 這件事等到「設定 → 筆記背景」去改太遠了。
                        if (backgroundBitmap != null) {
                            Text(
                                text = strings.noteBackgroundInk,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    start = Space.s300,
                                    top = Space.s200
                                )
                            )
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = Space.s300,
                                        vertical = Space.s150
                                    ),
                                horizontalArrangement = Arrangement.spacedBy(Space.s100),
                                verticalArrangement = Arrangement.spacedBy(Space.s100)
                            ) {
                                listOf(
                                    NoteBackgroundInk.AUTO to strings.noteBackgroundInkAuto,
                                    NoteBackgroundInk.DARK to strings.noteBackgroundInkDark,
                                    NoteBackgroundInk.LIGHT to strings.noteBackgroundInkLight
                                ).forEach { (option, label) ->
                                    FilterChip(
                                        selected = appSettings.backgroundInk == option,
                                        onClick = {
                                            viewModel.updateAppSettings {
                                                it.copy(noteBackgroundInk = option.id)
                                            }
                                            contentScope.launch {
                                                withFrameNanos { }
                                                withFrameNanos { }
                                                closeSheet()
                                            }
                                        },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                        Text(
                            text = strings.font,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = Space.s300,
                                top = Space.s200
                            )
                        )
                        // 字體清單來自「設定 → 字體」的字型庫；這裡改的是**預設值**，
                        // 選完就寫回設定，下次開任何筆記都還是這個字。
                        val fontContext = LocalContext.current
                        val fontList = remember { FontStore.list(fontContext) }
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Space.s300, vertical = Space.s150),
                            horizontalArrangement = Arrangement.spacedBy(Space.s100),
                            verticalArrangement = Arrangement.spacedBy(Space.s100)
                        ) {
                            FilterChip(
                                selected = appSettings.noteFont == FontStore.SYSTEM ||
                                    appSettings.noteFont.isBlank(),
                                onClick = {
                                    viewModel.updateAppSettings {
                                        it.copy(noteFont = FontStore.SYSTEM)
                                    }
                                },
                                label = { Text(strings.systemFont) }
                            )
                            fontList.forEach { font ->
                                FilterChip(
                                    selected = appSettings.noteFont == font.fileName,
                                    onClick = {
                                        viewModel.updateAppSettings {
                                            it.copy(noteFont = font.fileName)
                                        }
                                    },
                                    label = {
                                        Text(
                                            font.displayName,
                                            fontFamily = noteFontFamily(font.fileName)
                                        )
                                    }
                                )
                            }
                        }
                        SheetAction(
                            icon = Icons.Default.Image,
                            label = strings.backgroundImage,
                            onClick = {
                                closeSheet()
                                onOpenNoteBackground()
                            }
                        )
                        Spacer(modifier = Modifier.size(Space.s200))
                    }
                }

                EditorSheet.FORMAT -> {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = strings.format,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(
                                start = Space.s300,
                                end = Space.s300,
                                top = Space.s100,
                                bottom = Space.s150
                            )
                        )
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Space.s200),
                            horizontalArrangement = Arrangement.spacedBy(Space.s50),
                            verticalArrangement = Arrangement.spacedBy(Space.s50),
                            maxItemsInEachRow = 6
                        ) {
                            FormatAction.entries.forEach { action ->
                                FormatChip(
                                    label = action.label(strings),
                                    onClick = { closeSheet { applyFormat(action) } }
                                )
                            }
                        }
                        Text(
                            text = strings.editorMathSymbols,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = Space.s300,
                                top = Space.s250,
                                bottom = Space.s100
                            )
                        )
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Space.s200),
                            horizontalArrangement = Arrangement.spacedBy(Space.s50),
                            verticalArrangement = Arrangement.spacedBy(Space.s50),
                            maxItemsInEachRow = 6
                        ) {
                            MathSymbols.forEach { (snippet, glyph) ->
                                FormatChip(
                                    label = glyph,
                                    onClick = { closeSheet { insertMathSymbol(snippet) } }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.size(Space.s300))
                    }
                }

                EditorSheet.MORE -> {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        Text(
                            text = strings.lastEdited(lastEdited),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = Space.s300,
                                end = Space.s300,
                                top = Space.s100,
                                bottom = Space.s150
                            )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = Space.s300,
                                    end = Space.s300,
                                    top = Space.s100,
                                    bottom = Space.s150
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.layout,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            ButtonGroup(
                                overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
                                horizontalArrangement = Arrangement.spacedBy(Space.s50)
                            ) {
                                toggleableItem(
                                    checked = !verticalLayout,
                                    label = strings.horizontalLayout,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            viewModel.setNoteVertical(noteId, false)
                                        }
                                    }
                                )
                                toggleableItem(
                                    checked = verticalLayout,
                                    label = strings.verticalLayoutLabel,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            viewModel.setNoteVertical(noteId, true)
                                        }
                                    }
                                )
                            }
                        }
                        // 標題獨佔一行、選項放下面：四個選項跟標題擠同一行時，
                        // 標題會被壓成一個字寬，看起來就是「排版壞了」。
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = Space.s300,
                                    end = Space.s300,
                                    top = Space.s100,
                                    bottom = Space.s150
                                )
                        ) {
                            Text(
                                text = strings.writingGuide,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(Space.s100))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                ButtonGroup(
                                    overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
                                    horizontalArrangement = Arrangement.spacedBy(Space.s50)
                                ) {
                                    // note?.writingGuide 為 null＝跟隨預設，所以「跟隨默認」是第四個選項。
                                    val own = note?.writingGuide
                                    toggleableItem(
                                        checked = own == WritingGuide.NONE.id,
                                        label = strings.guideNone,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                viewModel.setNoteWritingGuide(noteId, WritingGuide.NONE)
                                            }
                                    }
                                )
                                toggleableItem(
                                    checked = own == WritingGuide.RULED.id,
                                    label = strings.guideRuled,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            viewModel.setNoteWritingGuide(noteId, WritingGuide.RULED)
                                        }
                                    }
                                )
                                toggleableItem(
                                    checked = own == WritingGuide.GRID.id,
                                    label = strings.guideGrid,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            viewModel.setNoteWritingGuide(noteId, WritingGuide.GRID)
                                        }
                                    }
                                )
                                toggleableItem(
                                    checked = own == null,
                                    label = strings.guideFollowDefault,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            viewModel.setNoteWritingGuide(noteId, null)
                                        }
                                    }
                                )
                            }
                            }
                        }
                        // 早期匯入的筆記沒有記下來源檔位置（那時還沒存），
                        // 型別也是空的；這裡讓使用者補一次，不必重新匯入產出重複筆記。
                        if (note?.sourceUri.isNullOrEmpty()) {
                            SheetAction(
                                icon = Icons.Default.AddLink,
                                label = strings.editorLinkPdf,
                                onClick = { closeSheet { openPdfPicker() } }
                            )
                        }
                        SheetAction(
                            icon = Icons.Default.Delete,
                            label = strings.delete,
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { closeSheet { handleDelete() } }
                        )
                        SheetAction(
                            icon = Icons.AutoMirrored.Filled.Label,
                            label = strings.label,
                            onClick = { closeSheet() }
                        )
                        SheetAction(
                            icon = Icons.Default.Share,
                            label = strings.share,
                            onClick = {
                                exportMode = ExportMode.SHARE
                                activeSheet = EditorSheet.EXPORT
                            }
                        )
                        SheetAction(
                            icon = Icons.Default.SaveAlt,
                            label = strings.editorSaveCopy,
                            onClick = {
                                exportMode = ExportMode.SAVE_COPY
                                activeSheet = EditorSheet.EXPORT
                            }
                        )
                        SheetAction(
                            icon = Icons.Default.Search,
                            label = strings.find,
                            onClick = { closeSheet() }
                        )
                        Spacer(modifier = Modifier.size(Space.s200))
                    }
                }

                EditorSheet.EXPORT -> {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            // 名片選項展開後會超出半屏高度，整塊要能捲。
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = if (exportMode == ExportMode.SHARE) strings.share else strings.editorSaveCopy,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(
                                start = Space.s300,
                                end = Space.s300,
                                top = Space.s100,
                                bottom = Space.s150
                            )
                        )
                        Text(
                            text = if (exportMode == ExportMode.SHARE) {
                                strings.exportChooseFormat
                            } else {
                                strings.exportHint
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = Space.s300,
                                end = Space.s300,
                                bottom = Space.s150
                            )
                        )
                        // 圖片與 PDF 會照這篇筆記的排版方向輸出；先講清楚，省得匯出後才發現方向不對。
                        Text(
                            text = if (verticalLayout) {
                                strings.exportDirectionVertical
                            } else {
                                strings.exportDirectionHorizontal
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(
                                start = Space.s300,
                                end = Space.s300,
                                bottom = Space.s150
                            )
                        )
                        NoteExportFormat.entries.forEach { format ->
                            SheetAction(
                                icon = format.icon,
                                label = format.label(strings),
                                onClick = { closeSheet { startExport(exportMode, format) } }
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                start = Space.s300,
                                end = Space.s300,
                                top = Space.s100
                            ),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        // ── 個人名片 ──
                        // 一列總開關 + 展開後的細節。展開區塊用 setting 為準（不是暫時狀態），
                        // 調一次之後每次分享都沿用，不必重勾。
                        ExportCardSection(
                            enabled = appSettings.cardEnabled,
                            vertical = verticalLayout,
                            onEnabledChange = { on ->
                                viewModel.updateAppSettings { it.copy(cardEnabled = on) }
                            },
                            profile = profile,
                            card = card,
                            settings = appSettings,
                            onPositionChange = { position ->
                                viewModel.updateAppSettings { it.copy(cardPosition = position.id) }
                            },
                            onShowAvatarChange = { on ->
                                viewModel.updateAppSettings { it.copy(cardShowAvatar = on) }
                            },
                            onShowNicknameChange = { on ->
                                viewModel.updateAppSettings { it.copy(cardShowNickname = on) }
                            },
                            onShowEmailChange = { on ->
                                viewModel.updateAppSettings { it.copy(cardShowEmail = on) }
                            },
                            onShowGithubChange = { on ->
                                viewModel.updateAppSettings { it.copy(cardShowGithub = on) }
                            }
                        )
                        Spacer(modifier = Modifier.size(Space.s200))
                    }
                }

                null -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            val topBarColor = editorColors.topBar
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = {
                    if (isPreview) {
                        Text(strings.preview, style = MaterialTheme.typography.titleLarge)
                    } else {
                        // 標題與保存狀態共用同一個位置：內容存好之後標題淡出、換成「已保存」；
                        // 一打字（或點一下標題）標題就淡回來。標題內容本身完全不變。
                        val titleAlpha by animateFloatAsState(
                            targetValue = if (showSaveHint) 0f else 1f,
                            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                            label = "title-alpha"
                        )
                        val hintAlpha by animateFloatAsState(
                            targetValue = if (showSaveHint) 1f else 0f,
                            animationSpec = tween(
                                durationMillis = 200,
                                delayMillis = 120,
                                easing = LinearOutSlowInEasing
                            ),
                            label = "save-hint-alpha"
                        )
                        Box {
                            BasicTextField(
                                value = titleValue,
                                onValueChange = { next ->
                                    titleValue = next
                                    noteEdited(title = next.text)
                                    recordHistory()
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.titleLarge.copy(
                                    color = editorColors.onBackground
                                ),
                                cursorBrush = SolidColor(editorColors.accent),
                                onTextLayout = { titleOverflow = it.didOverflowWidth },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(titleAlpha)
                                    .onFocusChanged { titleFocused = it.isFocused }
                                    .fadeEdges(
                                        start = fadeStart,
                                        end = fadeEnd,
                                        tailColor = topBarColor
                                    ),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (titleValue.text.isEmpty()) {
                                            Text(
                                                text = strings.titlePlaceholder,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = editorColors.onBackgroundVariant
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            if (hintAlpha > 0.01f) {
                                Text(
                                    text = saveHintText,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = saveHintColor,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .fillMaxWidth()
                                        .alpha(hintAlpha)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { if (isPreview) isPreview = false else handleExit() },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = editorColors.onBackground
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    // 從 PDF 匯入的筆記：頂欄多一顆入口。
                    // 已關聯原始檔→打開原始版面；還沒關聯→當場指定一次（舊筆記就是這種）。
                    // 轉出來的文字沒有版面（表格、圖片、公式都在轉換中損失），
                    // 需要對照原文時不必離開 App。
                    val pdfUri = note?.sourceUri.orEmpty()
                    if (note?.sourceFormat == ImportFormat.PDF.id) {
                        val linked = pdfUri.isNotBlank()
                        FilledTonalIconButton(
                            onClick = {
                                if (linked) {
                                    onOpenSourcePdf(pdfUri)
                                } else {
                                    openPdfPicker()
                                }
                            },
                            enabled = actionsEnabled,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = editorColors.buttonContainer,
                                contentColor = editorColors.buttonContent
                            )
                        ) {
                            Icon(
                                imageVector = if (linked) Icons.Default.PictureAsPdf
                                else Icons.Default.AddLink,
                                contentDescription = if (linked) strings.editorOpenPdf else strings.editorLinkPdf
                            )
                        }
                    }
                    FilledTonalIconButton(
                        onClick = { isPreview = !isPreview },
                        enabled = actionsEnabled,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = editorColors.buttonContainer,
                            contentColor = editorColors.buttonContent
                        )
                    ) {
                        Icon(
                            imageVector = if (isPreview) Icons.Default.Edit
                            else Icons.Default.Visibility,
                            contentDescription = if (isPreview) strings.backToEdit else strings.preview
                        )
                    }
                    if (isPreview) {
                        FilledTonalIconButton(
                            onClick = { showPreviewSettings = true },
                            enabled = actionsEnabled,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = editorColors.buttonContainer,
                                contentColor = editorColors.buttonContent
                            )
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = strings.previewSettings)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarColor,
                    scrolledContainerColor = topBarColor,
                    titleContentColor = editorColors.onBackground,
                    actionIconContentColor = editorColors.onBackground,
                    navigationIconContentColor = editorColors.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Crossfade(
            targetState = isPreview,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            animationSpec = modeChangeSpec,
            label = "edit-preview"
        ) { preview ->
            if (preview) {
                MarkdownPreview(
                    markdown = source,
                    config = previewConfig,
                    palette = palette,
                    vertical = verticalLayout,
                    noteUri = note?.uriString,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 手勢期間不讓 Compose 自己把游標帶進畫面：長按選字時焦點先到、選取後到，
                // 系統會拿「舊游標」去捲動，那就是跳到文件開頭的原因。
                CompositionLocalProvider(LocalBringIntoViewSpec provides bringIntoViewSpec) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(editorBackground)
                            .windowInsetsPadding(editorImeInsets)
                            .onSizeChanged { if (bodyViewportPx != it.height) bodyViewportPx = it.height }
                            .pointerInput(Unit) {
                                try {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            bringIntoViewSpec.gestureInProgress =
                                                event.changes.any { it.pressed }
                                        }
                                    }
                                } finally {
                                    // 手勢被系統打斷（切到別的視窗、節點被移除）時別把旗標留在 true。
                                    bringIntoViewSpec.gestureInProgress = false
                                }
                            }
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { requestEditing() }
                    ) {
                        if (backgroundBitmap != null) {
                            Image(
                                bitmap = backgroundBitmap,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        val bodyTextStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = editorColors.onBackground,
                            fontFamily = bodyFontFamily
                        )
                        // 輔助線要貼著字畫，所以拿「實際排版的基線」當基準：
                        // 行框（lineHeight）比字高出一截，用行框底畫線，字就會浮在格子偏上。
                        var guideBaselinePx by remember { mutableStateOf(0f) }
                        var guideAdvancePx by remember { mutableStateOf(0f) }
                        // 格子的邊長不能用「字號」：M3 的 bodyLarge 帶 letterSpacing = 0.5sp，
                        // 每個字的實際步進是「字號 + 字距」，用字號鋪線一排下來就會愈來愈偏。
                        // 量「国国」兩個字，取第二個字的起點，就是一步的寬度。
                        val guideMeasurer = rememberTextMeasurer()
                        // 格子寬＝一個漢字的**真實步進**。先用樣字量一個初值，
                        // 之後每次排版都從實際的 TextLayoutResult 校正（見 onTextLayout）——
                        // 字型若換了、或量測與渲染有零點幾 px 的差，都會被逐字累積成「越寫越歪」。
                        val probeCellPx = run {
                            val probe = guideMeasurer.measure(
                                text = "国国",
                                style = bodyTextStyle,
                                softWrap = false,
                                maxLines = 1
                            )
                            probe.getHorizontalPosition(1, true)
                                .takeIf { it > 0f }
                                ?: with(density) { bodyTextStyle.fontSize.toPx() }
                        }
                        // 量到的「格寬」只是初值：真正的字寬要等實際排版才看得到
                        // （字型替換、字距換算都可能差 1%）。差的那一點會逐字累積，
                        // 一整行二十字就是四分之一格，看起來就是「越寫越歪」。
                        var guideCellPx by remember { mutableStateOf(probeCellPx) }
                        LaunchedEffect(probeCellPx) { guideCellPx = probeCellPx }
                        // 格線不畫「等距線」，而是從實際排版算出來（見 buildGridLines）：
                        // 等距線的週期是估的，估錯 1% 就會越寫越歪；取每個字的真實位置則不會。
                        var guideLines by remember { mutableStateOf(GuideLines()) }
                        // 方格紙的「行」就是格子的高：行高必須等於格高。
                        // 行距若比格高大，每一行都會落在格子裡不同的相位，
                        // 那就必然出現「字不在格子裡」——這不是偏移，是節拍不同。
                        val gridStyle = if (
                            writingGuide == WritingGuide.GRID && guideCellPx > 0f
                        ) {
                            bodyTextStyle.copy(
                                lineHeight = with(density) { guideCellPx.toSp() },
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.None
                                )
                            )
                        } else {
                            bodyTextStyle
                        }
                        if (verticalLayout) {
                            VerticalBodyEditor(
                                value = bodyFieldValue,
                                onValueChange = { next ->
                                    bodyFieldValue = next
                                    noteEdited(body = next.text)
                                    recordHistory()
                                },
                                textStyle = bodyTextStyle,
                                focusRequester = focusRequester,
                                bottomReserve = editorBottomReserve,
                                showLineNumbers = appSettings.showLineNumbers,
                                writingGuide = writingGuide,
                                placeholder = strings.bodyPlaceholder,
                                accentColor = editorColors.accent,
                                variantColor = editorColors.onBackgroundVariant,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(bodyScroll)
                                    .padding(
                                        start = Space.s200,
                                        end = Space.s200,
                                        top = Space.s100
                                    )
                            ) {
                                // 作文格的橫排改由我們自己排：一格一字、自己決定換行與位置。
                                // 交給文字引擎就永遠差一點——它按比例排，格子是等寬的，
                                // 兩套座標系放一起，誤差必定逐字累積。
                                val gridActive = writingGuide == WritingGuide.GRID
                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    // 三個尺寸分開：字身（1em）、列距（比字身寬）、行距（比格高）。
                                    val fontPx = with(density) { bodyTextStyle.fontSize.toPx() }
                                    val cellPx = fontPx * CELL_PITCH_EM
                                    val columns = max(1, floor(constraints.maxWidth / cellPx).toInt())
                                    val cellLayout = if (gridActive) {
                                        remember(bodyFieldValue.text, columns, cellPx, gridStyle) {
                                            buildHCellLayout(
                                                text = bodyFieldValue.text,
                                                columns = columns,
                                                cellPx = cellPx
                                            ) { piece ->
                                                guideMeasurer.measure(
                                                    text = AnnotatedString(piece),
                                                    style = gridStyle,
                                                    softWrap = false,
                                                    maxLines = 1
                                                ).size.width.toFloat()
                                            }
                                        }
                                    } else {
                                        null
                                    }
                                    // 行距比格高多一些，字下方才有空隙。
                                    val rowPitchPx = fontPx * ROW_PITCH_EM
                                    // 內層在 verticalScroll 裡，maxHeight 是無限大，拿不到可視高度，
                                    // 改用外層 Box 實測的高度，再扣掉上下內距與底部工具列的預留。
                                    val viewportPx = (bodyViewportPx -
                                        with(density) {
                                            (Space.s100 + editorBottomReserve).toPx()
                                        }).coerceAtLeast(1f)
                                    // 格子紙要鋪滿整個可視區，而不是只鋪到最後一行字：
                                    // 只鋪內容高度的話，短內容下面三分之二會是一片空白，
                                    // 看起來就像格子畫到一半斷掉。
                                    val editorHeightPx = max(
                                        (cellLayout?.rows ?: 1) * rowPitchPx,
                                        viewportPx
                                    )
                                    val editorHeight = with(density) { editorHeightPx.toDp() }

                                    // 作文格時，選取底色與輸入法的組字底線都由我們自己畫；
                                    // 欄位本身要保持完全透明，否則會在字的下方多印一層。
                                    CompositionLocalProvider(
                                        LocalTextSelectionColors provides TextSelectionColors(
                                            handleColor = Color.Transparent,
                                            backgroundColor = Color.Transparent
                                        )
                                    ) {
                                    BasicTextField(
                                        value = bodyFieldValue,
                                        onValueChange = { next ->
                                            bodyFieldValue = next
                                            noteEdited(body = next.text)
                                            recordHistory()
                                        },
                                        // 作文格時文字由下面的 Canvas 自己畫，這裡只負責輸入，
                                        // 所以字與游標都設成透明，避免出現兩份。
                                        textStyle = if (gridActive) {
                                            gridStyle.copy(color = Color.Transparent)
                                        } else {
                                            gridStyle
                                        },
                                        visualTransformation = VisualTransformation.None,
                                        cursorBrush = if (gridActive) {
                                            SolidColor(Color.Transparent)
                                        } else {
                                            SolidColor(editorColors.accent)
                                        },
                                        // 排版階段只記錄游標位置，不在這裡改滾動狀態：
                                        // 邊排版邊捲動，下一幀會拿過期的位置再算一次，越算越偏。
                                        onTextLayout = { layout ->
                                            val offset = bodyFieldValue.selection.start
                                                .coerceIn(0, layout.layoutInput.text.length)
                                            val rect = layout.getCursorRect(offset)
                                            if (rect != caretRect) caretRect = rect
                                            val baseline = layout.getLineBaseline(0)
                                            val advance = if (layout.lineCount > 1) {
                                                layout.getLineBaseline(1) - baseline
                                            } else {
                                                layout.size.height.toFloat()
                                            }
                                            if (baseline != guideBaselinePx) guideBaselinePx = baseline
                                            if (advance != guideAdvancePx) guideAdvancePx = advance
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            // 固定高度會把溢出可視區的段落直接裁掉，外層又因為
                                            // 「內容就只有這麼高」而滑不動——結果就是只能看到上面一小段正文。
                                            // 作文格是自己排版、行數已知，才用固定高度；其餘一律讓它跟著內容長。
                                            .then(
                                                if (gridActive) {
                                                    Modifier.height(editorHeight)
                                                } else {
                                                    Modifier.heightIn(min = editorHeight)
                                                }
                                            )
                                            .focusRequester(focusRequester)
                                            .onGloballyPositioned { coords ->
                                                // 相對滾動內容的起點，等於欄位上方那段內距。
                                                val top = coords.positionInParent().y
                                                if (top != bodyTextTopPx) bodyTextTopPx = top
                                            },
                                        decorationBox = { innerTextField ->
                                            Box {
                                                if (!gridActive) {
                                                    // 分隔線仍然貼著字畫（實測基線 + 行距）。
                                                    WritingGuideBackground(
                                                        guide = writingGuide,
                                                        baselinePx = guideBaselinePx,
                                                        lineAdvancePx = guideAdvancePx,
                                                        cellPx = guideCellPx,
                                                        color = editorColors.onBackgroundVariant,
                                                        modifier = Modifier.matchParentSize()
                                                    )
                                                    if (bodyFieldValue.text.isEmpty()) {
                                                        Text(
                                                            text = strings.bodyPlaceholder,
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            color = editorColors.onBackgroundVariant
                                                        )
                                                    }
                                                }
                                                innerTextField()
                                            }
                                        }
                                    )

                                    }
                                    if (gridActive && cellLayout != null) {
                                        HorizontalCellText(
                                            layout = cellLayout,
                                            cellPx = cellPx,
                                            fontPx = fontPx,
                                            style = gridStyle,
                                            textColor = editorColors.onBackground,
                                            caretColor = editorColors.accent,
                                            selectionColor = editorColors.accent.copy(alpha = 0.22f),
                                            guideColor = editorColors.onBackgroundVariant
                                                .copy(alpha = 0.16f),
                                            measurer = guideMeasurer,
                                            selection = bodyFieldValue.selection,
                                            caretVisible = true,
                                            rowPitchPx = rowPitchPx,
                                            scrollState = bodyScroll,
                                            viewportHeightPx = viewportPx,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(editorHeight),
                                            onTap = { offset ->
                                                bodyFieldValue = bodyFieldValue.copy(
                                                    selection = TextRange(offset),
                                                    composition = null
                                                )
                                                requestEditing()
                                            }
                                        )
                                        if (bodyFieldValue.text.isEmpty()) {
                                            Text(
                                                text = strings.bodyPlaceholder,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = editorColors.onBackgroundVariant
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(editorBottomReserve))
                            }
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(editorColors.bottomBar)
                                .padding(horizontal = Space.s200, vertical = Space.s150),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(Space.s100)) {
                                EditorToolButton(
                                    icon = Icons.Default.Add,
                                    description = strings.insert,
                                    containerColor = editorColors.buttonContainer,
                                    contentColor = editorColors.buttonContent,
                                    onClick = { openSheet(EditorSheet.INSERT) }
                                )
                                EditorToolButton(
                                    icon = Icons.Default.Palette,
                                    description = strings.style,
                                    containerColor = editorColors.buttonContainer,
                                    contentColor = editorColors.buttonContent,
                                    onClick = { openSheet(EditorSheet.STYLE) }
                                )
                                EditorToolButton(
                                    icon = Icons.Default.FormatSize,
                                    description = strings.format,
                                    containerColor = editorColors.buttonContainer,
                                    contentColor = editorColors.buttonContent,
                                    onClick = { openSheet(EditorSheet.FORMAT) }
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Space.s100),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 初始不顯示；開始編輯後才出現在「更多」左邊。
                                // 右組本身貼右對齊，新按鈕只會往左長，不會推動左側工具組。
                                AnimatedVisibility(
                                    visible = canUndo || canRedo,
                                    enter = fadeIn(tween(120)) + expandHorizontally(
                                        animationSpec = tween(180),
                                        expandFrom = Alignment.End
                                    ),
                                    exit = fadeOut(tween(120)) + shrinkHorizontally(
                                        animationSpec = tween(180),
                                        shrinkTowards = Alignment.End
                                    )
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s100)) {
                                        EditorToolButton(
                                            icon = Icons.AutoMirrored.Filled.Undo,
                                            description = strings.editorUndo,
                                            enabled = canUndo,
                                            containerColor = editorColors.buttonContainer,
                                            contentColor = editorColors.buttonContent,
                                            onClick = {
                                                editHistory.undo()?.let { applyHistory(it) }
                                            }
                                        )
                                        EditorToolButton(
                                            icon = Icons.AutoMirrored.Filled.Redo,
                                            description = strings.editorRedo,
                                            enabled = canRedo,
                                            containerColor = editorColors.buttonContainer,
                                            contentColor = editorColors.buttonContent,
                                            onClick = {
                                                editHistory.redo()?.let { applyHistory(it) }
                                            }
                                        )
                                    }
                                }
                                EditorToolButton(
                                    icon = Icons.Default.MoreVert,
                                    description = strings.more,
                                    containerColor = editorColors.buttonContainer,
                                    contentColor = editorColors.buttonContent,
                                    onClick = { openSheet(EditorSheet.MORE) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EditorToolButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(Space.s500)
            // indication 沒被裁到形狀時，按住會露出方形遮罩。
            .clip(CircleShape),
        shape = CircleShape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            // 不可用時只降透明度，不換色系——固定背景色系下才不會突然冒出主題色。
            disabledContainerColor = containerColor.copy(alpha = 0.38f),
            disabledContentColor = contentColor.copy(alpha = 0.38f)
        )
    ) {
        Icon(
            icon,
            contentDescription = description,
            modifier = Modifier.size(Space.s250)
        )
    }
}

/** 背景色預覽圓點；「默認」用當前主題 surface，讓「跟隨主題」這件事看得出來。 */
@Composable
private fun BackgroundSwatch(preset: NoteBackground) {
    val color = preset.surface?.let { Color(it) } ?: MaterialTheme.colorScheme.surface
    Box(
        modifier = Modifier
            .size(Space.s200)
            .background(color, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
    )
}

/**
 * 編輯頁的一整套顏色。
 *
 * 固定色系（非「默認」）不會隨系統亮暗翻轉：文字、頂欄、底欄、按鈕、強調色
 * 都由預設自己帶，對比度在定義時就固定下來，避免出現「淺底配白字」這種反色錯誤。
 */
private data class EditorColors(
    val background: Color,
    val onBackground: Color,
    val onBackgroundVariant: Color,
    val topBar: Color,
    val bottomBar: Color,
    val accent: Color,
    val error: Color,
    val buttonContainer: Color,
    val buttonContent: Color,
    val isDark: Boolean,
    val isFixed: Boolean
)

@Composable
private fun rememberEditorColors(
    preset: NoteBackground,
    hasImage: Boolean = false,
    ink: NoteBackgroundInk = NoteBackgroundInk.AUTO
): EditorColors {
    val scheme = MaterialTheme.colorScheme
    return remember(preset, scheme, hasImage, ink) {
        val background = preset.surface?.let { Color(it) } ?: scheme.surface
        // 圖片背景時 preset 的文字色沒有意義（圖片自己決定底色），改用使用者指定的深淺。
        val forcedInk = if (hasImage && ink != NoteBackgroundInk.AUTO) ink else null
        val onBackground = when (forcedInk) {
            NoteBackgroundInk.DARK -> INK_DARK
            NoteBackgroundInk.LIGHT -> INK_LIGHT
            else -> preset.onSurface?.let { Color(it) } ?: scheme.onSurface
        }
        val onVariant = when (forcedInk) {
            // 次要文字是同一個墨色調淡，不要借用主題的 onSurfaceVariant：
            // 深色主題的淺灰放在照片上會直接消失。
            NoteBackgroundInk.DARK -> INK_DARK.copy(alpha = 0.66f)
            NoteBackgroundInk.LIGHT -> INK_LIGHT.copy(alpha = 0.72f)
            else -> preset.onSurfaceVariant?.let { Color(it) } ?: scheme.onSurfaceVariant
        }
        val fixedLook = preset.isFixed || forcedInk != null
        // 「底色是深是淺」在有圖片時取墨色本身：圖片亮度是隨機的，
        // 但使用者選了深字，就代表他認為底是淺的（狀態列圖標要跟著變黑）。
        val dark = when (forcedInk) {
            NoteBackgroundInk.DARK -> false
            NoteBackgroundInk.LIGHT -> true
            else -> background.luminance() < 0.5f
        }
        EditorColors(
            background = background,
            onBackground = onBackground,
            onBackgroundVariant = onVariant,
            topBar = preset.topBar?.let { Color(it) } ?: scheme.surfaceContainer,
            bottomBar = preset.bottomBar?.let { Color(it) } ?: Color.Transparent,
            accent = preset.accent?.let { Color(it) } ?: scheme.primary,
            // error 也要跟固定色系配對：深底用淺紅、淺底用深紅，
            // 不能借用相反主題的 error（深色主題的淺紅放在米紙上幾乎看不見）。
            error = when {
                !fixedLook -> scheme.error
                dark -> Color(0xFFFFB4AB)
                else -> Color(0xFFB3261E)
            },
            buttonContainer = if (fixedLook) {
                onBackground.copy(alpha = 0.08f)
            } else {
                scheme.surfaceContainerHigh
            },
            buttonContent = onVariant,
            isDark = dark,
            isFixed = fixedLook
        )
    }
}

/** 背景圖片上的深墨：不用純黑，照片上純黑會死板；比 M3 的 onSurface 再淡一點點。 */
private val INK_DARK = Color(0xFF1C1B1F)

/** 背景圖片上的淺墨：不用純白，純白在亮部會糊成一團。 */
private val INK_LIGHT = Color(0xFFF7F5F2)

@Composable
private fun FormatChip(label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(Space.s600)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
            // 波紋自繪並裁成圓形，按住時才不會出現方形遮罩。
            .pressRipple(interactionSource = interactionSource, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 「另存新檔」的契約：檔名與 MIME 由呼叫端帶進來。
 * 內建的 CreateDocument 只有在建構時能指定 MIME，沒辦法一個 launcher 對應五種格式。
 */
private class CreateDocumentContract : ActivityResultContract<DocumentRequest, Uri?>() {
    override fun createIntent(context: Context, input: DocumentRequest): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = input.mimeType
            putExtra(Intent.EXTRA_TITLE, input.fileName)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

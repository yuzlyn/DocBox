package com.yuzlyn.mdeditor.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yuzlyn.mdeditor.data.backgrounds.BackgroundStore
import com.yuzlyn.mdeditor.data.settings.AppSettings
import com.yuzlyn.mdeditor.data.settings.NoteBackground
import com.yuzlyn.mdeditor.data.settings.NoteBackgroundInk
import com.yuzlyn.mdeditor.ui.i18n.LocalAppStrings
import com.yuzlyn.mdeditor.ui.i18n.label
import com.yuzlyn.mdeditor.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 筆記背景：預設（莫奈）／純色／圖片，三者互斥。
 *
 * 「默認」交還給主題（Monet 取色、深色模式會跟著翻），純色與圖片則是使用者
 * 明確指定的結果，不隨系統亮暗改變。選了圖片就自動把純色清掉，
 * 不會出現「圖片底下還藏著一個米紙色」這種曖昧狀態。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteBackgroundSettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var images by remember { mutableStateOf(BackgroundStore.list(context)) }
    var pendingDelete by remember { mutableStateOf<File?>(null) }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val path = withContext(Dispatchers.IO) { BackgroundStore.import(context, uri) }
            images = BackgroundStore.list(context)
            if (path != null) {
                onChange(
                    settings.copy(
                        noteBackground = path,
                        noteBackgroundPreset = NoteBackground.DEFAULT.id
                    )
                )
            }
        }
    }

    SettingsDetailScaffold(title = strings.noteBackgroundPage, onBack = onBack) {
        DetailSection(strings.cardShapeDefault) {
            val defaultSelected = settings.noteBackground.isBlank() &&
                settings.background == NoteBackground.DEFAULT
            SettingsRadioRow(
                selected = defaultSelected,
                onClick = {
                    if (!defaultSelected) {
                        onChange(
                            settings.copy(
                                noteBackground = "",
                                noteBackgroundPreset = NoteBackground.DEFAULT.id
                            )
                        )
                    }
                }
            ) {
                RadioLabel(strings.noteBackgroundDefault, defaultSelected)
            }
        }

        DetailSection(strings.noteBackgroundColor) {
            NoteBackground.entries
                .filter { it != NoteBackground.DEFAULT }
                .forEach { preset ->
                    val selected = settings.noteBackground.isBlank() &&
                        settings.background == preset
                    SettingsRadioRow(
                        selected = selected,
                        onClick = {
                            onChange(
                                settings.copy(
                                    noteBackground = "",
                                    noteBackgroundPreset = preset.id
                                )
                            )
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Space.s150)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(Space.s300)
                                    .clip(RoundedCornerShape(Space.s100))
                                    .background(Color(preset.surface ?: 0xFF000000))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(Space.s100)
                                    )
                            )
                            RadioLabel(preset.label(strings), selected)
                        }
                    }
                }
        }

        DetailSection(strings.noteBackground) {
            if (images.isEmpty()) {
                Text(
                    text = strings.noteBackgroundEmpty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Space.s200, vertical = Space.s150)
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.s150),
                verticalArrangement = Arrangement.spacedBy(Space.s150)
            ) {
                images.forEach { file ->
                    ImageThumb(
                        file = file,
                        selected = settings.noteBackground == file.absolutePath,
                        onClick = {
                            onChange(
                                settings.copy(
                                    noteBackground = file.absolutePath,
                                    noteBackgroundPreset = NoteBackground.DEFAULT.id
                                )
                            )
                        },
                        onDelete = { pendingDelete = file }
                    )
                }
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(Space.s200))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(Space.s200)
                        )
                        .clickable {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = strings.noteBackgroundImport,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = strings.noteBackgroundImport,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.s100)
            )
        }

        // 文字深淺：只有選了圖片才需要——純色與默認都有配好的對比度。
        // 圖片自己決定底色，只有使用者知道那張照片偏亮還是偏暗。
        if (settings.noteBackground.isNotBlank()) {
            DetailSection(strings.noteBackgroundInk) {
                val ink = settings.backgroundInk
                listOf(
                    NoteBackgroundInk.AUTO to strings.noteBackgroundInkAuto,
                    NoteBackgroundInk.DARK to strings.noteBackgroundInkDark,
                    NoteBackgroundInk.LIGHT to strings.noteBackgroundInkLight
                ).forEach { (option, label) ->
                    SettingsRadioRow(
                        selected = ink == option,
                        onClick = { onChange(settings.copy(noteBackgroundInk = option.id)) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Space.s150)
                        ) {
                            // 用「紙色 + 墨色」的小樣張表示結果，比三個文字標籤更快看懂差在哪。
                            InkSwatch(option)
                            RadioLabel(label, ink == option)
                        }
                    }
                }
                Text(
                    text = strings.noteBackgroundInkHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Space.s200, vertical = Space.s100)
                )
            }
        }
    }

    pendingDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(strings.noteBackgroundDeleteTitle) },
            text = { Text(strings.noteBackgroundDeleteMessage(file.name)) },
            confirmButton = {
                TextButton(onClick = {
                    BackgroundStore.delete(file.absolutePath)
                    onChange(
                        if (settings.noteBackground == file.absolutePath) {
                            settings.copy(
                                noteBackground = "",
                                noteBackgroundPreset = NoteBackground.DEFAULT.id
                            )
                        } else {
                            settings
                        }
                    )
                    images = BackgroundStore.list(context)
                    pendingDelete = null
                }) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun InkSwatch(option: NoteBackgroundInk) {
    // 跟隨主題畫成棋盤格（＝交給主題），深／淺各畫一個對應的小方塊。
    val box = when (option) {
        NoteBackgroundInk.AUTO -> MaterialTheme.colorScheme.surfaceContainerHigh
        NoteBackgroundInk.DARK -> Color(0xFFF3EFE8)
        NoteBackgroundInk.LIGHT -> Color(0xFF232527)
    }
    val ink = when (option) {
        NoteBackgroundInk.AUTO -> MaterialTheme.colorScheme.onSurfaceVariant
        NoteBackgroundInk.DARK -> Color(0xFF1C1B1F)
        NoteBackgroundInk.LIGHT -> Color(0xFFF7F5F2)
    }
    Box(
        modifier = Modifier
            .size(Space.s300)
            .clip(RoundedCornerShape(Space.s100))
            .background(box)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(Space.s100)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "字",
            style = MaterialTheme.typography.labelSmall,
            color = ink
        )
    }
}

@Composable
private fun ImageThumb(
    file: File,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val bitmap = rememberAvatarBitmap(file.absolutePath)
    val shape = RoundedCornerShape(Space.s200)
    Box(modifier = Modifier.size(96.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = shape
                )
                .clickable(onClick = onClick)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(Space.s75)
                        .size(Space.s300)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(Space.s200)
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Space.s50)
                .size(Space.s300)
                // 維持 24dp 圓形視覺，但實際觸控區至少 48dp。
                .minimumInteractiveComponentSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = LocalAppStrings.current.delete,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(Space.s200)
            )
        }
    }
}

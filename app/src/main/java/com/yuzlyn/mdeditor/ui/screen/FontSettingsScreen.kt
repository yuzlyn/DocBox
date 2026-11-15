package com.yuzlyn.mdeditor.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yuzlyn.mdeditor.data.fonts.FontStore
import com.yuzlyn.mdeditor.data.settings.AppSettings
import com.yuzlyn.mdeditor.ui.i18n.LocalAppStrings
import com.yuzlyn.mdeditor.ui.theme.Space
import com.yuzlyn.mdeditor.ui.theme.noteFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 字型設定頁：匯入、刪除、分別指定「編輯」與「閱讀」的預設字型。
 *
 * 字型庫就是 `filesDir/fonts` 的檔案清單（見 [FontStore]），沒有第二份索引，
 * 所以刪檔就是刪字型，不會出現「列表還在、檔案沒了」的鬼狀態。
 *
 * 每一列的預覽直接用該字型渲染自己的名字 —— 選字型這件事，看比讀有用。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var fonts by remember { mutableStateOf(FontStore.list(context)) }
    var failed by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<FontStore.FontFile?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val imported = withContext(Dispatchers.IO) { FontStore.import(context, uri) }
            fonts = FontStore.list(context)
            failed = imported == null
        }
    }

    SettingsDetailScaffold(title = strings.font, onBack = onBack) {
        DetailSection(strings.fontLibrary) {
            if (fonts.isEmpty()) {
                Text(
                    text = strings.fontEmpty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Space.s200, vertical = Space.s150)
                )
            }
            fonts.forEach { font ->
                val family = noteFontFamily(font.fileName)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Space.s150),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.s200)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = font.displayName,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = family
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${font.fileName} · ${font.sizeBytes / 1024} KB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { pendingDelete = font }) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = strings.delete,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        importLauncher.launch(
                            arrayOf(
                                "font/ttf",
                                "font/otf",
                                "application/x-font-ttf",
                                "application/x-font-opentype",
                                "application/octet-stream"
                            )
                        )
                    }
                    .padding(vertical = Space.s200),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s200)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(Space.s250),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = strings.fontImport,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = strings.fontImportHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        DetailSection(strings.fontDefaultEdit) {
            FontChoiceRow(
                label = strings.systemFont,
                selected = settings.noteFont == FontStore.SYSTEM || settings.noteFont.isBlank(),
                onClick = { onChange(settings.copy(noteFont = FontStore.SYSTEM)) }
            )
            fonts.forEach { font ->
                FontChoiceRow(
                    label = font.displayName,
                    selected = settings.noteFont == font.fileName,
                    onClick = { onChange(settings.copy(noteFont = font.fileName)) }
                )
            }
        }

        DetailSection(strings.fontDefaultReader) {
            FontChoiceRow(
                label = strings.systemFont,
                selected = settings.readerFont == FontStore.SYSTEM || settings.readerFont.isBlank(),
                onClick = { onChange(settings.copy(readerFont = FontStore.SYSTEM)) }
            )
            fonts.forEach { font ->
                FontChoiceRow(
                    label = font.displayName,
                    selected = settings.readerFont == font.fileName,
                    onClick = { onChange(settings.copy(readerFont = font.fileName)) }
                )
            }
        }
    }

    pendingDelete?.let { font ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(strings.fontDeleteTitle) },
            text = { Text(strings.fontDeleteMessage(font.displayName)) },
            confirmButton = {
                TextButton(onClick = {
                    FontStore.delete(context, font.fileName)
                    // 正在用的字型被刪掉就退回系統字型，而不是留一個指不到檔案的值。
                    onChange(
                        settings.copy(
                            noteFont = if (settings.noteFont == font.fileName) {
                                FontStore.SYSTEM
                            } else {
                                settings.noteFont
                            },
                            readerFont = if (settings.readerFont == font.fileName) {
                                FontStore.SYSTEM
                            } else {
                                settings.readerFont
                            }
                        )
                    )
                    fonts = FontStore.list(context)
                    pendingDelete = null
                }) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(strings.cancel) }
            }
        )
    }

    if (failed) {
        AlertDialog(
            onDismissRequest = { failed = false },
            title = { Text(strings.fontImportFailed) },
            confirmButton = {
                TextButton(onClick = { failed = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun FontChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    SettingsRadioRow(selected = selected, onClick = onClick) {
        RadioLabel(label, selected)
    }
}

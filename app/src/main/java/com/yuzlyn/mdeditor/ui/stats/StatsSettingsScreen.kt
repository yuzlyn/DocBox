package com.yuzlyn.mdeditor.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.yuzlyn.mdeditor.data.settings.AppSettings
import com.yuzlyn.mdeditor.ui.i18n.LocalAppStrings
import com.yuzlyn.mdeditor.ui.screen.SegmentedListGroup
import com.yuzlyn.mdeditor.ui.screen.SettingsRow
import com.yuzlyn.mdeditor.ui.theme.Space

/**
 * 統計的開關面板。
 *
 * 「全部關掉」是第一等公民：總開關一關，側欄的統計頁直接消失、也不再寫任何檔案。
 * 下面四個子開關只有在總開關開著時才有意義，關著時全部禁用（不是隱藏）——
 * 讓使用者還看得到自己先前怎麼設定的，重新打開時不必重配。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsSettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    var confirmClear by remember { mutableStateOf(false) }
    val enabled = settings.statsEnabled

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = { Text(strings.statsSettingsTitle) },
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
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.s200, vertical = Space.s200),
            verticalArrangement = Arrangement.spacedBy(Space.s400)
        ) {
            SegmentedListGroup(
                title = strings.statsSettingsTitle,
                rows = listOf(
                    toggleRow(
                        icon = Icons.Default.BarChart,
                        title = strings.statsEnabledLabel,
                        subtitle = strings.statsEnabledSummary,
                        checked = enabled,
                        enabled = true,
                        onCheckedChange = { onChange(settings.copy(statsEnabled = it)) }
                    ),
                    toggleRow(
                        icon = Icons.Default.Schedule,
                        title = strings.statsTrackReading,
                        subtitle = strings.statsTrackReadingSummary,
                        checked = settings.statsTrackReading,
                        enabled = enabled,
                        onCheckedChange = { onChange(settings.copy(statsTrackReading = it)) }
                    ),
                    toggleRow(
                        icon = Icons.Default.EditNote,
                        title = strings.statsTrackDocuments,
                        subtitle = strings.statsTrackDocumentsSummary,
                        checked = settings.statsTrackDocuments,
                        enabled = enabled,
                        onCheckedChange = { onChange(settings.copy(statsTrackDocuments = it)) }
                    ),
                    toggleRow(
                        icon = Icons.Default.DateRange,
                        title = strings.statsShowHeatmap,
                        subtitle = "",
                        checked = settings.statsShowHeatmap,
                        enabled = enabled,
                        onCheckedChange = { onChange(settings.copy(statsShowHeatmap = it)) }
                    ),
                    toggleRow(
                        icon = Icons.Default.MenuBook,
                        title = strings.statsShowRanking,
                        subtitle = "",
                        checked = settings.statsShowRanking,
                        enabled = enabled,
                        onCheckedChange = { onChange(settings.copy(statsShowRanking = it)) }
                    )
                )
            )

            SegmentedListGroup(
                title = strings.statsClearData,
                rows = listOf(
                    SettingsRow(
                        icon = Icons.Default.Delete,
                        title = strings.statsClearData,
                        subtitle = strings.statsClearDataSummary,
                        showChevron = false,
                        onClick = { confirmClear = true }
                    )
                )
            )
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(strings.statsClearData) },
            text = { Text(strings.statsClearDataSummary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClear()
                    }
                ) {
                    Text(strings.statsClearData, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(strings.cancel) }
            }
        )
    }
}

/** 一行開關：整列可點，行為與右邊的 Switch 一致。 */
@Composable
private fun toggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
): SettingsRow = SettingsRow(
    icon = icon,
    title = title,
    subtitle = subtitle,
    showChevron = false,
    trailing = {
        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null
        )
    },
    onClick = { if (enabled) onCheckedChange(!checked) }
)

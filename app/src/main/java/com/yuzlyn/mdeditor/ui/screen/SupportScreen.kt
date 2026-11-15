package com.yuzlyn.mdeditor.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yuzlyn.mdeditor.ui.component.Appear
import com.yuzlyn.mdeditor.ui.component.QrCard
import com.yuzlyn.mdeditor.ui.component.QrZoomDialog
import com.yuzlyn.mdeditor.ui.component.supportCodes
import com.yuzlyn.mdeditor.ui.i18n.LocalAppStrings
import com.yuzlyn.mdeditor.ui.theme.Space

/**
 * 支持作者：獨立一頁，只放兩張收款碼。
 *
 * 原本這兩張碼塞在作者頁底部，頁面又長、碼又只能縮到 300dp；拆出來之後
 * 作者頁只剩「人是誰」，這一頁只剩「怎麼支持」，兩件事不再互相牽扯。
 *
 * 從作者頁那一條點進來（`Author` → `Support`），返回鍵回作者頁。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val strings = LocalAppStrings.current
    val codes = remember(strings) { supportCodes(strings) }
    // -1 = 沒有放大任何一張。
    var zoomed by remember { mutableStateOf(-1) }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = { Text(strings.authorSupportTitle) },
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
                .padding(horizontal = Space.s200, vertical = Space.s300),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Appear(index = 0) {
                    Text(
                        text = strings.authorSupportLead,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(Space.s400))
                Appear(index = 1, stepMillis = 60L) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        // 這一頁只有碼，放寬到 320dp：掃得動比排得好看重要。
                        val qrWidth = minOf(maxWidth * 0.78f, 320.dp)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Space.s500),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            codes.forEachIndexed { index, code ->
                                QrCard(
                                    code = code,
                                    width = qrWidth,
                                    zoomHint = strings.authorSupportZoom,
                                    onClick = { zoomed = index }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Space.s400))
            }
        }
    }

    if (zoomed in codes.indices) {
        QrZoomDialog(code = codes[zoomed], onDismiss = { zoomed = -1 })
    }
}

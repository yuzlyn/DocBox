package com.yuzlyn.mdeditor.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yuzlyn.mdeditor.data.settings.AppLanguage
import com.yuzlyn.mdeditor.ui.component.ConstrainedContent
import com.yuzlyn.mdeditor.ui.i18n.LocalAppStrings
import com.yuzlyn.mdeditor.ui.i18n.PROJECT_REPO_URL
import com.yuzlyn.mdeditor.ui.i18n.helpDoc
import com.yuzlyn.mdeditor.ui.component.Appear
import com.yuzlyn.mdeditor.ui.theme.Space

/**
 * 說明頁：把 README 直接放進 App，依當前語言顯示，頁尾附專案倉庫與作者 GitHub。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    language: AppLanguage,
    onBack: () -> Unit,
    /** 作者那一列改成本地頁面，不再直接跳出去瀏覽器。 */
    onOpenAuthor: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val doc = remember(language) { language.helpDoc() }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = { Text(strings.help, style = MaterialTheme.typography.titleLarge) },
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
        ConstrainedContent(
            maxWidth = 720.dp,
            modifier = Modifier.padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.s200, vertical = Space.s200),
                verticalArrangement = Arrangement.spacedBy(Space.s400)
            ) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s100)) {
                Text(
                    text = strings.appName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = doc.tagline,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = doc.intro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.s100)
                )
            }

            doc.sections.forEachIndexed { index, section ->
                Appear(index = index + 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(Space.s150)) {
                        Text(
                            text = section.heading,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        section.paragraph?.let { paragraph ->
                            Text(
                                text = paragraph,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        section.bullets.forEach { bullet ->
                            Row(horizontalArrangement = Arrangement.spacedBy(Space.s150)) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = bullet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            SegmentedListGroup(
                title = doc.linksTitle,
                rows = listOf(
                    SettingsRow(
                        icon = Icons.Default.Link,
                        title = doc.repoLabel,
                        subtitle = PROJECT_REPO_URL,
                        onClick = { uriHandler.openUri(PROJECT_REPO_URL) }
                    ),
                    SettingsRow(
                        icon = Icons.Default.Person,
                        title = doc.githubLabel,
                        subtitle = strings.authorRole,
                        onClick = onOpenAuthor
                    )
                )
            )

            Text(
                text = listOf(strings.appName, versionName)
                    .filter { it.isNotBlank() }
                    .joinToString(" "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            }
        }
    }
}

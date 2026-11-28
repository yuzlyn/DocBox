package com.yuzlyn.mdeditor.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yuzlyn.mdeditor.ui.component.Appear
import com.yuzlyn.mdeditor.ui.i18n.AUTHOR_EMAIL
import com.yuzlyn.mdeditor.ui.i18n.AUTHOR_GITHUB_URL
import com.yuzlyn.mdeditor.ui.i18n.AUTHOR_GITHUB_USER
import com.yuzlyn.mdeditor.ui.i18n.LocalAppStrings
import com.yuzlyn.mdeditor.ui.i18n.PROJECT_REPO_URL
import com.yuzlyn.mdeditor.ui.theme.Space

/**
 * 作者頁：一個**本地**頁面，不跳去瀏覽器。從側欄的「作者」或說明頁頁尾進來。
 *
 * 頭像：真正的頭像命名成 `author_avatar.png` 放進
 * `app/src/main/res/drawable-nodpi/` 就自動生效——這裡是用資源名稱探測的，
 * 沒有圖就退回主色底的字首圓章。
 *
 * 「支持作者」只是列表裡的一列，樣式與上面的信箱／GitHub 完全一致；
 * 兩張收款碼搬到獨立頁（[SupportScreen]），不再把這一頁撐長。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorScreen(
    onBack: () -> Unit,
    onOpenSupport: () -> Unit
) {
    val strings = LocalAppStrings.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }
    // 頭像檔名由使用者決定：把 author_avatar.png 丟進 drawable-nodpi 就生效，
    // 所以只能反射查 id；只查一次，不隨配置變更重算，也不會拿到過期值。
    @SuppressLint("DiscouragedApi", "LocalContextResourcesRead")
    val avatarId = remember(context) {
        context.resources.getIdentifier("author_avatar", "drawable", context.packageName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = { Text(strings.authorTitle) },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Appear(index = 0) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (avatarId != 0) {
                            Image(
                                painter = painterResource(avatarId),
                                contentDescription = AUTHOR_GITHUB_USER,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(112.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(112.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = AUTHOR_GITHUB_USER.take(1).uppercase(),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Space.s200))
                Appear(index = 1) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = AUTHOR_GITHUB_USER,
                            // emphasized：同字級、更重的字重，用來標記「這是誰」。
                            style = MaterialTheme.typography.headlineMedium
                                .copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.authorRole,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Space.s300))
                Appear(index = 2) {
                    // 簡介做成帶主色豎條的容器：M3 的「以容器聚內容」＋一點色彩層次。
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .height(IntrinsicSize.Min)
                                .padding(Space.s200),
                            horizontalArrangement = Arrangement.spacedBy(Space.s200)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = strings.authorBio,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Space.s300))
                Appear(index = 3) {
                    SegmentedListGroup(
                        title = strings.authorTitle,
                        rows = listOf(
                            SettingsRow(
                                icon = Icons.Default.MailOutline,
                                title = strings.authorEmail,
                                subtitle = AUTHOR_EMAIL,
                                onClick = {
                                    runCatching { uriHandler.openUri("mailto:$AUTHOR_EMAIL") }
                                }
                            ),
                            SettingsRow(
                                icon = Icons.Default.Code,
                                title = strings.authorGithub,
                                subtitle = AUTHOR_GITHUB_URL,
                                onClick = {
                                    runCatching { uriHandler.openUri(AUTHOR_GITHUB_URL) }
                                }
                            ),
                            SettingsRow(
                                icon = Icons.Default.Folder,
                                title = strings.authorRepo,
                                subtitle = PROJECT_REPO_URL,
                                onClick = {
                                    runCatching { uriHandler.openUri(PROJECT_REPO_URL) }
                                }
                            ),
                            // 與上面三列同一款式：一條可點的列，收款碼在下一頁。
                            SettingsRow(
                                icon = Icons.Default.Favorite,
                                title = strings.authorSupportTitle,
                                subtitle = strings.authorSupportEntry,
                                onClick = onOpenSupport
                            )
                        )
                    )
                }

                Spacer(modifier = Modifier.height(Space.s400))
                Appear(index = 4) {
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
                Spacer(modifier = Modifier.height(Space.s200))
            }
        }
    }
}

package com.yuzlyn.mdeditor.ui.screen

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yuzlyn.mdeditor.data.profile.ProfileData
import com.yuzlyn.mdeditor.ui.component.ConstrainedContent
import com.yuzlyn.mdeditor.data.profile.githubHandle
import com.yuzlyn.mdeditor.ui.component.pressRipple
import com.yuzlyn.mdeditor.ui.component.pressScale
import com.yuzlyn.mdeditor.ui.i18n.LocalAppStrings
import com.yuzlyn.mdeditor.ui.theme.Space

/** 讀取本地頭像檔案為 ImageBitmap；路徑為空或解碼失敗時回傳 null。 */
@Composable
fun rememberAvatarBitmap(avatarPath: String): ImageBitmap? = remember(avatarPath) {
    avatarPath.takeIf { it.isNotBlank() }?.let { path ->
        runCatching { BitmapFactory.decodeFile(path) }.getOrNull()?.asImageBitmap()
    }
}

/** 圓形頭像：有頭像顯示圖片，否則顯示 Person 佔位圖示。 */
@Composable
fun Avatar(
    avatarPath: String,
    size: Dp,
    contentDescription: String?,
    onClick: () -> Unit
) {
    val bitmap = rememberAvatarBitmap(avatarPath)
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .size(size)
            // 原生 ripple 不受外層 clip 影響，改用自繪且裁成圓形的波紋。
            // 縮放交給 pressScale：和頂欄／側欄圓鈕共用「最短保持 140ms」的節奏，
            // 快速點一下也看得到；pressRipple 只負責波紋，避免兩套縮放各跑各的。
            .pressScale(interactionSource = interactionSource)
            .pressRipple(
                interactionSource = interactionSource,
                shape = CircleShape,
                animateScale = false
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(size / 2),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/** 個人名片頁：上傳頭像、自訂暱稱與個人資訊（Email、GitHub）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: ProfileData,
    onProfileChange: ((ProfileData) -> ProfileData) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = androidx.compose.ui.platform.LocalContext.current

    var nickname by remember { mutableStateOf(profile.nickname) }
    var email by remember { mutableStateOf(profile.email) }
    var github by remember { mutableStateOf(profile.github) }
    var avatarPath by remember { mutableStateOf(profile.avatarPath) }

    fun persist() {
        onProfileChange {
            it.copy(nickname = nickname, email = email, github = github, avatarPath = avatarPath)
        }
    }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val dest = java.io.File(context.filesDir, "avatar.jpg")
            val copied = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                    true
                } ?: false
            }.getOrDefault(false)
            if (copied) {
                avatarPath = dest.absolutePath
                persist()
            }
        }
    }

    fun launchPicker() {
        pickImage.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = {
                    Text(strings.profileTitle, style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        ConstrainedContent(
            maxWidth = 560.dp,
            modifier = Modifier.padding(innerPadding).imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.s300),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.s200)
            ) {
            Spacer(modifier = Modifier.height(Space.s200))
            Avatar(
                avatarPath = avatarPath,
                size = Space.s900 * 2f,
                contentDescription = strings.avatar,
                onClick = { launchPicker() }
            )
            TextButton(onClick = { launchPicker() }) {
                Text(strings.uploadAvatar)
            }
            ProfileField(
                value = nickname,
                label = strings.nickname,
                onValueChange = { value -> nickname = value; persist() }
            )
            ProfileField(
                value = email,
                label = strings.email,
                onValueChange = { value -> email = value; persist() }
            )
            ProfileField(
                value = github,
                label = strings.github,
                // 使用者只填 yuzlyn，欄位下方就即時顯示名片上會長成什麼樣：
                // 「自動補 github.com」這件事必須看得見，不然他會自己再貼一次完整網址。
                supporting = githubHandle(github)
                    ?.let { handle -> strings.githubPreview(handle) }
                    ?: strings.githubHint,
                onValueChange = { value -> github = value; persist() }
            )
            Spacer(modifier = Modifier.height(Space.s300))
            }
        }
    }
}

@Composable
private fun ProfileField(
    value: String,
    label: String,
    supporting: String? = null,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = supporting?.let { text ->
            { Text(text, style = MaterialTheme.typography.labelSmall) }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

package com.yuzlyn.mdeditor.ui.screen

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.yuzlyn.mdeditor.ui.theme.Space
import com.yuzlyn.mdeditor.ui.i18n.LocalAppStrings
import com.yuzlyn.mdeditor.ui.viewmodel.FileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * PDF 唯讀檢視。
 *
 * 用系統內建的 [PdfRenderer] 逐頁點陣化：不用引入第三方套件，
 * 也不必把整份文件一次載進記憶體——[LazyColumn] 只會渲染可見的幾頁。
 * PDF 無法在 App 內編輯，這裡只提供閱讀。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    uriString: String,
    onBack: () -> Unit,
    /**
     * 需要「匯入為筆記」時才傳；傳 null 就只閱讀，頂欄也不會出現那顆按鈕
     * （從筆記頂欄點進來的場景，再匯入一次只會多出一篇重複的筆記）。
     */
    onImportAsNote: (suspend (Uri) -> FileViewModel.ImportNoteResult)? = null,
    onImported: (String) -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val uri = remember(uriString) { uriString.toUri() }

    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("PDF") }
    var importing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    DisposableEffect(uri) {
        var descriptor: ParcelFileDescriptor? = null
        try {
            descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            if (descriptor == null) {
                error = strings.pdfOpenFailed
            } else {
                val opened = PdfRenderer(descriptor)
                renderer = opened
                pageCount = opened.pageCount
            }
        } catch (e: Exception) {
            error = e.message ?: strings.pdfOpenFailed
        }
        onDispose {
            // PdfRenderer.close() 會一併釋放檔案描述符，再關一次只是保險。
            runCatching { renderer?.close() }
            renderer = null
            runCatching { descriptor?.close() }
        }
    }

    LaunchedEffect(uri) {
        title = runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull()
            ?.substringBeforeLast(".pdf")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "PDF"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    // 把 PDF 的文字層抽出來存成筆記；抽不到時這裡會直接說原因。
                    // 從筆記進來的檢視沒有這個回呼，按鈕就不出現。
                    val importAction = onImportAsNote
                    if (importAction != null) {
                        IconButton(
                            enabled = !importing && error == null,
                            onClick = {
                                scope.launch {
                                    importing = true
                                    when (val result = importAction(uri)) {
                                        is FileViewModel.ImportNoteResult.Success ->
                                            onImported(result.noteId)

                                        is FileViewModel.ImportNoteResult.Failure ->
                                            snackbarHostState.showSnackbar(result.message)
                                    }
                                    importing = false
                                }
                            }
                        ) {
                            if (importing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(Space.s250),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.NoteAdd, contentDescription = strings.pdfImportAsNote)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        // 用局部變數接住：robust smart cast，避免在重組期間 renderer 被清空後 !! 炸掉。
        val activeRenderer = renderer
        when {
            error != null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(Space.s300),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = error ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            activeRenderer == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentPadding = PaddingValues(Space.s150),
                verticalArrangement = Arrangement.spacedBy(Space.s150)
            ) {
                items(count = pageCount) { index ->
                    // 寬螢幕以 960dp 為閱讀上限；量置中後的實際頁寬，
                    // 不再用整個螢幕寬度決定點陣圖尺寸。
                    var targetWidth by remember { mutableIntStateOf(0) }
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 960.dp)
                                .fillMaxWidth()
                                .onSizeChanged { targetWidth = it.width }
                        ) {
                            PdfPage(
                                renderer = activeRenderer,
                                pageIndex = index,
                                targetWidth = targetWidth
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPage(renderer: PdfRenderer, pageIndex: Int, targetWidth: Int) {
    val strings = LocalAppStrings.current
    val bitmap by produceState<Bitmap?>(initialValue = null, renderer, pageIndex, targetWidth) {
        // 首次佈局前還沒有頁寬；量到寬度後 key 改變，會重新啟動這段渲染。
        if (targetWidth > 0) {
            value = withContext(Dispatchers.IO) {
                runCatching { renderPage(renderer, pageIndex, targetWidth) }.getOrNull()
            }
        }
    }

    val image = bitmap
    if (image == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f / 1.414f)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Image(
            bitmap = image.asImageBitmap(),
            contentDescription = strings.pdfPage(pageIndex + 1),
            modifier = Modifier
                .fillMaxWidth()
                .background(ComposeColor.White),
            contentScale = ContentScale.FillWidth
        )
    }
}

/**
 * PdfRenderer 同一時間只能開一頁，而且必須關閉後才能開下一頁；
 * 用 synchronized 序列化，避免 LazyColumn 同時組合多頁時互踩。
 */
private fun renderPage(renderer: PdfRenderer, index: Int, width: Int): Bitmap {
    synchronized(renderer) {
        val page = renderer.openPage(index)
        try {
            val height = (width * page.height.toFloat() / page.width)
                .toInt()
                .coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        } finally {
            page.close()
        }
    }
}

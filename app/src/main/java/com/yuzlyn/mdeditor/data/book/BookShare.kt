package com.yuzlyn.mdeditor.data.book

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

/**
 * 把書庫裡的原檔交給系統分享面板。
 *
 * 直接授權 `filesDir/books` 裡的檔案，不先複製到 cache：一本書動輒幾十 MB，
 * 為了分享再寫一份副本既慢又佔空間，而接收端要的也正是同一個檔案。
 * 授權範圍只有 FileProvider 給出的那幾個 `content://` Uri，整個目錄並不對外。
 */
object BookShare {

    private const val AUTHORITY_SUFFIX = ".fileprovider"

    /** 單一檔案的 `content://` Uri；檔案不在書庫目錄時丟 `IllegalArgumentException`。 */
    fun uriOf(context: Context, file: File): Uri = FileProvider.getUriForFile(
        context,
        context.packageName + AUTHORITY_SUFFIX,
        file
    )

    /** 依副檔名給型別；`MimeTypeMap` 不認得的電子書格式補在這裡。 */
    fun mimeTypeOf(file: File): String {
        val extension = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: when (extension) {
                "epub" -> "application/epub+zip"
                "mobi" -> "application/x-mobipocket-ebook"
                "azw", "azw3" -> "application/vnd.amazon.ebook"
                "fb2" -> "application/x-fictionbook+xml"
                "cbz" -> "application/vnd.comicbook+zip"
                "cbr" -> "application/vnd.comicbook-rar"
                "pdf" -> "application/pdf"
                "txt" -> "text/plain"
                else -> "application/octet-stream"
            }
    }

    /**
     * 組出分享 Intent。
     *
     * 單選給精確型別，聊天軟體才會顯示檔案預覽；多選時各書型別可能不同，
     * 型別放寬成「任意」交給接收端自己判斷。
     *
     * `ClipData` 一定要跟 `EXTRA_STREAM` 一起寫：部分接收端（含舊版微信、
     * 一些檔案管理器）只看 ClipData 判斷有沒有讀取權限，只給 EXTRA_STREAM
     * 會出現「檔案無法開啟」。
     */
    fun intent(context: Context, files: List<File>): Intent {
        require(files.isNotEmpty()) { "BookShare: no files" }
        val uris = files.map { uriOf(context, it) }
        val clip = ClipData.newRawUri(files.first().name, uris.first()).apply {
            uris.drop(1).forEach { addItem(ClipData.Item(it)) }
        }
        val grant = Intent.FLAG_GRANT_READ_URI_PERMISSION

        return if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = mimeTypeOf(files.first())
                putExtra(Intent.EXTRA_STREAM, uris.first())
                clipData = clip
                addFlags(grant)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                clipData = clip
                addFlags(grant)
            }
        }
    }
}

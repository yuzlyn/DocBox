package com.yuzlyn.mdeditor.data.backgrounds

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.Locale

/**
 * 筆記背景圖庫。
 *
 * 舊版只有 `filesDir/note_background.jpg` 一張，選新的就把舊的蓋掉——連「後悔」
 * 的機會都沒有。改成一個資料夾：匯入的圖都留著，列表本身是資料夾的內容，
 * 選哪張只是把路徑記進設定，刪圖也不會動到別人的筆記。
 */
object BackgroundStore {

    private const val DIR = "note_backgrounds"

    fun directory(context: Context): File =
        File(context.filesDir, DIR).apply { if (!exists()) mkdirs() }

    /** 圖庫內容，新的排前面（檔名用時間戳，字串排序即時間排序）。 */
    fun list(context: Context): List<File> =
        directory(context).listFiles()
            ?.filter { it.isFile && it.length() > 0 }
            ?.sortedByDescending { it.name }
            ?: emptyList()

    fun import(context: Context, uri: Uri): String? {
        val resolver = context.contentResolver
        val rawName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        val extension = rawName?.substringAfterLast('.', "")?.lowercase(Locale.ROOT)
            ?.takeIf { it in setOf("jpg", "jpeg", "png", "webp", "heic", "heif") } ?: "jpg"
        val stamp = System.currentTimeMillis()
        var target = File(directory(context), "bg-$stamp.$extension")
        var index = 2
        while (target.exists()) {
            target = File(directory(context), "bg-$stamp-$index.$extension")
            index++
        }
        val ok = runCatching {
            resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
        }.getOrDefault(false)
        if (!ok) {
            target.delete()
            return null
        }
        return target.absolutePath
    }

    fun delete(path: String): Boolean {
        if (path.isBlank()) return false
        val file = File(path)
        if (!file.exists()) return false
        // 只刪自己資料夾裡的檔，避免設定值被人改成別的路徑時亂刪。
        return runCatching { file.delete() }.getOrDefault(false)
    }

    /** 這張圖還在不在（設定值可能指向已被清掉的外部檔案）。 */
    fun exists(path: String): Boolean = path.isNotBlank() && File(path).isFile
}

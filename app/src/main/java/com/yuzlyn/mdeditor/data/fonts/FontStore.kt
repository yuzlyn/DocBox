package com.yuzlyn.mdeditor.data.fonts

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/**
 * 使用者匯入的字型。
 *
 * 為什麼不再用 `assets/fonts`：內建字型只能改程式碼才換得掉，使用者連「換一種字」
 * 都得等一個新版本。改成把字型檔複製到 `filesDir/fonts/`，App 只認檔名——
 * 匯入、刪除、設預設都在執行時完成，不必重新打包。
 *
 * 檔名就是識別碼（存在 `AppSettings.noteFont` / `readerFont` 裡）；
 * 資料夾掃出來的清單就是字型庫，不需要另外維護一份索引，
 * 也就沒有「索引和檔案對不上」這種狀態。
 */
object FontStore {

    /** 系統字型：不算在字型庫裡，用它表示「不套自訂字型」。 */
    const val SYSTEM = "system"

    private const val DIR = "fonts"

    /** 只認這幾種；其餘（woff、ttc 的某些變體）Compose 讀不動，匯入了也是壞的。 */
    private val SUPPORTED = setOf("ttf", "otf")

    data class FontFile(val fileName: String, val displayName: String, val sizeBytes: Long)

    fun directory(context: Context): File =
        File(context.filesDir, DIR).apply { if (!exists()) mkdirs() }

    /** 字型庫（依檔名排序，順序穩定）。 */
    fun list(context: Context): List<FontFile> =
        directory(context).listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in SUPPORTED }
            ?.sortedBy { it.name.lowercase() }
            ?.map { FontFile(it.name, it.name.substringBeforeLast('.'), it.length()) }
            ?: emptyList()

    fun fileOf(context: Context, fileName: String): File? {
        if (fileName.isBlank() || fileName == SYSTEM) return null
        val file = File(directory(context), fileName)
        return file.takeIf { it.isFile }
    }

    /**
     * 複製一份進來，回傳新檔名；失敗回傳 null。
     *
     * 檔名取「顯示名稱去掉副檔名」清洗過的字串，重名時補 `-2`、`-3`——
     * 直接覆蓋掉別的字型是最糟的結果，使用者不會預期「匯入」等於「刪掉舊的」。
     */
    fun import(context: Context, uri: Uri): String? {
        val resolver = context.contentResolver
        val rawName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        val extension = rawName?.substringAfterLast('.', "")?.lowercase()
            ?.takeIf { it in SUPPORTED } ?: "ttf"
        val base = sanitize(rawName?.substringBeforeLast('.') ?: "font")
        val target = uniqueFile(context, base, extension)
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
        // 有些「字型」其實是改了副檔名的別的東西：能讀、卻解析不出字形。
        // 交給 Typeface 試一次，失敗就別留在庫裡——留著只會在列表上露一個方框。
        val usable = runCatching {
            android.graphics.Typeface.createFromFile(target) != null
        }.getOrDefault(false)
        if (!usable) {
            target.delete()
            return null
        }
        return target.name
    }

    fun delete(context: Context, fileName: String): Boolean {
        val file = fileOf(context, fileName) ?: return false
        return file.delete()
    }

    private fun sanitize(name: String): String {
        val cleaned = name
            .replace(Regex("""[\\/:*?"<>|\s]+"""), "-")
            .trim('-', '.')
            .take(48)
        return cleaned.ifBlank { "font" }
    }

    private fun uniqueFile(context: Context, base: String, extension: String): File {
        val dir = directory(context)
        var candidate = File(dir, "$base.$extension")
        var index = 2
        while (candidate.exists()) {
            candidate = File(dir, "$base-$index.$extension")
            index++
        }
        return candidate
    }
}

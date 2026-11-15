package com.yuzlyn.mdeditor.data.book

/**
 * 書內資源在閱讀器 URL 空間裡的位置。
 *
 * 為什麼要把 key 編碼：EPUB 的路徑帶中文、空格、`#`、`?`，AZW3 的 key 是
 * `embed:0001:image/jpeg` 這種帶冒號的字串。直接塞進 URL 會因為各家 WebView
 * 對百分號編碼的處理不一致而 404，改用 base64url 之後字元集只剩 `[A-Za-z0-9_-]`，
 * 沒有編碼歧義。
 *
 * 這裡自己寫 base64 而不是用 `android.util.Base64`：解析層要能在純 JVM 單元測試裡跑，
 * 一碰 Android framework 就得開 Robolectric，不值得。
 */
object ReaderPaths {

    /** WebViewAssetLoader 註冊的虛擬主機。 */
    const val HOST = "appassets.androidplatform.net"

    /** 註冊在該主機下的路徑前綴。 */
    const val PREFIX = "/reader/"

    const val ORIGIN = "https://$HOST"

    fun chapter(index: Int): String = "${PREFIX}chapter/$index"

    fun absoluteResource(key: String): String = "${PREFIX}raw/${encode(key)}"

    /**
     * 使用者匯入的字型。
     *
     * 不放在 `raw/` 底下：那一條走的是「書裡的資源」（ZIP 內部），字型在
     * `filesDir/fonts`，來源不同，分開才不會有人以為要把它塞進 EPUB。
     */
    fun fontFile(fileName: String): String = "${PREFIX}font/${encode(fileName)}"

    fun encode(key: String): String = base64UrlEncode(key.toByteArray(Charsets.UTF_8))

    fun decode(token: String): String? =
        runCatching { String(base64UrlDecode(token), Charsets.UTF_8) }.getOrNull()

    /**
     * 去掉 `/reader/` 前綴。
     *
     * WebView 給的是完整路徑（`/reader/raw/xxx`），單元測試與內部呼叫用相對路徑
     * （`raw/xxx`）；這裡兩種都收，兩邊才不會各說各話。
     */
    private fun relative(path: String): String {
        val trimmed = path.trimStart('/')
        val prefix = PREFIX.trim('/') + "/"
        return if (trimmed.startsWith(prefix)) trimmed.removePrefix(prefix) else trimmed
    }

    /** 從 `reader/raw/<token>` 取出資源 key。 */
    fun resourceKeyFromPath(path: String): String? {
        val trimmed = relative(path)
        if (!trimmed.startsWith("raw/")) return null
        return decode(trimmed.removePrefix("raw/").substringBefore('/'))
    }

    /** 從 `reader/font/<token>` 取出字型檔名。 */
    fun fontNameFromPath(path: String): String? {
        val trimmed = relative(path)
        if (!trimmed.startsWith("font/")) return null
        return decode(trimmed.removePrefix("font/").substringBefore('/'))
    }

    /** 從 `reader/chapter/<n>` 取出章節序號。 */
    fun chapterFromPath(path: String): Int? {
        val trimmed = relative(path)
        if (!trimmed.startsWith("chapter/")) return null
        return trimmed.removePrefix("chapter/").substringBefore('/').toIntOrNull()
    }

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    fun base64UrlEncode(bytes: ByteArray): String {
        val out = StringBuilder((bytes.size + 2) / 3 * 4)
        var index = 0
        while (index < bytes.size) {
            val b0 = bytes[index].toInt() and 0xFF
            val b1 = if (index + 1 < bytes.size) bytes[index + 1].toInt() and 0xFF else -1
            val b2 = if (index + 2 < bytes.size) bytes[index + 2].toInt() and 0xFF else -1
            out.append(ALPHABET[b0 shr 2])
            out.append(ALPHABET[((b0 and 0x03) shl 4) or (if (b1 >= 0) b1 shr 4 else 0)])
            if (b1 >= 0) {
                out.append(ALPHABET[((b1 and 0x0F) shl 2) or (if (b2 >= 0) b2 shr 6 else 0)])
            }
            if (b2 >= 0) {
                out.append(ALPHABET[b2 and 0x3F])
            }
            index += 3
        }
        return out.toString()
    }

    fun base64UrlDecode(text: String): ByteArray {
        val cleaned = text.trimEnd('=')
        val out = java.io.ByteArrayOutputStream(cleaned.length * 3 / 4)
        var buffer = 0
        var bits = 0
        for (char in cleaned) {
            val value = ALPHABET.indexOf(char)
            if (value < 0) continue
            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out.write((buffer shr bits) and 0xFF)
            }
        }
        return out.toByteArray()
    }
}

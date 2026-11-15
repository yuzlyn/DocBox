package com.yuzlyn.mdeditor.data.book

import java.io.Closeable
import java.util.Locale

/** 書的詮釋資料；缺的欄位一律給 null，UI 自己決定怎麼收尾。 */
data class BookMetadata(
    val title: String,
    val author: String? = null,
    val language: String? = null,
    val publisher: String? = null,
    val description: String? = null,
    val format: BookFormat
)

/** 目錄的一項。`chapter` 是線性閱讀序號，不是原始檔案序號。 */
data class TocEntry(
    val title: String,
    val chapter: Int,
    val depth: Int = 0,
    /** 章內錨點（EPUB 的 `#id`、KF8 的 `kindle:pos` 偏移），沒有就是 null。 */
    val anchor: String? = null
)

/** 一章的內容。`basePath` 是這一章在書裡的所在目錄，用來解析相對路徑。 */
class ChapterContent(
    val html: String,
    val basePath: String
)

/** 一包資源（圖片、CSS、字型）。 */
class BookResource(val bytes: ByteArray, val mime: String)

/**
 * 開書失敗的種類。
 *
 * 資料層只負責說「哪一種錯」，句子交給介面按語言組——以前這裡直接塞中文，
 * 切成英文法文時閱讀器就跳出一句中文，看起來像沒翻譯完。
 */
enum class BookError {
    /** 檔名與檔頭都認不出來。 */
    UNKNOWN_FORMAT,
    INVALID_PATH,
    FILE_NOT_FOUND,
    /** 開得了但讀不到，多半是授權失效。 */
    FILE_UNREADABLE,
    FILE_EMPTY,
    FILE_TOO_SMALL,
    NOT_EPUB,
    NOT_MOBI,
    NOT_FB2,
    /** EPUB 的 container.xml 沒指定 OPF。 */
    EPUB_NO_ROOTFILE,
    EPUB_OPF_UNREADABLE,
    /** 解析成功但沒有可讀章節。 */
    NO_CONTENT,
    CHAPTER_NOT_FOUND,
    CHAPTER_UNREADABLE,
    DRM_PROTECTED,
    /** 記錄表、索引、壓縮表彼此對不上，通常是檔案下載不完整或被改過。 */
    CORRUPT_STRUCTURE,
    UNSUPPORTED_COMPRESSION,
    PARSE_FAILED,
    OPEN_FAILED
}

/**
 * 解析或開啟書籍失敗。
 *
 * [detail] 只放語言中立的技術細節（路徑、章節序號、壓縮編號），
 * 介面會把它放在括號裡接在譯文後面；不要往這裡塞句子。
 */
class BookException(
    val error: BookError,
    val detail: String? = null,
    cause: Throwable? = null
) : Exception(
    buildString {
        append(error.name)
        if (detail != null) append(" (").append(detail).append(')')
    },
    cause
)

/**
 * 一本可以讀的書。
 *
 * 設計上刻意不把整本書讀進記憶體：章節與資源都是**按需**取，
 * 一本 200MB 的漫畫 EPUB 也只會佔用當前章節的量。
 *
 * 資源用「key」當識別碼而不是檔案路徑，因為兩種容器的最底層完全不同：
 * EPUB 是 ZIP 內的相對路徑，AZW3 是 `kindle:embed:0001` 這種記錄編號。
 * 呼叫端不該知道這些差異，只要把 [resolveHref] 的結果原樣丟給 [readResource] 就好。
 */
interface Book : Closeable {

    val metadata: BookMetadata

    /** 線性閱讀順序的章節數。 */
    val chapterCount: Int

    val toc: List<TocEntry>

    /** 翻頁方向：`ltr` 或 `rtl`。直排中文書常是 `rtl`。 */
    val readingDirection: String

    /** 取出第 [index] 章的原始標記（EPUB 是 XHTML，MOBI 是 HTML）。 */
    suspend fun readChapter(index: Int): ChapterContent

    /**
     * 把章節內的相對連結解析成資源 key。
     * 解析不出來（外部網址、郵件連結、空的錨點）就回 null。
     */
    fun resolveHref(basePath: String, href: String): String?

    /** 這個 key 是不是另一章的正文？是就回章節序號，用來實現章節之間的跳轉。 */
    fun chapterOfResource(key: String): Int?

    /** 讀一個資源；找不到回 null。 */
    suspend fun readResource(key: String): BookResource?

    /** 封面圖（原始位元組），沒有回 null。 */
    suspend fun cover(): ByteArray?
}

/** XML／HTML 的共用小工具。書裡的標記不保證合法，所以全部走寬容解析。 */
internal object Xml {

    /** 把一個標籤的所有屬性抓成 map；屬性名忽略命名空間前綴。 */
    fun attrs(tag: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        TAG_ATTR.findAll(tag).forEach { match ->
            val name = match.groupValues[1].substringAfter(':').lowercase(Locale.ROOT)
            val value = match.groupValues[2].ifEmpty { match.groupValues[3] }
            result[name] = unescape(value)
        }
        return result
    }

    private val TAG_ATTR = Regex("""([\w:.-]+)\s*=\s*(?:"([^"]*)"|'([^']*)')""")

    fun unescape(text: String): String {
        if (text.indexOf('&') < 0) return text
        val builder = StringBuilder(text.length)
        var index = 0
        while (index < text.length) {
            val amp = text.indexOf('&', index)
            if (amp < 0) {
                builder.append(text, index, text.length)
                break
            }
            builder.append(text, index, amp)
            val end = text.indexOf(';', amp)
            if (end < 0 || end - amp > 10) {
                builder.append('&')
                index = amp + 1
                continue
            }
            val entity = text.substring(amp + 1, end)
            val decoded = decodeEntity(entity)
            if (decoded == null) {
                builder.append(text, amp, end + 1)
            } else {
                builder.append(decoded)
            }
            index = end + 1
        }
        return builder.toString()
    }

    private fun decodeEntity(entity: String): String? = when {
        entity.equals("amp", true) -> "&"
        entity.equals("lt", true) -> "<"
        entity.equals("gt", true) -> ">"
        entity.equals("quot", true) -> "\""
        entity.equals("apos", true) -> "'"
        entity.equals("nbsp", true) -> "\u00A0"
        entity.startsWith("#x", true) || entity.startsWith("#X", true) ->
            entity.drop(2).toIntOrNull(16)?.let(::codePoint)
        entity.startsWith("#") -> entity.drop(1).toIntOrNull()?.let(::codePoint)
        else -> null
    }

    private fun codePoint(value: Int): String? =
        runCatching { String(Character.toChars(value)) }.getOrNull()

    /** 去掉標籤，留下純文字（目錄標題、描述這類場合用）。 */
    fun textOf(html: String): String =
        unescape(html.replace(Regex("""(?s)<[^>]*>"""), " "))
            .replace(Regex("""\s+"""), " ")
            .trim()

    fun attr(tag: String, name: String): String? = attrs(tag)[name.lowercase(Locale.ROOT)]

    /** 讀 XML 宣告或 `<meta charset>` 指定的編碼；認不得回 null。 */
    fun charsetOf(bytes: ByteArray): java.nio.charset.Charset? {
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        ) {
            return Charsets.UTF_8
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return Charsets.UTF_16BE
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return Charsets.UTF_16LE
        }
        val head = String(bytes, 0, minOf(bytes.size, 1024), Charsets.ISO_8859_1)
        val declared = DECL.find(head)?.groupValues?.get(1)
            ?: META.find(head)?.groupValues?.get(1)
        if (declared.isNullOrBlank()) return null
        return runCatching { java.nio.charset.Charset.forName(declared.trim()) }.getOrNull()
    }

    private val DECL = Regex("""(?i)encoding\s*=\s*["']([\w.:-]+)["']""")
    private val META =
        Regex("""(?i)<meta[^>]+charset\s*=\s*["']?([\w.:-]+)""")

    /** 位元組 → 字串：先看宣告，再看 UTF-8 是否解得開，最後退回 GBK。 */
    fun decode(bytes: ByteArray): String {
        charsetOf(bytes)?.let { charset ->
            return runCatching { String(bytes, charset) }.getOrDefault("")
                .ifBlank { String(bytes, Charsets.UTF_8) }
        }
        val utf8 = String(bytes, Charsets.UTF_8)
        if (!utf8.contains('\uFFFD')) return utf8
        return runCatching {
            String(bytes, java.nio.charset.Charset.forName("GBK"))
        }.getOrDefault(utf8)
    }
}

/** 讓 `#fragment` 這種純錨點保持原樣：交給 WebView 自己捲動，不必繞一圈。 */
internal fun isPureAnchor(href: String): Boolean =
    href.isEmpty() || href.startsWith("#")

/** 外部連結（http/https/mailto）不進閱讀器，交給系統處理。 */
internal fun isExternal(href: String): Boolean =
    href.startsWith("http://", true) ||
            href.startsWith("https://", true) ||
            href.startsWith("mailto:", true) ||
            href.startsWith("tel:", true)

/** 依副檔名給 MIME；書裡的 manifest 常常不寫，猜錯會讓圖片顯示不出來。 */
internal fun mimeOfPath(path: String): String = when (path.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "gif" -> "image/gif"
    "webp" -> "image/webp"
    "bmp" -> "image/bmp"
    "svg" -> "image/svg+xml"
    "css" -> "text/css"
    "xhtml", "html", "htm" -> "application/xhtml+xml"
    "ncx" -> "application/x-dtbncx+xml"
    "opf" -> "application/oebps-package+xml"
    "ttf" -> "font/ttf"
    "otf" -> "font/otf"
    "woff" -> "font/woff"
    "woff2" -> "font/woff2"
    "mp3" -> "audio/mpeg"
    "mp4" -> "video/mp4"
    "js" -> "text/javascript"
    "xml" -> "application/xml"
    else -> "application/octet-stream"
}

/** 用檔頭補 MIME：AZW3 的資源沒有附檔名，只能看魔數。 */
internal fun sniffMime(bytes: ByteArray): String? {
    if (bytes.size < 4) return null
    fun at(index: Int) = bytes[index].toInt() and 0xFF
    return when {
        at(0) == 0xFF && at(1) == 0xD8 -> "image/jpeg"
        at(0) == 0x89 && at(1) == 0x50 && at(2) == 0x4E && at(3) == 0x47 -> "image/png"
        at(0) == 0x47 && at(1) == 0x49 && at(2) == 0x46 -> "image/gif"
        at(0) == 'B'.code && at(1) == 'M'.code -> "image/bmp"
        at(0) == 'R'.code && at(1) == 'I'.code && at(2) == 'F'.code && at(3) == 'F'.code ->
            if (bytes.size > 11 && at(8) == 'W'.code) "image/webp" else null
        at(0) == 0x00 && at(1) == 0x01 && at(2) == 0x00 && at(3) == 0x00 -> "font/ttf"
        at(0) == 'O'.code && at(1) == 'T'.code && at(2) == 'T'.code && at(3) == 'O'.code -> "font/otf"
        at(0) == 'w'.code && at(1) == 'O'.code && at(2) == 'F'.code -> "font/woff"
        at(0) == '<'.code -> "image/svg+xml"
        else -> null
    }
}
